package com.measurepulse.ar.domain.advisor

import com.measurepulse.ar.domain.floorplan.FloorPlan
import com.measurepulse.ar.domain.floorplan.OpeningType
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Rule-based material & appliance estimates from a floor plan (used by "AI Fit Check" and the PDF).
 * All rates are standard trade rules of thumb and are shown to users as estimates.
 */
object RoomEstimator {

    const val TILE_WASTE = 0.10
    /** Typical interior emulsion coverage per litre per coat. */
    const val PAINT_M2_PER_L = 10.0
    const val PAINT_COATS = 2
    /** Cooling load rule of thumb: 20 BTU/h per sq.ft at a 2.4 m (8 ft) ceiling. */
    const val BTU_PER_SQFT = 20.0
    const val SQFT_PER_SQM = 10.763910416709722
    val TV_SIZES = listOf(32, 43, 50, 55, 65, 75, 85, 98)

    data class Tiles(val sizeCm: Int, val count: Int, val boxes: Int)
    data class Paint(val litres: Double, val wallAreaSqM: Double)
    data class Cooling(val btu: Int, val tons: Double)
    data class Tv(val viewingDistanceM: Double, val diagonalIn: Int)
    data class FurnitureFit(val name: String, val fits: Boolean, val note: String)

    data class Estimates(
        val tiles: Tiles,
        val paint: Paint,
        val cooling: Cooling,
        val tv: Tv,
        val baseboardM: Double,
        val furniture: List<FurnitureFit>,
    )

    fun estimate(plan: FloorPlan, tileCm: Int = 60): Estimates = Estimates(
        tiles = tiles(plan.areaSqM, tileCm),
        paint = paint(plan.netWallAreaSqM),
        cooling = cooling(plan.areaSqM, plan.ceilingM),
        tv = tv(plan),
        baseboardM = (plan.perimeterM - plan.openings.filter { it.type == OpeningType.DOOR }.sumOf { it.widthM }).coerceAtLeast(0.0),
        furniture = furniture(plan),
    )

    fun tiles(areaSqM: Double, tileCm: Int): Tiles {
        val tileArea = (tileCm / 100.0) * (tileCm / 100.0)
        val count = ceil(areaSqM * (1 + TILE_WASTE) / tileArea - 1e-9).toInt()
        val perBox = when { tileCm >= 80 -> 2; tileCm >= 60 -> 4; else -> 10 }
        return Tiles(tileCm, count, ceil(count / perBox.toDouble()).toInt())
    }

    fun paint(netWallSqM: Double) = Paint(
        litres = (netWallSqM * PAINT_COATS / PAINT_M2_PER_L * 10).roundToInt() / 10.0,
        wallAreaSqM = netWallSqM,
    )

    /** Taller ceilings hold more air: scale linearly above 2.4 m. Rounded up to the next 1,000 BTU. */
    fun cooling(areaSqM: Double, ceilingM: Double): Cooling {
        val heightFactor = (ceilingM / 2.4).coerceAtLeast(1.0)
        val btu = (ceil(areaSqM * SQFT_PER_SQM * BTU_PER_SQFT * heightFactor / 1000.0) * 1000).toInt()
        return Cooling(btu, (btu / 12000.0 * 4).roundToInt() / 4.0) // nearest 0.25 ton
    }

    /** Seating ~75% of the room's shorter side from the TV; size from [RoomAdvisor.tv] (SMPTE 30°–THX 36°). */
    fun tv(plan: FloorPlan): Tv {
        val distance = (min(plan.widthM, plan.depthM) * 0.75 * 100).roundToInt() / 100.0
        return Tv(distance, RoomAdvisor.tv(distance, plan.ceilingM).recommendedIn)
    }

    /** Does each common item fit with a 60 cm walkway around it? */
    fun furniture(plan: FloorPlan): List<FurnitureFit> {
        val long = maxOf(plan.widthM, plan.depthM)
        val short = min(plan.widthM, plan.depthM)
        fun check(name: String, w: Double, d: Double, walkway: Double = 0.6): FurnitureFit {
            val fits = w + walkway <= long && d + walkway <= short
            val spare = min(long - w, short - d)
            return FurnitureFit(name, fits, if (fits) "%.2f m spare".format(java.util.Locale.US, spare) else "Needs more space")
        }
        return listOf(
            check("King bed", 1.93, 2.03),
            check("Queen bed", 1.60, 2.03),
            check("3-seater sofa", 2.10, 0.92),
            check("6-seat dining table", 1.80, 0.90, walkway = 1.2),
        )
    }
}
