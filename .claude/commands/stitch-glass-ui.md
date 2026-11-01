---
description: Implement a screen from ./design_screenshots/ in Jetpack Compose using the Frosted Twilight Glass + Electric Lime design
---

Read ./design_screenshots/ (HTML, PNG, and DESIGN.md) and implement the exact Frosted Twilight Glass & Electric Lime UI in Jetpack Compose:

- Twilight Indigo gradient background (#272446 -> #1C1E36 -> #111324).
- Electric Acid Lime (#DDF83D) with forced Deep Midnight Black (#111322) bold text/icons on all active pills and circular CTA buttons.
- Smoked Translucent Frosted Glass cards: rgba(18, 21, 42, 0.68) with blur and a 1.dp rgba(255,255,255,0.28) specular border, over high-res twilight architectural room backgrounds.
- 3D Iridescent Violet-Blue-Pink AI Orb with waveform.
- High-visibility Ice White (#E2E8F0) subtitles.

Steps:
1. Open `design_screenshots/stitch_measurepulse_ai_ar_measurement/<screen>/screen.png` and `code.html` for the target screen, plus `twilight_pulse_spatial_ar/DESIGN.md` for tokens.
2. Reuse the shared theme/components in the app (GlassCard, LimePill, LimeCircleButton, AiOrb, TwilightBackground) — don't duplicate styles.
3. Fonts: Plus Jakarta Sans for text, JetBrains Mono for every numeric measurement and unit.
4. Never put white or light text on #DDF83D; always #111322.

Target screen: $ARGUMENTS
