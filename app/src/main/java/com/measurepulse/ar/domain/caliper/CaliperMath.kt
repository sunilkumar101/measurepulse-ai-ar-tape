package com.measurepulse.ar.domain.caliper

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** ISO metric coarse threads: nominal diameter, pitch and hex across-flats (ISO 4032 / 4017). */
data class IsoThread(val name: String, val diameterMm: Double, val pitchMm: Double, val hexAfMm: Double)

data class BoltMatch(val thread: IsoThread, val kind: Kind, val errorMm: Double) {
    enum class Kind { SHANK, HEX_HEAD }

    val label: String
        get() = when (kind) {
            Kind.SHANK -> "${thread.name} bolt (Ø ${fmt(thread.diameterMm)} mm)"
            Kind.HEX_HEAD -> "${thread.name} hex head · ${fmt(thread.hexAfMm)} mm wrench"
        }

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}

/**
 * On-screen caliper maths. The screen is used at true scale:
 *   px per mm = (dpi / 25.4) × calibration factor
 * where the factor comes from aligning the jaws to a credit card (ISO/IEC 7810 ID-1: 85.60 mm).
 */
object CaliperMath {

    const val CARD_LONG_MM = 85.60
    const val CARD_SHORT_MM = 53.98
    const val MM_PER_INCH = 25.4

    val ISO_THREADS = listOf(
        IsoThread("M3", 3.0, 0.5, 5.5),
        IsoThread("M4", 4.0, 0.7, 7.0),
        IsoThread("M5", 5.0, 0.8, 8.0),
        IsoThread("M6", 6.0, 1.0, 10.0),
        IsoThread("M8", 8.0, 1.25, 13.0),
        IsoThread("M10", 10.0, 1.5, 16.0),
        IsoThread("M12", 12.0, 1.75, 18.0),
        IsoThread("M14", 14.0, 2.0, 21.0),
        IsoThread("M16", 16.0, 2.0, 24.0),
        IsoThread("M18", 18.0, 2.5, 27.0),
        IsoThread("M20", 20.0, 2.5, 30.0),
        IsoThread("M22", 22.0, 2.5, 34.0),
        IsoThread("M24", 24.0, 3.0, 36.0),
    )

    /** Thread-pitch gauge comb options. */
    val GAUGE_PITCHES = listOf(0.5, 0.7, 0.8, 1.0, 1.25, 1.5, 1.75, 2.0)

    fun pxPerMm(dpi: Float, calibration: Double): Double = dpi / MM_PER_INCH * calibration

    /**
     * New calibration factor after the jaws were set to a known [referenceMm] spanning [measuredPx].
     * The real pixels-per-mm is measuredPx / referenceMm, so the factor is that over the DPI-reported value.
     */
    fun calibrationFactor(measuredPx: Float, dpi: Float, referenceMm: Double = CARD_LONG_MM): Double =
        rawCalibration(measuredPx, dpi, referenceMm).coerceIn(MIN_CALIBRATION, MAX_CALIBRATION)

    /** False when the jaws clearly aren't on the card edges (a real screen is never off by more than 30 %). */
    fun isPlausibleCalibration(measuredPx: Float, dpi: Float, referenceMm: Double): Boolean =
        rawCalibration(measuredPx, dpi, referenceMm) in MIN_CALIBRATION..MAX_CALIBRATION

    private fun rawCalibration(measuredPx: Float, dpi: Float, referenceMm: Double) =
        measuredPx / (dpi / MM_PER_INCH) / referenceMm

    const val MIN_CALIBRATION = 0.7
    const val MAX_CALIBRATION = 1.3

    fun inches(mm: Double) = mm / MM_PER_INCH

    /** Nearest fraction of an inch with the given denominator, reduced: 24.25 mm → "61/64\"", 30 mm → "1 3/16\"". */
    fun fractionalInch(mm: Double, denominator: Int = 64): String {
        val units = (inches(mm) * denominator).roundToInt()
        val whole = units / denominator
        var num = units % denominator
        var den = denominator
        if (num != 0) {
            val g = gcd(num, den); num /= g; den /= g
        }
        return when {
            num == 0 -> "$whole\""
            whole == 0 -> "$num/$den\""
            else -> "$whole $num/$den\""
        }
    }

    /**
     * All ISO matches within tolerance, closest first: the gap may be a bolt shank diameter
     * (±0.35 mm — threads measure slightly under nominal) or a hex head across flats (±0.4 mm).
     * Several can be plausible (8 mm = M8 shank or M5 hex head), so the UI shows them all.
     * Ties (within 0.05 mm) list the shank first.
     */
    fun boltMatches(mm: Double): List<BoltMatch> =
        ISO_THREADS.flatMap { t ->
            listOf(
                BoltMatch(t, BoltMatch.Kind.SHANK, abs(mm - t.diameterMm)),
                BoltMatch(t, BoltMatch.Kind.HEX_HEAD, abs(mm - t.hexAfMm)),
            )
        }.filter { it.errorMm <= if (it.kind == BoltMatch.Kind.SHANK) 0.35 else 0.4 }
            .sortedWith(compareBy<BoltMatch> { (it.errorMm / 0.05).toInt() }.thenBy { it.kind.ordinal })

    /** Which standard thread uses this pitch (for the gauge label). */
    fun threadsForPitch(pitchMm: Double): List<String> = ISO_THREADS.filter { abs(it.pitchMm - pitchMm) < 1e-6 }.map { it.name }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
