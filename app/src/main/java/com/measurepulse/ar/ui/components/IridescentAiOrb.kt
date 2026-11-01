package com.measurepulse.ar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.measurepulse.ar.ui.theme.MpColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * 3D iridescent AI orb: a lit violet sphere with a slowly rotating violet-blue-pink-cyan sheen,
 * specular highlight, rim shadow and a soft outer glow. [animated] = false for static thumbnails.
 */
@Composable
fun IridescentAiOrb(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = true,
    showStatusDot: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val spin by if (animated) transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin",
    ) else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(25f) }
    val breathe by if (animated) transition.animateFloat(
        0.92f, 1.0f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "breathe",
    ) else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }

    Canvas(modifier.size(size).semantics { contentDescription = "AI orb" }) {
        val r = this.size.minDimension / 2f
        val c = center
        // Outer glow
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFA855F7).copy(alpha = 0.55f * breathe), Color.Transparent),
                center = c, radius = r * 1.0f,
            ),
            radius = r,
        )
        val sr = r * 0.82f
        // Sphere body, lit from the top-left
        drawCircle(
            Brush.radialGradient(
                0f to Color.White,
                0.22f to MpColors.OrbLilac,
                0.48f to MpColors.OrbViolet,
                0.74f to MpColors.OrbIndigo,
                1f to Color(0xFF0D0133),
                center = Offset(c.x - sr * 0.32f, c.y - sr * 0.4f),
                radius = sr * 1.6f,
            ),
            radius = sr, center = c,
        )
        // Rotating iridescent sheen
        rotate(spin, c) {
            drawCircle(
                Brush.sweepGradient(
                    listOf(
                        MpColors.HyperMagenta.copy(alpha = 0.0f),
                        MpColors.OrbPink.copy(alpha = 0.45f),
                        MpColors.OrbCyan.copy(alpha = 0.30f),
                        Color(0xFF6366F1).copy(alpha = 0.35f),
                        MpColors.HyperMagenta.copy(alpha = 0.0f),
                    ),
                    center = c,
                ),
                radius = sr, center = c,
            )
        }
        // Rim shadow for volume
        drawCircle(
            Brush.radialGradient(
                0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f),
                center = c, radius = sr,
            ),
            radius = sr, center = c,
        )
        // Specular highlight
        drawOval(
            Color.White.copy(alpha = 0.85f),
            topLeft = Offset(c.x - sr * 0.55f, c.y - sr * 0.66f),
            size = Size(sr * 0.42f, sr * 0.22f),
        )
        if (showStatusDot) {
            val dotR = r * 0.16f
            val dc = Offset(c.x + sr * 0.05f, c.y + sr * 0.98f)
            drawCircle(MpColors.Ink, radius = dotR * 1.45f, center = dc)
            drawCircle(MpColors.ElectricLime, radius = dotR, center = dc)
        }
    }
}

/** Live waveform bars (white with lime accents) that oscillate like an audio/laser echo meter. */
@Composable
fun AiWaveformBars(
    modifier: Modifier = Modifier,
    bars: Int = 13,
    barWidth: Dp = 3.dp,
    gap: Dp = 3.dp,
    height: Dp = 18.dp,
    animated: Boolean = true,
) {
    val t by if (animated) rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "t",
    ) else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val limeBars = setOf(2, 6, 10)
    Canvas(modifier.size(width = (barWidth + gap) * bars - gap, height = height)) {
        val bw = barWidth.toPx()
        val step = bw + gap.toPx()
        for (i in 0 until bars) {
            val phase = i * 0.9f
            val base = 0.35f + 0.65f * ((sin(i * 1.7f) + 1f) / 2f)
            val amp = 0.55f + 0.45f * sin(t * (1f + (i % 3) * 0.35f) + phase)
            val h = (this.size.height * base * amp).coerceIn(this.size.height * 0.22f, this.size.height)
            drawRoundRect(
                color = if (i in limeBars) MpColors.ElectricLime else Color.White.copy(alpha = 0.6f + 0.35f * base),
                topLeft = Offset(i * step, this.size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f),
            )
        }
    }
}
