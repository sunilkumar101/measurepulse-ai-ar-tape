package com.measurepulse.ar.ui.theme

import androidx.compose.ui.graphics.Color

/** Frosted Twilight Glass & Electric Lime palette (see design_screenshots/.../DESIGN.md). */
object MpColors {
    // Canvas gradient: zenith -> horizon -> nadir
    val TwilightZenith = Color(0xFF272446)
    val TwilightHorizon = Color(0xFF1C1E36)
    val TwilightNadir = Color(0xFF111324)
    val DeepVoid = Color(0xFF0B0D1E)

    // Accents
    val ElectricLime = Color(0xFFDDF83D)
    /** The ONLY color allowed for text/icons placed on [ElectricLime]. */
    val Ink = Color(0xFF111322)
    val HyperMagenta = Color(0xFFB845ED)
    val TwilightOrchid = Color(0xFF6D3A9C)
    val OrbViolet = Color(0xFF8B5CF6)
    val OrbIndigo = Color(0xFF4338CA)
    val OrbLilac = Color(0xFFD8B4FE)
    val OrbPink = Color(0xFFF472B6)
    val OrbCyan = Color(0xFF67E8F9)
    val Cyan = Color(0xFF67E8F9)
    val Star = Color(0xFFFACC15)
    val Alert = Color(0xFFFF3B82)

    // Typography
    val TextPrimary = Color(0xFFF3F4FB)
    val IceWhite = Color(0xFFE2E8F0)
    val TextMuted = Color(0xFF9E9FB9)

    // Glass
    /** Smoked translucent card tint: rgba(18, 21, 42, 0.68). */
    val GlassSmoke = Color(18, 21, 42, (0.68f * 255).toInt())
    /** Specular rim: rgba(255,255,255,0.28). */
    val GlassRim = Color(255, 255, 255, (0.28f * 255).toInt())
    val GlassPill = Color(255, 255, 255, (0.08f * 255).toInt())
    val GlassPillRim = Color(255, 255, 255, (0.16f * 255).toInt())
    val GlassHighlight = Color(255, 255, 255, (0.12f * 255).toInt())
    val Divider = Color(255, 255, 255, (0.10f * 255).toInt())
}
