package com.measurepulse.ar.ui.screens.projects

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.export.BlueprintPdfExporter
import com.measurepulse.ar.data.export.ExportLibrary
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.data.local.toFloorPlan
import com.measurepulse.ar.data.monetization.ExportGate
import com.measurepulse.ar.data.monetization.ExportKind
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class ProjectFilter(val label: String, val categories: Set<ScanCategory>?) {
    ALL("All Scans", null),
    FLOORPLANS("Floorplans", setOf(ScanCategory.FLOORPLAN)),
    FIT_CHECKS("AR Fit Checks", setOf(ScanCategory.AR_TAPE)),
    TOOLS("Tool Readings", setOf(ScanCategory.LEVEL, ScanCategory.CALIPER, ScanCategory.RANGEFINDER)),
    PDFS("Material PDFs", emptySet()),
}

sealed interface ProjectsEvent {
    data class Launch(val intent: Intent) : ProjectsEvent
    data class Message(val text: String) : ProjectsEvent
}

@HiltViewModel
class SavedProjectsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: RoomScanRepository,
    prefsRepo: UserPreferencesRepository,
    private val blueprints: BlueprintPdfExporter,
    private val gate: ExportGate,
) : ViewModel() {

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly, UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    private val _filter = MutableStateFlow(ProjectFilter.ALL)
    val filter = _filter.asStateFlow()
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _pdfs = MutableStateFlow<List<File>>(emptyList())
    val pdfs = _pdfs.asStateFlow()
    private val _busyId = MutableStateFlow<Long?>(null)
    val busyId = _busyId.asStateFlow()

    val allCount: StateFlow<Int> = repo.observeScans().map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val scans: StateFlow<List<RoomScanEntity>> = combine(repo.observeScans(), _filter, _query) { all, f, q ->
        all.filter { s ->
            (f.categories == null || s.category in f.categories) &&
                (q.isBlank() || s.title.contains(q, ignoreCase = true) || (s.detail?.contains(q, ignoreCase = true) == true))
        }.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _events = Channel<ProjectsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refreshPdfs() }

    fun setFilter(f: ProjectFilter) { _filter.value = f; if (f == ProjectFilter.PDFS) refreshPdfs() }
    fun setQuery(q: String) { _query.value = q }

    fun refreshPdfs() = viewModelScope.launch {
        _pdfs.value = withContext(Dispatchers.IO) { ExportLibrary.list(context) }
    }

    /** 1-tap export of a project's scaled blueprint PDF. */
    fun exportBlueprint(scan: RoomScanEntity) {
        if (_busyId.value != null) return
        viewModelScope.launch {
            _busyId.value = scan.id
            try {
                if (!gate.requestPdfExport(ExportKind.BLUEPRINT)) return@launch
                val file = blueprints.export(scan.toFloorPlan(), BlueprintPdfExporter.Meta(scan.title, scan.qualityLabel, scan.createdAt))
                refreshPdfs()
                _events.send(ProjectsEvent.Launch(blueprints.shareIntent(file)))
            } catch (t: Throwable) {
                _events.send(ProjectsEvent.Message("Export failed: ${t.message}"))
            } finally {
                _busyId.value = null
            }
        }
    }

    fun delete(scan: RoomScanEntity) = viewModelScope.launch {
        repo.delete(scan.id)
        _events.send(ProjectsEvent.Message("Deleted “${scan.title}”"))
    }

    fun sharePdf(file: File) = viewModelScope.launch { _events.send(ProjectsEvent.Launch(ExportLibrary.shareIntent(context, file))) }
    fun openPdf(file: File) = viewModelScope.launch { _events.send(ProjectsEvent.Launch(ExportLibrary.openIntent(context, file))) }

    fun deletePdf(file: File) = viewModelScope.launch {
        withContext(Dispatchers.IO) { file.delete() }
        refreshPdfs()
    }
}
