---
name: Twilight Pulse Spatial AR
colors:
  surface: '#101223'
  surface-dim: '#101223'
  surface-bright: '#36384b'
  surface-container-lowest: '#0b0d1e'
  surface-container-low: '#181a2c'
  surface-container: '#1c1e30'
  surface-container-high: '#27293b'
  surface-container-highest: '#323346'
  on-surface: '#e0e0f9'
  on-surface-variant: '#c6c9ae'
  inverse-surface: '#e0e0f9'
  inverse-on-surface: '#2d2f41'
  outline: '#90937a'
  outline-variant: '#464834'
  surface-tint: '#b9d307'
  primary: '#ffffff'
  on-primary: '#2c3400'
  primary-container: '#d5ef34'
  on-primary-container: '#5d6b00'
  inverse-primary: '#576400'
  secondary: '#eab2ff'
  on-secondary: '#510072'
  secondary-container: '#8d00c2'
  on-secondary-container: '#edbcff'
  tertiary: '#ffffff'
  on-tertiary: '#480e76'
  tertiary-container: '#f1dbff'
  on-tertiary-container: '#7f4caf'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d5ef34'
  primary-fixed-dim: '#b9d307'
  on-primary-fixed: '#191e00'
  on-primary-fixed-variant: '#414c00'
  secondary-fixed: '#f7d8ff'
  secondary-fixed-dim: '#eab2ff'
  on-secondary-fixed: '#310047'
  on-secondary-fixed-variant: '#7400a1'
  tertiary-fixed: '#f1dbff'
  tertiary-fixed-dim: '#deb7ff'
  on-tertiary-fixed: '#2d0050'
  on-tertiary-fixed-variant: '#602c8e'
  background: '#101223'
  on-background: '#e0e0f9'
  surface-variant: '#323346'
typography:
  headline-hero:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  headline-hero-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '800'
    lineHeight: 36px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: 0em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  metric-display:
    fontFamily: JetBrains Mono
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.04em
  metric-display-mobile:
    fontFamily: JetBrains Mono
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.03em
  metric-value:
    fontFamily: JetBrains Mono
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: -0.02em
  metric-unit:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.04em
  label-spatial:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.08em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  margin: 1.25rem
  margin-tablet: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.875rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes a high-precision spatial computing interface for Android AR instrumentation. The aesthetic merges the deep optical atmosphere of night-vision instrumentation with tactile aerospace luxury: Frosted Twilight Glassmorphism paired with high-voltage Electric Acid Lime precision targets.

### Personality & Emotional Response
- **Surgical Precision:** Instruments must feel calibrated to the millimeter. UI controls act as dynamic HUD telemetry overlays that respect the spatial camera feed rather than obstructing it.
- **Atmospheric Depth:** The environment recedes into deep twilight violet voids, allowing high-visibility chartreuse vectors, crosshairs, and bounding boxes to achieve instantaneous optical recognition.
- **Spatial Tactility:** Panels feel like curved optical lenses hovering in mid-air—frosted, light-catching, and specular-rimmed with micro-beveled borders.

### Design Movement: Twilight Glassmorphism
The aesthetic fuses heavy frosted glass surfaces (`backdrop-filter: blur(28px)`), rich indigo-violet radial illumination, specular gradient rims, and unapologetic hyper-contrast acid lime accents. The system avoids flat utility panels in favor of floating, pill-shaped HUD pods and an iridescent ambient audio/spatial telemetry orb.

## Colors

The palette is engineered around high luminance contrast between dark ambient spatial fields and fluorescent focal points.

### Primary Accents & Functional Reticles
- **Primary (`#DDF83D` - Electric Acid Lime):** Primary action triggers, active capture toggles, live LiDAR crosshairs, spatial dimension callouts, and leveling bubble center states. Text overlaid on `#DDF83D` must always use the deep ink tone `#111322` at bold or semi-bold weights.
- **Secondary (`#B845ED` - Hyper Magenta):** Iridescent mesh accents, secondary spatial planes, raycast hit points, audio frequency waves, and completed boundary sweeps.
- **Tertiary (`#6D3A9C` - Twilight Orchid):** Deep atmospheric edge glows, radial lighting sources behind modal cards, inactive plane grids, and volumetric depth rings.

### Dark Field & Glass Hierarchy
- **Canvas Base Background:** Linear gradient sweeping from `#272446` (zenith) to `#1C1E36` (horizon) down to `#111324` (nadir).
- **Glass Base Tint:** `rgba(35, 39, 68, 0.55)` layered with top-down specular surface shine `rgba(255, 255, 255, 0.12)`.
- **Specular Border Rim:** `1px solid rgba(255, 255, 255, 0.24)` with top edge highlights elevating to `rgba(255, 255, 255, 0.40)`.
- **Ink / Contrast Anchor:** `#111322` for high-legibility glyphs on lime surfaces.
- **Neutral Typography:** `#F3F4FB` for primary headings and metric values; `#9E9FB9` for auxiliary labels, units, and structural metadata.

## Typography

The typographic hierarchy divides duties strictly between narrative communication and numeric telemetry:

1. **Plus Jakarta Sans:** Expresses brand confidence, status messaging, scan directions, and UI navigation. Hero titles leverage bold italic styling (`font-style: italic; font-weight: 800`) to evoke forward velocity, real-time compute speeds, and futuristic spatial capture.
2. **JetBrains Mono:** Dedicated exclusively to spatial mathematics, pitch/yaw/roll telemetry, LiDAR point counts, square meters/footage, and dynamic dimensional distance anchors. Its fixed-width character grid eliminates numeric jitter during real-time 60fps tracking calculations.

### Micro Typography Rules
- Every metric numeric value (`metric-value`, `metric-display`) should sit adjacent to its dimensional unit (`metric-unit`) with a non-breaking space.
- Spatial labels (`label-spatial`) default to uppercase transformations with `+0.08em` tracking for legibility against noisy camera feeds.

## Layout & Spacing

The layout is built as a dynamic HUD (Heads-Up Display) over a primary camera viewport, transitioning into structured grid sheets for blueprint CAD reviews.

### HUD & Safe Margin Discipline
- **Camera Viewport Canvas:** AR controls float within strict device safe-zones (`safe-area-inset`). Margins are locked to `1.25rem` on mobile portrait modes to preserve thumbs-down operation without obscuring perimeter targets.
- **Top Instrumentation Cluster:** Anchored 12px below the device camera cutout/status bar, displaying tracking fidelity, battery, active lens mode, and scan engine FPS.
- **Bottom Capture Control Center:** Elevated floating glass tray hosting mode chips, the central iridescent capture orb, and acid lime primary trigger pods.

### Adaptability
- **Mobile (Phone Portrait):** Single column spatial layout. Control cards dock along the bottom half with horizontal snap-scrolling chips. Floating pill readouts hug screen corners.
- **Foldable / Tablet (Landscape AR):** Split-view HUD. Live point cloud viewfinder dominates the left 65%, while real-time CAD wireframe preview and dimensional breakdown panels anchor to the right 35% using a fluid 6-column grid with `1.5rem` gutters.

## Elevation & Depth

Spatial layering relies on physical refraction, specular light reflections, and atmospheric backglow rather than flat opacity drops or standard gray shadows.

### Elevation Tiers

- **Tier 0 (Live Spatial Feed):** Unfiltered pass-through AR camera view and point-cloud mesh grid.
- **Tier 1 (Surface Reticles & Spatial Pins):** Direct 3D world anchors. Elements use unblurred, high-glow acid lime (`#DDF83D`) with radial drop blooms: `0 0 16px rgba(221, 248, 61, 0.45)`.
- **Tier 2 (Floating Glass Pills & HUD Readouts):**
  - Background: `linear-gradient(135deg, rgba(255, 255, 255, 0.12) 0%, rgba(35, 39, 68, 0.55) 100%)`
  - Backdrop Blur: `28px` with saturation boost `backdrop-filter: blur(28px) saturate(180%)`
  - Border: `1px solid rgba(255, 255, 255, 0.24)`
  - Shadow: `0 12px 32px -4px rgba(17, 19, 36, 0.65), 0 0 1px 1px rgba(255, 255, 255, 0.15) inset`
- **Tier 3 (Modals & Blueprint Drawers):**
  - Background: `linear-gradient(180deg, rgba(39, 36, 70, 0.78) 0%, rgba(17, 19, 36, 0.92) 100%)`
  - Backdrop Blur: `36px`
  - Specular Rim: Top edge highlighted with a subtle gradient border running from `rgba(255, 255, 255, 0.45)` down to `rgba(184, 69, 237, 0.20)`.
  - Ambient Glow: Deep under-chassis illumination tinted with Tertiary Orchid: `0 24px 64px -12px rgba(109, 58, 156, 0.40)`.

## Shapes

The system relies on pill forms and large rounded-3xl contours (`28px` to full pill radii), reflecting automotive dashboard pods and optical lenses.

### Radii Architecture
- **Control Pills & Status Tags:** Full pill radius (`9999px`). Content must never collide with curved ends; enforce horizontal padding equivalent to height × 0.4.
- **Glass Viewport Cards & Modals:** `28px` (`rounded-3xl`). Delivers smooth curvature that softens the technical aesthetic.
- **Precision Focus Brackets & Reticles:** Crisp, segmented geometric framing: 90-degree corner targets with micro internal radii (`4px`) to contrast against the pill-shaped UI wrappers.

## Components

### 1. Primary Action Capture Trigger
- **Style:** Pill-shaped capsule or concentric circular ring.
- **Color:** Filled with `#DDF83D` (Electric Acid Lime). Text and icons set to `#111322` at weight 700.
- **States:**
  - *Rest:* Inner solid chartreuse core with an outer 4px pulsed tracking halo (`rgba(221, 248, 61, 0.25)`).
  - *Active / Capturing:* Scales down to 0.96; glow blooms to `0 0 24px rgba(221, 248, 61, 0.8)`.
  - *Disabled:* Frosted glass pod at `rgba(255, 255, 255, 0.08)` with `#9E9FB9` typography.

### 2. Floating Telemetry Pills
- **Style:** Single-line, compact horizontal pods with `white-space: nowrap`.
- **Structure:** Left-aligned icon or flashing scan LED, middle `JetBrains Mono` measurement value, right-aligned muted unit indicator.
- **Surface:** Frosted glass (`blur(28px)`), `1px` specular rim, `padding: 0.375rem 0.875rem`.

### 3. Spatial Iridescent Orb & Waveform Meter
- **Signature Element:** A floating 3D spherical orb exhibiting continuous organic chromatic shifts between `#6D3A9C`, `#B845ED`, and electric cyan hints.
- **Integration:** Encased in a frosted glass bezel. Flanked by vertical audio/LiDAR waveform frequency bars that oscillate in real-time acid lime (`#DDF83D`) to signal acoustic surface density and laser pulse echo returns.

### 4. Chips & Measurement Mode Selectors
- **Style:** Full pill buttons docked in horizontal scroll bars.
- **Inactive:** Glass surface (`rgba(255, 255, 255, 0.08)`), text `#F3F4FB`, border `rgba(255, 255, 255, 0.16)`.
- **Active:** Solid Electric Acid Lime (`#DDF83D`) fill with `#111322` text, accompanied by an intense subtle chartreuse drop-glow.

### 5. Input Fields & Precision Steppers
- **Style:** Recessed dark glass troughs (`rgba(17, 19, 36, 0.6)`) with `16px` corner rounding.
- **Focus:** Border transitions from subtle glass rim to `1px solid #DDF83D` accompanied by a localized `0 0 12px rgba(221, 248, 61, 0.3)` focus aura. Value typed rendered in `JetBrains Mono`.

### 6. Laser Leveling Bubble Reticle
- **Architecture:** Concentric glass guidance rings with crosshair graduations.
- **Level State:** Center level indicator turns electric chartreuse (`#DDF83D`) with dynamic crosshair snapping and an audible micro-haptic state trigger when reaching 0.0° tolerance.