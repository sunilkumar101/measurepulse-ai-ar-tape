package com.measurepulse.ar.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.measurepulse.ar.R
import com.measurepulse.ar.domain.advisor.RoomEstimator
import com.measurepulse.ar.domain.floorplan.FloorPlan
import com.measurepulse.ar.domain.floorplan.OpeningType
import com.measurepulse.ar.domain.floorplan.PlanPoint
import com.measurepulse.ar.domain.math.AreaUnit
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.LengthUnit
import com.measurepulse.ar.domain.math.VolumeUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2

/**
 * Generates a print-ready A4 "Architectural Blueprint & Room Specification" PDF with the native
 * [PdfDocument] API:
 *  - Page 1: title block, the room drawn at a true architectural scale (1:20 … 1:500) with
 *    dimension lines, doors (swing arcs) and windows, a scale bar and headline totals.
 *  - Page 2: wall-by-wall table, floor/wall area totals, and material & appliance estimates.
 * Files go to cache/exports and are shared through FileProvider.
 */
@Singleton
class BlueprintPdfExporter @Inject constructor(@ApplicationContext private val context: Context) {

    data class Meta(val title: String, val qualityLabel: String, val createdAt: Long)

    // Body text uses the system sans: the bundled static 400 weight loses its word-space width inside PdfDocument.
    private val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val sansBold: Typeface = font(R.font.plus_jakarta_sans_800, Typeface.DEFAULT_BOLD)
    private val mono: Typeface = font(R.font.jetbrains_mono_500, Typeface.MONOSPACE)
    private val monoBold: Typeface = font(R.font.jetbrains_mono_700, Typeface.MONOSPACE)

    suspend fun export(plan: FloorPlan, meta: Meta): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        try {
            page(doc, 1) { drawPageOne(it, plan, meta) }
            page(doc, 2) { drawPageTwo(it, plan, meta) }
            val dir = ExportLibrary.dir(context)
            val safe = meta.title.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(40).ifEmpty { "Room" }
            val file = File(dir, "MeasurePulse_${safe}_Blueprint.pdf")
            file.outputStream().use { doc.writeTo(it) }
            file
        } finally {
            doc.close()
        }
    }

    fun shareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension.replace('_', ' '))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share PDF blueprint").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // ---------------------------------------------------------------- page 1

    private fun drawPageOne(c: Canvas, plan: FloorPlan, meta: Meta) {
        drawHeader(c, meta, "Architectural Room Blueprint", 1)

        // Title block
        c.drawText(meta.title, M, 128f, text(sansBold, 17f, INK))
        c.drawText(
            "Area ${area(plan.areaSqM)}  ·  Perimeter ${len(plan.perimeterM)}  ·  Ceiling ${len(plan.ceilingM)}",
            M, 146f, text(sans, 9.5f, MUTED),
        )

        // Drawing frame
        val frame = RectF(M, 160f, W - M, 600f)
        c.drawRoundRect(frame, 8f, 8f, fill(PAPER))
        drawGrid(c, frame)
        c.drawRoundRect(frame, 8f, 8f, stroke(LINE, 0.8f))

        val scale = chooseScale(plan, frame)
        val ptPerM = PT_PER_MM * 1000f / scale
        val minX = plan.corners.minOf { it.x }; val minY = plan.corners.minOf { it.y }
        val ox = frame.centerX() - (plan.widthM * ptPerM / 2).toFloat()
        val oy = frame.centerY() - (plan.depthM * ptPerM / 2).toFloat()
        val map = { p: PlanPoint -> floatArrayOf(ox + ((p.x - minX) * ptPerM).toFloat(), oy + ((p.y - minY) * ptPerM).toFloat()) }

        drawPlan(c, plan, map, ptPerM.toDouble())

        // Scale bar + scale note
        val barM = if (scale <= 50) 1.0 else if (scale <= 100) 2.0 else 5.0
        val barLen = (barM * ptPerM).toFloat()
        val bx = frame.left + 16f; val by = frame.bottom - 18f
        for (i in 0 until 4) {
            val seg = barLen / 4
            c.drawRect(bx + i * seg, by - 4f, bx + (i + 1) * seg, by, fill(if (i % 2 == 0) INK else Color.WHITE))
        }
        c.drawRect(bx, by - 4f, bx + barLen, by, stroke(INK, 0.6f))
        c.drawText("0", bx - 2f, by - 7f, text(mono, 6.5f, INK))
        c.drawText(DimensionFormatter.formatLength(barM, LengthUnit.METERS), bx + barLen - 12f, by - 7f, text(mono, 6.5f, INK))
        c.drawText("SCALE 1:$scale @ A4", bx + barLen + 16f, by - 1f, text(monoBold, 7.5f, INK))
        // North-up note
        c.drawText("Plan view · corners A, B, C… in capture order", frame.right - 190f, by - 1f, text(sans, 7f, MUTED))

        // Totals strip
        val cells = listOf(
            "FLOOR AREA" to area(plan.areaSqM),
            "PERIMETER" to len(plan.perimeterM),
            "VOLUME" to vol(plan.volumeCuM),
            "NET WALL AREA" to area(plan.netWallAreaSqM),
        )
        val cw = (W - 2 * M - 3 * 8f) / 4
        cells.forEachIndexed { i, (k, v) ->
            val r = RectF(M + i * (cw + 8f), 618f, M + i * (cw + 8f) + cw, 672f)
            c.drawRoundRect(r, 8f, 8f, fill(PANEL))
            c.drawText(k, r.left + 10f, r.top + 16f, text(monoBold, 6.8f, MUTED))
            c.drawText(v.substringBefore(" ("), r.left + 10f, r.top + 34f, text(monoBold, 11.5f, INK))
            v.substringAfter(" (", "").removeSuffix(")").takeIf { it.isNotEmpty() }?.let {
                c.drawText(it, r.left + 10f, r.top + 46f, text(mono, 7f, MUTED))
            }
        }

        // Legend
        val ly = 696f
        c.drawText("LEGEND", M, ly, text(monoBold, 7.5f, INK))
        c.drawLine(M + 50f, ly - 3f, M + 75f, ly - 3f, stroke(INK, 3.5f))
        c.drawText("Wall", M + 80f, ly, text(sans, 7.5f, INK))
        c.drawArc(RectF(M + 115f, ly - 12f, M + 131f, ly + 4f), 270f, 90f, false, stroke(INK, 0.7f))
        c.drawText("Door swing", M + 135f, ly, text(sans, 7.5f, INK))
        c.drawLine(M + 190f, ly - 5f, M + 212f, ly - 5f, stroke(WINDOW, 1f))
        c.drawLine(M + 190f, ly - 1f, M + 212f, ly - 1f, stroke(WINDOW, 1f))
        c.drawText("Window", M + 217f, ly, text(sans, 7.5f, INK))
        c.drawText("Dimensions are wall-to-wall at floor level, from ${meta.qualityLabel}.", M, ly + 16f, text(sans, 7.5f, MUTED))

        drawFooter(c, 1)
    }

    private fun drawPlan(c: Canvas, plan: FloorPlan, map: (PlanPoint) -> FloatArray, ptPerM: Double) {
        val walls = plan.walls
        // Floor fill
        val floor = Path()
        plan.corners.forEachIndexed { i, p -> val q = map(p); if (i == 0) floor.moveTo(q[0], q[1]) else floor.lineTo(q[0], q[1]) }
        floor.close()
        c.drawPath(floor, fill(FLOOR))
        c.drawPath(floor, stroke(INK, 3.5f).apply { strokeJoin = Paint.Join.MITER })

        val signed = signedArea(plan.corners)
        for (w in walls) {
            val a = map(w.a); val b = map(w.b)
            val dir = floatArrayOf(b[0] - a[0], b[1] - a[1]).let { val l = kotlin.math.hypot(it[0], it[1]); floatArrayOf(it[0] / l, it[1] / l) }
            val left = floatArrayOf(-dir[1], dir[0])
            // Outward normal in page space (page y grows down like plan y, so orientation carries over).
            val out = if (signed > 0) floatArrayOf(-left[0], -left[1]) else left

            // Openings
            for (o in plan.openingsOn(w.index)) {
                val s = ((o.offsetM) * ptPerM).toFloat()
                val e = ((o.offsetM + o.widthM) * ptPerM).toFloat()
                val p1 = floatArrayOf(a[0] + dir[0] * s, a[1] + dir[1] * s)
                val p2 = floatArrayOf(a[0] + dir[0] * e, a[1] + dir[1] * e)
                c.drawLine(p1[0], p1[1], p2[0], p2[1], stroke(FLOOR, 4.5f))
                if (o.type == OpeningType.WINDOW) {
                    val k = 1.6f
                    c.drawLine(p1[0] + out[0] * k, p1[1] + out[1] * k, p2[0] + out[0] * k, p2[1] + out[1] * k, stroke(WINDOW, 0.9f))
                    c.drawLine(p1[0] - out[0] * k, p1[1] - out[1] * k, p2[0] - out[0] * k, p2[1] - out[1] * k, stroke(WINDOW, 0.9f))
                } else {
                    val hinge = if (o.hingeAtStart) p1 else p2
                    val free = if (o.hingeAtStart) p2 else p1
                    val r = (o.widthM * ptPerM).toFloat()
                    val inward = floatArrayOf(-out[0], -out[1])
                    val swing = if (o.swingIn) inward else out
                    val leafEnd = floatArrayOf(hinge[0] + swing[0] * r, hinge[1] + swing[1] * r)
                    c.drawLine(hinge[0], hinge[1], leafEnd[0], leafEnd[1], stroke(INK, 1.1f))
                    val a0 = Math.toDegrees(atan2((free[1] - hinge[1]).toDouble(), (free[0] - hinge[0]).toDouble())).toFloat()
                    val a1 = Math.toDegrees(atan2((leafEnd[1] - hinge[1]).toDouble(), (leafEnd[0] - hinge[0]).toDouble())).toFloat()
                    var sweep = a1 - a0
                    while (sweep > 180) sweep -= 360f
                    while (sweep < -180) sweep += 360f
                    c.drawArc(RectF(hinge[0] - r, hinge[1] - r, hinge[0] + r, hinge[1] + r), a0, sweep, false,
                        stroke(INK, 0.6f).apply { pathEffect = DashPathEffect(floatArrayOf(2.5f, 2f), 0f) })
                }
            }

            // Dimension line outside the wall
            val off = 16f
            val d1 = floatArrayOf(a[0] + out[0] * off, a[1] + out[1] * off)
            val d2 = floatArrayOf(b[0] + out[0] * off, b[1] + out[1] * off)
            val dim = stroke(DIM, 0.6f)
            c.drawLine(d1[0], d1[1], d2[0], d2[1], dim)
            c.drawLine(a[0] + out[0] * 4f, a[1] + out[1] * 4f, a[0] + out[0] * (off + 4f), a[1] + out[1] * (off + 4f), dim)
            c.drawLine(b[0] + out[0] * 4f, b[1] + out[1] * 4f, b[0] + out[0] * (off + 4f), b[1] + out[1] * (off + 4f), dim)
            for (p in listOf(d1, d2)) { // architectural 45° ticks
                c.drawLine(p[0] - 2.5f, p[1] + 2.5f, p[0] + 2.5f, p[1] - 2.5f, stroke(DIM, 0.9f))
            }
            val label = DimensionFormatter.formatLength(w.lengthM, LengthUnit.METERS)
            val mid = floatArrayOf((d1[0] + d2[0]) / 2, (d1[1] + d2[1]) / 2)
            var ang = Math.toDegrees(atan2(dir[1].toDouble(), dir[0].toDouble())).toFloat()
            if (ang > 90f) ang -= 180f
            if (ang < -90f) ang += 180f
            val tp = text(monoBold, 7.5f, DIM).apply { textAlign = Paint.Align.CENTER }
            c.save()
            c.rotate(ang, mid[0], mid[1])
            val tw = tp.measureText(label) + 6f
            c.drawRect(mid[0] - tw / 2, mid[1] - 6f, mid[0] + tw / 2, mid[1] + 3f, fill(PAPER))
            c.drawText(label, mid[0], mid[1] + 1.5f, tp)
            c.restore()
        }

        // Corner markers
        plan.corners.forEachIndexed { i, p ->
            val q = map(p)
            c.drawCircle(q[0], q[1], 5.5f, fill(INK))
            c.drawText(FloorPlan.cornerLabel(i), q[0], q[1] + 2.5f, text(monoBold, 6.5f, Color.WHITE).apply { textAlign = Paint.Align.CENTER })
        }
    }

    // ---------------------------------------------------------------- page 2

    private fun drawPageTwo(c: Canvas, plan: FloorPlan, meta: Meta) {
        drawHeader(c, meta, "Room Specification & Estimates", 2)
        var y = 128f
        c.drawText("Wall schedule", M, y, text(sansBold, 13f, INK))
        y += 14f

        val cols = floatArrayOf(M, M + 54f, M + 134f, M + 234f, M + 410f)
        val headers = listOf("WALL", "LENGTH", "IMPERIAL", "OPENINGS", "NET AREA")
        c.drawRect(M, y, W - M, y + 18f, fill(INK))
        headers.forEachIndexed { i, h -> c.drawText(h, cols[i] + 6f, y + 12f, text(monoBold, 7f, LIME)) }
        y += 18f
        plan.walls.forEachIndexed { i, w ->
            if (y > 470f) return@forEachIndexed
            if (i % 2 == 0) c.drawRect(M, y, W - M, y + 17f, fill(PANEL))
            val openings = plan.openingsOn(i).joinToString(", ") {
                (if (it.type == OpeningType.DOOR) "Door " else "Window ") + DimensionFormatter.formatLength(it.widthM, LengthUnit.METERS)
            }.ifEmpty { "—" }
            val row = listOf(
                "${FloorPlan.cornerLabel(i)}→${FloorPlan.cornerLabel((i + 1) % plan.corners.size)}",
                DimensionFormatter.formatLength(w.lengthM, LengthUnit.METERS),
                DimensionFormatter.formatFeetInches(w.lengthM, FractionPrecision.EIGHTH),
                openings,
                DimensionFormatter.formatArea(plan.netWallArea(i), AreaUnit.SQUARE_METERS),
            )
            row.forEachIndexed { k, v -> c.drawText(v, cols[k] + 6f, y + 12f, text(if (k == 0) monoBold else mono, 7.8f, INK)) }
            y += 17f
        }
        c.drawLine(M, y, W - M, y, stroke(LINE, 0.6f))
        y += 26f

        // Totals
        c.drawText("Area totals", M, y, text(sansBold, 13f, INK)); y += 8f
        val totals = listOf(
            "Floor area" to area(plan.areaSqM),
            "Perimeter" to len(plan.perimeterM),
            "Ceiling height" to len(plan.ceilingM),
            "Room volume" to vol(plan.volumeCuM),
            "Gross wall area" to area(plan.grossWallAreaSqM),
            "Doors & windows" to area(plan.openingsAreaSqM),
            "Net wall area (paintable)" to area(plan.netWallAreaSqM),
        )
        y = keyValueTable(c, totals, y)
        y += 22f

        // Estimates
        val e = RoomEstimator.estimate(plan)
        c.drawText("Material & appliance estimates", M, y, text(sansBold, 13f, INK)); y += 8f
        val rec = listOf(
            "Floor tiles ${e.tiles.sizeCm}×${e.tiles.sizeCm} cm" to "${e.tiles.count} tiles (~${e.tiles.boxes} boxes, incl. 10% waste)",
            "Wall paint, 2 coats" to "%.1f L for %.1f m²".format(Locale.US, e.paint.litres, e.paint.wallAreaSqM),
            "Air conditioning" to "%,d BTU/h  ≈ %.2f ton".format(Locale.US, e.cooling.btu, e.cooling.tons),
            "Recommended TV" to "${e.tv.diagonalIn}\" at ~%.1f m viewing distance".format(Locale.US, e.tv.viewingDistanceM),
            "Skirting / baseboard" to "%.1f m (doors excluded)".format(Locale.US, e.baseboardM),
        ) + e.furniture.map { it.name to (if (it.fits) "Fits · ${it.note}" else it.note) }
        y = keyValueTable(c, rec, y)

        y += 20f
        val note = text(sans, 7.5f, MUTED)
        listOf(
            "Estimates use standard trade rules of thumb (tile waste 10%, paint 10 m²/L per coat, 20 BTU/h per sq.ft",
            "scaled for ceiling height, SMPTE 30° TV viewing angle). Phone-based measurements carry the stated",
            "accuracy — verify critical dimensions with a physical tape before ordering materials or appliances.",
        ).forEach { c.drawText(it, M, y, note); y += 11f }

        drawFooter(c, 2)
    }

    private fun keyValueTable(c: Canvas, rows: List<Pair<String, String>>, startY: Float): Float {
        var y = startY
        rows.forEachIndexed { i, (k, v) ->
            if (i % 2 == 0) c.drawRect(M, y, W - M, y + 17f, fill(PANEL))
            c.drawText(k, M + 8f, y + 12f, text(sans, 8.5f, INK))
            c.drawText(v, W - M - 8f, y + 12f, text(monoBold, 8.2f, INK).apply { textAlign = Paint.Align.RIGHT })
            y += 17f
        }
        return y
    }

    // ---------------------------------------------------------------- chrome

    private fun drawHeader(c: Canvas, meta: Meta, subtitle: String, page: Int) {
        c.drawRect(0f, 0f, W, 92f, fill(INK))
        c.drawCircle(M + 4f, 34f, 4f, fill(LIME))
        c.drawText("MEASUREPULSE.AI", M + 14f, 37.5f, text(monoBold, 9f, LIME))
        c.drawText(subtitle, M, 66f, text(sansBold, 19f, Color.WHITE))
        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(meta.createdAt))
        val right = text(mono, 8f, Color.argb(200, 226, 232, 240)).apply { textAlign = Paint.Align.RIGHT }
        c.drawText("Captured $date", W - M, 37.5f, right)
        c.drawText("Sheet $page of 2 · A4", W - M, 66f, right)
        c.drawRect(0f, 92f, W, 95f, fill(LIME))
    }

    private fun drawFooter(c: Canvas, page: Int) {
        c.drawLine(M, H - 40f, W - M, H - 40f, stroke(LINE, 0.6f))
        c.drawText("Generated with MeasurePulse AI · AR Laser Tape, Room Blueprint, Caliper & Level", M, H - 26f, text(sans, 7f, MUTED))
        c.drawText("$page", W - M, H - 26f, text(monoBold, 7f, MUTED).apply { textAlign = Paint.Align.RIGHT })
    }

    private fun drawGrid(c: Canvas, r: RectF) {
        val minor = stroke(Color.argb(22, 28, 30, 54), 0.4f)
        var x = r.left; while (x < r.right) { c.drawLine(x, r.top, x, r.bottom, minor); x += 14.17f }
        var y = r.top; while (y < r.bottom) { c.drawLine(r.left, y, r.right, y, minor); y += 14.17f }
    }

    /** Largest standard architectural scale at which the plan (plus room for dimensions) fits. */
    private fun chooseScale(plan: FloorPlan, frame: RectF): Int {
        val pad = 70f
        for (s in SCALES) {
            val ptPerM = PT_PER_MM * 1000f / s
            if (plan.widthM * ptPerM <= frame.width() - pad && plan.depthM * ptPerM <= frame.height() - pad) return s
        }
        return SCALES.last()
    }

    // ---------------------------------------------------------------- helpers

    private fun page(doc: PdfDocument, n: Int, block: (Canvas) -> Unit) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), n).create())
        page.canvas.drawColor(Color.WHITE)
        block(page.canvas)
        doc.finishPage(page)
    }

    private fun font(id: Int, fallback: Typeface) = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback

    private fun text(tf: Typeface, size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = size; this.color = color }
    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    private fun stroke(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND }

    private fun len(m: Double) = "${DimensionFormatter.formatLength(m, LengthUnit.METERS)} (${DimensionFormatter.formatFeetInches(m, FractionPrecision.EIGHTH)})"
    private fun area(m2: Double) = "${DimensionFormatter.formatArea(m2, AreaUnit.SQUARE_METERS)} (${DimensionFormatter.formatArea(m2, AreaUnit.SQUARE_FEET)})"
    private fun vol(m3: Double) = "${DimensionFormatter.formatVolume(m3, VolumeUnit.CUBIC_METERS)} (${DimensionFormatter.formatVolume(m3, VolumeUnit.CUBIC_FEET)})"

    private fun signedArea(ps: List<PlanPoint>): Double {
        var s = 0.0
        for (i in ps.indices) { val p = ps[i]; val q = ps[(i + 1) % ps.size]; s += p.x * q.y - q.x * p.y }
        return s / 2
    }

    private companion object {
        const val W = 595f
        const val H = 842f
        const val M = 36f
        const val PT_PER_MM = 72f / 25.4f
        val SCALES = listOf(20, 25, 50, 75, 100, 200, 500)
        val INK = Color.rgb(17, 19, 34)
        val LIME = Color.rgb(221, 248, 61)
        val MUTED = Color.rgb(100, 104, 130)
        val LINE = Color.rgb(205, 208, 222)
        val PAPER = Color.rgb(248, 249, 253)
        val PANEL = Color.rgb(240, 242, 249)
        val FLOOR = Color.rgb(234, 238, 252)
        val DIM = Color.rgb(84, 46, 140)
        val WINDOW = Color.rgb(37, 99, 235)
    }
}
