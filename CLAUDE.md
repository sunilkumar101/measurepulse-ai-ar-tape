# MeasurePulse AI — Master Project Guide

**App:** MeasurePulse AI: 3D AR Laser Tape, Room Blueprint, Caliper & Level
**Package:** `com.measurepulse.ar`
**Stack:** Native Android · Kotlin · Jetpack Compose · ARCore (optional) · CameraX · ML Kit · Jetpack Glance · AdMob + UMP · Play Billing 8 (v7 is blocked for new Play submissions since 31 Aug 2026)
**Goal:** Build → live-test on a connected Android phone (ADB) → monetize (AdMob + $2.99 Pro) → Remotion promo video → GitHub Pages SEO site + Privacy Policy + app-ads.txt → Search Console → Google Play (ASO).

---

## 10-Prompt Workflow Rule

The project runs in exactly **10 sequential prompts**. Do only the current prompt's work. When a prompt is finished, verify the work, then print:

> ✅ PROMPT [X]/10 COMPLETED — Please paste Prompt #[X+1] now.

…and wait for the next prompt.

| # | Scope |
|---|---|
| 1 | Master roadmap, skills setup, reference GitHub repos & CLAUDE.md |
| 2 | Android 16 (API 36 + 16KB page size) Gradle setup, Frosted Twilight Glass & #DDF83D Electric Lime theme, Precision Unit Engine, Screens 1–2 |
| 3 | Screen 3 — 3D AR Camera Laser Tape, anti-shake voice ("Mark") / volume-key pin, 3D box furniture fit, non-AR gyro fallback |
| 4 | Screen 4 — Room Blueprint Details, sliding frosted glass sheet, 2D CAD floorplan, scaled PDF blueprint exporter |
| 5 | Screen 5 (AI exterior height/distance rangefinder), Screen 6 (vernier caliper & thread pitch gauge), Screen 7 (2D bullseye bubble level + AR wall frame laser) |
| 6 | Screen 8 ("Help With AI" spatial room advisor: tiles, paint, AC tonnage/BTU, TV size), Screen 9 (saved projects gallery), Screen 10 (3 frosted glass Jetpack Glance home-screen widgets) |
| 7 | Google AdMob + GDPR/UMP consent + Google Play Billing ($2.99 Pro) — shipped on Billing 8.3.0 |
| 8 | Connected phone (ADB) full automated live testing, UI/UX visibility audit, self-healing bug-fix loop |
| 9 | Remotion 60fps autoplay store banner video & 8 framed frosted glass ASO screenshots |
| 10 | GitHub push, GitHub Pages SEO site + Privacy Policy + app-ads.txt, Search Console verification/sitemap, Play Console publishing |

**Status:** Prompt 1 ✅ · Prompt 2 ✅ (Gradle/API 36, theme + glass components, DimensionFormatter, Screens 1–2) · Prompt 3 ✅ (Screen 3 AR tape, dual engine) · Prompt 4 ✅ (Screen 4 blueprint, CAD editor, A4 PDF) · Prompt 5 ✅ (Screens 5–7) · Prompt 6 ✅ (Screens 8–10 + 3 Glance widgets) · Prompt 7 ✅ (AdMob + UMP + Billing Pro; 63 unit tests) · Prompt 8 ✅ (ADB audit of all 10 screens + 3 widgets; raw shots in `playstore_assets/raw_screenshots/`) · Prompt 9 ✅ (Remotion banner 16:9 + 9:16, 8 store shots, feature graphic, icon). All 10 screens are built; `ComingNextScreen` is no longer used. · Prompt 10 ✅ (signed AAB with real AdMob IDs, GitHub repo + Pages site, fastlane metadata, Play app created + v1 internal-testing draft; Search Console skipped — see `PLAY_STORE_AND_SEO_SUBMISSION_SUMMARY.md`).

### Screen 3 — AR tape (`data/ar/`, `ui/screens/artape/`)
- `SpatialMeasureController` owns the `MeasurementSession` (pure, unit-tested state for Single Line / Room Chain / 3D Box Fit / Angle) and is fed by one `MeasureEngine`:
  - **Engine A** `engine/ArCoreMeasureEngine` — GLSurfaceView + centre hit-test (Depth > Plane-in-polygon > Feature), Anchors for locked points, estimated-floor fallback on low-texture surfaces. **Untested on the dev phone (no ARCore).**
  - **Engine B** `engine/SensorFloorMeasureEngine` — CameraX preview + GAME_ROTATION_VECTOR (falls back to ROTATION_VECTOR, then accel+mag); floor ray from a configurable camera height (`UserPreferences.cameraHeightM`, default 1.50 m). Math in `SensorFloorMath` (tested).
- Anti-shake: `StabilizedSampler` locks the median of the 450 ms *before* the press (ignores the last 70 ms jolt). `JitterFilter` holds live readouts to 1 mm.
- Triggers: `HardwareKeyRouter` (MainActivity forwards Volume Up/Down while Screen 3 is open) and `VoiceMarkTrigger` (speech keyword "mark", auto-falls back to a sound-burst trigger — the Oppo test phone uses the fallback).
- Overlay: `MeasureOverlay.kt` draws lasers/anchors/labels/box in a DrawScope; the same code burns them into snapshots (`SnapshotSaver` → Pictures/MeasurePulse).
- Room Chain "Finish" saves a `RoomScanEntity` with `polygonJson` and opens `blueprint/{id}` (Screen 4).

### Screens 5–7 — tools (`domain/rangefinder|caliper|level`, `data/sensors`, `data/vision`)
- Shared: `MotionSensors.orientation()` (pitch = optical-axis elevation, roll) and `MotionSensors.gravity()` (TYPE_GRAVITY else accelerometer, low-pass α 0.12); `CameraIntrinsics.read()`; `ui/components/CameraPreview` (CameraX 4:3, zoom, optional analyzer) + `CameraPermissionGate`. Tool readings save via `RoomScanRepository.saveToolReading`.
- **Rangefinder** (`RangefinderMath`, tested): elevation(y) = pitch + atan((cy−y)/f). Mode A: reference of known height → D = H/(tan top − tan bottom), then target H = D·(tan top − tan base). Mode B: D = h/tan(−θ1), H = D·tan θ2 + h. ML Kit base detector only gives boxes (no person/car classes) → height comes from the preset; draggable brackets when nothing is detected. Zoom = CameraX zoom ratio (digital on the test phone).
- **Caliper** (`CaliperMath`, tested): px/mm = ydpi/25.4 × `caliperScale` (DataStore). Card calibration uses the 85.60 mm edge, or 53.98 mm automatically when the ruler is too short (test phone: ~78 mm usable). `boltMatches` returns *all* plausible ISO matches (shank ±0.35 mm, hex AF ±0.4 mm). Test phone: 269 PPI.
- **Level** (`LevelMath`, tested): flat (|gz| > 0.7 g) → bullseye X/Y; on edge → roll. Units °, X:12, %. Beep (ToneGenerator) + haptic on entering ±0.1°. Zero offsets in DataStore. Wall laser rotates the line by **+roll** (phone clockwise → roll negative → horizon appears counter-clockwise); needs the phone upright.
- Needs `com.google.guava:guava` (android): CameraControl returns ListenableFuture and the standalone `listenablefuture:1.0` resolves to the empty 9999 stub.

### Screens 8–10 + widgets
- **Advisor** (`domain/advisor/RoomAdvisor`, tested): tiles (+10 %, adhesive 4 kg/m² in 20 kg bags, grout by joint formula), paint (2 coats @ 10 m²/L, primer @ 12 m²/L, best 10/4/1 L can mix), AC (20 BTU/sq.ft × ceiling × sun + occupants → standard sizes), TV (SMPTE 30° – THX 36°). Rule-based and labelled as estimates; unit prices editable (locale currency). `RoomEstimator.tv` delegates to it. Route `ai_advisor?scanId=`. Shopping list PDF: `MaterialListPdfExporter`.
- **PDF storage**: all exports go to `files/exports` (`ExportLibrary`) so the Projects gallery's "Material PDFs" tab can list/re-share them.
- **Projects gallery** (`ui/screens/projects`): filters All/Floorplans/AR Fit Checks/Tool Readings/Material PDFs, search, 1-tap blueprint export, delete with confirm. Tool readings open their tool screen.
- **Widgets** (`ui/widgets`): `SpatialGlassHubWidgetReceiver` 4×2, `AiQuickConverterWidgetReceiver` 2×2, `MinimalGlassBarWidgetReceiver` 4×1. Data via Hilt `WidgetEntryPoint`; refreshed from `MeasurePulseApplication` when scans/prefs change. Taps → `WidgetRoutes.intent(route)` → `DeepLinks` → NavHost. Backgrounds are shape drawables (`widget_*`) because Glance `cornerRadius` only works on Android 12+. Widgets can't stream sensors: the level badge shows the last saved reading.
- **Screen 10** (`ui/screens/settings/GlassWidgetsAndProScreen`, route `pro`): widget previews + `requestPinGlanceAppWidget` (works on the Oppo launcher) + Pro pass card (Play Billing purchase, Restore Purchase, Privacy & ads) + debug-only monetization tools card.

### Publishing (Prompt 10) — `docs/`, `fastlane/`
- Repo: github.com/sunilkumar101/measurepulse-ai-ar-tape (public). Pages serves `docs/` from `main` at https://sunilkumar101.github.io/measurepulse-ai-ar-tape/ (index, privacy-policy, terms, 404, sitemap, robots, app-ads.txt template). Preview locally with the `docs-site` launch config (port 8080). Bump `site.css?v=` when CSS changes.
- Never commit: keystore/, keystore.properties, local.properties, service-account JSON, .cache/, .agents/, .claude/skills/. Raw phone captures must have the status bar blanked (personal notification icons).
- Privacy policy promises camera/depth/voice never leave the device: `VoiceMarkTrigger` must only use `createOnDeviceSpeechRecognizer` (else the local sound-burst trigger). Keep it that way.
- Store listing: `fastlane/metadata/android/en-US/` (title ≤30, short ≤80, full ≤4000). Lanes: `bundle exec fastlane internal | metadata` (needs Ruby + `fastlane/play-service-account.json`).

### Monetization (Prompt 7) — `ads/`, `billing/`, `data/monetization/`
- **IDs:** `app/build.gradle.kts` — debug ALWAYS uses Google's sample IDs (app, banner `/9214589741`, interstitial `/1033173712`, rewarded `/5224354917`, native `/2247696110`). Release reads `measurepulse.admob.{appId,banner,interstitial,rewarded,native}` from `local.properties` or `gradle.properties` (blank → sample IDs). Manifest app ID = `${admobAppId}` placeholder. Test-device hash for the Oppo lives in git-ignored `local.properties` (`measurepulse.admob.testDeviceIds`).
- **Startup:** `MainActivity` collects `ProEntitlement.proFlow`; only for free users → `ConsentManager.gather()` (UMP; debug = forced EEA geography on test devices) → `AdMobManager.initializeIfAllowed()` (MobileAds.initialize on IO). Pro users never see consent or load the SDK. `MeasurePulseApplication` registers `CurrentActivity` and calls `BillingManager.start()`.
- **Placements:** banner `AdMobBannerView` (anchored adaptive, takes space only once filled) on Home (top of the floating stack), Projects (above dock), Rangefinder (above bottom panel; brackets clamp above it). NO ads on Onboarding, Caliper, Level. Rewarded via `RewardedExportGate` + `MonetizationHost` dialog (in NavHost) for blueprint PDF (Screen 4 + Projects) and shopping list (Screen 8). Interstitial `showInterstitialIfEligible()` after AR-tape room close+save and after the AI audit PDF is saved.
- **Pacing:** `AdPacing` — interstitial only if ≥180 s since the last full-screen ad of any kind (rewarded counts, so no ad-after-ad). Timestamp persisted in SharedPreferences `ad_pacing`.
- **Billing:** `billing/BillingManager` (Billing 8.3.0) — INAPP `measurepulse_pro_lifetime`, acknowledge on purchase, `restorePurchases()`, startup re-sync revokes refunded passes. `ProEntitlement` (DataStore `monetization`): `purchased` + debug-only `debugOverride`. Product shows "not on sale yet" until it exists in Play Console (Prompt 10).
- **Debug tools (Screen 10, debug builds only):** Debug Pro switch, Reset consent, Reset ad cooldown, Test interstitial.
- **Ad-close safety net:** `AdMobManager.awaitAdClosed` stops waiting 2 s after one of our screens resumes without Google's dismiss callback (e.g. a widget/deep link opened MainActivity on top of AdActivity) — otherwise "Loading video…" hangs.
- **Billing messages** are a `SharedFlow` collected by the visible Screen 10 composable (not the ViewModel): a Channel let a stale back-stack copy of Screen 10 swallow Restore/Upgrade results.

### Screen 4 — Room Blueprint (`domain/floorplan/`, `ui/screens/blueprint/`, `data/export/`)
- `FloorPlan` (pure, tested): shoelace area, perimeter, volume, net wall area, `moveCorner` (15 cm snap → 90°), `setWallLength` (slides half the corners → rectangles stay rectangular), `squareUp`, split/delete corner, doors & windows. `FloorPlanCodec` stores corners/openings as JSON arrays (Room DB v3: `openingsJson`, AutoMigrations 1→2→3). Scans without a polygon open as a W×L rectangle.
- `RoomEstimator`: tiles (+10% waste), paint (10 m²/L, 2 coats), AC (20 BTU/sq.ft × ceiling factor), TV (SMPTE 30°), baseboard, furniture fit. Shown as "AI Fit Check" — rule-based estimates, labelled as such.
- `RoomBlueprintViewModel`: undo history, 400 ms debounced auto-save via `RoomScanRepository.savePlan`.
- `BlueprintPdfExporter`: native `PdfDocument`, 2 A4 pages (1:20…1:500 scaled plan + wall schedule/totals/estimates), shared via FileProvider (`${applicationId}.fileprovider`, `res/xml/file_paths.xml`). Body text uses system sans (the bundled 400 font drops word spaces in PDFs).
- Export goes through `data/monetization/ExportGate` → `RewardedExportGate` (Pro = instant; free = opt-in rewarded video; no fill/offline = unlocked anyway).

### Promo video & store assets (Prompt 9) — `promo-video/`
- Remotion 4.0.539, 60 fps. Beats live in `src/timeline.json` (seconds); scenes `src/scenes/S1Hook…S5Outro` lay themselves out from the canvas aspect, so ONE component renders both `MeasurePulseAutoplayBanner` (1920×1080) and `…Vertical` (1080×1920). Stills: `PlayStoreGlassScreenshots` folder (8 × `StoreShot`, `FeatureGraphic`, `HiResIcon`).
- Uses the app's own fonts (`public/fonts`), photos (`public/bg`) and real phone captures (`public/screens/raw`). `03_ar_laser_tape_live`/`05_rangefinder_live` are the real HUD composited over a room photo (phone camera was face-down); `*_pro` captures are ad-free (Debug Pro). Status bar is repainted (`Phone.tsx`) to hide personal notification icons.
- `npm run render:16x9 | render:9x16 | render:stills` write into `../playstore_assets/`; `npm run sheet` renders beat contact sheets to `out/sheets/` for review. Studio: `.claude/launch.json` → `remotion-studio` (port 3000).

### UI rules from the Prompt 8 audit (360 dp test phone)
- Tab/filter rows that must show every option use `FilterChipPill(compact = true)` in a `Row(SpaceBetween)` with short labels; long rows that scroll use `Modifier.edgeBleed(16.dp)` + matching contentPadding so chips are cut by the screen edge, not the padding.
- Chip groups inside cards (paint/tile chips, CAD tools) use `FlowRow`, never a clipped `Row`/`LazyRow`.
- Floating CTAs/docks sit on a bottom scrim with stops `0 → transparent, ~0.25 → TwilightNadir 0.96, 1 → TwilightNadir`; the shared `StatusBarScrim()` is solid under the status bar.
- `GlassPill` with an `Add` icon must not also start its label with "+".
- Caliper calibration: factor = nominalMm / cardMm (tested); implausible (outside 0.7–1.3) is refused, not clamped.
- Audit helper: `adb shell am start -n com.measurepulse.ar/.MainActivity -a com.measurepulse.ar.OPEN.x --es com.measurepulse.ar.ROUTE <route>` opens any route (incl. `onboarding`). Screenshots must be taken right after an action to catch the 2.6–3.2 s toasts (a uiautomator dump takes ~2 s). Oppo blocks `pm grant` too; vendor `libEGL`/`CameraManagerGlobal`/`Parcel` E-lines in the app pid are platform noise.

### Code map
- `domain/math/DimensionFormatter.kt` — Precision Unit Engine (m/cm/mm/in/ft/ft-in 1/8–1/32", m²/sq.ft, m³/cu.ft, parser). Values in SI; NBSP between value and unit.
- `ui/theme/` — `MpColors`, `MpType` (Plus Jakarta Sans + JetBrains Mono, bundled static TTFs in `res/font`), `MeasurePulseTheme`.
- `ui/components/` — `TwilightGradientBackground` (publishes photo as glass backdrop), `SmokedFrostedGlassCard` (real backdrop blur API 31+), `ElectricLimePill`, `GlassPill`, `FilterChipPill`, `GlassCircleButton`, `LimeCircleButton`, `IridescentAiOrb`, `AiWaveformBars`, `FloatingGlassCtaBar`, `FloatingBottomGlassDock`, `Modifier.glow/limeGlow/pressScale`.
- `data/` — Room `room_scans` (+ `SampleScans` seeded on first launch), DataStore `UserPreferencesRepository` (onboarding, unit system, fraction precision), `DeviceCapabilities` (ARCore availability).
- Images: `res/drawable-nodpi/bg_*.jpg` (from the Stitch designs), mapped by `ui/screens/ScanImages`.

---

## Screen ↔ Design Asset Map

All Stitch designs live in `design_screenshots/stitch_measurepulse_ai_ar_measurement/`. Each folder has `screen.png` (visual truth) and `code.html` (layout/values). Design tokens: `twilight_pulse_spatial_ar/DESIGN.md`.

| Screen | Name | Design folder |
|---|---|---|
| 1 | Onboarding / Splash | `onboarding_splash_screen/` |
| 2 | Home Spatial Command Hub | `home_spatial_command_hub/` |
| 3 | 3D AR Camera Laser Tape | `3d_ar_camera_laser_tape_measure/` |
| 4 | Room Blueprint Details | `room_blueprint_details/` |
| 5 | AI Exterior Height/Distance Rangefinder | `ai_exterior_height_distance_rangefinder/` |
| 6 | Vernier Caliper & Thread Pitch Gauge | `precision_vernier_caliper_ruler/` |
| 7 | Bullseye Bubble Level + AR Wall Frame Laser | `precision_bullseye_spirit_level_wall_laser/` |
| 8 | "Help With AI" Spatial Room Advisor | `ai_spatial_room_renovation_advisor/` |
| 9 | Saved Projects Gallery | `saved_spatial_projects_gallery/` |
| 10 | Glance Home-Screen Widgets + Pro Pass | `frosted_glass_home_widgets_pro_pass/` |

Read a design only when implementing that screen (token-saving).

---

## Design System — Frosted Twilight Glass & Electric Lime

**Hard rules (never break):**
- **Background:** vertical gradient `#272446` → `#1C1E36` → `#111324`.
- **Primary accent:** Electric Acid Lime `#DDF83D`. Any text/icon **on** lime is **always** Deep Midnight Black `#111322`, bold (700) or semi-bold. Applies to every active pill, chip, and circular CTA button.
- **Glass cards:** smoked translucent `rgba(18, 21, 42, 0.68)` + blur + `1.dp` specular border `rgba(255,255,255,0.28)`, layered over high-res twilight architectural room backgrounds. Card radius 28.dp; pills fully rounded.
- **AI Orb:** 3D iridescent violet-blue-pink orb (`#6D3A9C`, `#B845ED`, cyan hints) with lime `#DDF83D` waveform bars.
- **Subtitles:** Ice White `#E2E8F0`. Headings/metric values `#F3F4FB`; muted labels/units `#9E9FB9`.
- **Secondary:** Hyper Magenta `#B845ED`; Tertiary Twilight Orchid `#6D3A9C`.
- **Lime glow:** `0 0 16px rgba(221,248,61,0.45)` on reticles/pins; active capture glow `rgba(221,248,61,0.8)`.

**Typography:**
- Plus Jakarta Sans — all UI text; hero titles 800 weight, italic allowed.
- JetBrains Mono — **every** numeric measurement, unit, angle, and telemetry readout (no jitter at 60fps). Value and unit separated by a non-breaking space.
- Spatial labels: uppercase, +0.08em tracking.

**Layout:** respect safe areas (edge-to-edge with WindowInsets); 20.dp mobile margins; top HUD cluster 12.dp below status bar; bottom floating glass control tray.

**Compose blur note:** `Modifier.blur()` blurs the content itself, not what's behind it. Fake backdrop blur by drawing a blurred copy of the background image clipped to the card shape (API 31+ `RenderEffect`), with a solid translucent fallback below API 31.

Use `/stitch-glass-ui <screen>` to implement a screen from its design.

---

## Technical Rules

### Android 16 / Play compliance (check with `/check-android16`)
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (decide finally in Prompt 2).
- `ndkVersion = "28.2.13676358"`; `packaging.jniLibs.useLegacyPackaging = false` (16KB page size).
- `enableEdgeToEdge()` in MainActivity + proper WindowInsets on every screen.
- `android:enableOnBackInvokedCallback="true"` (predictive back).
- ARCore **optional**: `<meta-data android:name="com.google.ar.core" android:value="optional"/>` + `<uses-feature android:name="android.hardware.camera.ar" android:required="false"/>`. Every AR feature must have a non-AR fallback (gyro/sensor based).

### Code rules
- 100% original Kotlin + Jetpack Compose. Reference repos are for reading ideas only — never copy code.
- Single-activity, Compose Navigation, MVVM with `ViewModel` + `StateFlow`.
- Precision Unit Engine is the single source of truth for unit conversion/formatting (metric ↔ imperial, fractional inches).
- Secrets (keystore, service-account JSON, `local.properties`) never go into git — see `.gitignore`.
- Debug builds always use AdMob **test IDs**; real IDs go only in `measurepulse.admob.*` properties for release.

### Local environment (Windows)
- Android SDK: `C:\Users\sunil\AppData\Local\Android\Sdk` (platforms 36/36.1, build-tools 36.x, NDK 28.2.13676358 installed).
- `adb` is **not on PATH** — call it as `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"` or add platform-tools to PATH.
- System Java is JDK 25, which Gradle 8.13 can't run on. **Always** build with `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (JDK 21).
- Toolchain: Gradle 8.13 wrapper, AGP 8.13.0, Kotlin 2.2.20, KSP 2.2.20-2.0.4, Hilt 2.57.2, Compose BOM 2025.07.00 (no `Modifier.dropShadow` — use `Modifier.glow`).
- `buildToolsVersion = "36.0.0"` is pinned because the local build-tools 35.0.0 install is corrupt.
- Release builds are signed with the Play upload key from git-ignored `keystore.properties` → `keystore/measurepulse-upload.jks` (passwords in `keystore/KEYSTORE_CREDENTIALS.txt`); without it they fall back to debug signing. Release cold start ≈ 0.5 s; debug ≈ 4 s (unminified, interpreted — debug only).
- Use Git Bash for binary redirects (`adb exec-out screencap -p > build/live_screen.png`); PowerShell 5 corrupts them. Set `MSYS_NO_PATHCONV=1` so `/sdcard/...` isn't rewritten to a Windows path.
- Test phone: Oppo CPH1937, Android 11, 720×1600 (360 dp wide), **no ARCore** → app runs in "Sensor Mode". Oppo blocks `pm clear` and `cmd package compile` over ADB.
- `tools/adb_ui.py` tap prefers clickable + exact-text nodes; text fields need a tap on their placeholder before `input text`; never send BACK to dismiss a keyboard that may not be open.
- `tools/adb_ui.py dump | tap "<text>" | shot <png>` drives the UI via uiautomator. Compose `onClickLabel`s are not visible to uiautomator — tap by on-screen text/content-desc or coordinates. The 3-button nav bar starts at y=1512 on the test phone: never tap below that.
- The test phone screen sleeps during long runs: `adb shell input keyevent KEYCODE_WAKEUP` + swipe up (no PIN). Do NOT change system settings (e.g. stay-awake). Debug cold start can take ~6–9 s before Home is tappable.
- The test phone sometimes drops off ADB (Oppo resets USB mode to charge-only): re-select File transfer and re-allow USB debugging.
- 16 KB check: all arm64-v8a / x86_64 `.so` files have 16 KB LOAD alignment and pass `zipalign -c -P 16`.

---

## Reference Repos (`reference_repos/`, git-ignored)

**Token-saving rule:** don't scan these wholesale. Open only the specific file needed, when needed.

| Repo | Look here for |
|---|---|
| `AR-Measure-App` | ARCore hit-testing & 3D anchor distance math (Screen 3/4) |
| `ar-object-measure` | ML Kit object detection rangefinder (Screen 5) |
| `RulerKit` | Compose Canvas ruler tick drawing (Screen 6) |
| `bubble-level-app` | Accelerometer/magnetometer low-pass filter spirit level (Screen 7) |

---

## Project Slash Commands (`.claude/commands/`)

- `/verify-build` — `./gradlew assembleDebug`, fix errors in affected files only.
- `/stitch-glass-ui <screen>` — implement a screen from `design_screenshots/` exactly.
- `/adb-live-test <screen>` — install on phone, drive UI with adb, screenshot to `build/live_screen.png`, check contrast + logcat, fix.
- `/check-android16` — API 36 / 16KB / edge-to-edge / predictive back / ARCore-optional audit.

## Installed Skills (`.claude/skills/` → `.agents/skills/`)

- **remotion-dev/skills:** `remotion-best-practices`, `remotion-create`, `remotion-render`, `remotion-studio`, `remotion-docs`, `remotion-captions`, `remotion-markup`, `remotion-multimedia`, `remotion-interactivity`, `remotion-maps`, `remotion-saas`, `remotion-upgrade`.
- **whaleyxbt/claude-motion:** `claude-motion-design` (renamed — Remotion timing/easing/typography taste rules), `review-loop`, `sound-design`, `web-sims`.
- **LottieFiles/motion-design-skill:** `motion-design` (motion principles, easing, choreography).

Used mainly in Prompt 9 (promo video in `/promo-video/`).
