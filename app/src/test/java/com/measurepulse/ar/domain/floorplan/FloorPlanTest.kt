package com.measurepulse.ar.domain.floorplan

import com.measurepulse.ar.domain.advisor.RoomEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorPlanTest {
    private val room = FloorPlan.rectangle(4.6, 5.4, 2.85)

    @Test fun metrics() {
        assertEquals(24.84, room.areaSqM, 1e-9)
        assertEquals(20.0, room.perimeterM, 1e-9)
        assertEquals(24.84 * 2.85, room.volumeCuM, 1e-9)
        val withOpenings = room.addOpening(0, OpeningType.DOOR).addOpening(1, OpeningType.WINDOW)
        assertEquals(57.0 - 1.89 - 1.44, withOpenings.netWallAreaSqM, 1e-9)
        assertEquals(4.6 * 2.85 - 1.89, withOpenings.netWallArea(0), 1e-9)
        // Door is centred on its wall
        assertEquals(1.85, withOpenings.openingsOn(0).single().offsetM, 1e-9)
    }

    @Test fun setWallLengthKeepsRectangle() {
        val p = room.setWallLength(0, 5.0)
        assertEquals(listOf(5.0, 5.4, 5.0, 5.4), p.walls.map { (it.lengthM * 1e6).toLong() / 1e6 })
        assertEquals(27.0, p.areaSqM, 1e-9)
        val q = room.setWallLength(3, 6.0) // closing wall D→A
        assertEquals(listOf(4.6, 6.0, 4.6, 6.0), q.walls.map { (it.lengthM * 1e6).toLong() / 1e6 })
    }

    @Test fun dragSnapsToRightAngles() {
        val p = room.moveCorner(2, PlanPoint(4.9, 5.48))
        assertEquals(PlanPoint(4.9, 5.4), p.corners[2])
        val back = room.moveCorner(2, PlanPoint(4.7, 5.5))
        assertEquals(PlanPoint(4.6, 5.4), back.corners[2])
        val free = room.moveCorner(2, PlanPoint(4.7, 5.5), snap = false)
        assertEquals(PlanPoint(4.7, 5.5), free.corners[2])
    }

    @Test fun squareUpFixesSkew() {
        val skewed = FloorPlan(listOf(PlanPoint(0.0, 0.0), PlanPoint(4.0, 0.1), PlanPoint(4.1, 3.0), PlanPoint(0.05, 3.05)))
        val sq = skewed.squareUp()
        assertEquals(12.0, sq.areaSqM, 1e-9)
        sq.walls.forEach { w -> assertTrue(w.a.x == w.b.x || w.a.y == w.b.y) }
    }

    @Test fun splitAndDeleteCorner() {
        val split = room.addOpening(1, OpeningType.WINDOW).splitWall(0)
        assertEquals(5, split.corners.size)
        assertEquals(room.areaSqM, split.areaSqM, 1e-9)
        assertEquals(2, split.openings.single().wallIndex) // window moved from wall 1 to wall 2
        assertEquals(4, split.deleteCorner(1).corners.size)
        assertEquals(3, FloorPlan.rectangle(1.0, 1.0, 2.5).deleteCorner(0).deleteCorner(0).corners.size)
    }

    @Test fun codecRoundTrip() {
        val p = room.addOpening(0, OpeningType.DOOR).addOpening(2, OpeningType.WINDOW)
        assertEquals(p.corners, FloorPlanCodec.decodeCorners(FloorPlanCodec.encodeCorners(p.corners)))
        assertEquals(p.openings, FloorPlanCodec.decodeOpenings(FloorPlanCodec.encodeOpenings(p.openings)))
        assertEquals(null, FloorPlanCodec.decodeCorners("[[1,2]]")) // fewer than 3 corners
        assertEquals(emptyList<Opening>(), FloorPlanCodec.decodeOpenings(null))
        assertEquals(3, FloorPlanCodec.decodeCorners("[[0.0000,0.0000],[3.2,0],[3.2,4.4]]")!!.size)
    }

    @Test fun estimates() {
        val p = room.addOpening(0, OpeningType.DOOR).addOpening(1, OpeningType.WINDOW)
        val e = RoomEstimator.estimate(p)
        assertEquals(76, e.tiles.count) // 24.84 m² +10% waste / 0.36 m²
        assertEquals(19, e.tiles.boxes)
        assertEquals(10.7, e.paint.litres, 1e-9)
        assertEquals(7000, e.cooling.btu)
        assertEquals(0.5, e.cooling.tons, 1e-9)
        assertEquals(85, e.tv.diagonalIn) // 3.45 m → SMPTE 30° needs ≥ 83.6"
        assertEquals(19.1, e.baseboardM, 1e-9)
        assertTrue(e.furniture.first { it.name == "King bed" }.fits)
        assertFalse(RoomEstimator.furniture(FloorPlan.rectangle(2.2, 2.2, 2.5)).first { it.name == "King bed" }.fits)
    }
}
