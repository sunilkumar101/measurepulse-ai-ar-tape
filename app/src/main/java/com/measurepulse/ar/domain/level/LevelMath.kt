package com.measurepulse.ar.domain.level

import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

enum class LevelUnit(val label: String) {
    DEGREES("Degrees °"),
    ROOF_PITCH("Pitch X:12"),
    SLOPE("Slope %"),
}

enum class LevelSurface { FLAT, EDGE }

/** One level reading in degrees. Flat: [x]/[y] tilt of the screen plane. Edge: [x] = roll from true horizontal. */
data class LevelReading(val surface: LevelSurface, val x: Double, val y: Double) {
    /** Combined tilt (flat) or the edge angle. */
    val magnitude: Double get() = if (surface == LevelSurface.FLAT) sqrt(x * x + y * y) else abs(x)
    fun isLevel(toleranceDeg: Double = LevelMath.LEVEL_TOLERANCE_DEG) = magnitude <= toleranceDeg
}

/**
 * Spirit-level maths on the gravity vector (device axes: x right, y up, z out of the screen).
 * The raw vector is smoothed with a low-pass filter (α = 0.12) before use.
 */
object LevelMath {

    const val ALPHA = 0.12f
    const val LEVEL_TOLERANCE_DEG = 0.1

    /** In-place low-pass: g ← g + α·(raw − g). Returns true once initialised. */
    fun lowPass(state: FloatArray, raw: FloatArray, initialised: Boolean, alpha: Float = ALPHA) {
        for (i in 0..2) state[i] = if (!initialised) raw[i] else state[i] + alpha * (raw[i] - state[i])
    }

    /**
     * Phone lying on a surface (|gz| dominant) → bullseye mode with X/Y tilt.
     * Phone standing on an edge against a wall/frame → tube mode with the roll angle.
     */
    fun reading(g: FloatArray, zeroX: Double = 0.0, zeroY: Double = 0.0): LevelReading {
        val gx = g[0].toDouble(); val gy = g[1].toDouble(); val gz = g[2].toDouble()
        val norm = sqrt(gx * gx + gy * gy + gz * gz).takeIf { it > 1e-6 } ?: return LevelReading(LevelSurface.FLAT, 0.0, 0.0)
        return if (abs(gz) / norm > 0.70) {
            val x = Math.toDegrees(atan2(gx, abs(gz))) - zeroX
            val y = Math.toDegrees(atan2(gy, abs(gz))) - zeroY
            LevelReading(LevelSurface.FLAT, x, y)
        } else {
            LevelReading(LevelSurface.EDGE, rollDeg(g) - zeroX, 0.0)
        }
    }

    /**
     * Angle of the phone's long edge from true horizontal (for edge mode and the wall laser).
     * 0° when held upright in portrait; positive when rotated clockwise.
     */
    fun rollDeg(g: FloatArray): Double {
        val r = Math.toDegrees(atan2(g[0].toDouble(), g[1].toDouble()))
        // Fold to ±45° around the nearest 90° so landscape/portrait edges both read as "level".
        var a = r
        while (a > 45) a -= 90
        while (a < -45) a += 90
        return a
    }

    fun format(deg: Double, unit: LevelUnit): String = when (unit) {
        LevelUnit.DEGREES -> String.format(Locale.US, "%.1f°", deg)
        LevelUnit.ROOF_PITCH -> String.format(Locale.US, "%.1f:12", 12 * tan(Math.toRadians(abs(deg))))
        LevelUnit.SLOPE -> String.format(Locale.US, "%.1f%%", 100 * tan(Math.toRadians(abs(deg))))
    }
}
