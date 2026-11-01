package com.measurepulse.ar.data.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SensorFloorMathTest {
    /** Rotation about world X by [deg] (phone tilting up from face-down). */
    private fun rotX(deg: Double): FloatArray {
        val a = Math.toRadians(deg); val c = cos(a).toFloat(); val s = sin(a).toFloat()
        return floatArrayOf(1f, 0f, 0f, 0f, c, -s, 0f, s, c)
    }

    @Test fun pointingStraightDownHitsBelowFeet() {
        val hit = SensorFloorMath.floorHit(rotX(0.0), 1.5)!!
        assertEquals(0.0, hit.x, 1e-6); assertEquals(0.0, hit.y, 1e-6)
    }

    @Test fun fortyFiveDegreesHitsAtCameraHeight() {
        val hit = SensorFloorMath.floorHit(rotX(45.0), 1.5)!!
        assertEquals(1.5, hit.y, 1e-4)
    }

    @Test fun aboveHorizonHasNoHit() {
        assertNull(SensorFloorMath.floorHit(rotX(95.0), 1.5))
    }

    @Test fun reticleProjectsToScreenCentre() {
        val r = rotX(50.0)
        val hit = SensorFloorMath.floorHit(r, 1.5)!!
        val p = SensorFloorMath.project(r, SensorFloorMath.camera(1.5), hit, 800f, 360f, 800f)
        assertNotNull(p)
        assertEquals(360f, p!!.x, 0.5f); assertEquals(800f, p.y, 0.5f)
    }

    @Test fun pointsFurtherAwayAppearHigherOnScreen() {
        val r = rotX(50.0)
        val cam = SensorFloorMath.camera(1.5)
        val near = SensorFloorMath.project(r, cam, Vec3(0.0, 1.0, 0.0), 800f, 360f, 800f)!!
        val far = SensorFloorMath.project(r, cam, Vec3(0.0, 3.0, 0.0), 800f, 360f, 800f)!!
        assertTrue(far.y < near.y)
    }

    @Test fun accuracyGrowsWithDistance() {
        val close = SensorFloorMath.accuracyAt(rotX(30.0), 1.5)
        val far = SensorFloorMath.accuracyAt(rotX(70.0), 1.5)
        assertTrue(close < 0.02)
        assertTrue(far > close)
    }
}
