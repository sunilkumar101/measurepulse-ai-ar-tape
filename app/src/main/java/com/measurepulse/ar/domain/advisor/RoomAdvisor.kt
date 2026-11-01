package com.measurepulse.ar.domain.advisor

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.tan

enum class SunExposure(val label: String, val factor: Double) {
    SHADED("Shaded", 0.90),
    NORMAL("Normal", 1.00),
    SUNNY("Sunny / West", 1.10),
}

/** Everything the advisor needs about a room. Areas in m², lengths in m. */
data class AdvisorInput(
    val roomName: String,
    val floorAreaSqM: Double,
    val perimeterM: Double,
    val ceilingM: Double,
    val doors: Int,
    val windows: Int,
    /** Exact opening area when known from a floor plan; otherwise standard sizes × counts. */
    val openingsAreaSqM: Double? = null,
    val viewingDistanceM: Double,
    val sun: SunExposure = SunExposure.NORMAL,
    val occupants: Int = 2,
    val tileCm: Int = 60,
    val tileBoxPrice: Double = 28.0,
    val paintLitrePrice: Double = 9.0,
) {
    val volumeCuM: Double get() = floorAreaSqM * ceilingM
    val grossWallSqM: Double get() = perimeterM * ceilingM
    val openingsSqM: Double get() = openingsAreaSqM ?: (doors * DOOR_SQM + windows * WINDOW_SQM)
    val netWallSqM: Double get() = (grossWallSqM - openingsSqM).coerceAtLeast(0.0)

    companion object {
        const val DOOR_SQM = 0.90 * 2.10
        const val WINDOW_SQM = 1.20 * 1.20
    }
}

data class TilePlan(
    val tileCm: Int, val tiles: Int, val wasteTiles: Double, val perBox: Int, val boxes: Int,
    val adhesiveKg: Double, val adhesiveBags: Int, val groutKg: Double, val cost: Double,
)

data class PaintCan(val litres: Double, val count: Int)

data class PaintPlan(
    val netWallSqM: Double, val litres: Double, val gallons: Double, val primerLitres: Double,
    val cans: List<PaintCan>, val cost: Double,
)

data class CoolingPlan(val btu: Int, val tons: Double, val rawBtu: Int)

data class TvPlan(
    val viewingDistanceM: Double,
    /** SMPTE 30° (minimum immersive) → THX 36° (cinema) diagonal range, inches. */
    val minDiagonalIn: Double, val maxDiagonalIn: Double,
    val recommendedIn: Int,
    /** Screen centre height for seated eye level. */
    val mountCentreM: Double,
)

data class AdvisorResult(val input: AdvisorInput, val tiles: TilePlan, val paint: PaintPlan, val cooling: CoolingPlan, val tv: TvPlan) {
    val totalCost: Double get() = tiles.cost + paint.cost
}

/**
 * Rule-based renovation & appliance advisor ("Help With AI"). Every number comes from a
 * published trade rule of thumb so results are explainable:
 *  - Tiles: +10 % cutting margin; adhesive 4 kg/m² (10 mm notch trowel, 20 kg bags);
 *    grout kg/m² = (L+W)/(L·W) · joint · depth · 1.6 (density), +10 %.
 *  - Paint: 2 coats at 10 m²/L on the net wall area; primer 1 coat at 12 m²/L.
 *  - Cooling: 20 BTU/h per sq.ft at 2.4 m ceilings × ceiling ratio × sun factor, +600 BTU/h
 *    per occupant above two, rounded up to the next standard split-AC size.
 *  - TV: SMPTE 30° and THX 36° horizontal viewing angles on a 16:9 screen.
 */
object RoomAdvisor {

    const val TILE_WASTE = 0.10
    const val ADHESIVE_KG_PER_SQM = 4.0
    const val ADHESIVE_BAG_KG = 20.0
    const val GROUT_JOINT_MM = 2.0
    const val TILE_THICKNESS_MM = 9.0
    const val PAINT_SQM_PER_L = 10.0
    const val PRIMER_SQM_PER_L = 12.0
    const val LITRES_PER_US_GALLON = 3.785
    val CAN_SIZES = listOf(10.0, 4.0, 1.0)
    val AC_SIZES_BTU = listOf(9_000, 12_000, 18_000, 24_000, 30_000, 36_000, 48_000)
    val TV_SIZES = listOf(43, 50, 55, 65, 75, 85, 98)
    private const val SQFT_PER_SQM = 10.763910416709722

    fun analyze(input: AdvisorInput) = AdvisorResult(input, tiles(input), paint(input), cooling(input), tv(input.viewingDistanceM, input.ceilingM))

    fun tilesPerBox(tileCm: Int) = when { tileCm >= 120 -> 2; tileCm >= 80 -> 3; tileCm >= 60 -> 4; tileCm >= 45 -> 6; else -> 11 }

    fun tiles(input: AdvisorInput): TilePlan {
        val area = input.floorAreaSqM
        val side = input.tileCm / 100.0
        val exact = area / (side * side)
        val tiles = ceil(exact * (1 + TILE_WASTE) - 1e-9).toInt()
        val perBox = tilesPerBox(input.tileCm)
        val boxes = ceil(tiles / perBox.toDouble()).toInt()
        val adhesive = area * ADHESIVE_KG_PER_SQM
        val mm = input.tileCm * 10.0
        val groutPerSqM = (mm + mm) / (mm * mm) * GROUT_JOINT_MM * TILE_THICKNESS_MM * 1.6
        val grout = round1(area * groutPerSqM * 1.1)
        return TilePlan(
            tileCm = input.tileCm, tiles = tiles, wasteTiles = round1(exact * TILE_WASTE), perBox = perBox, boxes = boxes,
            adhesiveKg = round1(adhesive), adhesiveBags = ceil(adhesive / ADHESIVE_BAG_KG - 1e-9).toInt(),
            groutKg = grout, cost = boxes * input.tileBoxPrice,
        )
    }

    fun paint(input: AdvisorInput): PaintPlan {
        val net = input.netWallSqM
        val litres = round1(net * 2 / PAINT_SQM_PER_L)
        return PaintPlan(
            netWallSqM = round1(net), litres = litres, gallons = round1(litres / LITRES_PER_US_GALLON),
            primerLitres = round1(net / PRIMER_SQM_PER_L), cans = cansFor(litres), cost = litres * input.paintLitrePrice,
        )
    }

    /** Cheapest-waste combination of 10 L / 4 L / 1 L cans that covers [litres] (fewest cans on ties). */
    fun cansFor(litres: Double): List<PaintCan> {
        if (litres <= 0) return emptyList()
        var best: Triple<Double, Int, IntArray>? = null
        val maxTen = ceil(litres / 10).toInt()
        for (a in 0..maxTen) for (b in 0..3) {
            val rest = litres - a * 10 - b * 4
            val c = if (rest <= 0) 0 else ceil(rest - 1e-9).toInt()
            if (c > 3) continue // 4×1 L is never better than 1×4 L
            val total = a * 10.0 + b * 4 + c
            if (total + 1e-9 < litres) continue
            val waste = total - litres
            val count = a + b + c
            if (best == null || waste < best.first - 1e-9 || (kotlin.math.abs(waste - best.first) < 1e-9 && count < best.second)) {
                best = Triple(waste, count, intArrayOf(a, b, c))
            }
        }
        val n = best!!.third
        return CAN_SIZES.mapIndexedNotNull { i, size -> if (n[i] > 0) PaintCan(size, n[i]) else null }
    }

    fun cooling(input: AdvisorInput): CoolingPlan {
        val heightFactor = (input.ceilingM / 2.4).coerceAtLeast(1.0)
        val raw = input.floorAreaSqM * SQFT_PER_SQM * 20 * heightFactor * input.sun.factor + 600 * (input.occupants - 2).coerceAtLeast(0)
        val size = AC_SIZES_BTU.firstOrNull { it >= raw } ?: AC_SIZES_BTU.last()
        return CoolingPlan(size, size / 12_000.0, raw.roundToInt())
    }

    /**
     * Horizontal viewing angle θ on a 16:9 screen: width = 2·d·tan(θ/2), diagonal = width / 0.8716.
     * Recommends the smallest standard size that reaches SMPTE 30°, capped at THX 36°.
     */
    fun tv(viewingDistanceM: Double, ceilingM: Double): TvPlan {
        fun diag(angleDeg: Double) = 2 * viewingDistanceM * tan(Math.toRadians(angleDeg / 2)) / 0.8716 / 0.0254
        val min = diag(30.0); val max = diag(36.0)
        val rec = TV_SIZES.firstOrNull { it >= min && it <= max }
            ?: TV_SIZES.lastOrNull { it <= max } ?: TV_SIZES.first()
        return TvPlan(viewingDistanceM, round1(min), round1(max), rec, if (ceilingM >= 2.0) 1.05 else 0.95)
    }

    private fun round1(v: Double) = (v * 10).roundToInt() / 10.0
}
