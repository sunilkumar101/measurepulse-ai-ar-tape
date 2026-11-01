package com.measurepulse.ar.data.ar

/**
 * Anti-shake point locking.
 *
 * Pressing a button (or a volume key) jolts the phone at the exact moment of the press, so the
 * reticle position *at* the trigger is the least trustworthy one. This keeps a short history of
 * reticle hits and, on trigger, returns the per-axis median of the samples taken shortly
 * *before* the press — the point the user was actually holding steady on.
 */
class StabilizedSampler(
    private val windowMs: Long = 450,
    private val ignoreLastMs: Long = 70,
    private val capacity: Int = 90,
) {
    private data class Sample(val t: Long, val p: Vec3, val accuracy: Double)

    private val samples = ArrayDeque<Sample>()

    fun add(timeMs: Long, point: Vec3, accuracyM: Double) {
        samples.addLast(Sample(timeMs, point, accuracyM))
        while (samples.size > capacity) samples.removeFirst()
        while (samples.isNotEmpty() && timeMs - samples.first().t > windowMs * 4) samples.removeFirst()
    }

    fun clear() = samples.clear()

    /** Result of a lock: the stabilised point, its accuracy and how much the hand moved (spread). */
    data class Locked(val point: Vec3, val accuracyM: Double, val spreadM: Double)

    fun lock(triggerTimeMs: Long): Locked? {
        if (samples.isEmpty()) return null
        var window = samples.filter { it.t in (triggerTimeMs - windowMs)..(triggerTimeMs - ignoreLastMs) }
        if (window.size < 3) window = samples.toList().takeLast(5)
        val median = Vec3(
            median(window.map { it.p.x }),
            median(window.map { it.p.y }),
            median(window.map { it.p.z }),
        )
        val spread = window.maxOf { it.p.distanceTo(median) }
        val accuracy = median(window.map { it.accuracy })
        return Locked(median, accuracy, spread)
    }

    private fun median(values: List<Double>): Double {
        val s = values.sorted()
        val m = s.size / 2
        return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2.0
    }
}

/** Holds a displayed value steady until it changes by at least [thresholdM] (default 1 mm). */
class JitterFilter(private val thresholdM: Double = 0.001) {
    private var shown: Double? = null

    fun filter(value: Double): Double {
        val current = shown
        if (current == null || kotlin.math.abs(value - current) >= thresholdM) shown = value
        return shown!!
    }

    fun reset() { shown = null }
}

/** Exponential smoothing of the live reticle point to calm sensor noise without adding lag. */
class PointSmoother(private val alpha: Double = 0.35, private val snapDistanceM: Double = 0.25) {
    private var last: Vec3? = null

    fun smooth(p: Vec3?): Vec3? {
        if (p == null) { last = null; return null }
        val prev = last
        // Large jumps (moved to another surface) snap instantly instead of sliding across the room.
        val next = if (prev == null || prev.distanceTo(p) > snapDistanceM) p else prev + (p - prev) * alpha
        last = next
        return next
    }
}
