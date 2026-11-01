package com.measurepulse.ar.ui.screens.artape

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.measurepulse.ar.data.ar.BoxFit
import com.measurepulse.ar.data.ar.EngineFrame
import com.measurepulse.ar.data.ar.HitSource
import com.measurepulse.ar.data.ar.MeasureMode
import com.measurepulse.ar.data.ar.MeasurementSession
import com.measurepulse.ar.data.ar.ScreenPoint
import com.measurepulse.ar.data.ar.Vec3
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.LengthUnit
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.theme.JetBrainsMono
import com.measurepulse.ar.ui.theme.MpColors
import java.util.Locale

// ---------- Model ----------

enum class LabelStyle { LIVE, LOCKED, ANGLE }

data class OverlaySegment(val a: Offset, val b: Offset, val live: Boolean)

data class OverlayLabel(
    val at: Offset,
    val primary: String,
    val secondary: String?,
    val accuracy: String?,
    val style: LabelStyle,
)

data class BoxOverlay(val corners: List<Offset?>, val color: Color, val ghost: Boolean, val caption: String)

data class OverlayModel(
    val segments: List<OverlaySegment> = emptyList(),
    val points: List<Offset> = emptyList(),
    val labels: List<OverlayLabel> = emptyList(),
    val box: BoxOverlay? = null,
    val reticleValid: Boolean = false,
    val reticleCaption: String? = null,
)

/** Builds everything that should be drawn for this frame, already projected to screen pixels. */
fun buildOverlay(
    frame: EngineFrame?,
    session: MeasurementSession,
    system: UnitSystem,
    precision: FractionPrecision,
    filterLive: (Double) -> Double,
): OverlayModel {
    if (frame == null) return OverlayModel()
    val project = { v: Vec3 -> frame.projector.project(v)?.toOffset() }
    val reticle = frame.reticle

    val segments = mutableListOf<OverlaySegment>()
    val labels = mutableListOf<OverlayLabel>()
    for (seg in session.segments(reticle, frame.reticleAccuracyM)) {
        val a = project(seg.a) ?: continue
        val b = project(seg.b) ?: continue
        segments += OverlaySegment(a, b, seg.live)
        val len = if (seg.live) filterLive(seg.lengthM) else seg.lengthM
        labels += OverlayLabel(
            at = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f),
            primary = primaryLength(len, system, precision),
            secondary = secondaryLength(len, system, precision),
            accuracy = accuracyText(seg.accuracyM, system),
            style = if (seg.live) LabelStyle.LIVE else LabelStyle.LOCKED,
        )
    }

    session.angleDeg(reticle)?.let { angle ->
        session.angleVertex()?.let(project)?.let { v ->
            labels += OverlayLabel(v + Offset(0f, -70f), String.format(Locale.US, "%.1f°", angle), null, null, LabelStyle.ANGLE)
        }
    }

    val box = session.boxFit(reticle, frame.up, frame.cameraPosition)?.let { fit -> boxOverlay(fit, project, system) }

    val caption = when {
        reticle == null -> null
        frame.hitSource == HitSource.NONE -> null
        else -> "${frame.hitSource.label.uppercase()} · ${
            DimensionFormatter.formatLength(reticle.distanceTo(frame.cameraPosition), system, FractionPrecision.EIGHTH)
        } AWAY"
    }

    return OverlayModel(
        segments = segments,
        points = session.points.mapNotNull { project(it.position) },
        labels = labels,
        box = box,
        reticleValid = reticle != null,
        reticleCaption = caption,
    )
}

private fun boxOverlay(fit: BoxFit, project: (Vec3) -> Offset?, system: UnitSystem): BoxOverlay {
    val color = when (fit.fits) {
        true -> MpColors.ElectricLime
        false -> MpColors.Alert
        null -> Color.White
    }
    val dims = listOf(fit.item.widthM, fit.item.depthM, fit.item.heightM).joinToString(" × ") {
        if (system == UnitSystem.METRIC) String.format(Locale.US, "%.2f", it)
        else DimensionFormatter.formatFeetInches(it, FractionPrecision.EIGHTH)
    } + if (system == UnitSystem.METRIC) "${DimensionFormatter.NBSP}m" else ""
    return BoxOverlay(fit.corners.map(project), color, fit.ghost, "${fit.item.name}  $dims")
}

private fun ScreenPoint.toOffset() = Offset(x, y)

// ---------- Formatting ----------

fun primaryLength(m: Double, system: UnitSystem, precision: FractionPrecision) = DimensionFormatter.formatLength(m, system, precision)

fun secondaryLength(m: Double, system: UnitSystem, precision: FractionPrecision) =
    "(" + DimensionFormatter.formatLength(m, if (system == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC, precision) + ")"

fun accuracyText(errM: Double, system: UnitSystem): String = if (system == UnitSystem.METRIC) {
    "±" + String.format(Locale.US, "%.1f", errM * 100) + DimensionFormatter.NBSP + "cm"
} else {
    "±" + String.format(Locale.US, "%.1f", errM / DimensionFormatter.METERS_PER_INCH) + "\""
}

/** Clearance like "+15 CM" or "+5 7/8\"" (sign always shown). */
fun clearanceText(m: Double, system: UnitSystem): String {
    val sign = if (m >= 0) "+" else "−"
    val abs = kotlin.math.abs(m)
    return sign + if (system == UnitSystem.METRIC) {
        String.format(Locale.US, "%.0f", abs * 100) + DimensionFormatter.NBSP + "CM"
    } else DimensionFormatter.formatLength(abs, LengthUnit.FEET_INCHES, FractionPrecision.EIGHTH)
}

// ---------- Drawing ----------

private val Lime = MpColors.ElectricLime

/**
 * Draws the laser overlay. Used both live (Compose Canvas) and for snapshots (offscreen canvas),
 * so a saved photo shows exactly what was on screen. [showReticle] = false for snapshots.
 */
fun DrawScope.drawMeasureOverlay(model: OverlayModel, textMeasurer: TextMeasurer, showReticle: Boolean = true) {
    model.box?.let { drawBox(it, textMeasurer) }

    for (s in model.segments) drawLaser(s.a, s.b, s.live)
    for (p in model.points) drawAnchor(p)
    if (showReticle) drawReticle(center, model.reticleValid, model.reticleCaption, textMeasurer)
    for (l in model.labels) drawLabel(l, textMeasurer)
}

private fun DrawScope.drawLaser(a: Offset, b: Offset, live: Boolean) {
    val alpha = if (live) 0.85f else 1f
    drawLine(Lime.copy(alpha = 0.22f * alpha), a, b, strokeWidth = 12.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Lime.copy(alpha = 0.45f * alpha), a, b, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Lime.copy(alpha = alpha), a, b, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.8f * alpha), a, b, strokeWidth = 0.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawAnchor(p: Offset) {
    drawCircle(Brush.radialGradient(listOf(Lime.copy(alpha = 0.55f), Color.Transparent), p, 22.dp.toPx()), 22.dp.toPx(), p)
    drawCircle(Lime, 10.dp.toPx(), p)
    drawCircle(MpColors.Ink, 6.5.dp.toPx(), p)
    drawCircle(Color.White, 3.dp.toPx(), p)
}

private fun DrawScope.drawReticle(c: Offset, valid: Boolean, caption: String?, tm: TextMeasurer) {
    val color = if (valid) Lime else Color.White.copy(alpha = 0.6f)
    val r = 30.dp.toPx()
    drawCircle(color.copy(alpha = 0.10f), r, c)
    drawCircle(color, r, c, style = Stroke(1.5.dp.toPx()))
    val t1 = 8.dp.toPx(); val gap = 6.dp.toPx()
    val w = 1.5.dp.toPx()
    drawLine(color, Offset(c.x - r - t1 / 2, c.y), Offset(c.x - gap, c.y), w)
    drawLine(color, Offset(c.x + gap, c.y), Offset(c.x + r + t1 / 2, c.y), w)
    drawLine(color, Offset(c.x, c.y - r - t1 / 2), Offset(c.x, c.y - gap), w)
    drawLine(color, Offset(c.x, c.y + gap), Offset(c.x, c.y + r + t1 / 2), w)
    drawCircle(color, 3.dp.toPx(), c)
    if (caption != null) {
        val layout = tm.measure(caption, TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.06.sp, color = Color.White))
        val pad = 7.dp.toPx()
        val tl = Offset(c.x - layout.size.width / 2f, c.y + r + 14.dp.toPx())
        drawRoundRect(Color(0xCC111322), tl - Offset(pad, pad / 2), Size(layout.size.width + pad * 2, layout.size.height + pad), CornerRadius(50f, 50f))
        drawText(layout, topLeft = tl)
    }
}

private fun DrawScope.drawLabel(l: OverlayLabel, tm: TextMeasurer) {
    val onLime = l.style != LabelStyle.LOCKED
    val main = if (onLime) MpColors.Ink else Lime
    val sub = if (onLime) MpColors.Ink.copy(alpha = 0.75f) else MpColors.IceWhite
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = main, fontWeight = FontWeight.Bold, fontSize = if (l.style == LabelStyle.ANGLE) 20.sp else 16.sp)) { append(l.primary) }
        l.secondary?.let { withStyle(SpanStyle(color = sub, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)) { append("  $it") } }
        l.accuracy?.let { withStyle(SpanStyle(color = sub, fontWeight = FontWeight.Medium, fontSize = 11.sp)) { append("  •  $it") } }
    }
    val layout = tm.measure(text, TextStyle(fontFamily = JetBrainsMono, letterSpacing = (-0.2).sp), maxLines = 1, softWrap = false)
    val padH = 14.dp.toPx(); val padV = 8.dp.toPx()
    val size = Size(layout.size.width + padH * 2, layout.size.height + padV * 2)
    // Sit just above the line's midpoint, clamped on screen.
    val x = (l.at.x - size.width / 2f).coerceIn(8.dp.toPx(), (this.size.width - size.width - 8.dp.toPx()).coerceAtLeast(8.dp.toPx()))
    val y = (l.at.y - size.height - 14.dp.toPx()).coerceIn(8.dp.toPx(), this.size.height - size.height - 8.dp.toPx())
    val tl = Offset(x, y)
    val radius = CornerRadius(size.height / 2f, size.height / 2f)
    if (onLime) {
        drawRoundRect(Lime.copy(alpha = 0.35f), tl - Offset(4f, 4f), Size(size.width + 8f, size.height + 8f), CornerRadius(radius.x + 4f, radius.y + 4f))
        drawRoundRect(Lime, tl, size, radius)
    } else {
        drawRoundRect(Color(18, 21, 42, (0.82f * 255).toInt()), tl, size, radius)
        drawRoundRect(Color.White.copy(alpha = 0.28f), tl, size, radius, style = Stroke(1.dp.toPx()))
    }
    drawText(layout, topLeft = tl + Offset(padH, padV))
}

private fun DrawScope.drawBox(box: BoxOverlay, tm: TextMeasurer) {
    val c = box.corners
    if (c.any { it == null }) return
    val p = c.map { it!! }
    val faces = listOf(
        listOf(0, 1, 2, 3), listOf(4, 5, 6, 7), listOf(0, 1, 5, 4),
        listOf(1, 2, 6, 5), listOf(2, 3, 7, 6), listOf(3, 0, 4, 7),
    )
    val fill = box.color.copy(alpha = if (box.ghost) 0.06f else 0.10f)
    for (f in faces) {
        val path = Path().apply {
            moveTo(p[f[0]].x, p[f[0]].y)
            for (i in 1 until f.size) lineTo(p[f[i]].x, p[f[i]].y)
            close()
        }
        drawPath(path, fill)
    }
    val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
    val stroke = 2.dp.toPx()
    for ((a, b) in edges) {
        drawLine(box.color.copy(alpha = 0.25f), p[a], p[b], strokeWidth = stroke * 3, cap = StrokeCap.Round)
        drawLine(box.color.copy(alpha = if (box.ghost) 0.7f else 1f), p[a], p[b], strokeWidth = stroke, cap = StrokeCap.Round)
    }
    // Caption above the top face.
    val top = listOf(p[4], p[5], p[6], p[7])
    val anchor = Offset(top.map { it.x }.average().toFloat(), top.minOf { it.y })
    val layout = tm.measure(box.caption, TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
    val pad = 8.dp.toPx()
    val tl = Offset(anchor.x - layout.size.width / 2f, anchor.y - layout.size.height - 18.dp.toPx())
    drawRoundRect(Color(0xD9111322), tl - Offset(pad, pad / 2), Size(layout.size.width + pad * 2, layout.size.height + pad), CornerRadius(40f, 40f))
    drawText(layout, topLeft = tl)
}
