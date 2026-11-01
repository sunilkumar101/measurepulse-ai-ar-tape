package com.measurepulse.ar.ui.screens.rangefinder

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.SnapshotSaver
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.data.sensors.CameraIntrinsics
import com.measurepulse.ar.data.sensors.MotionSensors
import com.measurepulse.ar.data.sensors.Orientation
import com.measurepulse.ar.data.vision.ObjectBoxAnalyzer
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.domain.rangefinder.RangeResult
import com.measurepulse.ar.domain.rangefinder.ReferencePreset
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

enum class RangeMode(val label: String) { AUTO_HEIGHT("AI Auto-Height"), TWO_ANGLE("2-Angle Trig") }

/** Where the user is in the two-step flow of each mode. */
enum class RangeStep { REFERENCE, TARGET, BASE, PEAK, DONE }

/** Mode A: reference locked → distance + the ground elevation at the reference. */
data class LockedReference(val preset: ReferencePreset, val distanceM: Double, val baseDeg: Double, val uncertaintyM: Double)

@HiltViewModel
class RangefinderViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val repo: RoomScanRepository,
    private val snapshots: SnapshotSaver,
) : ViewModel() {

    val intrinsics = CameraIntrinsics.read(context)
    val analyzer = ObjectBoxAnalyzer()

    val orientation: StateFlow<Orientation?> = MotionSensors.orientation(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), null)

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    private val _mode = MutableStateFlow(RangeMode.AUTO_HEIGHT)
    val mode = _mode.asStateFlow()
    private val _step = MutableStateFlow(RangeStep.REFERENCE)
    val step = _step.asStateFlow()
    private val _preset = MutableStateFlow(ReferencePreset.PERSON)
    val preset = _preset.asStateFlow()
    private val _zoom = MutableStateFlow(1f)
    val zoom = _zoom.asStateFlow()
    private val _autoTrack = MutableStateFlow(true)
    val autoTrack = _autoTrack.asStateFlow()

    private val _reference = MutableStateFlow<LockedReference?>(null)
    val reference = _reference.asStateFlow()
    private val _baseAngle = MutableStateFlow<Double?>(null)
    val baseAngle = _baseAngle.asStateFlow()
    private val _final = MutableStateFlow<RangeResult?>(null)
    val final = _final.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setMode(m: RangeMode) {
        _mode.value = m
        reset()
    }

    fun setPreset(p: ReferencePreset) { _preset.value = p; if (_step.value != RangeStep.REFERENCE) reset() }
    fun setAutoTrack(on: Boolean) { _autoTrack.value = on }

    fun cycleZoom() {
        val steps = listOf(1f, 2f, 4f).filter { it <= intrinsics.maxZoom + 0.01f }
        _zoom.value = steps[(steps.indexOf(_zoom.value) + 1).mod(steps.size)]
    }

    fun reset() {
        _reference.value = null
        _baseAngle.value = null
        _final.value = null
        _step.value = if (_mode.value == RangeMode.AUTO_HEIGHT) RangeStep.REFERENCE else RangeStep.BASE
    }

    // ---- Mode A ----
    fun lockReference(live: RangeResult?, baseDeg: Double) {
        if (live == null) { send("Fit the brackets to the top and bottom of the ${_preset.value.label.lowercase()}"); return }
        _reference.value = LockedReference(_preset.value, live.distanceM, baseDeg, live.uncertaintyM)
        _step.value = RangeStep.TARGET
        send("Reference locked — now aim the crosshair at the top of the target")
    }

    // ---- Mode B ----
    fun lockBase(elevationDeg: Double) {
        if (elevationDeg > -0.5) { send("Aim below the horizon at the base of the building or tree"); return }
        _baseAngle.value = elevationDeg
        _step.value = RangeStep.PEAK
    }

    /** Locks the final result and saves it as a project (with a snapshot when available). */
    fun lockAndSave(result: RangeResult?, snapshot: Bitmap?) {
        if (result?.heightM == null) { send("Aim the crosshair at the top of the target first"); return }
        _final.value = result
        _step.value = RangeStep.DONE
        val sys = prefs.value.unitSystem
        viewModelScope.launch {
            runCatching {
                val image = snapshot?.let { snapshots.save(it).privateFile.absolutePath } ?: "bg_villa_cliff"
                val h = DimensionFormatter.formatLength(result.heightM, sys, FractionPrecision.EIGHTH)
                val d = DimensionFormatter.formatLength(result.distanceM, sys, FractionPrecision.EIGHTH)
                repo.saveToolReading(
                    title = "Height $h",
                    category = ScanCategory.RANGEFINDER,
                    detail = "Height $h • $d away",
                    qualityLabel = "Rangefinder ±" + DimensionFormatter.formatLength(result.uncertaintyM, UnitSystem.METRIC),
                    imageKey = image,
                    widthM = result.distanceM,
                    heightM = result.heightM,
                )
                prefsRepo.setLastMeasurement(result.heightM, "Height")
            }.onSuccess { send("Saved to Projects") }.onFailure { send("Couldn't save: ${it.message}") }
        }
    }

    private fun send(text: String) { viewModelScope.launch { _messages.send(text) } }

    override fun onCleared() { analyzer.close() }
}
