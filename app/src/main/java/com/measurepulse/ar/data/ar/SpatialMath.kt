package com.measurepulse.ar.data.ar

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** Immutable 3D vector in meters. World axes depend on the engine; see [EngineFrame.up]. */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    operator fun unaryMinus() = Vec3(-x, -y, -z)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length: Double get() = sqrt(this dot this)
    fun normalized(): Vec3 = length.let { if (it < 1e-12) ZERO else this * (1.0 / it) }
    fun distanceTo(o: Vec3) = (this - o).length

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }
}

/** A point on screen, in pixels of the camera view. */
data class ScreenPoint(val x: Float, val y: Float)

/** Maps a world point to screen pixels, or null when the point is behind the camera. */
fun interface ScreenProjector {
    fun project(world: Vec3): ScreenPoint?
}

object SpatialMath {

    /** Removes the vertical component, leaving the direction along the floor. */
    fun horizontal(v: Vec3, up: Vec3): Vec3 = v - up * (v dot up)

    /** Angle ABC at vertex [b], in degrees (0–180). */
    fun angleDeg(a: Vec3, b: Vec3, c: Vec3): Double? {
        val u = (a - b).normalized()
        val v = (c - b).normalized()
        if (u == Vec3.ZERO || v == Vec3.ZERO) return null
        return Math.toDegrees(acos((u dot v).coerceIn(-1.0, 1.0)))
    }

    /** Area of a (closed) polygon projected onto the plane with normal [up]. */
    fun polygonArea(points: List<Vec3>, up: Vec3): Double {
        if (points.size < 3) return 0.0
        val n = up.normalized()
        var sum = Vec3.ZERO
        for (i in points.indices) sum += points[i] cross points[(i + 1) % points.size]
        return abs(sum dot n) / 2.0
    }

    fun pathLength(points: List<Vec3>, closed: Boolean): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) total += points[i].distanceTo(points[i + 1])
        if (closed && points.size > 2) total += points.last().distanceTo(points.first())
        return total
    }

    /** Intersection of a ray with a plane, or null if parallel / behind the origin. */
    fun rayPlane(origin: Vec3, dir: Vec3, planePoint: Vec3, normal: Vec3): Vec3? {
        val denom = dir dot normal
        if (abs(denom) < 1e-9) return null
        val t = ((planePoint - origin) dot normal) / denom
        return if (t <= 0) null else origin + dir * t
    }

    /**
     * Flattens a floor polygon into 2D meters for blueprints: origin at the first point,
     * x along the first wall, y perpendicular on the floor.
     */
    fun toFloorPlan(points: List<Vec3>, up: Vec3): List<Pair<Double, Double>> {
        if (points.isEmpty()) return emptyList()
        val origin = points.first()
        val u = horizontal((points.getOrNull(1) ?: (origin + Vec3(1.0, 0.0, 0.0))) - origin, up).normalized()
            .takeIf { it != Vec3.ZERO } ?: Vec3(1.0, 0.0, 0.0)
        val v = (up.normalized() cross u).normalized()
        return points.map { p -> val d = p - origin; (d dot u) to (d dot v) }
    }
}
