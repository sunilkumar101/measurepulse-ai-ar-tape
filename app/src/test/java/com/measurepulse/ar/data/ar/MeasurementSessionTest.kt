package com.measurepulse.ar.data.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementSessionTest {
    // Y-up world, like ARCore.
    private val up = Vec3(0.0, 1.0, 0.0)
    private val camera = Vec3(0.0, 1.5, 3.0)
    private var nextId = 1L
    private fun pt(x: Double, z: Double, y: Double = 0.0) = MeasurePoint(nextId++, Vec3(x, y, z), 0.005)

    @Test fun singleLine_pairsPointsAndLiveSegment() {
        var s = MeasurementSession()
        s = s.add(pt(0.0, 0.0))
        val live = s.segments(Vec3(2.42, 0.0, 0.0), 0.005)
        assertEquals(1, live.size)
        assertTrue(live[0].live)
        assertEquals(2.42, live[0].lengthM, 1e-9)

        s = s.add(pt(2.42, 0.0))
        val done = s.segments(Vec3(9.0, 0.0, 0.0), 0.005)
        assertEquals(1, done.size)
        assertFalse(done[0].live)
        assertEquals(0.005 * kotlin.math.sqrt(2.0), done[0].accuracyM, 1e-12)
    }

    @Test fun roomChain_areaPerimeterAndClose() {
        var s = MeasurementSession().withMode(MeasureMode.ROOM_CHAIN)
        listOf(pt(0.0, 0.0), pt(4.6, 0.0), pt(4.6, 5.4), pt(0.0, 5.4)).forEach { s = s.add(it) }
        assertTrue(s.canCloseRoom)
        s = s.closeRoom()
        assertFalse(s.canAddPoint)
        val m = s.roomMetrics(null, up)!!
        assertEquals(24.84, m.areaSqM, 1e-9)
        assertEquals(20.0, m.perimeterM, 1e-9)
        assertEquals(4, m.wallCount)
        assertEquals(4, s.segments(null, 0.0).size)
        // Undo re-opens the room first, then removes points.
        assertFalse(s.undo().roomClosed)
    }

    @Test fun angle_rightAngleAndLive() {
        var s = MeasurementSession().withMode(MeasureMode.ANGLE)
        s = s.add(pt(1.0, 0.0)).add(pt(0.0, 0.0))
        assertEquals(90.0, s.angleDeg(Vec3(0.0, 0.0, 1.0))!!, 1e-9)
        s = s.add(pt(-1.0, 0.0))
        assertEquals(180.0, s.angleDeg(null)!!, 1e-9)
        assertFalse(s.canAddPoint)
    }

    @Test fun boxFit_clearanceAndCentering() {
        var s = MeasurementSession().withMode(MeasureMode.BOX_FIT).withBoxItem(BoxPresets.TV_65)
        s = s.add(pt(0.0, 0.0)).add(pt(1.60, 0.0))
        val fit = s.boxFit(null, up, camera)!!
        assertEquals(1.60, fit.availableWidthM!!, 1e-9)
        assertEquals(0.15, fit.clearanceM!!, 1e-9)
        assertEquals(true, fit.fits)
        assertEquals(8, fit.corners.size)
        // Centred: 7.5 cm each side
        assertEquals(0.075, fit.corners[0].x, 1e-9)
        // Top corners are lifted by the TV height
        assertEquals(BoxPresets.TV_65.heightM, fit.corners[4].y, 1e-9)

        val tooBig = s.withBoxItem(BoxPresets.SOFA_3).boxFit(null, up, camera)!!
        assertEquals(false, tooBig.fits)
        assertEquals(-0.50, tooBig.clearanceM!!, 1e-9)
    }

    @Test fun boxFit_ghostAtReticleWithoutPoints() {
        val s = MeasurementSession().withMode(MeasureMode.BOX_FIT)
        val fit = s.boxFit(Vec3(0.0, 0.0, 0.0), up, camera)
        assertNotNull(fit)
        assertTrue(fit!!.ghost)
        assertNull(fit.clearanceM)
    }

    @Test fun modeChangeClearsPoints() {
        val s = MeasurementSession().add(pt(0.0, 0.0)).withMode(MeasureMode.ANGLE)
        assertTrue(s.isEmpty)
    }

    @Test fun anchoredPositionsUpdatePoints() {
        val p = pt(0.0, 0.0)
        val s = MeasurementSession().add(p).withAnchoredPositions(mapOf(p.id to Vec3(0.01, 0.0, 0.0)))
        assertEquals(0.01, s.points[0].position.x, 1e-12)
    }
}

class SpatialMathTest {
    @Test fun floorPlanFlattening() {
        val up = Vec3(0.0, 1.0, 0.0)
        val plan = SpatialMath.toFloorPlan(listOf(Vec3(1.0, 0.0, 1.0), Vec3(4.0, 0.0, 1.0), Vec3(4.0, 0.0, 3.0)), up)
        assertEquals(0.0, plan[0].first, 1e-9)
        assertEquals(3.0, plan[1].first, 1e-9)
        assertEquals(0.0, plan[1].second, 1e-9)
        assertEquals(2.0, kotlin.math.abs(plan[2].second), 1e-9)
    }

    @Test fun rayPlane() {
        val hit = SpatialMath.rayPlane(Vec3(0.0, 1.5, 0.0), Vec3(0.0, -1.0, -1.0).normalized(), Vec3.ZERO, Vec3(0.0, 1.0, 0.0))!!
        assertEquals(-1.5, hit.z, 1e-9)
        assertNull(SpatialMath.rayPlane(Vec3(0.0, 1.5, 0.0), Vec3(0.0, 1.0, 0.0), Vec3.ZERO, Vec3(0.0, 1.0, 0.0)))
    }
}

class AntiShakeTest {
    @Test fun lockIgnoresTheJoltAtThePress() {
        val s = StabilizedSampler(windowMs = 450, ignoreLastMs = 70)
        // Steady on x=2.0 for 400 ms, then the button press jolts to x=2.3 in the last 50 ms.
        for (t in 0L..400L step 16) s.add(t, Vec3(2.0 + (t % 3) * 0.0005, 0.0, 0.0), 0.004)
        for (t in 410L..450L step 16) s.add(t, Vec3(2.3, 0.0, 0.0), 0.004)
        val locked = s.lock(450)!!
        assertEquals(2.0, locked.point.x, 0.002)
        assertEquals(0.004, locked.accuracyM, 1e-12)
    }

    @Test fun jitterFilterHoldsBelowOneMillimetre() {
        val f = JitterFilter(0.001)
        assertEquals(2.4200, f.filter(2.4200), 0.0)
        assertEquals(2.4200, f.filter(2.4204), 0.0)
        assertEquals(2.4212, f.filter(2.4212), 0.0)
    }
}

class AngleGuardTest {
    @Test fun jitterSizedArmsGiveNoAngle() {
        val s = MeasurementSession().withMode(MeasureMode.ANGLE)
            .add(MeasurePoint(1, Vec3(0.001, 0.0, 0.0), 0.01))
            .add(MeasurePoint(2, Vec3(0.0, 0.0, 0.0), 0.01))
        assertNull(s.angleDeg(Vec3(0.0, 0.0, 0.003)))
    }
}
