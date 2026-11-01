package com.measurepulse.ar.ui.screens.artape

import android.content.Context
import com.measurepulse.ar.ads.AdMobManager
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.ArSupport
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.DeviceCapabilities
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.SnapshotSaver
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.ar.BoxItem
import com.measurepulse.ar.data.ar.EngineKind
import com.measurepulse.ar.data.ar.HardwareKeyRouter
import com.measurepulse.ar.data.ar.MeasureMode
import com.measurepulse.ar.data.ar.MeasurementSession
import com.measurepulse.ar.data.ar.SpatialMath
import com.measurepulse.ar.data.ar.SpatialMeasureController
import com.measurepulse.ar.data.ar.SpatialMeasureController.LockResult
import com.measurepulse.ar.data.ar.VoiceMarkTrigger
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LockSource { BUTTON, VOLUME_KEY, VOICE }

sealed interface ArTapeEvent {
    data class Message(val text: String) : ArTapeEvent
    data class PointLocked(val source: LockSource) : ArTapeEvent
    data class OpenBlueprint(val scanId: Long) : ArTapeEvent
}

@HiltViewModel
class ArTapeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val scans: RoomScanRepository,
    private val snapshots: SnapshotSaver,
    capabilities: DeviceCapabilities,
    val keyRouter: HardwareKeyRouter,
    private val ads: AdMobManager,
) : ViewModel() {

    val controller = SpatialMeasureController(viewModelScope)
    val session = controller.session
    val frame = controller.frame

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    private val _arSupport = MutableStateFlow(ArSupport.CHECKING)
    val arSupport = _arSupport.asStateFlow()

    /** null while ARCore availability is being checked. */
    private val _engine = MutableStateFlow<EngineKind?>(null)
    val engine = _engine.asStateFlow()

    private val _voiceOn = MutableStateFlow(false)
    val voiceOn = _voiceOn.asStateFlow()

    private val voice = VoiceMarkTrigger(appContext) { lockPoint(LockSource.VOICE) }
    val voiceStrategy = voice.strategy
    val voiceLevel = voice.level

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _events = Channel<ArTapeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val support = capabilities.arSupport()
            _arSupport.value = support
            _engine.value = if (support == ArSupport.SUPPORTED) EngineKind.ARCORE else EngineKind.SENSOR_FLOOR
        }
    }

    // ---------- Engines ----------

    fun onArCoreFailed() {
        if (_engine.value == EngineKind.ARCORE) {
            _engine.value = EngineKind.SENSOR_FLOOR
            send(ArTapeEvent.Message("ARCore unavailable — switched to Gyro mode"))
        }
    }

    fun toggleEngine() {
        if (_arSupport.value != ArSupport.SUPPORTED) return
        _engine.value = if (_engine.value == EngineKind.ARCORE) EngineKind.SENSOR_FLOOR else EngineKind.ARCORE
    }

    fun setCameraHeight(meters: Double) = viewModelScope.launch { prefsRepo.setCameraHeight(meters) }

    fun setUnitSystem(system: UnitSystem) = viewModelScope.launch { prefsRepo.setUnitSystem(system) }

    // ---------- Measuring ----------

    fun lockPoint(source: LockSource) {
        when (controller.lockPoint()) {
            is LockResult.Locked -> {
                send(ArTapeEvent.PointLocked(source))
                // A completed Single Line becomes the "last measurement" shown on the converter widget.
                val s = session.value
                if (s.mode == MeasureMode.SINGLE_LINE && s.points.size % 2 == 0) {
                    s.segments(null, 0.0).lastOrNull()?.let { seg ->
                        viewModelScope.launch { prefsRepo.setLastMeasurement(seg.lengthM, "AR Tape") }
                    }
                }
            }
            LockResult.NoSurface -> send(ArTapeEvent.Message("No surface under the reticle — aim at the floor or a wall"))
            LockResult.NotAllowed -> send(ArTapeEvent.Message(
                if (session.value.mode == MeasureMode.ROOM_CHAIN) "Room is closed — undo to add corners" else "Done — tap Clear to measure again"
            ))
        }
    }

    fun undo() = controller.undo()
    fun clear() = controller.clear()
    fun setMode(mode: MeasureMode) = controller.setMode(mode)
    fun setBoxItem(item: BoxItem) = controller.setBoxItem(item)

    /** Closes the room polygon, saves it as a floorplan project and opens Screen 4. */
    fun closeRoomAndSave(snapshot: Bitmap?) {
        val s = session.value
        if (!s.canCloseRoom && !s.roomClosed) return
        val f = frame.value ?: return
        val up = f.up
        if (SpatialMath.polygonArea(s.points.map { it.position }, up) < MeasurementSession.MIN_ROOM_AREA_SQM) {
            send(ArTapeEvent.Message("Room is too small — place corners further apart"))
            return
        }
        controller.closeRoom()
        val pts = session.value.points
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val image = snapshot?.let { snapshots.save(it).privateFile.absolutePath }
                val plan = SpatialMath.toFloorPlan(pts.map { it.position }, up)
                val area = SpatialMath.polygonArea(pts.map { it.position }, up)
                val accuracy = pts.maxOf { it.accuracyM }
                val id = scans.saveRoomChain(
                    floorPlan = plan, areaSqM = area, accuracyM = accuracy,
                    engineLabel = if (f.kind == EngineKind.ARCORE) "AR Chain" else "Gyro Chain",
                    imageKey = image,
                )
                // Natural break after a completed multi-wall room: frequency-capped interstitial (free users only).
                ads.showInterstitialIfEligible("room_blueprint_saved")
                _events.send(ArTapeEvent.OpenBlueprint(id))
            }.onFailure { send(ArTapeEvent.Message("Couldn't save the room: ${it.message}")) }
            _busy.value = false
        }
    }

    fun saveSnapshot(bitmap: Bitmap?) {
        if (bitmap == null) { send(ArTapeEvent.Message("Camera image not ready yet")); return }
        viewModelScope.launch {
            _busy.value = true
            runCatching { snapshots.save(bitmap) }
                .onSuccess { send(ArTapeEvent.Message(if (it.galleryUri != null) "Saved to Pictures/MeasurePulse" else "Snapshot saved")) }
                .onFailure { send(ArTapeEvent.Message("Snapshot failed: ${it.message}")) }
            _busy.value = false
        }
    }

    // ---------- Voice ----------

    fun setVoiceEnabled(enabled: Boolean) {
        _voiceOn.value = enabled
        if (enabled) voice.start() else voice.stop()
    }

    /** Pause the mic while the screen is in the background; resume if the user had it on. */
    fun onScreenVisible(visible: Boolean) {
        if (!_voiceOn.value) return
        if (visible) voice.start() else voice.stop()
    }

    private fun send(e: ArTapeEvent) { viewModelScope.launch { _events.send(e) } }

    override fun onCleared() {
        voice.stop()
    }
}
