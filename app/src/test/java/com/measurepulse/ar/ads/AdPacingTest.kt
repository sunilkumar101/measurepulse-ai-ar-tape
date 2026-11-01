package com.measurepulse.ar.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPacingTest {
    private val t0 = 1_700_000_000_000L

    @Test fun firstEverInterstitialIsAllowed() {
        assertTrue(AdPacing.interstitialAllowed(t0, 0L))
    }

    @Test fun blockedInsideTheThreeMinuteCooldown() {
        assertFalse(AdPacing.interstitialAllowed(t0 + 1_000, t0))
        assertFalse(AdPacing.interstitialAllowed(t0 + 179_999, t0))
    }

    @Test fun allowedExactlyAtAndAfter180Seconds() {
        assertTrue(AdPacing.interstitialAllowed(t0 + 180_000, t0))
        assertTrue(AdPacing.interstitialAllowed(t0 + 3_600_000, t0))
    }

    @Test fun clockMovedBackwardsDoesNotLockAdsForever() {
        assertTrue(AdPacing.interstitialAllowed(t0 - 60_000, t0))
    }

    @Test fun secondsLeftRoundsUp() {
        assertEquals(180, AdPacing.secondsUntilAllowed(t0, t0))
        assertEquals(1, AdPacing.secondsUntilAllowed(t0 + 179_500, t0))
        assertEquals(0, AdPacing.secondsUntilAllowed(t0 + 180_000, t0))
    }
}
