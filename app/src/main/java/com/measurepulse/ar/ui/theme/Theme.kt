package com.measurepulse.ar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TwilightScheme = darkColorScheme(
    primary = MpColors.ElectricLime,
    onPrimary = MpColors.Ink,
    primaryContainer = MpColors.ElectricLime,
    onPrimaryContainer = MpColors.Ink,
    secondary = MpColors.HyperMagenta,
    onSecondary = MpColors.TextPrimary,
    tertiary = MpColors.TwilightOrchid,
    onTertiary = MpColors.TextPrimary,
    background = MpColors.TwilightNadir,
    onBackground = MpColors.TextPrimary,
    surface = MpColors.TwilightHorizon,
    onSurface = MpColors.TextPrimary,
    surfaceVariant = MpColors.TwilightZenith,
    onSurfaceVariant = MpColors.IceWhite,
    outline = MpColors.GlassRim,
    outlineVariant = MpColors.Divider,
    error = MpColors.Alert,
)

/** The app is dark-only by design: twilight glass needs a dark field for contrast. */
@Composable
fun MeasurePulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TwilightScheme,
        typography = MeasurePulseTypography,
        content = content,
    )
}
