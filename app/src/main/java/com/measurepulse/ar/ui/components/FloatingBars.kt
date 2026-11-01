package com.measurepulse.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType

private val BarGlass = Brush.verticalGradient(
    listOf(Color(0xFF2A2750).copy(alpha = 0.92f), Color(0xFF14162B).copy(alpha = 0.94f)),
)
private val BarRim = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.12f)),
)

/** Floating glass action bar: [ (+) lime circle | label | >> ]. The whole bar is one tap target. */
@Composable
fun FloatingGlassCtaBar(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    supportingText: String? = null,
) {
    val interaction = rememberPressInteraction()
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .glow(Color.Black, radius = 20.dp, alpha = 0.5f)
            .clip(CircleShape)
            .background(BarGlass)
            .border(1.dp, BarRim, CircleShape)
            .glassClickable(interaction, label, onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .limeGlow(radius = 12.dp, alpha = 0.55f)
                .size(46.dp)
                .clip(CircleShape)
                .background(MpColors.ElectricLime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MpColors.Ink, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) {
            Column2(label, supportingText)
        }
        Icon(
            Icons.Rounded.KeyboardDoubleArrowRight, null,
            tint = MpColors.ElectricLime,
            modifier = Modifier.padding(end = 12.dp).size(26.dp),
        )
    }
}

@Composable
private fun Column2(label: String, supportingText: String?) {
    androidx.compose.foundation.layout.Column {
        Text(
            label, style = MpType.Button.copy(fontSize = 15.sp), color = MpColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (supportingText != null) {
            Text(
                supportingText.uppercase(), style = MpType.LabelSpatial.copy(fontSize = 10.sp),
                color = MpColors.IceWhite.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class DockDestination { HOME, AI, TOOLS }

/**
 * Floating bottom glass dock: Home circle · centre "Help With AI" orb pill · Projects/Tools circle.
 * The selected circle turns lime with a Deep Midnight Black icon.
 */
@Composable
fun FloatingBottomGlassDock(
    selected: DockDestination,
    onHome: () -> Unit,
    onAi: () -> Unit,
    onTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .glow(Color.Black, radius = 24.dp, alpha = 0.55f)
            .clip(CircleShape)
            .background(BarGlass)
            .border(1.dp, BarRim, CircleShape)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DockCircle(Icons.Rounded.Home, "Home", selected == DockDestination.HOME, onHome)
        AiDockPill(selected == DockDestination.AI, onAi, Modifier.weight(1f))
        DockCircle(Icons.Rounded.GridView, "Projects and tools", selected == DockDestination.TOOLS, onTools)
    }
}

@Composable
private fun DockCircle(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = rememberPressInteraction()
    Box(
        Modifier
            .pressScale(interaction)
            .then(if (selected) Modifier.limeGlow(radius = 12.dp, alpha = 0.5f) else Modifier)
            .size(52.dp)
            .clip(CircleShape)
            .background(if (selected) MpColors.ElectricLime else Color.White.copy(alpha = 0.08f))
            .then(if (selected) Modifier else Modifier.border(1.dp, MpColors.GlassPillRim, CircleShape))
            .glassClickable(interaction, label, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (selected) MpColors.Ink else MpColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AiDockPill(selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val interaction = rememberPressInteraction()
    Row(
        modifier
            .pressScale(interaction)
            .height(52.dp)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(MpColors.TwilightOrchid.copy(alpha = 0.55f), Color(0xFF3B2F7A).copy(alpha = 0.45f)),
                )
            )
            .border(1.dp, if (selected) MpColors.ElectricLime else Color.White.copy(alpha = 0.22f), CircleShape)
            .glassClickable(interaction, "Help With AI", onClick)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IridescentAiOrb(size = 40.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "Help With AI", style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = MpColors.TextPrimary, maxLines = 1, softWrap = false,
        )
        Spacer(Modifier.width(8.dp))
        AiWaveformBars(bars = 5, height = 14.dp, barWidth = 2.5.dp, gap = 2.5.dp)
    }
}
