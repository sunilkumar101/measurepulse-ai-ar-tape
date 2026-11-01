package com.measurepulse.ar.domain.math

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToLong

/** Units a single length can be shown in. All values are stored internally in meters. */
enum class LengthUnit(val symbol: String) {
    METERS("m"),
    CENTIMETERS("cm"),
    MILLIMETERS("mm"),
    INCHES("in"),
    FEET_DECIMAL("ft"),
    /** Architectural feet + fractional inches, e.g. 7' 11 1/4". */
    FEET_INCHES("ft-in"),
}

enum class AreaUnit(val symbol: String) { SQUARE_METERS("m²"), SQUARE_FEET("sq.ft") }

enum class VolumeUnit(val symbol: String) { CUBIC_METERS("m³"), CUBIC_FEET("cu.ft") }

/** Smallest inch fraction used when rounding architectural lengths. */
enum class FractionPrecision(val denominator: Int, val label: String) {
    EIGHTH(8, "1/8\""),
    SIXTEENTH(16, "1/16\""),
    THIRTY_SECOND(32, "1/32\""),
}

enum class UnitSystem { METRIC, IMPERIAL }

/** A length split into architectural parts. [numerator]/[denominator] is already reduced (e.g. 4/16 -> 1/4). */
data class FeetInches(
    val negative: Boolean,
    val feet: Long,
    val inches: Int,
    val numerator: Int,
    val denominator: Int,
)

/**
 * Precision unit engine: the single source of truth for converting and formatting
 * lengths, areas and volumes. Values always go in as SI (meters, m², m³).
 *
 * Numbers are joined to their unit with a non-breaking space so a value never wraps
 * away from its unit in the UI.
 */
object DimensionFormatter {

    const val METERS_PER_INCH = 0.0254
    const val METERS_PER_FOOT = 0.3048
    const val SQ_FT_PER_SQ_M = 1.0 / (METERS_PER_FOOT * METERS_PER_FOOT)
    const val CU_FT_PER_CU_M = 1.0 / (METERS_PER_FOOT * METERS_PER_FOOT * METERS_PER_FOOT)

    /** Non-breaking space between a value and its unit. */
    const val NBSP = ' '

    // ---------- Conversion ----------

    fun fromMeters(meters: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.METERS -> meters
        LengthUnit.CENTIMETERS -> meters * 100.0
        LengthUnit.MILLIMETERS -> meters * 1000.0
        LengthUnit.INCHES -> meters / METERS_PER_INCH
        LengthUnit.FEET_DECIMAL, LengthUnit.FEET_INCHES -> meters / METERS_PER_FOOT
    }

    fun toMeters(value: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.METERS -> value
        LengthUnit.CENTIMETERS -> value / 100.0
        LengthUnit.MILLIMETERS -> value / 1000.0
        LengthUnit.INCHES -> value * METERS_PER_INCH
        LengthUnit.FEET_DECIMAL, LengthUnit.FEET_INCHES -> value * METERS_PER_FOOT
    }

    fun areaFromSquareMeters(sqMeters: Double, unit: AreaUnit): Double = when (unit) {
        AreaUnit.SQUARE_METERS -> sqMeters
        AreaUnit.SQUARE_FEET -> sqMeters * SQ_FT_PER_SQ_M
    }

    fun volumeFromCubicMeters(cuMeters: Double, unit: VolumeUnit): Double = when (unit) {
        VolumeUnit.CUBIC_METERS -> cuMeters
        VolumeUnit.CUBIC_FEET -> cuMeters * CU_FT_PER_CU_M
    }

    // ---------- Length formatting ----------

    /**
     * Formats [meters] in [unit].
     * - m: 2 decimals (2.42 m) · cm: 1 decimal (242.0 cm) · mm: 1 decimal (2425.0 mm)
     * - in / ft: 2 decimals · ft-in: architectural with [precision] (7' 11 1/4")
     */
    fun formatLength(
        meters: Double,
        unit: LengthUnit,
        precision: FractionPrecision = FractionPrecision.SIXTEENTH,
    ): String = when (unit) {
        LengthUnit.METERS -> withUnit(round(meters, 2), unit.symbol)
        LengthUnit.CENTIMETERS -> withUnit(round(fromMeters(meters, unit), 1), unit.symbol)
        LengthUnit.MILLIMETERS -> withUnit(round(fromMeters(meters, unit), 1), unit.symbol)
        LengthUnit.INCHES -> withUnit(round(fromMeters(meters, unit), 2), unit.symbol)
        LengthUnit.FEET_DECIMAL -> withUnit(round(fromMeters(meters, unit), 2), unit.symbol)
        LengthUnit.FEET_INCHES -> formatFeetInches(meters, precision)
    }

    /** Default length display for a unit system: meters (metric) or feet + fractional inches (imperial). */
    fun formatLength(
        meters: Double,
        system: UnitSystem,
        precision: FractionPrecision = FractionPrecision.SIXTEENTH,
    ): String = formatLength(
        meters,
        if (system == UnitSystem.METRIC) LengthUnit.METERS else LengthUnit.FEET_INCHES,
        precision,
    )

    /** Splits [meters] into feet, whole inches and a reduced fraction rounded to [precision]. */
    fun toFeetInches(meters: Double, precision: FractionPrecision = FractionPrecision.SIXTEENTH): FeetInches {
        val d = precision.denominator
        val totalInches = abs(meters) / METERS_PER_INCH
        // Round to the nearest 1/d inch; a tiny epsilon absorbs binary floating-point error (e.g. 95.2499999).
        val units = (totalInches * d + 1e-9).roundToLong()
        val unitsPerFoot = 12L * d
        val feet = units / unitsPerFoot
        val rem = units % unitsPerFoot
        val inches = (rem / d).toInt()
        var num = (rem % d).toInt()
        var den = d
        if (num != 0) {
            val g = gcd(num, den)
            num /= g
            den /= g
        }
        return FeetInches(negative = meters < 0 && units != 0L, feet = feet, inches = inches, numerator = num, denominator = den)
    }

    /** Architectural string, e.g. 7' 11 1/4", 6' 0", 5 1/2" (under one foot), -2' 3". */
    fun formatFeetInches(meters: Double, precision: FractionPrecision = FractionPrecision.SIXTEENTH): String {
        val fi = toFeetInches(meters, precision)
        val sign = if (fi.negative) "-" else ""
        val inchPart = buildString {
            val onlyFraction = fi.feet == 0L && fi.inches == 0 && fi.numerator != 0
            if (!onlyFraction) append(fi.inches)
            if (fi.numerator != 0) {
                if (!onlyFraction) append(' ')
                append(fi.numerator).append('/').append(fi.denominator)
            }
            append('"')
        }
        return if (fi.feet == 0L) "$sign$inchPart" else "$sign${fi.feet}'$NBSP$inchPart"
    }

    // ---------- Area / volume formatting ----------

    /** m² with 1 decimal (24.8 m²), sq.ft with 1 decimal (266.9 sq.ft). */
    fun formatArea(sqMeters: Double, unit: AreaUnit): String =
        withUnit(round(areaFromSquareMeters(sqMeters, unit), 1), unit.symbol)

    fun formatArea(sqMeters: Double, system: UnitSystem): String =
        formatArea(sqMeters, if (system == UnitSystem.METRIC) AreaUnit.SQUARE_METERS else AreaUnit.SQUARE_FEET)

    /** m³ with 2 decimals (12.35 m³), cu.ft with 1 decimal (436.1 cu.ft). */
    fun formatVolume(cuMeters: Double, unit: VolumeUnit): String = when (unit) {
        VolumeUnit.CUBIC_METERS -> withUnit(round(cuMeters, 2), unit.symbol)
        VolumeUnit.CUBIC_FEET -> withUnit(round(volumeFromCubicMeters(cuMeters, unit), 1), unit.symbol)
    }

    fun formatVolume(cuMeters: Double, system: UnitSystem): String =
        formatVolume(cuMeters, if (system == UnitSystem.METRIC) VolumeUnit.CUBIC_METERS else VolumeUnit.CUBIC_FEET)

    /** Compact "W × L" footprint, e.g. 5.80 m × 4.25 m, or 19' 0 3/8" × 13' 11 5/16". */
    fun formatFootprint(widthM: Double, lengthM: Double, system: UnitSystem): String =
        "${formatLength(widthM, system)} × ${formatLength(lengthM, system)}"

    // ---------- Parsing ----------

    /**
     * Parses user input like `7' 11 1/4"`, `7'11"`, `95.25"`, `7.5'` or `11 1/4` (inches) into meters.
     * Returns null when the text is not a valid length.
     */
    fun parseFeetInches(text: String): Double? {
        val s = text.trim().replace('’', '\'').replace('”', '"').replace("''", "\"")
        if (s.isEmpty()) return null
        val negative = s.startsWith("-")
        val body = s.removePrefix("-").trim()
        val regex = Regex("""^(?:(\d+(?:\.\d+)?)\s*')?\s*(?:(\d+(?:\.\d+)?)?(?:\s+(\d+)/(\d+)|\s*(\d+)/(\d+))?\s*"?)?$""")
        val m = regex.matchEntire(body) ?: return null
        val (ft, inch, n1, d1, n2, d2) = m.destructured
        if (ft.isEmpty() && inch.isEmpty() && n1.isEmpty() && n2.isEmpty()) return null
        val num = (n1.ifEmpty { n2 }).toDoubleOrNull()
        val den = (d1.ifEmpty { d2 }).toDoubleOrNull()
        if (den != null && den == 0.0) return null
        val totalInches = (ft.toDoubleOrNull() ?: 0.0) * 12.0 +
            (inch.toDoubleOrNull() ?: 0.0) +
            (if (num != null && den != null) num / den else 0.0)
        val meters = totalInches * METERS_PER_INCH
        return if (negative) -meters else meters
    }

    // ---------- Helpers ----------

    private fun withUnit(value: String, symbol: String) = "$value$NBSP$symbol"

    /** Half-up rounding on the decimal representation, so 2.425 -> 2.43 (not 2.42 from binary error). */
    private fun round(value: Double, decimals: Int): String {
        val bd = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP)
        // Avoid printing "-0.00".
        return if (bd.signum() == 0) bd.abs().toPlainString() else bd.toPlainString()
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
