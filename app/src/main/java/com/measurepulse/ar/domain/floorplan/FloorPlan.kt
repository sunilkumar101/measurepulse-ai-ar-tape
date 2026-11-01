package com.measurepulse.ar.domain.floorplan

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** A floor-plan point in meters (x right, y down/forward — same frame as Room Chain's toFloorPlan). */
data class PlanPoint(val x: Double, val y: Double) {
    operator fun plus(o: PlanPoint) = PlanPoint(x + o.x, y + o.y)
    operator fun minus(o: PlanPoint) = PlanPoint(x - o.x, y - o.y)
    operator fun times(s: Double) = PlanPoint(x * s, y * s)
    val length: Double get() = hypot(x, y)
    fun normalized(): PlanPoint = length.let { if (it < 1e-12) PlanPoint(0.0, 0.0) else this * (1 / it) }
}

enum class OpeningType { DOOR, WINDOW }

/**
 * A door or window on wall [wallIndex] (wall i runs from corner i to corner i+1).
 * [offsetM] = distance from the wall's start corner to the opening's near edge.
 */
data class Opening(
    val wallIndex: Int,
    val type: OpeningType,
    val offsetM: Double,
    val widthM: Double,
    val heightM: Double,
    /** Doors: hinge at the near (start-side) edge vs. the far edge. */
    val hingeAtStart: Boolean = true,
    /** Doors: swings into the room (true) or outward. */
    val swingIn: Boolean = true,
) {
    val areaSqM: Double get() = widthM * heightM

    companion object {
        const val DOOR_W = 0.90
        const val DOOR_H = 2.10
        const val WINDOW_W = 1.20
        const val WINDOW_H = 1.20
    }
}

data class Wall(val index: Int, val a: PlanPoint, val b: PlanPoint) {
    val lengthM: Double get() = (b - a).length
    val direction: PlanPoint get() = (b - a).normalized()
    /** Compass-style bearing of the wall direction, 0° = +x. */
    val angleDeg: Double get() = Math.toDegrees(atan2(b.y - a.y, b.x - a.x))
}

/**
 * Editable 2D room outline + openings + ceiling height. Immutable; every edit returns a new plan.
 * Corners are ordered around the room; the last corner connects back to the first.
 */
data class FloorPlan(
    val corners: List<PlanPoint>,
    val openings: List<Opening> = emptyList(),
    val ceilingM: Double = 2.50,
) {
    val walls: List<Wall>
        get() = corners.indices.map { i -> Wall(i, corners[i], corners[(i + 1) % corners.size]) }

    /** Shoelace formula. */
    val areaSqM: Double
        get() {
            if (corners.size < 3) return 0.0
            var s = 0.0
            for (i in corners.indices) {
                val p = corners[i]; val q = corners[(i + 1) % corners.size]
                s += p.x * q.y - q.x * p.y
            }
            return abs(s) / 2.0
        }

    val perimeterM: Double get() = if (corners.size < 2) 0.0 else walls.sumOf { it.lengthM }
    val volumeCuM: Double get() = areaSqM * ceilingM
    val grossWallAreaSqM: Double get() = perimeterM * ceilingM
    val openingsAreaSqM: Double get() = openings.sumOf { it.areaSqM }
    val netWallAreaSqM: Double get() = (grossWallAreaSqM - openingsAreaSqM).coerceAtLeast(0.0)
    val widthM: Double get() = corners.maxOf { it.x } - corners.minOf { it.x }
    val depthM: Double get() = corners.maxOf { it.y } - corners.minOf { it.y }

    fun openingsOn(wall: Int) = openings.filter { it.wallIndex == wall }

    /** Net area of one wall (gross minus its doors and windows). */
    fun netWallArea(wall: Int): Double =
        (walls[wall].lengthM * ceilingM - openingsOn(wall).sumOf { it.areaSqM }).coerceAtLeast(0.0)

    // ---------- Editing ----------

    /**
     * Moves a corner. With [snap], the corner locks to the x/y of its neighbours when within
     * [SNAP_M] — which squares the adjoining walls to the plan axes (90° corners) — and rounds to 1 cm.
     */
    fun moveCorner(index: Int, to: PlanPoint, snap: Boolean = true): FloorPlan {
        var p = to
        if (snap) {
            val prev = corners[(index - 1 + corners.size) % corners.size]
            val next = corners[(index + 1) % corners.size]
            var x = p.x; var y = p.y
            listOf(prev, next).forEach { n ->
                if (abs(x - n.x) < SNAP_M) x = n.x
                if (abs(y - n.y) < SNAP_M) y = n.y
            }
            p = PlanPoint(round1cm(x), round1cm(y))
        }
        return copy(corners = corners.toMutableList().also { it[index] = p }).clampOpenings()
    }

    /**
     * Sets wall [index] to [lengthM] by sliding the following half of the corners along the wall's
     * direction. For a rectangle this keeps every angle at 90° (the opposite wall follows).
     */
    fun setWallLength(index: Int, lengthM: Double): FloorPlan {
        if (lengthM <= 0.05 || corners.size < 3) return this
        val wall = walls[index]
        val delta = wall.direction * (lengthM - wall.lengthM)
        val n = corners.size
        val moved = corners.toMutableList()
        val count = n / 2
        for (k in 0 until count) {
            val j = (index + 1 + k) % n
            moved[j] = moved[j] + delta
        }
        return copy(corners = moved).clampOpenings()
    }

    /** Snaps every wall to the nearest axis (0/90/180/270°) — fixes slightly skewed AR corners. */
    fun squareUp(): FloorPlan {
        if (corners.size < 3) return this
        val out = corners.toMutableList()
        for (i in 0 until corners.size - 1) {
            val a = out[i]; val b = out[i + 1]
            val d = b - a
            out[i + 1] = if (abs(d.x) >= abs(d.y)) PlanPoint(b.x, a.y) else PlanPoint(a.x, b.y)
        }
        // Close the shape: the last corner shares x or y with the first.
        val first = out.first(); val last = out.last()
        val d = first - last
        out[out.size - 1] = if (abs(d.x) >= abs(d.y)) PlanPoint(last.x, first.y) else PlanPoint(first.x, last.y)
        return copy(corners = out.map { PlanPoint(round1cm(it.x), round1cm(it.y)) }).clampOpenings()
    }

    /** Inserts a new corner at the middle of wall [index]. Openings after it shift to the new wall numbering. */
    fun splitWall(index: Int): FloorPlan {
        val w = walls[index]
        val mid = PlanPoint((w.a.x + w.b.x) / 2, (w.a.y + w.b.y) / 2)
        val newCorners = corners.toMutableList().also { it.add(index + 1, mid) }
        val half = w.lengthM / 2
        val newOpenings = openings.map { o ->
            when {
                o.wallIndex < index -> o
                o.wallIndex > index -> o.copy(wallIndex = o.wallIndex + 1)
                o.offsetM >= half -> o.copy(wallIndex = index + 1, offsetM = o.offsetM - half)
                else -> o
            }
        }
        return copy(corners = newCorners, openings = newOpenings).clampOpenings()
    }

    /** Removes a corner (rooms keep at least 3). Openings on the two merged walls are dropped. */
    fun deleteCorner(index: Int): FloorPlan {
        if (corners.size <= 3) return this
        val n = corners.size
        val before = (index - 1 + n) % n
        val newOpenings = openings
            .filter { it.wallIndex != index && it.wallIndex != before }
            .map { if (it.wallIndex > index) it.copy(wallIndex = it.wallIndex - 1) else it }
        return copy(corners = corners.toMutableList().also { it.removeAt(index) }, openings = newOpenings)
    }

    /** Adds a door/window centred on [wall] (shrunk to fit short walls). */
    fun addOpening(wall: Int, type: OpeningType): FloorPlan {
        val len = walls[wall].lengthM
        val w = minOf(if (type == OpeningType.DOOR) Opening.DOOR_W else Opening.WINDOW_W, len * 0.8)
        val h = if (type == OpeningType.DOOR) minOf(Opening.DOOR_H, ceilingM) else Opening.WINDOW_H
        return copy(openings = openings + Opening(wall, type, ((len - w) / 2 * 1000).roundToInt() / 1000.0, w, h))
    }

    fun removeOpening(opening: Opening) = copy(openings = openings - opening)

    fun withCeiling(m: Double) = copy(ceilingM = m.coerceIn(1.5, 10.0))

    /** Keeps openings inside their walls after edits. */
    private fun clampOpenings(): FloorPlan = copy(
        openings = openings.filter { it.wallIndex < corners.size }.map { o ->
            val len = walls[o.wallIndex].lengthM
            val w = minOf(o.widthM, len)
            o.copy(widthM = w, offsetM = o.offsetM.coerceIn(0.0, (len - w).coerceAtLeast(0.0)))
        }
    )

    companion object {
        const val SNAP_M = 0.15

        fun rectangle(widthM: Double, depthM: Double, ceilingM: Double) = FloorPlan(
            corners = listOf(PlanPoint(0.0, 0.0), PlanPoint(widthM, 0.0), PlanPoint(widthM, depthM), PlanPoint(0.0, depthM)),
            ceilingM = ceilingM,
        )

        /** Corner label: A, B, … Z, A1, B1 … */
        fun cornerLabel(i: Int): String = ('A' + i % 26).toString() + if (i >= 26) (i / 26).toString() else ""

        private fun round1cm(v: Double) = (v * 100).roundToInt() / 100.0

        /** Point at distance [t] along a wall from its start. */
        fun along(w: Wall, t: Double) = w.a + w.direction * t

        /** Unit normal pointing to the left of the wall direction. */
        fun leftNormal(w: Wall) = PlanPoint(-w.direction.y, w.direction.x)

        fun rotate(p: PlanPoint, deg: Double): PlanPoint {
            val r = Math.toRadians(deg)
            return PlanPoint(p.x * cos(r) - p.y * sin(r), p.x * sin(r) + p.y * cos(r))
        }
    }
}
