package com.measurepulse.ar.ads

/**
 * Frequency cap for full-screen ads. An interstitial may only show when at least
 * [INTERSTITIAL_COOLDOWN_MS] has passed since the last full-screen ad of ANY kind —
 * so a user who just watched a rewarded video never gets an interstitial straight after it.
 */
object AdPacing {
    const val INTERSTITIAL_COOLDOWN_MS = 180_000L

    fun interstitialAllowed(nowMs: Long, lastFullScreenMs: Long): Boolean = when {
        lastFullScreenMs <= 0L -> true
        nowMs < lastFullScreenMs -> true // the clock moved backwards; don't lock ads out forever
        else -> nowMs - lastFullScreenMs >= INTERSTITIAL_COOLDOWN_MS
    }

    /** Seconds left before the next interstitial is allowed (0 when allowed). */
    fun secondsUntilAllowed(nowMs: Long, lastFullScreenMs: Long): Long =
        if (interstitialAllowed(nowMs, lastFullScreenMs)) 0
        else (INTERSTITIAL_COOLDOWN_MS - (nowMs - lastFullScreenMs) + 999) / 1000
}
