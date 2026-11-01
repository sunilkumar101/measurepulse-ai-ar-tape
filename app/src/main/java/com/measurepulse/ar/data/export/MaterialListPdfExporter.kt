package com.measurepulse.ar.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.measurepulse.ar.R
import com.measurepulse.ar.domain.advisor.AdvisorResult
import com.measurepulse.ar.domain.advisor.RoomAdvisor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** One-page A4 "AI Renovation & Material Shopping List" built from an [AdvisorResult]. */
@Singleton
class MaterialListPdfExporter @Inject constructor(@ApplicationContext private val context: Context) {

    private val sans = Typeface.create("sans-serif", Typeface.NORMAL)
    private val head = runCatching { ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_800) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    private val mono = runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono_700) }.getOrNull() ?: Typeface.MONOSPACE

    suspend fun export(r: AdvisorResult): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), 1).create())
            draw(page.canvas, r)
            doc.finishPage(page)
            val safe = r.input.roomName.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(40).ifEmpty { "Room" }
            val file = File(ExportLibrary.dir(context), "MeasurePulse_${safe}_Shopping_List.pdf")
            file.outputStream().use { doc.writeTo(it) }
            file
        } finally {
            doc.close()
        }
    }

    private fun draw(c: Canvas, r: AdvisorResult) {
        val money = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val i = r.input
        c.drawColor(Color.WHITE)
        c.drawRect(0f, 0f, W, 92f, fill(INK))
        c.drawCircle(M + 4f, 34f, 4f, fill(LIME))
        c.drawText("MEASUREPULSE.AI", M + 14f, 37.5f, text(mono, 9f, LIME))
        c.drawText("Renovation & Material Shopping List", M, 66f, text(head, 18f, Color.WHITE))
        c.drawText(SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()), W - M, 37.5f, text(mono, 8f, Color.LTGRAY).apply { textAlign = Paint.Align.RIGHT })
        c.drawRect(0f, 92f, W, 95f, fill(LIME))

        var y = 126f
        c.drawText(i.roomName, M, y, text(head, 16f, INK)); y += 18f
        c.drawText(
            String.format(Locale.US, "Floor %.1f m²  ·  Net wall %.1f m²  ·  Volume %.1f m³  ·  Ceiling %.2f m  ·  %d door(s), %d window(s)",
                i.floorAreaSqM, i.netWallSqM, i.volumeCuM, i.ceilingM, i.doors, i.windows),
            M, y, text(sans, 8.5f, MUTED),
        )
        y += 22f

        val t = r.tiles; val p = r.paint
        y = section(c, y, "FLOOR", listOf(
            Row("${t.tileCm}×${t.tileCm} cm floor tiles", "${t.tiles} tiles · ${t.boxes} boxes (${t.perBox}/box)", money.format(t.cost)),
            Row("Tile adhesive (20 kg bags)", String.format(Locale.US, "%d bags · %.0f kg", t.adhesiveBags, t.adhesiveKg), "—"),
            Row("Grout (2 mm joint)", String.format(Locale.US, "%.1f kg", t.groutKg), "—"),
        ))
        y = section(c, y, "WALLS", listOf(
            Row("Interior paint, 2 coats", String.format(Locale.US, "%.1f L (%.1f US gal)", p.litres, p.gallons), money.format(p.cost)),
            Row("Cans to buy", p.cans.joinToString(" + ") { "${it.count}×${it.litres.toInt()} L" }.ifEmpty { "—" }, "—"),
            Row("Primer, 1 coat", String.format(Locale.US, "%.1f L", p.primerLitres), "—"),
        ))
        y = section(c, y, "APPLIANCES", listOf(
            Row("Split AC (inverter)", String.format(Locale.US, "%.2f ton · %,d BTU/h", r.cooling.tons, r.cooling.btu), "—"),
            Row("TV size", String.format(Locale.US, "%d\" (ideal %.0f–%.0f\") at %.1f m", r.tv.recommendedIn, r.tv.minDiagonalIn, r.tv.maxDiagonalIn, r.tv.viewingDistanceM), "—"),
            Row("TV mount centre height", String.format(Locale.US, "%.2f m from floor", r.tv.mountCentreM), "—"),
        ))

        c.drawRoundRect(RectF(M, y, W - M, y + 40f), 8f, 8f, fill(INK))
        c.drawText("ESTIMATED MATERIAL COST", M + 12f, y + 25f, text(mono, 9f, LIME))
        c.drawText(money.format(r.totalCost), W - M - 12f, y + 26f, text(head, 15f, Color.WHITE).apply { textAlign = Paint.Align.RIGHT })
        y += 64f

        val note = text(sans, 7.8f, MUTED)
        listOf(
            String.format(Locale.US, "Unit prices used: %s per box of tiles, %s per litre of paint (edit them in the app).",
                money.format(i.tileBoxPrice), money.format(i.paintLitrePrice)),
            "Rules of thumb: +10% tile cutting margin, adhesive ${RoomAdvisor.ADHESIVE_KG_PER_SQM.toInt()} kg/m², paint ${RoomAdvisor.PAINT_SQM_PER_L.toInt()} m²/L per coat,",
            "AC 20 BTU/h per sq.ft scaled for ceiling, sun and occupants, TV per SMPTE 30° – THX 36° viewing angle.",
            "Phone measurements carry an error margin — confirm critical sizes before ordering.",
        ).forEach { c.drawText(it, M, y, note); y += 12f }

        c.drawLine(M, H - 40f, W - M, H - 40f, stroke(LINE))
        c.drawText("Generated with MeasurePulse AI · AR Laser Tape, Room Blueprint, Caliper & Level", M, H - 26f, text(sans, 7f, MUTED))
    }

    private data class Row(val item: String, val qty: String, val cost: String)

    private fun section(c: Canvas, startY: Float, title: String, rows: List<Row>): Float {
        var y = startY
        c.drawText(title, M, y, text(mono, 9f, PURPLE)); y += 8f
        c.drawRect(M, y, W - M, y + 18f, fill(INK))
        listOf("ITEM" to M + 8f, "QUANTITY" to M + 230f).forEach { (h, x) -> c.drawText(h, x, y + 12f, text(mono, 7f, LIME)) }
        c.drawText("EST. COST", W - M - 8f, y + 12f, text(mono, 7f, LIME).apply { textAlign = Paint.Align.RIGHT })
        y += 18f
        rows.forEachIndexed { idx, r ->
            if (idx % 2 == 0) c.drawRect(M, y, W - M, y + 20f, fill(PANEL))
            c.drawText(r.item, M + 8f, y + 13.5f, text(sans, 9f, INK))
            c.drawText(r.qty, M + 230f, y + 13.5f, text(mono, 8.5f, INK))
            c.drawText(r.cost, W - M - 8f, y + 13.5f, text(mono, 8.5f, INK).apply { textAlign = Paint.Align.RIGHT })
            y += 20f
        }
        return y + 20f
    }

    private fun text(tf: Typeface, size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = size; this.color = color }
    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private fun stroke(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 0.6f }

    private companion object {
        const val W = 595f; const val H = 842f; const val M = 36f
        val INK = Color.rgb(17, 19, 34); val LIME = Color.rgb(221, 248, 61)
        val MUTED = Color.rgb(100, 104, 130); val LINE = Color.rgb(205, 208, 222)
        val PANEL = Color.rgb(240, 242, 249); val PURPLE = Color.rgb(109, 58, 156)
    }
}
