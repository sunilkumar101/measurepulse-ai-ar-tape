package com.measurepulse.ar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val onboardingComplete: Boolean,
    val unitSystem: UnitSystem,
    val fractionPrecision: FractionPrecision,
    /** Phone height above the floor for the gyro (non-AR) engine. */
    val cameraHeightM: Double,
    /** Screen-ruler correction from the credit-card calibration (1.0 = trust the reported DPI). */
    val caliperScale: Double = 1.0,
    /** Spirit-level zero offsets (degrees) from "Calibrate Zero". */
    val levelZeroX: Double = 0.0,
    val levelZeroY: Double = 0.0,
    /** Last single measurement (for the 2×2 converter widget). */
    val lastMeasurementM: Double? = null,
    val lastMeasurementLabel: String? = null,
    /** Last saved spirit-level reading text (widgets can't stream sensors). */
    val lastLevelText: String? = null,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val PRECISION = stringPreferencesKey("fraction_precision")
        val CAMERA_HEIGHT = doublePreferencesKey("camera_height_m")
        val CALIPER_SCALE = doublePreferencesKey("caliper_scale")
        val LEVEL_ZERO_X = doublePreferencesKey("level_zero_x")
        val LEVEL_ZERO_Y = doublePreferencesKey("level_zero_y")
        val LAST_MEASUREMENT = doublePreferencesKey("last_measurement_m")
        val LAST_MEASUREMENT_LABEL = stringPreferencesKey("last_measurement_label")
        val LAST_LEVEL = stringPreferencesKey("last_level_text")
    }

    /** US, Liberia and Myanmar default to imperial; everyone else to metric. */
    private val defaultSystem: UnitSystem
        get() = when (Locale.getDefault().country) {
            "US", "LR", "MM" -> UnitSystem.IMPERIAL
            else -> UnitSystem.METRIC
        }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
            unitSystem = p[Keys.UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: defaultSystem,
            fractionPrecision = p[Keys.PRECISION]?.let { runCatching { FractionPrecision.valueOf(it) }.getOrNull() }
                ?: FractionPrecision.SIXTEENTH,
            cameraHeightM = p[Keys.CAMERA_HEIGHT] ?: DEFAULT_CAMERA_HEIGHT_M,
            caliperScale = p[Keys.CALIPER_SCALE] ?: 1.0,
            levelZeroX = p[Keys.LEVEL_ZERO_X] ?: 0.0,
            levelZeroY = p[Keys.LEVEL_ZERO_Y] ?: 0.0,
            lastMeasurementM = p[Keys.LAST_MEASUREMENT],
            lastMeasurementLabel = p[Keys.LAST_MEASUREMENT_LABEL],
            lastLevelText = p[Keys.LAST_LEVEL],
        )
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[Keys.ONBOARDING] = true }
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.dataStore.edit { it[Keys.UNIT_SYSTEM] = system.name }
    }

    suspend fun setCameraHeight(meters: Double) {
        context.dataStore.edit { it[Keys.CAMERA_HEIGHT] = meters.coerceIn(0.5, 2.5) }
    }

    suspend fun setCaliperScale(scale: Double) {
        context.dataStore.edit { it[Keys.CALIPER_SCALE] = scale }
    }

    suspend fun setLevelZero(x: Double, y: Double) {
        context.dataStore.edit { it[Keys.LEVEL_ZERO_X] = x; it[Keys.LEVEL_ZERO_Y] = y }
    }

    suspend fun setLastMeasurement(meters: Double, label: String) {
        context.dataStore.edit { it[Keys.LAST_MEASUREMENT] = meters; it[Keys.LAST_MEASUREMENT_LABEL] = label }
    }

    suspend fun setLastLevel(text: String) {
        context.dataStore.edit { it[Keys.LAST_LEVEL] = text }
    }

    suspend fun setFractionPrecision(precision: FractionPrecision) {
        context.dataStore.edit { it[Keys.PRECISION] = precision.name }
    }
}

const val DEFAULT_CAMERA_HEIGHT_M = 1.50
