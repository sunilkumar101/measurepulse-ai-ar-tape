package com.measurepulse.ar.ui.screens.advisor

import android.content.Context
import com.measurepulse.ar.ads.AdMobManager
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.export.ExportLibrary
import com.measurepulse.ar.data.export.MaterialListPdfExporter
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.isRoom
import com.measurepulse.ar.data.local.toFloorPlan
import com.measurepulse.ar.data.monetization.ExportGate
import com.measurepulse.ar.data.monetization.ExportKind
import com.measurepulse.ar.domain.advisor.AdvisorInput
import com.measurepulse.ar.domain.advisor.RoomAdvisor
import com.measurepulse.ar.domain.floorplan.OpeningType
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

enum class AdvisorTab(val label: String) { ALL("All"), TILES("Tiles"), PAINT("Paint"), APPLIANCES("AC & TV") }

sealed interface AdvisorEvent {
    data class Share(val intent: Intent) : AdvisorEvent
    data class Message(val text: String) : AdvisorEvent
}

@HiltViewModel
class AiRoomAdvisorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repo: RoomScanRepository,
    prefsRepo: UserPreferencesRepository,
    private val exporter: MaterialListPdfExporter,
    private val gate: ExportGate,
    private val ads: AdMobManager,
) : ViewModel() {

    private val requestedId: Long = savedState.get<Long>("scanId") ?: -1L

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly, UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    /** Rooms the advisor can analyse (floorplans + AR tape rooms). */
    val rooms: StateFlow<List<RoomScanEntity>> = repo.observeScans().map { all -> all.filter { it.isRoom } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selected = MutableStateFlow<RoomScanEntity?>(null)
    val selected = _selected.asStateFlow()

    private val _input = MutableStateFlow<AdvisorInput?>(null)
    val input = _input.asStateFlow()

    private val _tab = MutableStateFlow(AdvisorTab.ALL)
    val tab = _tab.asStateFlow()
    private val _exporting = MutableStateFlow(false)
    val exporting = _exporting.asStateFlow()

    private val _events = Channel<AdvisorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Custom room footprint (when no scan is selected).
    private var customW = 4.0
    private var customL = 5.0

    init {
        viewModelScope.launch {
            val start = (if (requestedId > 0) repo.byId(requestedId) else null) ?: repo.latestRoom()
            if (start != null) selectRoom(start) else useCustom()
        }
    }

    fun setTab(t: AdvisorTab) { _tab.value = t }

    fun selectRoom(scan: RoomScanEntity) {
        val plan = scan.toFloorPlan()
        val prev = _input.value
        _selected.value = scan
        _input.value = AdvisorInput(
            roomName = scan.title,
            floorAreaSqM = plan.areaSqM,
            perimeterM = plan.perimeterM,
            ceilingM = plan.ceilingM,
            doors = plan.openings.count { it.type == OpeningType.DOOR },
            windows = plan.openings.count { it.type == OpeningType.WINDOW },
            openingsAreaSqM = plan.openingsAreaSqM.takeIf { plan.openings.isNotEmpty() },
            viewingDistanceM = (min(plan.widthM, plan.depthM) * 0.75).coerceIn(1.0, 8.0),
            sun = prev?.sun ?: com.measurepulse.ar.domain.advisor.SunExposure.NORMAL,
            occupants = prev?.occupants ?: 2,
            tileCm = prev?.tileCm ?: 60,
            tileBoxPrice = prev?.tileBoxPrice ?: 28.0,
            paintLitrePrice = prev?.paintLitrePrice ?: 9.0,
        )
    }

    fun useCustom() {
        val prev = _input.value
        _selected.value = null
        _input.value = (prev ?: AdvisorInput("Custom room", 0.0, 0.0, 2.6, 1, 1, viewingDistanceM = 3.0)).copy(
            roomName = "Custom room", openingsAreaSqM = null,
            floorAreaSqM = customW * customL, perimeterM = 2 * (customW + customL),
        )
    }

    fun setCustomSize(w: Double, l: Double) {
        customW = w.coerceIn(1.0, 30.0); customL = l.coerceIn(1.0, 30.0)
        update { it.copy(floorAreaSqM = customW * customL, perimeterM = 2 * (customW + customL)) }
    }

    val customWidth get() = customW
    val customLength get() = customL

    /** Any manual change to doors/windows drops the plan's exact opening area in favour of standard sizes. */
    fun update(block: (AdvisorInput) -> AdvisorInput) {
        _input.value?.let { _input.value = block(it) }
    }

    fun exportPdf() {
        val i = _input.value ?: return
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            try {
                if (!gate.requestPdfExport(ExportKind.SHOPPING_LIST)) return@launch
                val file = exporter.export(RoomAdvisor.analyze(i))
                // The audit is saved to Projects › Material PDFs. Interstitial only if the 180 s cooldown allows —
                // it never follows a rewarded video directly.
                ads.showInterstitialIfEligible("ai_audit_saved")
                _events.send(AdvisorEvent.Share(ExportLibrary.shareIntent(context, file, "Share shopping list")))
            } catch (t: Throwable) {
                _events.send(AdvisorEvent.Message("PDF export failed: ${t.message}"))
            } finally {
                _exporting.value = false
            }
        }
    }
}
