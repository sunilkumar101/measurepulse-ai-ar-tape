package com.measurepulse.ar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.measurepulse.ar.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_400, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_500, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_600, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_700, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_800, FontWeight.ExtraBold),
    Font(R.font.plus_jakarta_sans_800, FontWeight.Black),
    Font(R.font.plus_jakarta_sans_italic_700, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_italic_700, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_italic_800, FontWeight.ExtraBold, FontStyle.Italic),
)

/** Used for every number, unit and telemetry readout (fixed width = no jitter while values update). */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_400, FontWeight.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium),
    Font(R.font.jetbrains_mono_600, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_700, FontWeight.Bold),
    Font(R.font.jetbrains_mono_700, FontWeight.ExtraBold),
    Font(R.font.jetbrains_mono_700, FontWeight.Black),
)

/** Design-system text styles beyond Material's defaults. */
object MpType {
    val HeroItalic = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
        fontSize = 38.sp, lineHeight = 41.sp, letterSpacing = (-0.03).em, color = MpColors.TextPrimary,
    )
    val HeadlineItalic = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic,
        fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em, color = MpColors.TextPrimary,
    )
    val SectionTitle = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        lineHeight = 20.sp, letterSpacing = (-0.01).em, color = MpColors.TextPrimary,
    )
    val CardTitle = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        lineHeight = 22.sp, letterSpacing = (-0.015).em, color = MpColors.TextPrimary,
    )
    val Body = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 14.sp,
        lineHeight = 21.sp, color = MpColors.IceWhite,
    )
    val Button = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
        letterSpacing = 0.01.em,
    )
    val Chip = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
    )
    val MetricDisplay = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 26.sp,
        lineHeight = 30.sp, letterSpacing = (-0.03).em, color = MpColors.TextPrimary,
    )
    val MetricValue = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
        lineHeight = 18.sp, letterSpacing = (-0.01).em, color = MpColors.IceWhite,
    )
    val MetricSmall = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp,
        lineHeight = 14.sp, color = MpColors.IceWhite,
    )
    /** Uppercase HUD labels with +0.08em tracking. Call `.uppercase()` on the text. */
    val LabelSpatial = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp,
        lineHeight = 14.sp, letterSpacing = 0.08.em, color = MpColors.TextMuted,
    )
}

val MeasurePulseTypography = Typography(
    displayLarge = MpType.HeroItalic,
    headlineLarge = MpType.HeadlineItalic,
    titleLarge = MpType.CardTitle,
    titleMedium = MpType.SectionTitle,
    bodyLarge = MpType.Body,
    bodyMedium = MpType.Body.copy(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = MpType.Button,
    labelMedium = MpType.Chip,
    labelSmall = MpType.LabelSpatial,
)
