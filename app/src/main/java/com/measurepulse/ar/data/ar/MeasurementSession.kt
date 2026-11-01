package com.measurepulse.ar.data.ar

import kotlin.math.sqrt

/** A drawn line between two world points. [live] = ends at the moving reticle. */
data class Segment(val a: Vec3, val b: Vec3, val accuracyM: Double, val live: Boolean) {
    val lengthM: Double get() = a.distanceTo(b)
}

/** 3D Box Fit result. [corners] = 4 base corners then 4 top corners (same order). */
data class BoxFit(
    val item: BoxItem,
    val corners: List<Vec3>,
    val availableWidthM: Double?,
    /** Total spare width (available − item width); negative = does not fit. Null until the gap is measured. */
    val clearanceM: Double?,
    val ghost: Boolean,
) {
    val fits: Boolean? get() = clearanceM?.let { it >= 0.0 }
}

data class RoomMetrics(val perimeterM: Double, val areaSqM: Double, val wallCount: Int, val closed: Boolean)

/**
 * Pure, immutable measurement state for all four modes. The UI/ViewModel replaces it on every action;
 * geometry is derived on demand from the points + the live reticle.
 */
data class MeasurementSession(
    val mode: MeasureMode = MeasureMode.SINGLE_LINE,
    val points: List<MeasurePoint> = emptyList(),
    val roomClosed: Boolean = false,
    val boxItem: BoxItem = BoxPresets.TV_65,
) {
    val canAddPoint: Boolean
        get() = when (mode) {
            MeasureMode.SINGLE_LINE -> true
            MeasureMode.ROOM_CHAIN -> !roomClosed
            MeasureMode.BOX_FIT -> points.size < 2
            MeasureMode.ANGLE -> points.size < 3
        }

    val canCloseRoom: Boolean get() = mode == MeasureMode.ROOM_CHAIN && !roomClosed && points.size >= 3

    val isEmpty: Boolean get() = points.isEmpty()

    fun add(point: MeasurePoint): MeasurementSession = if (!canAddPoint) this else copy(points = points + point)

    fun undo(): MeasurementSession = when {
        roomClosed -> copy(roomClosed = false)
        points.isNotEmpty() -> copy(points = points.dropLast(1))
        else -> this
    }

    fun clear(): MeasurementSession = copy(points = emptyList(), roomClosed = false)

    fun withMode(newMode: MeasureMode): MeasurementSession =
        if (newMode == mode) this else copy(mode = newMode, points = emptyList(), roomClosed = false)

    fun closeRoom(): MeasurementSession = if (canCloseRoom) copy(roomClosed = true) else this

    fun withBoxItem(item: BoxItem) = copy(boxItem = item)

    /** Applies drift-corrected anchor positions (ARCore). Unknown ids keep their stored position. */
    fun withAnchoredPositions(positions: Map<Long, Vec3>): MeasurementSession =
        if (positions.isEmpty()) this
        else copy(points = points.map { p -> positions[p.id]?.let { p.copy(position = it) } ?: p })

    // ---------- Derived geometry ----------

    fun segments(reticle: Vec3?, reticleAccuracy: Double): List<Segment> {
        val ps = points
        val out = mutableListOf<Segment>()
        fun seg(a: MeasurePoint, b: MeasurePoint) = Segment(a.position, b.position, combine(a.accuracyM, b.accuracyM), live = false)
        fun live(a: MeasurePoint) = reticle?.let { Segment(a.position, it, combine(a.accuracyM, reticleAccuracy), live = true) }

        when (mode) {
            MeasureMode.SINGLE_LINE -> {
                for (i in 0 until ps.size - 1 step 2) out += seg(ps[i], ps[i + 1])
                if (ps.size % 2 == 1) live(ps.last())?.let { out += it }
            }
            MeasureMode.ROOM_CHAIN -> {
                for (i in 0 until ps.size - 1) out += seg(ps[i], ps[i + 1])
                if (roomClosed && ps.size >= 3) out += seg(ps.last(), ps.first())
                else if (ps.isNotEmpty()) live(ps.last())?.let { out += it }
            }
            MeasureMode.BOX_FIT -> {
                if (ps.size >= 2) out += seg(ps[0], ps[1]) else if (ps.size == 1) live(ps[0])?.let { out += it }
            }
            MeasureMode.ANGLE -> {
                for (i in 0 until minOf(ps.size, 3) - 1) out += seg(ps[i], ps[i + 1])
                if (ps.size in 1..2) live(ps.last())?.let { out += it }
            }
        }
        return out
    }

    /** ANGLE mode: angle at the middle point; live with the reticle as the third point. */
    fun angleDeg(reticle: Vec3?): Double? {
        if (mode != MeasureMode.ANGLE || points.size < 2) return null
        val third = points.getOrNull(2)?.position ?: reticle ?: return null
        val vertex = points[1].position
        // Arms shorter than 2 cm are hand jitter, not a real angle.
        if (points[0].position.distanceTo(vertex) < MIN_ARM_M || third.distanceTo(vertex) < MIN_ARM_M) return null
        return SpatialMath.angleDeg(points[0].position, vertex, third)
    }

    /** ANGLE mode vertex (for label placement). */
    fun angleVertex(): Vec3? = if (mode == MeasureMode.ANGLE && points.size >= 2) points[1].position else null

    fun roomMetrics(reticle: Vec3?, up: Vec3): RoomMetrics? {
        if (mode != MeasureMode.ROOM_CHAIN || points.isEmpty()) return null
        val ps = points.map { it.position }
        val withLive = if (!roomClosed && reticle != null) ps + reticle else ps
        return RoomMetrics(
            perimeterM = SpatialMath.pathLength(withLive, closed = roomClosed),
            areaSqM = SpatialMath.polygonArea(withLive, up),
            wallCount = if (roomClosed) ps.size else (withLive.size - 1).coerceAtLeast(0),
            closed = roomClosed,
        )
    }

    /**
     * BOX_FIT: a box standing on the floor.
     * - 0 points: ghost box at the reticle, facing the camera.
     * - 1 point: box starts at the point, aligned toward the reticle.
     * - 2 points: the gap between them is the available width; the item is centred in it.
     */
    fun boxFit(reticle: Vec3?, up: Vec3, camera: Vec3): BoxFit? {
        if (mode != MeasureMode.BOX_FIT) return null
        val item = boxItem
        val upN = up.normalized()
        val p0 = points.getOrNull(0)?.position
        val p1 = points.getOrNull(1)?.position

        val origin: Vec3
        var u: Vec3
        var available: Double? = null
        when {
            p0 != null && p1 != null -> {
                u = SpatialMath.horizontal(p1 - p0, upN).normalized()
                available = SpatialMath.horizontal(p1 - p0, upN).length
                origin = p0 + u * ((available - item.widthM) / 2.0)
            }
            p0 != null -> {
                u = reticle?.let { SpatialMath.horizontal(it - p0, upN) }?.takeIf { it.length > 0.05 }?.normalized()
                    ?: facingAxis(p0, camera, upN)
                origin = p0
            }
            reticle != null -> {
                u = facingAxis(reticle, camera, upN)
                origin = reticle - u * (item.widthM / 2.0)
            }
            else -> return null
        }
        if (u == Vec3.ZERO) u = facingAxis(origin, camera, upN)
        // Depth goes away from the viewer (e.g. back against the wall).
        var v = (upN cross u).normalized()
        if ((v dot SpatialMath.horizontal(origin - camera, upN)) < 0) v = -v

        val w = u * item.widthM
        val d = v * item.depthM
        val h = upN * item.heightM
        val base = listOf(origin, origin + w, origin + w + d, origin + d)
        val corners = base + base.map { it + h }
        return BoxFit(
            item = item,
            corners = corners,
            availableWidthM = available,
            clearanceM = available?.let { it - item.widthM },
            ghost = points.isEmpty(),
        )
    }

    /** Horizontal axis perpendicular to the camera's view direction (so a box "faces" the user). */
    private fun facingAxis(at: Vec3, camera: Vec3, up: Vec3): Vec3 {
        val toPoint = SpatialMath.horizontal(at - camera, up).normalized()
        val axis = (up cross toPoint).normalized()
        return if (axis == Vec3.ZERO) Vec3(1.0, 0.0, 0.0) else axis
    }

    companion object {
        const val MIN_ARM_M = 0.02
        /** Smallest room Room Chain will save (smaller = points were not really spread out). */
        const val MIN_ROOM_AREA_SQM = 0.5

        /** Independent errors add in quadrature. */
        fun combine(a: Double, b: Double) = sqrt(a * a + b * b)
    }
}
