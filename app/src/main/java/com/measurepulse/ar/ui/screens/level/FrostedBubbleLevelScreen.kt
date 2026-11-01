package com.measurepulse.ar.ui.screens.level

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.R
import com.measurepulse.ar.domain.level.LevelMath
import com.measurepulse.ar.domain.level.LevelSurface
import com.measurepulse.ar.domain.level.LevelUnit
import com.measurepulse.ar.ui.components.CameraPermissionGate
import com.measurepulse.ar.ui.components.CameraPreview
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.LimeCircleButton
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.components.limeGlow
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

private val Glass = Color(18, 21, 42, (0.72f * 255).toInt())
private const val DIAL_RANGE_DEG = 10.0

/** Screen 7 — 2D bullseye bubble level + AR wall picture laser. */
@Composable
fun FrostedBubbleLevelScreen(onBack: () -> Unit, viewModel: LevelViewModel = hiltViewModel()) {
    val laser by viewModel.laser.collectAsStateWithLifecycle()
    if (laser) {
        CameraPermissionGate("The wall laser draws a level line over your camera view.", onBack = { viewModel.setLaser(false) }) {
            LevelContent(viewModel, onBack, laser = true)
        }
    } else {
        LevelContent(viewModel, onBack, laser = false)
    }
}

@Composable
private fun LevelContent(vm: LevelViewModel, onBack: () -> Unit, laser: Boolean) {
    val state by vm.state.collectAsStateWithLifecycle()
    val unit by vm.unit.collectAsStateWithLifecycle()
    val sound by vm.sound.collectAsStateWithLifecycle()
    val view = LocalView.current
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.messages.collect { toast = it } }
    LaunchedEffect(toast) { if (toast != null) { delay(2400); toast = null } }

    val reading = state?.reading
    // In laser mode the relevant angle is the phone's roll against the wall.
    val laserAngle = state?.let { it.rollDeg - (vm.prefs.value.levelZeroX.takeIf { reading?.surface == LevelSurface.EDGE } ?: 0.0) } ?: 0.0
    val isLevel = if (laser) reading?.surface == LevelSurface.EDGE && abs(laserAngle) <= LevelMath.LEVEL_TOLERANCE_DEG else reading?.isLevel() == true
    LaunchedEffect(isLevel) {
        if (vm.onReading(isLevel)) view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }

    val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            if (laser) {
                CameraPreview()
                WallLaser(laserAngle, isLevel && reading?.surface == LevelSurface.EDGE, unit, upright = reading?.surface == LevelSurface.EDGE)
            }
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // Header
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                    Row(
                        Modifier.weight(1f).clip(CircleShape).background(Glass).border(1.dp, MpColors.GlassRim, CircleShape).padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        StatusDot(size = 7.dp); Spacer(Modifier.width(8.dp))
                        Text(if (laser) "AR Wall Art Laser" else "Spirit Level & Wall Laser", style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
                    }
                    GlassCircleButton(
                        if (sound) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        if (sound) "Mute level beep" else "Turn on level beep", vm::toggleSound, iconTint = MpColors.ElectricLime,
                    )
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // LOCKED chip
                    if (isLevel) {
                        Row(
                            Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(CircleShape).background(Glass).border(1.5.dp, MpColors.ElectricLime, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MpColors.ElectricLime, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("LEVEL LOCKED ${LevelMath.format(0.0, unit)}", style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime)
                        }
                    }
                    if (!laser && reading != null) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            HorizontalVial(reading.x, unit)
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BullseyeDial(reading.surface, reading.x, reading.y, reading.magnitude, isLevel, unit)
                                Spacer(Modifier.width(10.dp))
                                VerticalVial(if (reading.surface == LevelSurface.FLAT) reading.y else 0.0, unit)
                            }
                        }
                    }
                    toast?.let {
                        Text(it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }

                // Bottom panel
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Color(28, 33, 58, (0.86f * 255).toInt()))
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), endY = 80f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Units: all three fit on a 360 dp phone.
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        LevelUnit.entries.forEach { u -> FilterChipPill(u.label, unit == u, null, onClick = { vm.setUnit(u) }, compact = true) }
                    }
                    // The wall laser toggle always stays visible, next to the live surface status.
                    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChipPill("AR Wall Laser", laser, Icons.Rounded.Straighten, onClick = { vm.setLaser(!laser) })
                        Row(
                            Modifier.weight(1f).clip(CircleShape).background(Color(0x66111324)).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Sensors, null, tint = MpColors.ElectricLime, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when { laser -> "Wall"; reading?.surface == LevelSurface.EDGE -> "Edge"; else -> "Flat" } + " · ±0.1°",
                                style = MpType.MetricSmall.copy(fontSize = 11.sp), color = MpColors.IceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LimeCircleButton(Icons.Rounded.Save, "Save level reading", vm::save, size = 52.dp)
                        ElectricLimePill(
                            "Calibrate Zero (${LevelMath.format(0.0, unit)})",
                            textStyle = MpType.Button.copy(fontSize = 15.sp), contentPadding = PaddingValues(vertical = 15.dp),
                            onClick = vm::calibrateZero, modifier = Modifier.weight(1f),
                        )
                        GlassCircleButton(Icons.Rounded.RestartAlt, "Reset calibration", { vm.resetZero() }, size = 52.dp, iconTint = MpColors.ElectricLime)
                    }
                }
            }
        }
    }
    if (laser) content() else TwilightGradientBackground(imageRes = R.drawable.bg_living_tv_wall) { content() }
}

@Composable
private fun BullseyeDial(surface: LevelSurface, x: Double, y: Double, magnitude: Double, level: Boolean, unit: LevelUnit) {
    Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Color(0xCC2A2D52), Color(0xE6111324)), center, r), r)
            drawCircle(Color.White.copy(alpha = 0.30f), r, style = Stroke(1.dp.toPx()))
            listOf(2.0, 5.0, 10.0).forEach { deg ->
                drawCircle(Color.White.copy(alpha = 0.22f), (r * deg / DIAL_RANGE_DEG).toFloat() - 2f, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            }
            drawLine(Color.White.copy(alpha = 0.18f), Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.18f), Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1.dp.toPx())
            listOf(0f, 90f, 180f, 270f).forEach { a -> rotate(a) { drawLine(MpColors.ElectricLime, Offset(center.x, 2f), Offset(center.x, 14.dp.toPx()), 2.dp.toPx(), StrokeCap.Round) } }
        }
        // Bubble: floats toward the HIGH side, clamped to the dial.
        val maxOff = 125f - 48f
        var bx = (x / DIAL_RANGE_DEG * 125).toFloat()
        var by = (-y / DIAL_RANGE_DEG * 125).toFloat()
        if (surface == LevelSurface.EDGE) by = 0f
        val len = hypot(bx, by)
        if (len > maxOff) { bx *= maxOff / len; by *= maxOff / len }
        Box(
            Modifier.offset(bx.dp, by.dp).then(if (level) Modifier.limeGlow(radius = 28.dp, alpha = 0.7f) else Modifier)
                .size(96.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFF2FF9E), MpColors.ElectricLime, Color(0xFF9DB51C)), Offset(80f, 70f), 220f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(MpColors.Ink.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(LevelMath.format(magnitude, unit), style = MpType.MetricValue.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, maxLines = 1)
                Text(if (level) "LEVEL" else if (surface == LevelSurface.EDGE) "EDGE" else "TILT", style = MpType.LabelSpatial.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun HorizontalVial(x: Double, unit: LevelUnit) {
    Row(
        Modifier.clip(CircleShape).background(Glass).border(1.dp, MpColors.GlassRim, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("X", style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.IceWhite)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(width = 120.dp, height = 22.dp).clip(CircleShape).background(MpColors.Ink), contentAlignment = Alignment.Center) {
            val off = (x / DIAL_RANGE_DEG * 46).coerceIn(-46.0, 46.0).toFloat()
            Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = 0.3f)))
            Box(Modifier.offset(off.dp).size(width = 28.dp, height = 16.dp).clip(CircleShape).background(MpColors.ElectricLime))
        }
        Spacer(Modifier.width(10.dp))
        Text(LevelMath.format(x, unit), style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, modifier = Modifier.width(64.dp))
    }
}

@Composable
private fun VerticalVial(y: Double, unit: LevelUnit) {
    Column(
        Modifier.clip(CircleShape).background(Glass).border(1.dp, MpColors.GlassRim, CircleShape).padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Y", style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.IceWhite)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(width = 22.dp, height = 100.dp).clip(CircleShape).background(MpColors.Ink), contentAlignment = Alignment.Center) {
            val off = (-y / DIAL_RANGE_DEG * 36).coerceIn(-36.0, 36.0).toFloat()
            Box(Modifier.height(1.dp).width(22.dp).background(Color.White.copy(alpha = 0.3f)))
            Box(Modifier.offset(y = off.dp).size(width = 16.dp, height = 28.dp).clip(CircleShape).background(MpColors.ElectricLime))
        }
        Spacer(Modifier.height(8.dp))
        Text(LevelMath.format(y, unit), style = MpType.MetricSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime)
    }
}

/** A world-horizontal lime laser line across the camera view (counter-rotated by the phone's roll). */
@Composable
private fun WallLaser(rollDeg: Double, level: Boolean, unit: LevelUnit, upright: Boolean) {
    if (!upright) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Hold the phone upright against the wall\nto project a level laser line",
                style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Glass).padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val len = hypot(size.width, size.height)
            val color = if (level) MpColors.ElectricLime else Color(0xFFFFB347)
            // Phone turned clockwise → negative roll → the true horizontal appears turned counter-clockwise.
            rotate(rollDeg.toFloat(), center) {
                drawLine(color.copy(alpha = 0.25f), Offset(center.x - len, center.y), Offset(center.x + len, center.y), 14.dp.toPx())
                drawLine(color.copy(alpha = 0.5f), Offset(center.x - len, center.y), Offset(center.x + len, center.y), 6.dp.toPx())
                drawLine(color, Offset(center.x - len, center.y), Offset(center.x + len, center.y), 2.5.dp.toPx())
                listOf(-0.42f, 0.42f).forEach { f ->
                    val p = Offset(center.x + size.width * f, center.y)
                    drawCircle(color.copy(alpha = 0.35f), 16.dp.toPx(), p); drawCircle(color, 9.dp.toPx(), p); drawCircle(MpColors.Ink, 4.dp.toPx(), p)
                }
            }
            // Fixed screen reference so the user sees which way to turn.
            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }
        Column(
            Modifier.offset(y = 70.dp).clip(RoundedCornerShape(20.dp)).background(Glass).border(1.dp, if (level) MpColors.ElectricLime else MpColors.GlassRim, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(LevelMath.format(rollDeg, unit), style = MpType.MetricDisplay.copy(fontSize = 26.sp), color = if (level) MpColors.ElectricLime else MpColors.TextPrimary)
            Text(
                if (level) "LEVEL — MARK YOUR NAIL" else if (rollDeg > 0) "ROTATE RIGHT ↻" else "ROTATE LEFT ↺",
                style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.IceWhite,
            )
        }
    }
}

