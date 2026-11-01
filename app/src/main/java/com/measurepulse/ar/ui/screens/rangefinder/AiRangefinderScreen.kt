package com.measurepulse.ar.ui.screens.rangefinder

import androidx.camera.view.PreviewView
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
import com.measurepulse.ar.ads.AdMobBannerView
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.data.ar.SensorFloorMath
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.domain.rangefinder.RangeResult
import com.measurepulse.ar.domain.rangefinder.RangefinderMath
import com.measurepulse.ar.domain.rangefinder.ReferencePreset
import com.measurepulse.ar.ui.components.AiWaveformBars
import com.measurepulse.ar.ui.components.CameraPermissionGate
import com.measurepulse.ar.ui.components.CameraPreview
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.FloatingGlassCtaBar
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.glassClickable
import com.measurepulse.ar.ui.components.rememberPressInteraction
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.tan

private val Glass = Color(18, 21, 42, (0.72f * 255).toInt())

/** Screen 5 — AI exterior height / distance rangefinder. */
@Composable
fun AiRangefinderScreen(onBack: () -> Unit, viewModel: RangefinderViewModel = hiltViewModel()) {
    CameraPermissionGate("The rangefinder measures what your camera sees.", onBack) {
        RangefinderContent(viewModel, onBack)
    }
}

@Composable
private fun RangefinderContent(vm: RangefinderViewModel, onBack: () -> Unit) {
    val orientation by vm.orientation.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()
    val preset by vm.preset.collectAsStateWithLifecycle()
    val zoom by vm.zoom.collectAsStateWithLifecycle()
    val autoTrack by vm.autoTrack.collectAsStateWithLifecycle()
    val reference by vm.reference.collectAsStateWithLifecycle()
    val baseAngle by vm.baseAngle.collectAsStateWithLifecycle()
    val final by vm.final.collectAsStateWithLifecycle()
    val detections by vm.analyzer.frames.collectAsStateWithLifecycle()
    val sys = prefs.unitSystem

    var size by remember { mutableStateOf(IntSize.Zero) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var topY by remember { mutableFloatStateOf(0f) }
    var bottomY by remember { mutableFloatStateOf(0f) }
    // Top edge of the bottom stack (banner + panel): bracket handles must stay above it to be draggable.
    var panelTop by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var bracketX by remember { mutableStateOf(0f to 0f) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { toast = it } }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }
    LaunchedEffect(size) {
        if (size.height > 0 && topY == 0f) {
            topY = size.height * 0.34f; bottomY = size.height * 0.60f
            bracketX = size.width * 0.38f to size.width * 0.62f
        }
    }
    // When the banner/panel grows (e.g. an ad fills), slide the brackets up so neither handle is covered.
    LaunchedEffect(panelTop, size) {
        val limit = panelTop - HANDLE_CLEARANCE_PX
        if (size.height > 0 && bottomY > limit) {
            val h = bottomY - topY
            bottomY = limit
            topY = (limit - h).coerceAtLeast(0f)
        }
    }
    // AI tracking: brackets follow the detected object nearest the screen centre.
    LaunchedEffect(detections, autoTrack, step) {
        val f = detections ?: return@LaunchedEffect
        if (!autoTrack || step != RangeStep.REFERENCE || size.width == 0) return@LaunchedEffect
        val c = Offset(size.width / 2f, size.height / 2f)
        val best = f.boxes.map { f.toView(it, size.width, size.height) }
            .filter { it[3] - it[1] > 40f }
            .minByOrNull { (Offset((it[0] + it[2]) / 2, (it[1] + it[3]) / 2) - c).getDistance() } ?: return@LaunchedEffect
        topY = best[1]; bottomY = best[3]; bracketX = best[0] to best[2]
    }

    // ---- Live geometry ----
    val w = size.width; val h = size.height
    val focal = if (w > 0) SensorFloorMath.focalPx(w, h, vm.intrinsics.vfovDeg) * zoom else 1f
    val cy = h / 2f
    val pitch = orientation?.pitchDeg ?: 0.0
    val roll = orientation?.rollDeg ?: 0.0
    val crossDeg = pitch
    fun rowOf(elevationDeg: Double) = (cy - focal * tan(Math.toRadians(elevationDeg - pitch))).toFloat()

    val refLive: RangeResult? = if (mode == RangeMode.AUTO_HEIGHT && step == RangeStep.REFERENCE && w > 0) {
        RangefinderMath.distanceFromReference(
            preset.heightM,
            RangefinderMath.elevationOfRow(pitch, bottomY, cy, focal),
            RangefinderMath.elevationOfRow(pitch, topY, cy, focal),
            preset.toleranceM,
        )
    } else null
    val live: RangeResult? = when (step) {
        RangeStep.REFERENCE -> refLive
        RangeStep.TARGET -> reference?.let { RangefinderMath.heightAtDistance(it.distanceM, it.baseDeg, crossDeg, it.uncertaintyM) }
        RangeStep.BASE -> RangefinderMath.groundDistanceResult(prefs.cameraHeightM, crossDeg)
        RangeStep.PEAK -> baseAngle?.let { RangefinderMath.twoAngle(prefs.cameraHeightM, it, crossDeg) }
        RangeStep.DONE -> final
    }
    val baseMarkerDeg = reference?.baseDeg ?: baseAngle

    Box(Modifier.fillMaxSize().background(Color.Black).onSizeChanged { size = it }) {
        CameraPreview(zoomRatio = zoom, analyzer = vm.analyzer, onPreviewView = { previewView = it })

        // ---- HUD overlay ----
        Canvas(
            Modifier.fillMaxSize().pointerInput(step) {
                if (step != RangeStep.REFERENCE) return@pointerInput
                var dragging = 0
                detectDragGestures(
                    onDragStart = { p ->
                        dragging = when {
                            abs(p.y - topY) < 60f && abs(p.y - bottomY) < 60f -> if (p.y < (topY + bottomY) / 2) 1 else 2
                            abs(p.y - topY) < 60f -> 1
                            abs(p.y - bottomY) < 60f -> 2
                            else -> 0
                        }
                        if (dragging != 0) vm.setAutoTrack(false)
                    },
                    onDrag = { change, d ->
                        if (dragging == 1) { change.consume(); topY = (topY + d.y).coerceIn(0f, bottomY - 20f) }
                        if (dragging == 2) { change.consume(); bottomY = (bottomY + d.y).coerceIn(topY + 20f, minOf(size.height.toFloat(), panelTop - HANDLE_CLEARANCE_PX).coerceAtLeast(topY + 20f)) }
                    },
                    onDragEnd = { dragging = 0 },
                )
            },
        ) {
            // Horizon
            if (w > 0) {
                val hy = rowOf(0.0)
                if (hy in 0f..this.size.height) drawLine(MpColors.Cyan.copy(alpha = 0.55f), Offset(0f, hy), Offset(this.size.width, hy), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            }
            // All detected objects (faint)
            detections?.let { f ->
                f.boxes.forEach { b ->
                    val r = f.toView(b, w, h)
                    drawRect(Color.White.copy(alpha = 0.35f), Offset(r[0], r[1]), Size(r[2] - r[0], r[3] - r[1]), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
                }
            }
            if (step == RangeStep.REFERENCE) drawBrackets(bracketX.first, bracketX.second, topY, bottomY)
            baseMarkerDeg?.let { b ->
                val y = rowOf(b)
                drawOval(MpColors.ElectricLime, Offset(center.x - 40.dp.toPx(), y - 10.dp.toPx()), Size(80.dp.toPx(), 20.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                drawCircle(MpColors.ElectricLime, 5.dp.toPx(), Offset(center.x, y))
            }
            drawCrosshair(active = step != RangeStep.REFERENCE)
        }

        // ---- Header ----
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Column(
                Modifier.weight(1f).clip(CircleShape).background(Glass).border(1.dp, MpColors.GlassRim, CircleShape).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(size = 7.dp); Spacer(Modifier.width(7.dp))
                    Text("AI Rangefinder", style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
                }
                Text("ML KIT · GYRO TRIG", style = MpType.LabelSpatial.copy(fontSize = 9.sp), color = MpColors.ElectricLime)
            }
            GlassCircleButton(Icons.AutoMirrored.Rounded.Undo, "Reset measurement", vm::reset)
            val zi = rememberPressInteraction()
            Box(
                Modifier.clip(CircleShape).background(Glass).border(1.dp, MpColors.GlassRim, CircleShape)
                    .glassClickable(zi, "Change zoom", vm::cycleZoom).padding(horizontal = 13.dp, vertical = 13.dp),
            ) { Text(String.format(Locale.US, "%.1fx", zoom), style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime) }
        }

        // ---- Result cards ----
        Column(
            Modifier.statusBarsPadding().padding(top = 72.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ResultCard(step, mode, live, sys)
            if (mode == RangeMode.AUTO_HEIGHT) reference?.let { ReferenceCard(it.preset, it.distanceM, it.uncertaintyM, sys, locked = true) }
                ?: refLive?.let { ReferenceCard(preset, it.distanceM, it.uncertaintyM, sys, locked = false) }
            if (abs(roll) > 4) {
                Text(
                    "Hold the phone upright — tilted ${String.format(Locale.US, "%.0f", abs(roll))}° sideways",
                    style = MpType.Chip.copy(fontSize = 12.sp), color = MpColors.Ink,
                    modifier = Modifier.clip(CircleShape).background(MpColors.Star).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            toast?.let {
                Text(it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                    modifier = Modifier.clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        // ---- Bottom panel (with the free-tier banner floating just above it) ----
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onGloballyPositioned { panelTop = it.positionInParent().y }) {
        AdMobBannerView(Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp))
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(28, 33, 58, (0.86f * 255).toInt()))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), endY = 80f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .navigationBarsPadding().padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RangeMode.entries, key = { it.name }) { m ->
                    FilterChipPill(m.label, mode == m, if (m == RangeMode.AUTO_HEIGHT) Icons.Rounded.Landscape else Icons.Rounded.ChangeHistory, onClick = { vm.setMode(m) })
                }
            }
            if (mode == RangeMode.AUTO_HEIGHT) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        if (autoTrack) ElectricLimePill("AI Track", icon = Icons.Rounded.AutoAwesome, glow = false, textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp), onClick = { vm.setAutoTrack(false) })
                        else GlassPill("AI Track off", icon = Icons.Rounded.AutoAwesome, textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp), onClick = { vm.setAutoTrack(true) })
                    }
                    items(ReferencePreset.entries, key = { it.name }) { p ->
                        val label = "${p.label} ${DimensionFormatter.formatLength(p.heightM, sys, FractionPrecision.EIGHTH)}"
                        if (preset == p) ElectricLimePill(label, glow = false, textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp))
                        else GlassPill(label, textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp), onClick = { vm.setPreset(p) })
                    }
                }
            }
            // Telemetry strip
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x66111324)).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Telemetry("TILT", String.format(Locale.US, "%+.1f°", pitch))
                Telemetry("FOCAL", "${RangefinderMath.equivalentFocalMm(vm.intrinsics.vfovDeg, zoom.toDouble())} mm")
                Telemetry("UNCERTAINTY", live?.let { "±" + DimensionFormatter.formatLength(it.uncertaintyM, sys, FractionPrecision.EIGHTH) } ?: "—")
            }
            FloatingGlassCtaBar(
                label = ctaLabel(step),
                supportingText = ctaHint(step, mode),
                icon = Icons.Rounded.Lock,
                onClick = {
                    when (step) {
                        RangeStep.REFERENCE -> vm.lockReference(refLive, RangefinderMath.elevationOfRow(pitch, bottomY, cy, focal))
                        RangeStep.BASE -> vm.lockBase(crossDeg)
                        RangeStep.TARGET, RangeStep.PEAK -> vm.lockAndSave(live, previewView?.bitmap)
                        RangeStep.DONE -> vm.reset()
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        }
    }
}

private const val HANDLE_CLEARANCE_PX = 48f

private fun ctaLabel(step: RangeStep) = when (step) {
    RangeStep.REFERENCE -> "Lock Reference Object"
    RangeStep.TARGET -> "Lock Target & Save Height"
    RangeStep.BASE -> "Lock Base Angle θ1"
    RangeStep.PEAK -> "Lock Peak θ2 & Save"
    RangeStep.DONE -> "Measure Again"
}

private fun ctaHint(step: RangeStep, mode: RangeMode) = when (step) {
    RangeStep.REFERENCE -> "Fit brackets to a known object"
    RangeStep.TARGET -> "Crosshair on the roof / tree top"
    RangeStep.BASE -> "Crosshair on its base"
    RangeStep.PEAK -> "Crosshair on the top"
    RangeStep.DONE -> if (mode == RangeMode.AUTO_HEIGHT) "Saved · AI Auto-Height" else "Saved · 2-Angle Trig"
}

@Composable
private fun ResultCard(step: RangeStep, mode: RangeMode, r: RangeResult?, sys: UnitSystem) {
    val title = when (step) {
        RangeStep.REFERENCE, RangeStep.BASE -> "DISTANCE"
        else -> "TARGET HEIGHT"
    }
    val value = when {
        r == null -> null
        step == RangeStep.REFERENCE || step == RangeStep.BASE -> r.distanceM
        else -> r.heightM
    }
    Column(
        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(24.dp)).background(Glass).border(1.dp, MpColors.GlassRim, RoundedCornerShape(24.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MpType.LabelSpatial.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
                modifier = Modifier.clip(CircleShape).background(MpColors.ElectricLime.copy(alpha = 0.15f)).padding(horizontal = 9.dp, vertical = 3.dp))
            Spacer(Modifier.width(10.dp))
            StatusDot(color = if (step == RangeStep.DONE) MpColors.ElectricLime else MpColors.Cyan, size = 6.dp)
            Spacer(Modifier.width(5.dp))
            Text(if (step == RangeStep.DONE) "LOCKED" else "LIVE", style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.IceWhite)
        }
        Spacer(Modifier.height(6.dp))
        val primary = value?.let { DimensionFormatter.formatLength(it, sys, FractionPrecision.EIGHTH) } ?: "—"
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MpColors.TextPrimary)) { append(primary) }
                if (value != null && title == "TARGET HEIGHT") withStyle(SpanStyle(fontSize = 13.sp, color = MpColors.ElectricLime)) { append(" high") }
            },
            style = MpType.MetricDisplay, maxLines = 1,
        )
        value?.let {
            Text("(" + DimensionFormatter.formatLength(it, if (sys == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC, FractionPrecision.EIGHTH) + ")",
                style = MpType.MetricSmall.copy(fontSize = 12.sp), color = MpColors.IceWhite.copy(alpha = 0.8f))
        }
        if (r != null && r.heightM != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Dist ${DimensionFormatter.formatLength(r.distanceM, sys, FractionPrecision.EIGHTH)}" +
                    (r.lineOfSightM?.let { " • LOS ${DimensionFormatter.formatLength(it, sys, FractionPrecision.EIGHTH)}" } ?: ""),
                style = MpType.MetricSmall.copy(fontSize = 12.sp), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        } else if (r == null) {
            Text(
                when (step) {
                    RangeStep.BASE -> "Aim below the horizon at the base"
                    RangeStep.REFERENCE -> if (mode == RangeMode.AUTO_HEIGHT) "Fit the brackets to the reference" else ""
                    else -> "Aim the crosshair at the top"
                },
                style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.IceWhite.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ReferenceCard(preset: ReferencePreset, distanceM: Double, uncertaintyM: Double, sys: UnitSystem, locked: Boolean) {
    Row(
        Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(22.dp)).background(Glass).border(1.dp, MpColors.GlassRim, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IridescentAiOrb(size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text("${preset.label} reference${if (locked) " · locked" else ""}", style = MpType.Chip.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
            Text(
                "Distance ${DimensionFormatter.formatLength(distanceM, sys, FractionPrecision.EIGHTH)}",
                style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, maxLines = 1,
            )
        }
        Spacer(Modifier.width(10.dp))
        AiWaveformBars(bars = 5, height = 14.dp, barWidth = 2.5.dp, gap = 2.5.dp)
        Spacer(Modifier.width(8.dp))
        Text("±" + DimensionFormatter.formatLength(uncertaintyM, sys, FractionPrecision.EIGHTH), style = MpType.MetricSmall.copy(fontWeight = FontWeight.Bold), color = MpColors.Ink,
            modifier = Modifier.clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun Telemetry(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.IceWhite.copy(alpha = 0.8f))
        Text(value, style = MpType.MetricValue.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
    }
}

private fun DrawScope.drawBrackets(left: Float, right: Float, top: Float, bottom: Float) {
    val lime = MpColors.ElectricLime
    val len = 22.dp.toPx(); val sw = 3.dp.toPx()
    listOf(Offset(left, top) to 1f, Offset(right, top) to -1f).forEach { (p, dir) ->
        drawLine(lime, p, p + Offset(dir * len, 0f), sw, StrokeCap.Round); drawLine(lime, p, p + Offset(0f, len), sw, StrokeCap.Round)
    }
    listOf(Offset(left, bottom) to 1f, Offset(right, bottom) to -1f).forEach { (p, dir) ->
        drawLine(lime, p, p + Offset(dir * len, 0f), sw, StrokeCap.Round); drawLine(lime, p, p - Offset(0f, len), sw, StrokeCap.Round)
    }
    // Drag handles for the top/bottom edges
    val hx = right + 26.dp.toPx()
    listOf(top, bottom).forEach { y ->
        drawLine(lime.copy(alpha = 0.5f), Offset(left, y), Offset(hx, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        drawCircle(lime.copy(alpha = 0.3f), 14.dp.toPx(), Offset(hx, y)); drawCircle(lime, 8.dp.toPx(), Offset(hx, y)); drawCircle(MpColors.Ink, 3.dp.toPx(), Offset(hx, y))
    }
}

private fun DrawScope.drawCrosshair(active: Boolean) {
    val c = center
    val color = if (active) MpColors.ElectricLime else Color.White.copy(alpha = 0.45f)
    val r = 34.dp.toPx()
    drawCircle(color, r, c, style = Stroke(2.dp.toPx()))
    drawCircle(color.copy(alpha = 0.25f), r * 2.6f, c, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))))
    val t = 14.dp.toPx()
    drawLine(color, Offset(c.x - r - t, c.y), Offset(c.x - r + 4f, c.y), 2.dp.toPx())
    drawLine(color, Offset(c.x + r - 4f, c.y), Offset(c.x + r + t, c.y), 2.dp.toPx())
    drawLine(color, Offset(c.x, c.y - r - t), Offset(c.x, c.y - r + 4f), 2.dp.toPx())
    drawLine(color, Offset(c.x, c.y + r - 4f), Offset(c.x, c.y + r + t), 2.dp.toPx())
    drawCircle(color, 4.dp.toPx(), c)
}
