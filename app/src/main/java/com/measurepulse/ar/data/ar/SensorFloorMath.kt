package com.measurepulse.ar.data.ar

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Engine B geometry: the phone is a camera at a known height above a flat floor.
 *
 * World frame (Android sensor convention): X east, Y north, Z up. The floor is z = 0 and the
 * camera sits at (0, 0, h). [r] is the 3×3 row-major rotation matrix from
 * `SensorManager.getRotationMatrixFromVector`, i.e. world = R · device. The back camera looks
 * along device −Z; in portrait, screen-right is device +X and screen-up is device +Y.
 */
object SensorFloorMath {

    val UP = Vec3(0.0, 0.0, 1.0)

    /** Gyro-fused orientation is good to roughly ±0.35° once settled. */
    const val ANGLE_ERROR_RAD = 0.0061
    /** How far off a typed-in camera height realistically is. */
    const val HEIGHT_ERROR_M = 0.02
    const val MAX_RANGE_M = 15.0

    fun deviceToWorld(r: FloatArray, v: Vec3) = Vec3(
        r[0] * v.x + r[1] * v.y + r[2] * v.z,
        r[3] * v.x + r[4] * v.y + r[5] * v.z,
        r[6] * v.x + r[7] * v.y + r[8] * v.z,
    )

    fun worldToDevice(r: FloatArray, v: Vec3) = Vec3(
        r[0] * v.x + r[3] * v.y + r[6] * v.z,
        r[1] * v.x + r[4] * v.y + r[7] * v.z,
        r[2] * v.x + r[5] * v.y + r[8] * v.z,
    )

    fun cameraForward(r: FloatArray) = deviceToWorld(r, Vec3(0.0, 0.0, -1.0)).normalized()

    fun camera(heightM: Double) = Vec3(0.0, 0.0, heightM)

    /** Where the centre of the camera view meets the floor, or null when aiming too close to/above the horizon. */
    fun floorHit(r: FloatArray, heightM: Double): Vec3? {
        val dir = cameraForward(r)
        if (dir.z > -sin(Math.toRadians(4.0))) return null
        val t = heightM / -dir.z
        val hit = Vec3(dir.x * t, dir.y * t, 0.0)
        return if (sqrt(hit.x * hit.x + hit.y * hit.y) > MAX_RANGE_M) null else hit
    }

    /**
     * 1-sigma error of a floor point. Ground distance g = h·tanθ (θ from vertical), so
     * dg = h·sec²θ·dθ from the angle, plus g·dh/h from the height setting.
     */
    fun accuracyAt(r: FloatArray, heightM: Double): Double {
        val cosT = (-cameraForward(r).z).coerceIn(0.05, 1.0)
        val tanT = sqrt(1 - cosT * cosT) / cosT
        val g = heightM * tanT
        val angular = heightM / (cosT * cosT) * ANGLE_ERROR_RAD
        val height = g * HEIGHT_ERROR_M / heightM
        return sqrt(angular * angular + height * height)
    }

    /** Pinhole projection of a world point into view pixels. */
    fun project(r: FloatArray, camera: Vec3, world: Vec3, focalPx: Float, cx: Float, cy: Float): ScreenPoint? {
        val d = worldToDevice(r, world - camera)
        if (d.z > -0.05) return null
        val x = cx + focalPx * (d.x / -d.z).toFloat()
        val y = cy - focalPx * (d.y / -d.z).toFloat()
        if (abs(x) > 1e5 || abs(y) > 1e5) return null
        return ScreenPoint(x, y)
    }

    /**
     * Focal length in view pixels for a 4:3 portrait preview shown with FILL_CENTER.
     * [vfovDeg] is the vertical (long-side) field of view of the sensor.
     */
    fun focalPx(viewW: Int, viewH: Int, vfovDeg: Double): Float {
        val scale = maxOf(viewW / 3.0, viewH / 4.0)
        val displayedH = 4.0 * scale
        return (displayedH / 2.0 / tan(Math.toRadians(vfovDeg) / 2.0)).toFloat()
    }
}
