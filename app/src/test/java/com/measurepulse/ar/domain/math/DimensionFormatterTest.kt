package com.measurepulse.ar.domain.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DimensionFormatterTest {

    private val nb = DimensionFormatter.NBSP
    private fun inchesToMeters(inches: Double) = inches * DimensionFormatter.METERS_PER_INCH

    // ---- Metric ----

    @Test fun meters_twoDecimals() {
        assertEquals("2.42${nb}m", DimensionFormatter.formatLength(2.4249, LengthUnit.METERS))
        assertEquals("2.43${nb}m", DimensionFormatter.formatLength(2.425, LengthUnit.METERS))
        assertEquals("0.00${nb}m", DimensionFormatter.formatLength(0.0, LengthUnit.METERS))
        assertEquals("0.00${nb}m", DimensionFormatter.formatLength(-0.001, LengthUnit.METERS))
    }

    @Test fun centimeters_oneDecimal() {
        assertEquals("242.0${nb}cm", DimensionFormatter.formatLength(2.42, LengthUnit.CENTIMETERS))
        assertEquals("242.5${nb}cm", DimensionFormatter.formatLength(2.425, LengthUnit.CENTIMETERS))
    }

    @Test fun millimeters_oneDecimal() {
        assertEquals("2425.0${nb}mm", DimensionFormatter.formatLength(2.425, LengthUnit.MILLIMETERS))
        assertEquals("1.5${nb}mm", DimensionFormatter.formatLength(0.0015, LengthUnit.MILLIMETERS))
    }

    // ---- Imperial decimal ----

    @Test fun decimalFeet() {
        assertEquals("10.00${nb}ft", DimensionFormatter.formatLength(3.048, LengthUnit.FEET_DECIMAL))
        assertEquals("7.94${nb}ft", DimensionFormatter.formatLength(2.42, LengthUnit.FEET_DECIMAL))
    }

    @Test fun decimalInches() {
        assertEquals("12.00${nb}in", DimensionFormatter.formatLength(0.3048, LengthUnit.INCHES))
    }

    // ---- Architectural feet & fractional inches ----

    @Test fun feetInches_quarterInch() {
        val m = inchesToMeters(95.25) // 7' 11 1/4"
        assertEquals("7'$nb" + "11 1/4\"", DimensionFormatter.formatLength(m, LengthUnit.FEET_INCHES))
    }

    @Test fun feetInches_fractionIsReduced() {
        val fi = DimensionFormatter.toFeetInches(inchesToMeters(10.5), FractionPrecision.THIRTY_SECOND)
        assertEquals(FeetInches(false, 0, 10, 1, 2), fi)
    }

    @Test fun feetInches_eighthPrecisionRounds() {
        // 0.20" rounds to the nearest 1/8" -> 1/4"
        assertEquals("1 1/4\"", DimensionFormatter.formatFeetInches(inchesToMeters(1.20), FractionPrecision.EIGHTH))
        // 0.06" is closer to 0 than to 1/8"
        assertEquals("2\"", DimensionFormatter.formatFeetInches(inchesToMeters(2.06), FractionPrecision.EIGHTH))
    }

    @Test fun feetInches_sixteenthAndThirtySecond() {
        assertEquals("3 5/16\"", DimensionFormatter.formatFeetInches(inchesToMeters(3.3125), FractionPrecision.SIXTEENTH))
        assertEquals("3 11/32\"", DimensionFormatter.formatFeetInches(inchesToMeters(3.34375), FractionPrecision.THIRTY_SECOND))
        // 3.36" at 1/16 precision rounds up to 3/8"
        assertEquals("3 3/8\"", DimensionFormatter.formatFeetInches(inchesToMeters(3.36), FractionPrecision.SIXTEENTH))
    }

    @Test fun feetInches_carriesIntoNextFoot() {
        // 11.99" at 1/8" precision -> exactly 1' 0"
        assertEquals("1'$nb" + "0\"", DimensionFormatter.formatFeetInches(inchesToMeters(11.99), FractionPrecision.EIGHTH))
    }

    @Test fun feetInches_wholeFeetAndNegative() {
        assertEquals("6'$nb" + "0\"", DimensionFormatter.formatFeetInches(1.8288))
        assertEquals("-2'$nb" + "3\"", DimensionFormatter.formatFeetInches(-inchesToMeters(27.0)))
    }

    @Test fun feetInches_fractionOnly() {
        assertEquals("1/16\"", DimensionFormatter.formatFeetInches(inchesToMeters(0.0625)))
        assertEquals("0\"", DimensionFormatter.formatFeetInches(0.0))
    }

    @Test fun unitSystemDefaults() {
        assertEquals("5.80${nb}m", DimensionFormatter.formatLength(5.8, UnitSystem.METRIC))
        assertEquals("6'$nb" + "0\"", DimensionFormatter.formatLength(1.8288, UnitSystem.IMPERIAL))
    }

    // ---- Area & volume ----

    @Test fun area() {
        assertEquals("24.8${nb}m²", DimensionFormatter.formatArea(24.8, AreaUnit.SQUARE_METERS))
        assertEquals("266.9${nb}sq.ft", DimensionFormatter.formatArea(24.8, AreaUnit.SQUARE_FEET))
        assertEquals("10.8${nb}sq.ft", DimensionFormatter.formatArea(1.0, UnitSystem.IMPERIAL))
    }

    @Test fun volume() {
        assertEquals("12.35${nb}m³", DimensionFormatter.formatVolume(12.3456, VolumeUnit.CUBIC_METERS))
        assertEquals("35.3${nb}cu.ft", DimensionFormatter.formatVolume(1.0, VolumeUnit.CUBIC_FEET))
        assertEquals("1.0${nb}cu.ft", DimensionFormatter.formatVolume(0.0283168466, UnitSystem.IMPERIAL))
    }

    // ---- Conversions round-trip ----

    @Test fun conversionsRoundTrip() {
        for (unit in LengthUnit.entries) {
            val back = DimensionFormatter.toMeters(DimensionFormatter.fromMeters(2.4321, unit), unit)
            assertEquals(unit.name, 2.4321, back, 1e-12)
        }
    }

    // ---- Parsing ----

    @Test fun parse() {
        assertEquals(inchesToMeters(95.25), DimensionFormatter.parseFeetInches("7' 11 1/4\"")!!, 1e-9)
        assertEquals(inchesToMeters(95.0), DimensionFormatter.parseFeetInches("7'11\"")!!, 1e-9)
        assertEquals(inchesToMeters(95.25), DimensionFormatter.parseFeetInches("95.25\"")!!, 1e-9)
        assertEquals(inchesToMeters(90.0), DimensionFormatter.parseFeetInches("7.5'")!!, 1e-9)
        assertEquals(inchesToMeters(11.25), DimensionFormatter.parseFeetInches("11 1/4")!!, 1e-9)
        assertEquals(inchesToMeters(0.5), DimensionFormatter.parseFeetInches("1/2\"")!!, 1e-9)
        assertEquals(-inchesToMeters(27.0), DimensionFormatter.parseFeetInches("-2' 3\"")!!, 1e-9)
        assertNull(DimensionFormatter.parseFeetInches("abc"))
        assertNull(DimensionFormatter.parseFeetInches(""))
        assertNull(DimensionFormatter.parseFeetInches("1/0"))
    }
}
