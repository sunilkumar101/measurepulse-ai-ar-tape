package com.measurepulse.ar.ui.screens.blueprint

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.export.BlueprintPdfExporter
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.toFloorPlan
import com.measurepulse.ar.data.monetization.ExportGate
import com.measurepulse.ar.data.monetization.ExportKind
import com.measurepulse.ar.domain.floorplan.FloorPlan
import com.measurepulse.ar.domain.floorplan.FloorPlanCodec
import com.measurepulse.ar.domain.floorplan.Opening
import com.measurepulse.ar.domain.floorplan.OpeningType
import com.measurepulse.ar.domain.floorplan.PlanPoint
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BlueprintTab(val label: String) { DETAILS("Details"), CAD("2D CAD Plan"), AI_FIT("AI Fit Check") }

sealed interface CadSelection {
    data object None : CadSelection
    data class Corner(val index: Int) : CadSelection
    data class Wall(val index: Int) : CadSelection
}

sealed interface BlueprintEvent {
    data class Share(val intent: Intent) : BlueprintEvent
    data class Message(val text: String) : BlueprintEvent
}

@HiltViewModel
class RoomBlueprintViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: RoomScanRepository,
    prefsRepo: UserPreferencesRepository,
    private val exporter: BlueprintPdfExporter,
    private val exportGate: ExportGate,
) : ViewModel() {

    private val scanId: Long = savedState.get<Long>("scanId") ?: -1L

    private val _scan = MutableStateFlow<RoomScanEntity?>(null)
    val scan = _scan.asStateFlow()
    private val _notFound = MutableStateFlow(false)
    val notFound = _notFound.asStateFlow()

    private val _plan = MutableStateFlow<FloorPlan?>(null)
    val plan: StateFlow<FloorPlan?> = _plan.asStateFlow()

    private val _tab = MutableStateFlow(BlueprintTab.DETAILS)
    val tab = _tab.asStateFlow()
    private val _selection = MutableStateFlow<CadSelection>(CadSelection.None)
    val selection = _selection.asStateFlow()
    private val _exporting = MutableStateFlow(false)
    val exporting = _exporting.asStateFlow()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    private val _events = Channel<BlueprintEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val history = ArrayDeque<FloorPlan>()
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observeScan(scanId).collect { entity ->
                if (entity == null) { _notFound.value = true; return@collect }
                _scan.value = entity
                if (_plan.value == null) _plan.value = planFrom(entity)
            }
        }
    }

    private fun planFrom(e: RoomScanEntity): FloorPlan = e.toFloorPlan()

    // ---------- Tabs / selection ----------

    fun setTab(t: BlueprintTab) { _tab.value = t }
    fun select(s: CadSelection) { _selection.value = s }

    // ---------- Editing ----------

    private fun edit(pushHistory: Boolean = true, block: (FloorPlan) -> FloorPlan) {
        val current = _plan.value ?: return
        val next = block(current)
        if (next == current) return
        if (pushHistory) {
            history.addLast(current)
            while (history.size > 40) history.removeFirst()
            _canUndo.value = true
        }
        _plan.value = next
        // Keep the selection valid after structural edits.
        when (val s = _selection.value) {
            is CadSelection.Corner -> if (s.index >= next.corners.size) _selection.value = CadSelection.None
            is CadSelection.Wall -> if (s.index >= next.corners.size) _selection.value = CadSelection.None
            CadSelection.None -> Unit
        }
        scheduleSave(next)
    }

    fun beginCornerDrag(index: Int) {
        _plan.value?.let { history.addLast(it); _canUndo.value = true }
        _selection.value = CadSelection.Corner(index)
    }

    fun dragCorner(index: Int, to: PlanPoint) = edit(pushHistory = false) { it.moveCorner(index, to, snap = true) }

    fun setWallLength(index: Int, meters: Double) = edit { it.setWallLength(index, meters) }
    fun squareUp() = edit { it.squareUp() }
    fun setCeiling(meters: Double) = edit { it.withCeiling(meters) }
    fun removeOpening(o: Opening) = edit { it.removeOpening(o) }

    fun addOpening(type: OpeningType) {
        val wall = (_selection.value as? CadSelection.Wall)?.index
        if (wall == null) { message("Tap a wall first, then add a ${if (type == OpeningType.DOOR) "door" else "window"}"); return }
        edit { it.addOpening(wall, type) }
    }

    fun splitSelectedWall() {
        val wall = (_selection.value as? CadSelection.Wall)?.index
        if (wall == null) { message("Tap a wall to add a corner in its middle"); return }
        edit { it.splitWall(wall) }
        _selection.value = CadSelection.Corner(wall + 1)
    }

    fun deleteSelectedCorner() {
        val corner = (_selection.value as? CadSelection.Corner)?.index
        val plan = _plan.value ?: return
        when {
            corner == null -> message("Tap a corner to delete it")
            plan.corners.size <= 3 -> message("A room needs at least 3 corners")
            else -> { edit { it.deleteCorner(corner) }; _selection.value = CadSelection.None }
        }
    }

    fun undo() {
        val prev = history.removeLastOrNull() ?: return
        _plan.value = prev
        _canUndo.value = history.isNotEmpty()
        _selection.value = CadSelection.None
        scheduleSave(prev)
    }

    private fun scheduleSave(plan: FloorPlan) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            _scan.value?.let { repo.savePlan(it, plan) }
        }
    }

    fun toggleFavorite() = viewModelScope.launch { repo.toggleFavorite(scanId) }

    // ---------- Export ----------

    fun exportPdf() {
        val plan = _plan.value ?: return
        val scan = _scan.value ?: return
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            try {
                if (!exportGate.requestPdfExport(ExportKind.BLUEPRINT)) return@launch
                val file = exporter.export(plan, BlueprintPdfExporter.Meta(scan.title, scan.qualityLabel, scan.createdAt))
                _events.send(BlueprintEvent.Share(exporter.shareIntent(file)))
            } catch (t: Throwable) {
                message("PDF export failed: ${t.message}")
            } finally {
                _exporting.value = false
            }
        }
    }

    private fun message(text: String) { viewModelScope.launch { _events.send(BlueprintEvent.Message(text)) } }
}
