package com.measurepulse.ar.domain.rangefinder

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.math.tan

/** Known-height reference objects for "AI Auto-Height". */
enum class ReferencePreset(val label: String, val heightM: Double, val toleranceM: Double) {
    PERSON("Person", 1.72, 0.09),
    DOOR("Door", 2.05, 0.05),
    CAR("Car", 1.45, 0.06),
    SUV("SUV", 1.75, 0.08),
    STORY("Story / Floor", 3.00, 0.25),
}

data class RangeResult(
    /** Horizontal distance from the phone to the target, meters. */
    val distanceM: Double,
    /** Target height (base to top), meters. Null when only distance is known. */
    val heightM: Double?,
    /** Straight line-of-sight distance to the target's top. */
    val lineOfSightM: Double?,
    /** 1-sigma uncertainty of the height (or distance when height is null). */
    val uncertaintyM: Double,
)

/**
 * Rangefinder geometry. Angles are *elevations* in degrees: 0° = horizon, + = above, − = below.
 *
 * A screen row y is turned into an elevation with the pinhole model:
 *   elevation(y) = cameraPitch + atan((cy − y) / focalPx)
 * (assumes the phone is held upright; roll is reported separately so the UI can warn).
 */
object RangefinderMath {

    /** Typical fused-sensor pitch error. */
    const val PITCH_ERROR_DEG = 0.3

    fun elevationOfRow(cameraPitchDeg: Double, y: Float, cy: Float, focalPx: Float): Double =
        cameraPitchDeg + Math.toDegrees(atan(((cy - y) / focalPx).toDouble()))

    private fun t(deg: Double) = tan(Math.toRadians(deg))

    /**
     * Mode A — a vertical object of known height spanning elevations [bottomDeg, topDeg]
     * stands at horizontal distance D = H / (tan(top) − tan(bottom)). Exact for upright objects,
     * independent of how high the phone is held.
     */
    fun distanceFromReference(
        refHeightM: Double,
        bottomDeg: Double,
        topDeg: Double,
        refToleranceM: Double = 0.0,
        edgeErrorRad: Double = 0.003,
    ): RangeResult? {
        val span = t(topDeg) - t(bottomDeg)
        if (span <= 1e-4 || refHeightM <= 0) return null
        val d = refHeightM / span
        if (d > 2000) return null
        // A pitch offset shifts both edges equally (mostly cancels); box-edge pixel error does not.
        val pitch = Math.toRadians(PITCH_ERROR_DEG) * abs(sec2(topDeg) - sec2(bottomDeg))
        val edges = sqrt(2.0) * edgeErrorRad * (sec2(topDeg) + sec2(bottomDeg)) / 2
        val spanErr = sqrt(pitch * pitch + edges * edges)
        val rel = sqrt((refToleranceM / refHeightM).let { it * it } + (spanErr / span).let { it * it })
        return RangeResult(d, null, null, d * rel)
    }

    /** Height of a second target at the same distance (e.g. a tree next to the person). */
    fun heightAtDistance(distanceM: Double, baseDeg: Double, topDeg: Double, distanceErrorM: Double = 0.0): RangeResult {
        val h = distanceM * (t(topDeg) - t(baseDeg))
        val angular = distanceM * Math.toRadians(PITCH_ERROR_DEG) * (sec2(topDeg) + sec2(baseDeg)) / 2
        val fromDistance = abs(h) * (distanceErrorM / distanceM)
        val los = distanceM / cos(Math.toRadians(topDeg))
        return RangeResult(distanceM, abs(h), abs(los), sqrt(angular * angular + fromDistance * fromDistance))
    }

    /**
     * Mode B — two-angle trigonometry from a known eye height h:
     *   base below the horizon at θ1 < 0 → D = h / tan(−θ1)
     *   top at θ2 → H = D·tan(θ2) + h
     */
    fun twoAngle(cameraHeightM: Double, baseDeg: Double, topDeg: Double): RangeResult? {
        if (baseDeg >= -0.5 || topDeg <= baseDeg) return null
        val d = cameraHeightM / t(-baseDeg)
        if (d > 2000) return null
        val h = d * t(topDeg) + cameraHeightM
        // dD/dθ1 = h·csc²θ1 ; propagate both angles + 2 cm eye-height uncertainty.
        val dErr = cameraHeightM / (kotlin.math.sin(Math.toRadians(-baseDeg)).let { it * it }) * Math.toRadians(PITCH_ERROR_DEG)
        val hErr = sqrt(
            (dErr * t(topDeg)).let { it * it } +
                (d * sec2(topDeg) * Math.toRadians(PITCH_ERROR_DEG)).let { it * it } +
                (0.02 * (1 + t(topDeg) / t(-baseDeg))).let { it * it }
        )
        return RangeResult(d, h, hypot(d, h - cameraHeightM), hErr)
    }

    /** Distance only, from aiming at the base of something on level ground. */
    fun groundDistance(cameraHeightM: Double, baseDeg: Double): Double? =
        if (baseDeg >= -0.5) null else cameraHeightM / t(-baseDeg)

    /** [groundDistance] with its uncertainty: dD = h·csc²θ·dθ plus 2 cm of eye-height error scaled by D/h. */
    fun groundDistanceResult(cameraHeightM: Double, baseDeg: Double): RangeResult? {
        val d = groundDistance(cameraHeightM, baseDeg) ?: return null
        val s = kotlin.math.sin(Math.toRadians(-baseDeg))
        val angular = cameraHeightM / (s * s) * Math.toRadians(PITCH_ERROR_DEG)
        val height = d * 0.02 / cameraHeightM
        return RangeResult(d, null, null, sqrt(angular * angular + height * height))
    }

    /** 35 mm-equivalent focal length for display ("48 mm"). */
    fun equivalentFocalMm(vfovDeg: Double, zoom: Double): Int =
        (24.0 / 2 / tan(Math.toRadians(vfovDeg) / 2) * zoom * 1.5).toInt() // long side ≈ 36 mm frame

    private fun sec2(deg: Double) = 1 / cos(Math.toRadians(deg)).let { it * it }
}
