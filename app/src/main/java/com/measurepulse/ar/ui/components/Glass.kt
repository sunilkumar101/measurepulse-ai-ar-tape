package com.measurepulse.ar.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.measurepulse.ar.ui.theme.MpColors
import kotlin.math.roundToInt

/** What sits behind glass cards on the current screen (set by [TwilightGradientBackground]). */
@Stable
class GlassBackdropState {
    var painter by mutableStateOf<Painter?>(null)
    var alignment: Alignment = Alignment.Center
    var origin by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropState?> { null }

val GlassCardShape = RoundedCornerShape(28.dp)

/** Specular rim: brighter on the top edge, fading down (light catching the bevel). */
private val SpecularRim = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.42f),
    0.35f to MpColors.GlassRim,
    1f to Color.White.copy(alpha = 0.14f),
)

/**
 * Smoked translucent frosted glass card: rgba(18,21,42,0.68) + backdrop blur + 1.dp specular rim, 28.dp corners.
 *
 * Compose's `Modifier.blur` blurs a node's own content, not what is behind it, so the card redraws the
 * screen's backdrop photo (from [LocalGlassBackdrop]) aligned under itself and blurs that copy.
 * Blur needs API 31+; older devices get a slightly denser tint instead.
 */
@Composable
fun SmokedFrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    tint: Color = MpColors.GlassSmoke,
    blurRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = LocalGlassBackdrop.current
    var cardPos by remember { mutableStateOf(Offset.Zero) }
    val interaction = remember { MutableInteractionSource() }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier
            .pressScale(interaction, enabled = onClick != null)
            .clip(shape)
            .then(if (backdrop?.painter != null) Modifier.onGloballyPositioned { cardPos = it.positionInRoot() } else Modifier)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction, indication = null, role = Role.Button,
                    onClickLabel = onClickLabel, onClick = onClick,
                ) else Modifier
            )
    ) {
        val painter = backdrop?.painter
        if (painter != null && canBlur && !backdrop.size.isEmpty()) {
            Box(Modifier.matchParentSize().clipToBounds()) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = backdrop.alignment,
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val p = measurable.measure(Constraints.fixed(backdrop.size.width, backdrop.size.height))
                            layout(p.width, p.height) {
                                p.place(
                                    (backdrop.origin.x - cardPos.x).roundToInt(),
                                    (backdrop.origin.y - cardPos.y).roundToInt(),
                                )
                            }
                        }
                        .blur(blurRadius),
                )
            }
        }
        val effectiveTint = if (painter != null && !canBlur) tint.copy(alpha = (tint.alpha + 0.14f).coerceAtMost(0.92f)) else tint
        Box(
            Modifier
                .matchParentSize()
                .background(effectiveTint)
                .background(
                    Brush.linearGradient(
                        0f to MpColors.GlassHighlight,
                        0.55f to Color.Transparent,
                    )
                )
                .border(1.dp, SpecularRim, shape)
        )
        Box(Modifier.padding(contentPadding), content = content)
    }
}

/** Shrinks to 0.96 while pressed (the design's "active/capturing" feel). */
fun Modifier.pressScale(interaction: MutableInteractionSource, enabled: Boolean = true): Modifier =
    if (!enabled) this else composed {
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            if (pressed) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "press",
        )
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    }

/** Shared helper for icon/text buttons that should scale on press. */
@Composable
fun rememberPressInteraction() = remember { MutableInteractionSource() }

internal fun Modifier.glassClickable(
    interaction: MutableInteractionSource,
    label: String?,
    onClick: () -> Unit,
) = clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = label, onClick = onClick)
