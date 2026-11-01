package com.measurepulse.ar.domain.floorplan

import java.util.Locale

/**
 * Tiny JSON-array codec for plans (no org.json so it runs in plain JVM unit tests).
 * - corners:  [[x,y],…]
 * - openings: [[wall,type,offset,width,height,flags],…]  type 0=door 1=window; flags bit0=hingeAtStart, bit1=swingIn
 */
object FloorPlanCodec {

    fun encodeCorners(corners: List<PlanPoint>): String =
        corners.joinToString(",", "[", "]") { "[${n(it.x)},${n(it.y)}]" }

    fun decodeCorners(json: String?): List<PlanPoint>? =
        parseRows(json)?.filter { it.size >= 2 }?.map { PlanPoint(it[0], it[1]) }?.takeIf { it.size >= 3 }

    fun encodeOpenings(openings: List<Opening>): String =
        openings.joinToString(",", "[", "]") { o ->
            val flags = (if (o.hingeAtStart) 1 else 0) or (if (o.swingIn) 2 else 0)
            "[${o.wallIndex},${if (o.type == OpeningType.DOOR) 0 else 1},${n(o.offsetM)},${n(o.widthM)},${n(o.heightM)},$flags]"
        }

    fun decodeOpenings(json: String?): List<Opening> =
        parseRows(json)?.filter { it.size >= 6 }?.map { r ->
            val flags = r[5].toInt()
            Opening(
                wallIndex = r[0].toInt(),
                type = if (r[1].toInt() == 0) OpeningType.DOOR else OpeningType.WINDOW,
                offsetM = r[2], widthM = r[3], heightM = r[4],
                hingeAtStart = flags and 1 != 0, swingIn = flags and 2 != 0,
            )
        } ?: emptyList()

    /** Parses `[[1,2],[3,4.5]]` into rows of doubles; null when malformed. */
    internal fun parseRows(json: String?): List<List<Double>>? {
        val s = json?.trim() ?: return null
        if (!s.startsWith("[") || !s.endsWith("]")) return null
        val inner = s.substring(1, s.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        return Regex("""\[([^\[\]]*)]""").findAll(inner).map { m ->
            m.groupValues[1].split(',').mapNotNull { it.trim().toDoubleOrNull() }
        }.toList()
    }

    private fun n(v: Double) = String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.').ifEmpty { "0" }
}
