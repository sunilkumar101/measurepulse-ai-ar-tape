package com.measurepulse.ar.ui.screens.level

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.data.sensors.MotionSensors
import com.measurepulse.ar.domain.level.LevelMath
import com.measurepulse.ar.domain.level.LevelReading
import com.measurepulse.ar.domain.level.LevelSurface
import com.measurepulse.ar.domain.level.LevelUnit
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Live level state: the zero-corrected reading plus the raw gravity (for the wall laser roll). */
data class LevelState(val reading: LevelReading, val gravity: FloatArray, val rollDeg: Double)

@HiltViewModel
class LevelViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val repo: RoomScanRepository,
) : ViewModel() {

    val usesGravitySensor = MotionSensors.hasGravitySensor(context)

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )

    private val gravity = MotionSensors.gravity(context)

    val state: StateFlow<LevelState?> = combine(gravity, prefs) { g, p ->
        LevelState(LevelMath.reading(g, p.levelZeroX, p.levelZeroY), g, LevelMath.rollDeg(g))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), null)

    private val _unit = MutableStateFlow(LevelUnit.DEGREES)
    val unit = _unit.asStateFlow()
    private val _laser = MutableStateFlow(false)
    val laser = _laser.asStateFlow()
    private val _sound = MutableStateFlow(true)
    val sound = _sound.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull()
    private var wasLevel = false

    fun setUnit(u: LevelUnit) { _unit.value = u }
    fun setLaser(on: Boolean) { _laser.value = on }
    fun toggleSound() { _sound.value = !_sound.value }

    /** Beeps once each time the reading enters the ±0.1° band. Returns true on that edge (for haptics). */
    fun onReading(level: Boolean): Boolean {
        val entered = level && !wasLevel
        wasLevel = level
        if (entered && _sound.value) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        return entered
    }

    /** Treat the current surface as zero (for phones whose sensor is slightly off, or a reference surface). */
    fun calibrateZero() {
        val s = state.value ?: return
        val p = prefs.value
        // Undo the existing offset to get the raw angle, then store it as the new zero.
        val rawX = s.reading.x + p.levelZeroX
        val rawY = s.reading.y + p.levelZeroY
        viewModelScope.launch {
            prefsRepo.setLevelZero(rawX, if (s.reading.surface == LevelSurface.FLAT) rawY else p.levelZeroY)
            _messages.send("Zero calibrated on this surface")
        }
    }

    fun resetZero() = viewModelScope.launch {
        prefsRepo.setLevelZero(0.0, 0.0)
        _messages.send("Calibration cleared")
    }

    fun save() {
        val s = state.value ?: return
        val text = when (s.reading.surface) {
            LevelSurface.FLAT -> "X ${LevelMath.format(s.reading.x, _unit.value)} · Y ${LevelMath.format(s.reading.y, _unit.value)}"
            LevelSurface.EDGE -> "Edge ${LevelMath.format(s.reading.x, _unit.value)}"
        }
        viewModelScope.launch {
            repo.saveToolReading(
                title = "Level reading", category = ScanCategory.LEVEL, detail = text,
                qualityLabel = "Level ±0.1°", imageKey = "bg_living_tv_wall",
            )
            prefsRepo.setLastLevel(text)
            _messages.send("Saved to Projects")
        }
    }

    override fun onCleared() { tone?.release() }
}
