package com.measurepulse.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType

/**
 * Coloured bloom drawn behind an element: 0 0 16px rgba(221,248,61,0.45) by default.
 * Put it BEFORE `clip` in the modifier chain. [cornerRadius] null = pill/circle.
 * Uses a native shadow layer (hardware-accelerated on API 28+, simply absent below).
 */
fun Modifier.glow(
    color: Color = MpColors.ElectricLime,
    radius: Dp = 16.dp,
    alpha: Float = 0.45f,
    cornerRadius: Dp? = null,
) = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = alpha).toArgb())
        }
        val r = cornerRadius?.toPx() ?: (size.minDimension / 2f)
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
}

fun Modifier.limeGlow(radius: Dp = 16.dp, alpha: Float = 0.45f) = glow(MpColors.ElectricLime, radius, alpha)

/**
 * Electric Acid Lime pill (#DDF83D). Text and icon are ALWAYS Deep Midnight Black (#111322), bold.
 * Use for active chips, primary CTAs and value badges.
 */
@Composable
fun ElectricLimePill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    textStyle: TextStyle = MpType.Chip.copy(fontWeight = FontWeight.ExtraBold),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
    glow: Boolean = true,
    leadingDot: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val interaction = rememberPressInteraction()
    Row(
        modifier
            .pressScale(interaction, enabled = onClick != null)
            .then(if (glow) Modifier.limeGlow() else Modifier)
            .clip(CircleShape)
            .background(MpColors.ElectricLime)
            .then(if (onClick != null) Modifier.glassClickable(interaction, onClickLabel ?: text, onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (leadingDot) Box(Modifier.size(6.dp).clip(CircleShape).background(MpColors.Ink))
        if (icon != null) Icon(icon, null, tint = MpColors.Ink, modifier = Modifier.size(textStyle.fontSize.value.dp + 3.dp))
        Text(text, style = textStyle, color = MpColors.Ink, maxLines = 1, softWrap = false)
        if (trailingIcon != null) Icon(trailingIcon, null, tint = MpColors.Ink, modifier = Modifier.size(textStyle.fontSize.value.dp + 6.dp))
    }
}

/** Inactive glass chip: rgba(255,255,255,0.08) with a 0.16 rim and near-white text. */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MpColors.IceWhite.copy(alpha = 0.85f),
    textStyle: TextStyle = MpType.Chip,
    textColor: Color = MpColors.TextPrimary,
    fill: Color = Color(0xFF14162B).copy(alpha = 0.55f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
    onClick: (() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = rememberPressInteraction()
    Row(
        modifier
            .pressScale(interaction, enabled = onClick != null)
            .clip(CircleShape)
            .background(fill)
            .background(MpColors.GlassPill)
            .border(1.dp, MpColors.GlassPillRim, CircleShape)
            .then(if (onClick != null) Modifier.glassClickable(interaction, text, onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke(this)
        if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(textStyle.fontSize.value.dp + 3.dp))
        Text(text, style = textStyle, color = textColor, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
    }
}

/** Selectable filter chip: lime when [selected], glass otherwise. */
@Composable
fun FilterChipPill(
    text: String,
    selected: Boolean,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        // Tighter variant for tab rows that must fit a 360 dp phone without clipping.
        val style = MpType.Chip.copy(fontSize = 13.sp)
        val padding = PaddingValues(horizontal = 11.dp, vertical = 9.dp)
        if (selected) ElectricLimePill(text = text, leadingDot = true, onClick = onClick, modifier = modifier, textStyle = style.copy(fontWeight = FontWeight.ExtraBold), contentPadding = padding)
        else GlassPill(text = text, onClick = onClick, modifier = modifier, textStyle = style, contentPadding = padding)
    } else if (selected) {
        ElectricLimePill(text = text, leadingDot = true, onClick = onClick, modifier = modifier)
    } else {
        GlassPill(text = text, icon = icon, onClick = onClick, modifier = modifier)
    }
}

/** Circular glass icon button (notifications, favourite, open arrow). */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconTint: Color = MpColors.TextPrimary,
    badge: Boolean = false,
) {
    val interaction = rememberPressInteraction()
    Box(
        modifier
            .defaultMinSize(48.dp, 48.dp)
            .pressScale(interaction)
            .glassClickable(interaction, contentDescription, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF14162B).copy(alpha = 0.6f))
                .background(MpColors.GlassHighlight)
                .border(1.dp, MpColors.GlassRim, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(size * 0.45f))
            if (badge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = size * 0.2f, end = size * 0.2f)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MpColors.Alert)
                )
            }
        }
    }
}

/** Solid lime circular CTA with a Deep Midnight Black icon. */
@Composable
fun LimeCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val interaction = rememberPressInteraction()
    Box(
        modifier
            .pressScale(interaction)
            .limeGlow(radius = 14.dp, alpha = 0.5f)
            .size(size)
            .clip(CircleShape)
            .background(MpColors.ElectricLime)
            .glassClickable(interaction, contentDescription, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = MpColors.Ink, modifier = Modifier.size(size * 0.5f))
    }
}

/** Small mono badge used for counts like "8 Ready". */
@Composable
fun CountBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        style = MpType.MetricSmall.copy(fontSize = 11.sp),
        color = MpColors.IceWhite.copy(alpha = 0.85f),
        maxLines = 1,
    )
}

/** Pulsing status LED dot (lime). */
@Composable
fun StatusDot(modifier: Modifier = Modifier, color: Color = MpColors.ElectricLime, size: Dp = 7.dp) {
    Box(
        modifier
            .glow(color, radius = 6.dp, alpha = 0.8f)
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
