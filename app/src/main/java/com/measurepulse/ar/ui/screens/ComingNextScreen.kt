package com.measurepulse.ar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType

/** Temporary destination for tools that are built in later prompts. */
@Composable
fun ComingNextScreen(title: String, description: String, onBack: () -> Unit) {
    TwilightGradientBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
            GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            SmokedFrostedGlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IridescentAiOrb(size = 72.dp)
                    Text(title, style = MpType.HeadlineItalic, textAlign = TextAlign.Center)
                    Text(description, style = MpType.Body, color = MpColors.IceWhite, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    ElectricLimePill("Arriving in the next build", glow = false)
                }
            }
            Spacer(Modifier.weight(1.4f))
        }
    }
}
