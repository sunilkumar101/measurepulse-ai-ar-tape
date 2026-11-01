package com.measurepulse.ar.domain.tools

import com.measurepulse.ar.domain.caliper.BoltMatch
import com.measurepulse.ar.domain.caliper.CaliperMath
import com.measurepulse.ar.domain.level.LevelMath
import com.measurepulse.ar.domain.level.LevelSurface
import com.measurepulse.ar.domain.level.LevelUnit
import com.measurepulse.ar.domain.rangefinder.RangefinderMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

class RangefinderMathTest {
    private fun deg(rise: Double, run: Double) = Math.toDegrees(atan(rise / run))

    @Test fun referenceObjectGivesDistance() {
        // 1.72 m person, 10 m away, phone at 1.5 m.
        val r = RangefinderMath.distanceFromReference(1.72, deg(-1.5, 10.0), deg(0.22, 10.0), 0.09)!!
        assertEquals(10.0, r.distanceM, 1e-9)
        assertTrue(r.uncertaintyM in 0.3..1.5)
    }

    @Test fun heightAtKnownDistance() {
        val r = RangefinderMath.heightAtDistance(10.0, deg(-1.5, 10.0), deg(10.5, 10.0))
        assertEquals(12.0, r.heightM!!, 1e-9)
    }

    @Test fun twoAngleTrig() {
        val r = RangefinderMath.twoAngle(1.5, deg(-1.5, 20.0), deg(8.5, 20.0))!!
        assertEquals(20.0, r.distanceM, 1e-9)
        assertEquals(10.0, r.heightM!!, 1e-9)
        assertNull(RangefinderMath.twoAngle(1.5, 2.0, 10.0)) // base must be below the horizon
    }

    @Test fun rowElevation() {
        assertEquals(10.0, RangefinderMath.elevationOfRow(10.0, 800f, 800f, 1000f), 1e-9)
        assertEquals(10.0 + 45.0, RangefinderMath.elevationOfRow(10.0, -200f, 800f, 1000f), 1e-9)
    }
}

class CaliperMathTest {
    @Test fun fractions() {
        assertEquals("61/64\"", CaliperMath.fractionalInch(24.25))
        assertEquals("1 3/16\"", CaliperMath.fractionalInch(30.0))
        assertEquals("1\"", CaliperMath.fractionalInch(25.4))
        assertEquals("0\"", CaliperMath.fractionalInch(0.1))
    }

    @Test fun boltMatches() {
        val m = CaliperMath.boltMatches(24.25).map { it.thread.name to it.kind }
        assertTrue(("M16" to BoltMatch.Kind.HEX_HEAD) in m)
        assertTrue(("M24" to BoltMatch.Kind.SHANK) in m)
        val eight = CaliperMath.boltMatches(7.9)
        assertEquals("M8", eight.first().thread.name) // shank wins the tie
        assertEquals(BoltMatch.Kind.SHANK, eight.first().kind)
        assertTrue(eight.any { it.thread.name == "M5" && it.kind == BoltMatch.Kind.HEX_HEAD })
        assertTrue(CaliperMath.boltMatches(9.0).isEmpty())
        assertEquals(listOf("M14", "M16"), CaliperMath.threadsForPitch(2.0))
    }

    @Test fun calibration() {
        val dpi = 320f
        // The real card spans 2 % more pixels than the reported DPI predicts → real px/mm is 2 % higher.
        val measured = (85.60 / 25.4 * dpi * 1.02).toFloat()
        val f = CaliperMath.calibrationFactor(measured, dpi)
        assertEquals(1.02, f, 1e-4)
        // After calibrating, the same jaw gap reads exactly the card length.
        assertEquals(85.60, measured / CaliperMath.pxPerMm(dpi, f), 1e-3)
        // Phone case: jaws read 40.69 mm on a 53.98 mm card edge → factor 0.754, gap now reads 53.98 mm.
        val px = (40.69 / 25.4 * dpi).toFloat()
        val g = CaliperMath.calibrationFactor(px, dpi, CaliperMath.CARD_SHORT_MM)
        assertEquals(53.98, px / CaliperMath.pxPerMm(dpi, g), 1e-3)
        // Jaws left at 20 mm on a 53.98 mm card are a user mistake, not a calibration.
        val wrong = (20.0 / 25.4 * dpi).toFloat()
        assertTrue(!CaliperMath.isPlausibleCalibration(wrong, dpi, CaliperMath.CARD_SHORT_MM))
        assertTrue(CaliperMath.isPlausibleCalibration(px, dpi, CaliperMath.CARD_SHORT_MM))
    }
}

class LevelMathTest {
    private val g = 9.81f

    @Test fun flatAndTilted() {
        val flat = LevelMath.reading(floatArrayOf(0f, 0f, g))
        assertEquals(LevelSurface.FLAT, flat.surface); assertTrue(flat.isLevel())
        val a = Math.toRadians(1.0)
        val tilted = LevelMath.reading(floatArrayOf((g * sin(a)).toFloat(), 0f, (g * cos(a)).toFloat()))
        assertEquals(1.0, tilted.x, 1e-4)
        // Zero calibration removes a constant offset
        assertEquals(0.0, LevelMath.reading(floatArrayOf((g * sin(a)).toFloat(), 0f, (g * cos(a)).toFloat()), zeroX = 1.0).x, 1e-4)
    }

    @Test fun edgeRoll() {
        val a = Math.toRadians(1.0)
        val r = LevelMath.reading(floatArrayOf((g * sin(a)).toFloat(), (g * cos(a)).toFloat(), 0f))
        assertEquals(LevelSurface.EDGE, r.surface)
        assertEquals(1.0, r.x, 1e-4)
        // Landscape (rotated 90°) also reads 0 when level
        assertEquals(0.0, LevelMath.rollDeg(floatArrayOf(g, 0f, 0f)), 1e-6)
    }

    @Test fun lowPassAndUnits() {
        val state = FloatArray(3)
        LevelMath.lowPass(state, floatArrayOf(1f, 1f, 1f), initialised = false)
        LevelMath.lowPass(state, floatArrayOf(2f, 2f, 2f), initialised = true)
        assertEquals(1.12f, state[0], 1e-6f)
        assertEquals("6.0:12", LevelMath.format(Math.toDegrees(atan(0.5)), LevelUnit.ROOF_PITCH))
        assertEquals("100.0%", LevelMath.format(45.0, LevelUnit.SLOPE))
        assertEquals("-0.1°", LevelMath.format(-0.1, LevelUnit.DEGREES))
    }
}

class GroundDistanceTest {
    @Test fun uncertaintyIsPositiveAndGrowsWithRange() {
        val near = RangefinderMath.groundDistanceResult(1.5, -45.0)!!
        val far = RangefinderMath.groundDistanceResult(1.5, -5.0)!!
        assertEquals(1.5, near.distanceM, 1e-9)
        assertTrue(near.uncertaintyM > 0 && far.uncertaintyM > near.uncertaintyM)
    }
}
