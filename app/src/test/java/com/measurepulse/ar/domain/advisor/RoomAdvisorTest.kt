package com.measurepulse.ar.domain.advisor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAdvisorTest {
    private val room = AdvisorInput(
        roomName = "Master Suite", floorAreaSqM = 24.84, perimeterM = 20.0, ceilingM = 2.85,
        doors = 1, windows = 2, viewingDistanceM = 3.4,
    )

    @Test fun tiles() {
        val t = RoomAdvisor.tiles(room)
        assertEquals(76, t.tiles)           // 24.84 / 0.36 × 1.1 = 75.9
        assertEquals(19, t.boxes)           // 4 per box
        assertEquals(5, t.adhesiveBags)     // 99.4 kg / 20 kg
        assertEquals(2.6, t.groutKg, 1e-9)  // 0.096 kg/m² × 24.84 × 1.1
        assertEquals(19 * 28.0, t.cost, 1e-9)
    }

    @Test fun paint() {
        val p = RoomAdvisor.paint(room)
        // 57.0 gross − 1.89 door − 2×1.44 windows = 52.23 m² → 10.4 L for 2 coats
        assertEquals(52.2, p.netWallSqM, 1e-9)
        assertEquals(10.4, p.litres, 1e-9)
        assertEquals(2.7, p.gallons, 1e-9)
        assertEquals(4.4, p.primerLitres, 1e-9)
        assertEquals(listOf(PaintCan(10.0, 1), PaintCan(1.0, 1)), p.cans)
    }

    @Test fun cans() {
        assertEquals(listOf(PaintCan(4.0, 1)), RoomAdvisor.cansFor(3.2))
        assertEquals(listOf(PaintCan(10.0, 1), PaintCan(4.0, 1)), RoomAdvisor.cansFor(13.6))
        assertTrue(RoomAdvisor.cansFor(0.0).isEmpty())
    }

    @Test fun cooling() {
        val c = RoomAdvisor.cooling(room)
        assertEquals(9_000, c.btu)           // ≈ 6,350 BTU/h → 0.75 ton
        val hot = RoomAdvisor.cooling(room.copy(floorAreaSqM = 30.0, sun = SunExposure.SUNNY, occupants = 4))
        assertEquals(12_000, hot.btu)
        val big = RoomAdvisor.cooling(room.copy(floorAreaSqM = 60.0))
        assertEquals(1.5, big.tons, 1e-9)    // 18,000 BTU/h
    }

    @Test fun tv() {
        val t = RoomAdvisor.tv(3.4, 2.85)
        assertEquals(82.4, t.minDiagonalIn, 0.2)
        assertEquals(99.8, t.maxDiagonalIn, 0.2)
        assertEquals(85, t.recommendedIn)
        assertEquals(55, RoomAdvisor.tv(2.2, 2.5).recommendedIn)
    }
}
