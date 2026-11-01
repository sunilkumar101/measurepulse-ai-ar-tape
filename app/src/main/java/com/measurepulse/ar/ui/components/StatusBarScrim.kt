package com.measurepulse.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.measurepulse.ar.ui.theme.MpColors

/** Solid under the status bar, then a short fade, so scrolled content never shows behind the clock and icons. */
@Composable
fun BoxScope.StatusBarScrim() {
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MpColors.TwilightZenith))
        Box(
            Modifier.fillMaxWidth().height(18.dp)
                .background(Brush.verticalGradient(listOf(MpColors.TwilightZenith, Color.Transparent)))
        )
    }
}
