package com.measurepulse.ar.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.ArSupport
import com.measurepulse.ar.data.DeviceCapabilities
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeFilter(val label: String, val categories: Set<ScanCategory>?) {
    ALL("All", null),
    AR_TAPE("3D AR Tape", setOf(ScanCategory.AR_TAPE)),
    FLOORPLANS("Floorplans", setOf(ScanCategory.FLOORPLAN)),
    LEVEL_CALIPER("Level & Caliper", setOf(ScanCategory.LEVEL, ScanCategory.CALIPER)),
    RANGEFINDER("Rangefinder", setOf(ScanCategory.RANGEFINDER)),
}

data class HomeUiState(
    val loading: Boolean = true,
    val featured: RoomScanEntity? = null,
    val recent: List<RoomScanEntity> = emptyList(),
    val totalCount: Int = 0,
    val filter: HomeFilter = HomeFilter.ALL,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val precision: FractionPrecision = FractionPrecision.SIXTEENTH,
    val arSupport: ArSupport = ArSupport.CHECKING,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scans: RoomScanRepository,
    private val prefs: UserPreferencesRepository,
    capabilities: DeviceCapabilities,
) : ViewModel() {

    private val filter = MutableStateFlow(HomeFilter.ALL)
    private val arSupport = MutableStateFlow(ArSupport.CHECKING)

    init {
        viewModelScope.launch { arSupport.value = capabilities.arSupport() }
    }

    // Seed first, then observe, so the very first frame never shows an empty "0 Ready" state.
    private val seededScans = flow {
        scans.seedIfEmpty()
        emitAll(scans.observeScans())
    }

    val state = combine(seededScans, prefs.preferences, filter, arSupport) { all, p, f, ar ->
        val visible = f.categories?.let { cats -> all.filter { it.category in cats } } ?: all
        val featured = visible.firstOrNull { it.isFeatured } ?: visible.firstOrNull()
        HomeUiState(
            loading = false,
            featured = featured,
            recent = visible.filter { it.id != featured?.id },
            totalCount = all.size,
            filter = f,
            unitSystem = p.unitSystem,
            precision = p.fractionPrecision,
            arSupport = ar,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setFilter(f: HomeFilter) { filter.value = f }

    fun toggleFavorite(id: Long) = viewModelScope.launch { scans.toggleFavorite(id) }

    fun setUnitSystem(system: UnitSystem) = viewModelScope.launch { prefs.setUnitSystem(system) }
}
