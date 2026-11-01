package com.measurepulse.ar.ui.screens.blueprint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.measurepulse.ar.domain.floorplan.FloorPlan
import com.measurepulse.ar.domain.floorplan.OpeningType
import com.measurepulse.ar.domain.floorplan.PlanPoint
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.theme.JetBrainsMono
import com.measurepulse.ar.ui.theme.MpColors
import kotlin.math.atan2
import kotlin.math.min

/** Plan ↔ screen mapping. Frozen while dragging so the drawing doesn't rescale under the finger. */
private data class PlanTransform(val minX: Double, val minY: Double, val scale: Float, val offset: Offset) {
    fun toScreen(p: PlanPoint) = Offset(offset.x + ((p.x - minX) * scale).toFloat(), offset.y + ((p.y - minY) * scale).toFloat())
    fun toPlan(o: Offset) = PlanPoint(minX + (o.x - offset.x) / scale, minY + (o.y - offset.y) / scale)

    companion object {
        fun fit(plan: FloorPlan, size: IntSize, padPx: Float): PlanTransform {
            val minX = plan.corners.minOf { it.x }; val minY = plan.corners.minOf { it.y }
            val w = plan.widthM.coerceAtLeast(0.5); val h = plan.depthM.coerceAtLeast(0.5)
            val scale = min((size.width - 2 * padPx) / w, (size.height - 2 * padPx) / h).toFloat().coerceAtLeast(1f)
            val off = Offset(
                (size.width - (w * scale).toFloat()) / 2f,
                (size.height - (h * scale).toFloat()) / 2f,
            )
            return PlanTransform(minX, minY, scale, off)
        }
    }
}

@Composable
fun CadPlanCanvas(
    plan: FloorPlan,
    selection: CadSelection,
    unitSystem: UnitSystem,
    onSelect: (CadSelection) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Int, PlanPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tm = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var frozen by remember { mutableStateOf<PlanTransform?>(null) }
    val currentPlan by rememberUpdatedState(plan)
    val select by rememberUpdatedState(onSelect)
    val dragStart by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)

    Canvas(
        modifier
            .semantics { contentDescription = "2D floor plan editor. Drag corners, tap walls to select." }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val t = frozen ?: PlanTransform.fit(currentPlan, IntSize(this.size.width, this.size.height), PAD.toPx())
                    select(hitTest(currentPlan, t, pos, 26.dp.toPx(), 20.dp.toPx()))
                }
            }
            .pointerInput(Unit) {
                var dragging = -1
                var touch = Offset.Zero
                var grabOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = { pos ->
                        val t = PlanTransform.fit(currentPlan, IntSize(this.size.width, this.size.height), PAD.toPx())
                        val hit = hitTest(currentPlan, t, pos, 30.dp.toPx(), 0f)
                        if (hit is CadSelection.Corner) {
                            frozen = t
                            dragging = hit.index
                            touch = pos
                            grabOffset = t.toScreen(currentPlan.corners[hit.index]) - pos
                            dragStart(hit.index)
                        }
                    },
                    onDrag = { change, amount ->
                        val t = frozen
                        if (dragging >= 0 && t != null) {
                            change.consume()
                            touch += amount
                            drag(dragging, t.toPlan(touch + grabOffset))
                        }
                    },
                    onDragEnd = { dragging = -1; frozen = null },
                    onDragCancel = { dragging = -1; frozen = null },
                )
            },
    ) {
        size = IntSize(this.size.width.toInt(), this.size.height.toInt())
        val t = frozen ?: PlanTransform.fit(plan, size, PAD.toPx())
        drawBlueprintGrid(t)
        drawPlan(plan, t, selection, unitSystem, tm)
    }
}

private val PAD = 58.dp

private fun hitTest(plan: FloorPlan, t: PlanTransform, pos: Offset, cornerR: Float, wallR: Float): CadSelection {
    plan.corners.forEachIndexed { i, p -> if ((t.toScreen(p) - pos).getDistance() <= cornerR) return CadSelection.Corner(i) }
    if (wallR > 0f) {
        var best = -1; var bestD = wallR
        plan.walls.forEach { w ->
            val d = distanceToSegment(pos, t.toScreen(w.a), t.toScreen(w.b))
            if (d < bestD) { bestD = d; best = w.index }
        }
        if (best >= 0) return CadSelection.Wall(best)
    }
    return CadSelection.None
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 == 0f) return (p - a).getDistance()
    val tt = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return (p - (a + ab * tt)).getDistance()
}

private fun DrawScope.drawBlueprintGrid(t: PlanTransform) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF14183A), Color(0xFF0D1028))))
    // 0.5 m minor / 1 m major grid aligned to the plan origin.
    val stepPx = t.scale * 0.5f
    if (stepPx < 6f) return
    val startX = t.offset.x - ((t.offset.x / stepPx).toInt() + 1) * stepPx
    val startY = t.offset.y - ((t.offset.y / stepPx).toInt() + 1) * stepPx
    var x = startX
    while (x < size.width) {
        val major = ((x - t.offset.x) / stepPx).let { kotlin.math.round(it).toInt() } % 2 == 0
        drawLine(MpColors.ElectricLime.copy(alpha = if (major) 0.10f else 0.045f), Offset(x, 0f), Offset(x, size.height), 1f)
        x += stepPx
    }
    var y = startY
    while (y < size.height) {
        val major = ((y - t.offset.y) / stepPx).let { kotlin.math.round(it).toInt() } % 2 == 0
        drawLine(MpColors.ElectricLime.copy(alpha = if (major) 0.10f else 0.045f), Offset(0f, y), Offset(size.width, y), 1f)
        y += stepPx
    }
}

private fun DrawScope.drawPlan(plan: FloorPlan, t: PlanTransform, selection: CadSelection, system: UnitSystem, tm: TextMeasurer) {
    val pts = plan.corners.map(t::toScreen)
    val floor = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
    drawPath(floor, MpColors.ElectricLime.copy(alpha = 0.07f))

    // Area watermark in the middle of the room
    val cx = pts.map { it.x }.average().toFloat(); val cy = pts.map { it.y }.average().toFloat()
    val areaLayout = tm.measure(
        DimensionFormatter.formatArea(plan.areaSqM, system),
        TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MpColors.ElectricLime.copy(alpha = 0.55f)),
    )
    drawText(areaLayout, topLeft = Offset(cx - areaLayout.size.width / 2f, cy - areaLayout.size.height / 2f))

    val signed = plan.corners.indices.sumOf { i ->
        val p = plan.corners[i]; val q = plan.corners[(i + 1) % plan.corners.size]; p.x * q.y - q.x * p.y
    }
    val wallStroke = 5.dp.toPx()

    plan.walls.forEach { w ->
        val a = pts[w.index]; val b = pts[(w.index + 1) % pts.size]
        val selected = selection is CadSelection.Wall && selection.index == w.index
        val color = if (selected) MpColors.ElectricLime else MpColors.IceWhite
        if (selected) drawLine(MpColors.ElectricLime.copy(alpha = 0.3f), a, b, wallStroke * 3, StrokeCap.Round)
        drawLine(color, a, b, wallStroke, StrokeCap.Square)

        val len = (b - a).getDistance().coerceAtLeast(1f)
        val dir = (b - a) / len
        val left = Offset(-dir.y, dir.x)
        val out = if (signed > 0) -left else left

        // Openings
        plan.openingsOn(w.index).forEach { o ->
            val s = a + dir * (o.offsetM * t.scale).toFloat()
            val e = a + dir * ((o.offsetM + o.widthM) * t.scale).toFloat()
            drawLine(Color(0xFF14183A), s, e, wallStroke + 2f)
            if (o.type == OpeningType.WINDOW) {
                val k = 3.dp.toPx()
                drawLine(MpColors.Cyan, s + out * k, e + out * k, 1.6.dp.toPx())
                drawLine(MpColors.Cyan, s - out * k, e - out * k, 1.6.dp.toPx())
                drawLine(MpColors.Cyan.copy(alpha = 0.6f), s, e, 1.dp.toPx())
            } else {
                val hinge = if (o.hingeAtStart) s else e
                val free = if (o.hingeAtStart) e else s
                val r = (o.widthM * t.scale).toFloat()
                val swing = if (o.swingIn) -out else out
                val leaf = hinge + swing * r
                drawLine(MpColors.ElectricLime, hinge, leaf, 2.dp.toPx(), StrokeCap.Round)
                val a0 = Math.toDegrees(atan2((free.y - hinge.y).toDouble(), (free.x - hinge.x).toDouble())).toFloat()
                val a1 = Math.toDegrees(atan2((leaf.y - hinge.y).toDouble(), (leaf.x - hinge.x).toDouble())).toFloat()
                var sweep = a1 - a0
                while (sweep > 180f) sweep -= 360f
                while (sweep < -180f) sweep += 360f
                drawArc(
                    MpColors.ElectricLime.copy(alpha = 0.8f), a0, sweep, false,
                    topLeft = Offset(hinge.x - r, hinge.y - r), size = Size(2 * r, 2 * r),
                    style = Stroke(1.3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                )
            }
        }

        // Dimension label outside the wall, rotated along it
        val mid = (a + b) / 2f + out * 20.dp.toPx()
        val label = DimensionFormatter.formatLength(w.lengthM, system, FractionPrecision.EIGHTH)
        val layout = tm.measure(label, TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) MpColors.Ink else MpColors.ElectricLime))
        var ang = Math.toDegrees(atan2(dir.y.toDouble(), dir.x.toDouble())).toFloat()
        if (ang > 90f) ang -= 180f
        if (ang < -90f) ang += 180f
        rotate(ang, mid) {
            val pad = Offset(8.dp.toPx(), 4.dp.toPx())
            val tl = Offset(mid.x - layout.size.width / 2f, mid.y - layout.size.height / 2f)
            val rect = Rect(tl - pad, Size(layout.size.width + pad.x * 2, layout.size.height + pad.y * 2))
            drawRoundRect(if (selected) MpColors.ElectricLime else Color(0xE6111322), rect.topLeft, rect.size, CornerRadius(rect.height / 2))
            if (!selected) drawRoundRect(MpColors.ElectricLime.copy(alpha = 0.5f), rect.topLeft, rect.size, CornerRadius(rect.height / 2), style = Stroke(1f))
            drawText(layout, topLeft = tl)
        }
    }

    // Corner handles
    pts.forEachIndexed { i, p ->
        val selected = selection is CadSelection.Corner && selection.index == i
        val r = if (selected) 15.dp.toPx() else 12.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(MpColors.ElectricLime.copy(alpha = if (selected) 0.6f else 0.35f), Color.Transparent), p, r * 2f), r * 2f, p)
        drawCircle(if (selected) MpColors.ElectricLime else MpColors.Ink, r, p)
        drawCircle(MpColors.ElectricLime, r, p, style = Stroke(2.dp.toPx()))
        val l = tm.measure(FloorPlan.cornerLabel(i), TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) MpColors.Ink else MpColors.ElectricLime))
        drawText(l, topLeft = Offset(p.x - l.size.width / 2f, p.y - l.size.height / 2f))
    }
}

