package com.measurepulse.ar.ui.screens.caliper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.domain.caliper.CaliperMath
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.FloatingGlassCtaBar
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.theme.JetBrainsMono
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val PlateGlass = Color(18, 21, 42, (0.68f * 255).toInt())
private val Violet = Color(0xFFD8B4FE)

/** Screen 6 — frosted glass vernier caliper, dual ruler & thread pitch gauge (true 1:1 scale). */
@Composable
fun FrostedCaliperScreen(onBack: () -> Unit, viewModel: CaliperViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val pitch by viewModel.pitch.collectAsStateWithLifecycle()
    val edge by viewModel.edge.collectAsStateWithLifecycle()
    val dpi = LocalContext.current.resources.displayMetrics.ydpi
    val pxPerMm = CaliperMath.pxPerMm(dpi, prefs.caliperScale).toFloat()
    val tm = rememberTextMeasurer()

    var topJaw by remember { mutableFloatStateOf(-1f) }
    var bottomJaw by remember { mutableFloatStateOf(-1f) }
    var toast by remember { mutableStateOf<String?>(null) }
    var plateHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { viewModel.messages.collect { toast = it } }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }

    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(pxPerMm) {
        if (topJaw < 0) {
            topJaw = with(density) { ORIGIN.toPx() }
            bottomJaw = topJaw + 20f * pxPerMm
        }
    }
    val gapPx = abs(bottomJaw - topJaw)
    val gapMm = if (topJaw < 0) 0.0 else gapPx / pxPerMm.toDouble()

    TwilightGradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Header
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Column(
                    Modifier.weight(1f).clip(CircleShape).background(PlateGlass).border(1.dp, MpColors.GlassRim, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(size = 7.dp); Spacer(Modifier.width(7.dp))
                        Text("Vernier Digital Caliper", style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
                    }
                    Text(
                        "${dpi.roundToInt()} PPI " + if (prefs.caliperScale != 1.0) "CARD CALIBRATED" else "FACTORY DPI",
                        style = MpType.LabelSpatial.copy(fontSize = 9.5.sp), color = MpColors.ElectricLime, maxLines = 1,
                    )
                }
                Text(
                    "1:1\nSCALE", style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp), color = MpColors.TextPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(PlateGlass).border(1.dp, MpColors.GlassRim, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            // Ruler plate (true scale)
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(32.dp)).background(PlateGlass)
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.12f))), RoundedCornerShape(32.dp))
                    .onSizeChanged { plateHeightPx = it.height.toFloat() },
            ) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(pxPerMm) {
                        var which = 0
                        detectDragGestures(
                            onDragStart = { p ->
                                val dt = abs(p.y - topJaw); val db = abs(p.y - bottomJaw)
                                which = when { minOf(dt, db) > 56.dp.toPx() -> 0; dt < db -> 1; else -> 2 }
                            },
                            onDrag = { change, d ->
                                if (which != 0) change.consume()
                                val max = size.height - 8.dp.toPx()
                                if (which == 1) topJaw = (topJaw + d.y).coerceIn(ORIGIN.toPx(), max)
                                if (which == 2) bottomJaw = (bottomJaw + d.y).coerceIn(ORIGIN.toPx(), max)
                            },
                            onDragEnd = { which = 0 },
                        )
                    },
                ) {
                    val y0 = ORIGIN.toPx()
                    drawDotGrid()
                    drawMetricScale(y0, pxPerMm, tm)
                    drawInchScale(y0, pxPerMm, tm)
                    if (tab == CaliperTab.THREAD) drawThreadComb(minOf(topJaw, bottomJaw), pitch, pxPerMm, tm)
                    if (topJaw >= 0) drawJaws(topJaw, bottomJaw, gapMm, tm, showGap = tab != CaliperTab.THREAD)
                }
                toast?.let {
                    Text(it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }

            // Bottom readout panel
            Column(
                Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(28, 33, 58, (0.80f * 255).toInt()))
                    .border(1.dp, MpColors.GlassRim, RoundedCornerShape(30.dp)).padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // All three tabs fit a 360 dp phone without clipping.
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    CaliperTab.entries.forEach { t ->
                        FilterChipPill(t.label, tab == t, icon = null, onClick = { viewModel.setTab(t) }, compact = true)
                    }
                }
                when (tab) {
                    CaliperTab.CALIBRATE -> CalibratePanel(edge, gapMm, prefs.caliperScale, plateMm = plateHeightPx / pxPerMm - 4.0, viewModel::setEdge,
                        onSet = { viewModel.calibrate(gapPx, dpi) }, onReset = { viewModel.resetCalibration() })
                    else -> {
                        Readout(gapMm, onNudge = { deltaMm -> bottomJaw = (bottomJaw + (deltaMm * pxPerMm).toFloat()).coerceAtLeast(topJaw) })
                        if (tab == CaliperTab.THREAD) PitchChips(pitch, viewModel::setPitch) else MatchCard(gapMm, dpi, prefs.caliperScale)
                        FloatingGlassCtaBar(
                            label = "Save Caliper Reading", icon = Icons.Rounded.Save,
                            supportingText = String.format(Locale.US, "%.2f mm · %s", gapMm, CaliperMath.fractionalInch(gapMm)),
                            onClick = { viewModel.save(gapMm) },
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

private val ORIGIN = 26.dp

@Composable
private fun Readout(mm: Double, onNudge: (Double) -> Unit) {
    Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MpColors.ElectricLime)) { append(String.format(Locale.US, "%.2f", mm)) }
                withStyle(SpanStyle(fontSize = 16.sp, color = MpColors.ElectricLime)) { append(" mm") }
            },
            style = MpType.MetricDisplay, maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(String.format(Locale.US, "%.3f in", CaliperMath.inches(mm)), style = MpType.MetricValue.copy(fontSize = 13.sp), color = MpColors.TextPrimary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF27293B)).padding(horizontal = 8.dp, vertical = 3.dp))
            Text(CaliperMath.fractionalInch(mm), style = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = Violet,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF27293B)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GlassCircleButton(Icons.Rounded.KeyboardArrowUp, "Close jaw 0.1 mm", { onNudge(-0.1) }, size = 34.dp)
            GlassCircleButton(Icons.Rounded.KeyboardArrowDown, "Open jaw 0.1 mm", { onNudge(0.1) }, size = 34.dp)
        }
    }
}

@Composable
private fun MatchCard(mm: Double, dpi: Float, scale: Double) {
    val matches = CaliperMath.boltMatches(mm)
    Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xCC111324)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MpColors.ElectricLime, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                matches.firstOrNull()?.let { "ISO match: ${it.label}" } ?: "No ISO bolt or hex size at this gap",
                style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (matches.size > 1) Text(
            "Also: " + matches.drop(1).take(2).joinToString { it.label },
            style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.IceWhite.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 26.dp, top = 2.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            String.format(Locale.US, "Resolution ±%.2f mm (1 px) · %d PPI · scale ×%.3f", 25.4 / dpi / scale, dpi.roundToInt(), scale),
            style = MpType.MetricSmall.copy(fontSize = 10.5.sp), color = MpColors.IceWhite.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun PitchChips(pitch: Double, onPick: (Double) -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(CaliperMath.GAUGE_PITCHES) { p ->
                val label = String.format(Locale.US, "%.2f", p).trimEnd('0').trimEnd('.')
                if (p == pitch) ElectricLimePill(label, glow = false, textStyle = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp))
                else GlassPill(label, textStyle = MpType.MetricValue.copy(fontSize = 13.sp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp), onClick = { onPick(p) })
            }
        }
        val threads = CaliperMath.threadsForPitch(pitch)
        Text(
            "Press the screw's threads onto the comb. ${String.format(Locale.US, "%.2f", pitch)} mm pitch" +
                if (threads.isNotEmpty()) " = ${threads.joinToString("/")} coarse." else " (fine thread).",
            style = MpType.Body.copy(fontSize = 12.5.sp), color = MpColors.IceWhite,
        )
    }
}

@Composable
private fun CalibratePanel(edge: CardEdge, gapMm: Double, scale: Double, plateMm: Double, onEdge: (CardEdge) -> Unit, onSet: () -> Unit, onReset: () -> Unit) {
    // Short phones can't fit the card's 85.60 mm edge between the jaws: use the 53.98 mm edge instead.
    val longFits = plateMm >= CardEdge.LONG.mm + 2
    LaunchedEffect(longFits) { if (!longFits && edge == CardEdge.LONG && plateMm > 0) onEdge(CardEdge.SHORT) }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Lay any bank/ID card on the ruler. Drag the jaws to its two edges, then tap Set.",
            style = MpType.Body.copy(fontSize = 13.sp), color = MpColors.IceWhite,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardEdge.entries.filter { it != CardEdge.LONG || longFits }.forEach { e ->
                if (e == edge) ElectricLimePill(e.label, glow = false, textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp))
                else GlassPill(e.label, textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp), onClick = { onEdge(e) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElectricLimePill(String.format(Locale.US, "Set gap = %.2f mm", edge.mm), icon = Icons.Rounded.CreditCard, onClick = onSet)
            GlassPill("Reset", onClick = onReset)
        }
        Text(
            String.format(Locale.US, "Jaws now read %.2f mm · current scale ×%.3f", gapMm, scale),
            style = MpType.MetricSmall.copy(fontSize = 11.sp), color = MpColors.IceWhite.copy(alpha = 0.8f),
        )
    }
}

// ---------------------------------------------------------------- drawing

private fun DrawScope.drawDotGrid() {
    val step = 18.dp.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) { drawCircle(Color.White.copy(alpha = 0.06f), 1.2f, Offset(x, y)); x += step }
        y += step
    }
}

private fun mono(size: Float, color: Color, bold: Boolean = false) =
    TextStyle(fontFamily = JetBrainsMono, fontSize = size.sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

private fun DrawScope.drawMetricScale(y0: Float, pxPerMm: Float, tm: TextMeasurer) {
    val x = 14.dp.toPx()
    var mm = 0
    while (true) {
        val y = y0 + mm * pxPerMm
        if (y > size.height - 4f) break
        val len = when { mm % 10 == 0 -> 26.dp.toPx(); mm % 5 == 0 -> 17.dp.toPx(); else -> 10.dp.toPx() }
        drawLine(if (mm % 10 == 0) MpColors.ElectricLime else Color.White.copy(alpha = 0.75f), Offset(x, y), Offset(x + len, y), if (mm % 10 == 0) 2.dp.toPx() else 1.dp.toPx())
        if (mm % 10 == 0) {
            val l = tm.measure(if (mm == 0) "0 cm" else (mm / 10).toString(), mono(12f, MpColors.TextPrimary, bold = true))
            drawText(l, topLeft = Offset(x + len + 6.dp.toPx(), y - l.size.height / 2f))
        }
        mm++
    }
}

private fun DrawScope.drawInchScale(y0: Float, pxPerMm: Float, tm: TextMeasurer) {
    val right = size.width - 14.dp.toPx()
    val per32 = 25.4f / 32f * pxPerMm
    val show32 = per32 >= 3.5f
    var i = 0
    while (true) {
        val y = y0 + i * per32
        if (y > size.height - 4f) break
        val len = when {
            i % 32 == 0 -> 26.dp.toPx(); i % 16 == 0 -> 20.dp.toPx(); i % 8 == 0 -> 15.dp.toPx()
            i % 4 == 0 -> 11.dp.toPx(); i % 2 == 0 -> 8.dp.toPx(); else -> 5.dp.toPx()
        }
        if (i % 2 == 0 || show32) {
            val color = when { i % 32 == 0 -> Violet; i % 16 == 0 -> Violet.copy(alpha = 0.9f); else -> Color.White.copy(alpha = 0.7f) }
            drawLine(color, Offset(right - len, y), Offset(right, y), if (i % 32 == 0) 2.dp.toPx() else 1.dp.toPx())
        }
        val label = when {
            i % 32 == 0 -> if (i == 0) "in 0" else (i / 32).toString()
            i % 16 == 0 -> "1/2"; i % 8 == 0 -> if (i % 32 == 8) "1/4" else "3/4"
            else -> null
        }
        label?.let {
            val l = tm.measure(it, mono(if (i % 32 == 0) 12f else 10f, if (i % 32 == 0) MpColors.TextPrimary else Violet, bold = i % 32 == 0))
            drawText(l, topLeft = Offset(right - len - 6.dp.toPx() - l.size.width, y - l.size.height / 2f))
        }
        i++
    }
}

private fun DrawScope.drawThreadComb(startY: Float, pitchMm: Double, pxPerMm: Float, tm: TextMeasurer) {
    val pitchPx = (pitchMm * pxPerMm).toFloat()
    val x0 = size.width * 0.50f
    val depth = 16.dp.toPx()
    val endY = minOf(startY + 40f * pxPerMm, size.height - 10f)
    val path = Path().apply {
        moveTo(x0, startY)
        var y = startY; var out = true
        while (y + pitchPx / 2 <= endY) { y += pitchPx / 2; lineTo(if (out) x0 + depth else x0, y); out = !out }
    }
    drawLine(MpColors.ElectricLime.copy(alpha = 0.35f), Offset(x0, startY), Offset(x0, endY), 6.dp.toPx())
    drawPath(path, MpColors.ElectricLime, style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Miter))
    val l = tm.measure(String.format(Locale.US, "%.2f mm pitch", pitchMm), mono(11f, MpColors.ElectricLime, bold = true))
    drawText(l, topLeft = Offset(x0 + depth + 10.dp.toPx(), startY + 6.dp.toPx()))
}

private fun DrawScope.drawJaws(top: Float, bottom: Float, gapMm: Double, tm: TextMeasurer, showGap: Boolean = true) {
    val lime = MpColors.ElectricLime
    val l = 14.dp.toPx(); val r = size.width - 14.dp.toPx()
    val a = minOf(top, bottom); val b = maxOf(top, bottom)
    drawRect(lime.copy(alpha = 0.07f), Offset(l, a), Size(r - l, b - a))
    for (y in listOf(top, bottom)) {
        drawLine(lime.copy(alpha = 0.25f), Offset(l, y), Offset(r, y), 8.dp.toPx(), StrokeCap.Round)
        drawLine(lime, Offset(l, y), Offset(r, y), 2.dp.toPx(), StrokeCap.Round)
        drawCircle(lime.copy(alpha = 0.3f), 16.dp.toPx(), Offset(r - 16.dp.toPx(), y))
        drawCircle(lime, 10.dp.toPx(), Offset(r - 16.dp.toPx(), y))
        drawCircle(MpColors.Ink, 4.dp.toPx(), Offset(r - 16.dp.toPx(), y))
    }
    // Jaw pills start right of the ruler's mm numbers so they never cover them
    fun pill(text: String, y: Float, solid: Boolean) {
        val layout = tm.measure(text, mono(12f, if (solid) MpColors.Ink else MpColors.TextPrimary, bold = true))
        val pad = Offset(12.dp.toPx(), 6.dp.toPx())
        val tl = Offset(74.dp.toPx(), y - layout.size.height / 2f - pad.y)
        val sz = Size(layout.size.width + pad.x * 2, layout.size.height + pad.y * 2)
        drawRoundRect(if (solid) lime else Color(0xE6111324), tl, sz, CornerRadius(sz.height / 2))
        drawText(layout, topLeft = tl + pad)
    }
    pill("Jaw 0.00 mm", a, solid = false)
    pill(String.format(Locale.US, "Jaw %.2f mm", gapMm), b, solid = true)
    // Gap label centred between the jaws
    // Hidden in Thread Pitch mode, where the comb sits in the middle (the readout below still shows the gap).
    if (showGap && b - a > 34.dp.toPx()) {
        val g = tm.measure(String.format(Locale.US, "GAP: %.2f mm", gapMm), mono(13f, lime, bold = true))
        val pad = Offset(12.dp.toPx(), 6.dp.toPx())
        val tl = Offset(size.width / 2 - g.size.width / 2f - pad.x + 30.dp.toPx(), (a + b) / 2 - g.size.height / 2f - pad.y)
        drawRoundRect(Color(0xE6111324), tl, Size(g.size.width + pad.x * 2, g.size.height + pad.y * 2), CornerRadius(40f))
        drawText(g, topLeft = tl + pad)
    }
}
