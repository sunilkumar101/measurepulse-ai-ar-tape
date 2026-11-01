package com.measurepulse.ar.ui.screens.caliper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.domain.caliper.CaliperMath
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class CaliperTab(val label: String) { CALIPER("Caliper"), THREAD("Thread Pitch"), CALIBRATE("Calibrate") }

enum class CardEdge(val label: String, val mm: Double) {
    LONG("Long edge 85.60 mm", CaliperMath.CARD_LONG_MM),
    SHORT("Short edge 53.98 mm", CaliperMath.CARD_SHORT_MM),
}

@HiltViewModel
class CaliperViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val repo: RoomScanRepository,
) : ViewModel() {

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    private val _tab = MutableStateFlow(CaliperTab.CALIPER)
    val tab = _tab.asStateFlow()
    private val _pitch = MutableStateFlow(1.25)
    val pitch = _pitch.asStateFlow()
    private val _edge = MutableStateFlow(CardEdge.LONG)
    val edge = _edge.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setTab(t: CaliperTab) { _tab.value = t }
    fun setPitch(p: Double) { _pitch.value = p }
    fun setEdge(e: CardEdge) { _edge.value = e }

    /** Jaws currently span the card edge: derive and store the correction factor. */
    fun calibrate(gapPx: Float, dpi: Float) {
        if (!CaliperMath.isPlausibleCalibration(gapPx, dpi, _edge.value.mm)) {
            viewModelScope.launch { _messages.send("Jaws don't match the card — drag them to the card's two edges first") }
            return
        }
        val factor = CaliperMath.calibrationFactor(gapPx, dpi, _edge.value.mm)
        viewModelScope.launch {
            prefsRepo.setCaliperScale(factor)
            _messages.send(String.format(Locale.US, "Calibrated: screen scale ×%.3f", factor))
        }
    }

    fun resetCalibration() = viewModelScope.launch {
        prefsRepo.setCaliperScale(1.0)
        _messages.send("Calibration reset to the display's reported DPI")
    }

    fun save(mm: Double) {
        if (mm < 0.2) { viewModelScope.launch { _messages.send("Open the jaws around the object first") }; return }
        val match = CaliperMath.boltMatches(mm).firstOrNull()
        viewModelScope.launch {
            repo.saveToolReading(
                title = String.format(Locale.US, "Caliper %.2f mm", mm),
                category = ScanCategory.CALIPER,
                detail = String.format(Locale.US, "%.3f in · %s", CaliperMath.inches(mm), CaliperMath.fractionalInch(mm)) +
                    (match?.let { " · ${it.thread.name}" } ?: ""),
                qualityLabel = "Caliper ±0.1 mm",
                imageKey = "bg_city_lounge",
                widthM = mm / 1000.0,
            )
            prefsRepo.setLastMeasurement(mm / 1000.0, "Caliper")
            _messages.send("Saved to Projects")
        }
    }
}
