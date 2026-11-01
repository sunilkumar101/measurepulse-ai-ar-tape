package com.measurepulse.ar.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import com.measurepulse.ar.ui.theme.MpColors

/** Twilight Indigo canvas: #272446 -> #1C1E36 -> #111324. */
val TwilightGradient = Brush.verticalGradient(
    0f to MpColors.TwilightZenith,
    0.5f to MpColors.TwilightHorizon,
    1f to MpColors.TwilightNadir,
)

/**
 * Full-screen twilight background.
 *
 * - With [imageRes]: a full-bleed architectural photo with top/bottom readability vignettes.
 *   The photo is also published as the glass backdrop, so [SmokedFrostedGlassCard]s on top
 *   show a real blurred copy of it (true frosted glass on API 31+).
 * - Without: the indigo gradient plus soft ambient glow orbs (violet, indigo, lime).
 */
@Composable
fun TwilightGradientBackground(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    imageAlignment: Alignment = Alignment.Center,
    ambientGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = remember { GlassBackdropState() }
    Box(
        modifier
            .fillMaxSize()
            .background(TwilightGradient)
            .onGloballyPositioned {
                backdrop.origin = it.positionInRoot()
                backdrop.size = it.size
            }
    ) {
        if (imageRes != null) {
            backdrop.painter = painterResource(imageRes)
            backdrop.alignment = imageAlignment
            // Crossfade when the screen swaps photos (e.g. onboarding pages).
            Crossfade(targetState = imageRes, animationSpec = tween(450), label = "bg") { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = imageAlignment,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Readability vignettes (top for header, bottom for actions) + indigo colour bleed.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MpColors.TwilightOrchid.copy(alpha = 0.10f))
                    .background(
                        Brush.verticalGradient(
                            0f to MpColors.DeepVoid.copy(alpha = 0.92f),
                            0.28f to MpColors.TwilightNadir.copy(alpha = 0.35f),
                            0.5f to Color.Transparent,
                            0.66f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.96f),
                        )
                    )
            )
        } else {
            backdrop.painter = null
            if (ambientGlow) AmbientGlowOrbs()
        }
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            content()
        }
    }
}

/** Soft radial light sources behind the content (no blur needed: the gradients fade to transparent). */
@Composable
fun AmbientGlowOrbs(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                fun glow(center: Offset, radius: Float, color: Color) = drawCircle(
                    brush = Brush.radialGradient(listOf(color, Color.Transparent), center, radius),
                    radius = radius,
                    center = center,
                )
                glow(Offset(size.width * 1.0f, size.height * 0.04f), size.width * 0.75f, Color(0xFF9333EA).copy(alpha = 0.30f))
                glow(Offset(0f, size.height * 0.42f), size.width * 0.7f, Color(0xFF6366F1).copy(alpha = 0.22f))
                glow(Offset(size.width, size.height * 0.8f), size.width * 0.6f, MpColors.ElectricLime.copy(alpha = 0.08f))
            }
    )
}

internal fun IntSize.isEmpty() = width == 0 || height == 0
