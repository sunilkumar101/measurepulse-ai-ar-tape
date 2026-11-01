package com.measurepulse.ar.ui.screens.artape

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Polyline
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.data.ArSupport
import com.measurepulse.ar.data.ar.BoxPresets
import com.measurepulse.ar.data.ar.EngineKind
import com.measurepulse.ar.data.ar.MeasureMode
import com.measurepulse.ar.data.ar.MeasurementSession
import com.measurepulse.ar.data.ar.TrackingStatus
import com.measurepulse.ar.data.ar.VoiceMarkTrigger
import com.measurepulse.ar.data.ar.engine.MeasureEngine
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.components.AiWaveformBars
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.FloatingGlassCtaBar
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.components.glassClickable
import com.measurepulse.ar.ui.components.limeGlow
import com.measurepulse.ar.ui.components.rememberPressInteraction
import com.measurepulse.ar.ui.theme.JetBrainsMono
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Screen 3 — full-screen 3D AR camera laser tape. */
@Composable
fun ArTapeMeasureScreen(
    onBack: () -> Unit,
    onOpenBlueprint: (Long) -> Unit,
    viewModel: ArTapeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraAsked by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it; cameraAsked = true
    }
    LaunchedEffect(Unit) { if (!cameraGranted) cameraLauncher.launch(Manifest.permission.CAMERA) }

    if (!cameraGranted) {
        CameraPermissionCard(
            permanentlyDenied = cameraAsked,
            onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            },
            onBack = onBack,
        )
        return
    }
    ArTapeContent(viewModel, onBack, onOpenBlueprint)
}

@Composable
private fun ArTapeContent(vm: ArTapeViewModel, onBack: () -> Unit, onOpenBlueprint: (Long) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val engineKind by vm.engine.collectAsStateWithLifecycle()
    val arSupport by vm.arSupport.collectAsStateWithLifecycle()
    val voiceOn by vm.voiceOn.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var engineRef by remember { mutableStateOf<MeasureEngine?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var flash by remember { mutableStateOf(0) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showCustomBox by remember { mutableStateOf(false) }

    // ---- Hardware volume keys lock points ----
    val volumeHandler = remember { { vm.lockPoint(LockSource.VOLUME_KEY) } }
    DisposableEffect(Unit) {
        vm.keyRouter.register(volumeHandler)
        onDispose { vm.keyRouter.unregister(volumeHandler) }
    }

    // ---- Mic follows the screen lifecycle ----
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) vm.onScreenVisible(true)
            if (e == Lifecycle.Event.ON_PAUSE) vm.onScreenVisible(false)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setVoiceEnabled(true) else toast = "Microphone permission is needed for the \"Mark\" voice trigger"
    }
    val toggleVoice = {
        if (voiceOn) vm.setVoiceEnabled(false)
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.setVoiceEnabled(true)
        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // ---- Snapshot: camera frame + burned-in laser overlay ----
    val currentPrefs by rememberUpdatedState(prefs)
    val takeAnnotated: ((Bitmap?) -> Unit) -> Unit = { done ->
        val engine = engineRef
        if (engine == null) done(null) else engine.capture { cam ->
            val p = currentPrefs
            val model = buildOverlay(vm.frame.value, vm.session.value, p.unitSystem, p.fractionPrecision) { it }
            done(cam?.let { composeSnapshot(it, model, textMeasurer, density) })
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is ArTapeEvent.Message -> toast = e.text
                is ArTapeEvent.PointLocked -> {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
                    )
                    flash++
                    if (e.source != LockSource.BUTTON) toast = if (e.source == LockSource.VOICE) "Point locked by voice" else "Point locked by volume key"
                }
                is ArTapeEvent.OpenBlueprint -> onOpenBlueprint(e.scanId)
            }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ---- Camera / engine ----
        when (engineKind) {
            EngineKind.ARCORE -> ArCoreCameraLayer(vm.controller, prefs.cameraHeightM, { engineRef = it }, vm::onArCoreFailed)
            EngineKind.SENSOR_FLOOR -> SensorCameraLayer(vm.controller, prefs.cameraHeightM) { engineRef = it }
            null -> Text("Starting camera…", style = MpType.Body, modifier = Modifier.align(Alignment.Center))
        }

        // ---- Laser overlay (isolated so 60 fps frames only redraw this) ----
        LiveOverlay(vm, prefs.unitSystem, prefs.fractionPrecision, textMeasurer, flash)

        // Top scrim for legibility over bright scenes
        Box(Modifier.fillMaxWidth().height(170.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))))

        // ---- Top HUD ----
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.weight(1f))
                TitlePill(vm)
                Spacer(Modifier.weight(1f))
                GlassCircleButton(Icons.AutoMirrored.Rounded.Undo, "Undo last point", vm::undo)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EngineChip(
                    engineKind = engineKind, cameraHeightM = prefs.cameraHeightM, unitSystem = prefs.unitSystem,
                    canSwitch = arSupport == ArSupport.SUPPORTED,
                    onClick = { if (engineKind == EngineKind.SENSOR_FLOOR) showHeightDialog = true else vm.toggleEngine() },
                    onLongClick = vm::toggleEngine,
                )
                Spacer(Modifier.weight(1f))
                GlassCircleButton(Icons.Rounded.DeleteSweep, "Clear all points", vm::clear, size = 40.dp)
                GlassCircleButton(Icons.Rounded.CameraAlt, "Save annotated snapshot", {
                    takeAnnotated { vm.saveSnapshot(it) }
                }, size = 40.dp)
            }
            HintPill(vm, toast, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        }

        // ---- Bottom controls ----
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(330.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
        )
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ModeResultRow(vm, session, prefs.unitSystem, busy, onCloseRoom = {
                takeAnnotated { vm.closeRoomAndSave(it) }
            }, onCustomBox = { showCustomBox = true })

            VoicePill(vm, voiceOn, toggleVoice, Modifier.padding(horizontal = 16.dp))

            // Keep the selected mode fully on screen (Angle sits past the right edge on narrow phones).
            val modeListState = rememberLazyListState()
            LaunchedEffect(session.mode) { modeListState.animateScrollToItem(maxOf(0, session.mode.ordinal - 1)) }
            LazyRow(
                state = modeListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(MeasureMode.entries, key = { it.name }) { m ->
                    FilterChipPill(m.label, session.mode == m, m.icon, onClick = { vm.setMode(m) })
                }
            }

            FloatingGlassCtaBar(
                label = ctaLabel(session),
                supportingText = "Volume key = hands-free lock",
                onClick = {
                    if (session.canAddPoint) vm.lockPoint(LockSource.BUTTON) else vm.clear()
                },
                icon = if (session.canAddPoint) Icons.Rounded.Add else Icons.Rounded.DeleteSweep,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    if (showHeightDialog) {
        CameraHeightDialog(
            current = prefs.cameraHeightM, unitSystem = prefs.unitSystem,
            canUseArCore = arSupport == ArSupport.SUPPORTED,
            onUseArCore = { vm.toggleEngine(); showHeightDialog = false },
            onDismiss = { showHeightDialog = false },
            onConfirm = { vm.setCameraHeight(it); showHeightDialog = false },
        )
    }
    if (showCustomBox) {
        CustomBoxDialog(
            initial = session.boxItem, unitSystem = prefs.unitSystem,
            onDismiss = { showCustomBox = false },
            onConfirm = { vm.setBoxItem(it); showCustomBox = false },
        )
    }
}

private val MeasureMode.icon: ImageVector
    get() = when (this) {
        MeasureMode.SINGLE_LINE -> Icons.Rounded.Straighten
        MeasureMode.ROOM_CHAIN -> Icons.Rounded.Polyline
        MeasureMode.BOX_FIT -> Icons.Rounded.ViewInAr
        MeasureMode.ANGLE -> Icons.Rounded.Architecture
    }

private fun ctaLabel(s: MeasurementSession): String = when {
    !s.canAddPoint && s.mode == MeasureMode.ROOM_CHAIN -> "Room closed — tap to clear"
    !s.canAddPoint -> "Done — tap to clear"
    s.mode == MeasureMode.ROOM_CHAIN && s.points.isEmpty() -> "Drop First Corner"
    s.mode == MeasureMode.ROOM_CHAIN -> "Drop Corner ${s.points.size + 1}"
    s.mode == MeasureMode.BOX_FIT && s.points.isEmpty() -> "Mark Left Side of Space"
    s.mode == MeasureMode.BOX_FIT -> "Mark Right Side of Space"
    s.mode == MeasureMode.ANGLE -> listOf("Drop Arm Point A", "Drop Vertex", "Drop Arm Point C")[s.points.size]
    else -> "Drop Laser Anchor Point"
}

@Composable
private fun LiveOverlay(
    vm: ArTapeViewModel,
    system: UnitSystem,
    precision: com.measurepulse.ar.domain.math.FractionPrecision,
    textMeasurer: TextMeasurer,
    flash: Int,
) {
    val frame by vm.frame.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val model = buildOverlay(frame, session, system, precision, vm.controller::filteredLength)
    var flashAlpha by remember { mutableStateOf(0f) }
    LaunchedEffect(flash) {
        if (flash == 0) return@LaunchedEffect
        flashAlpha = 0.9f
        while (flashAlpha > 0f) { delay(16); flashAlpha = (flashAlpha - 0.06f).coerceAtLeast(0f) }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawMeasureOverlay(model, textMeasurer)
        if (flashAlpha > 0f) {
            drawCircle(MpColors.ElectricLime.copy(alpha = flashAlpha * 0.5f), 30.dp.toPx() + (1f - flashAlpha) * 60.dp.toPx(), center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
        }
    }
}

/** Renders the overlay into a copy of the camera frame and adds a small watermark. */
private fun composeSnapshot(camera: Bitmap, model: OverlayModel, tm: TextMeasurer, density: Density): Bitmap {
    val out = camera.copy(Bitmap.Config.ARGB_8888, true)
    val img = out.asImageBitmap()
    val canvas = androidx.compose.ui.graphics.Canvas(img)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, canvas, Size(out.width.toFloat(), out.height.toFloat())) {
        drawMeasureOverlay(model, tm, showReticle = false)
        val stamp = "MeasurePulse AI · " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val layout = tm.measure(stamp, TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f)))
        drawText(layout, topLeft = androidx.compose.ui.geometry.Offset(16.dp.toPx(), size.height - layout.size.height - 16.dp.toPx()))
    }
    return img.asAndroidBitmap()
}

@Composable
private fun TitlePill(vm: ArTapeViewModel) {
    val frame by vm.frame.collectAsStateWithLifecycle()
    val (state, color) = when {
        frame == null -> "Starting" to MpColors.TextMuted
        frame!!.status != TrackingStatus.TRACKING -> "Scanning" to MpColors.Star
        frame!!.reticle != null -> "Locked" to MpColors.ElectricLime
        else -> "Aim" to MpColors.Cyan
    }
    Row(
        Modifier.clip(CircleShape).background(Color(0xCC14162B)).border(1.dp, MpColors.GlassRim, CircleShape)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color = color, size = 7.dp)
        Spacer(Modifier.width(8.dp))
        Text("AR LASER TAPE", style = MpType.Chip.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(
            state, style = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = color,
            modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).border(1.dp, color.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun EngineChip(
    engineKind: EngineKind?,
    cameraHeightM: Double,
    unitSystem: UnitSystem,
    canSwitch: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val label = when (engineKind) {
        EngineKind.ARCORE -> "ARCORE DEPTH" + if (canSwitch) " · TAP FOR GYRO" else ""
        EngineKind.SENSOR_FLOOR -> "GYRO · H ${DimensionFormatter.formatLength(cameraHeightM, unitSystem)}"
        null -> "CHECKING AR"
    }
    GlassPill(
        label,
        icon = if (engineKind == EngineKind.SENSOR_FLOOR) Icons.Rounded.Edit else Icons.Rounded.ViewInAr,
        iconTint = if (engineKind == EngineKind.ARCORE) MpColors.ElectricLime else MpColors.Cyan,
        textStyle = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.sp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        fill = Color(0xCC14162B),
        onClick = onClick,
    )
}

@Composable
private fun HintPill(vm: ArTapeViewModel, toast: String?, modifier: Modifier) {
    val frame by vm.frame.collectAsStateWithLifecycle()
    val text = toast ?: frame?.hint
    AnimatedVisibility(text != null, modifier, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text.orEmpty(),
            style = MpType.Chip.copy(fontSize = 13.sp),
            color = if (toast != null) MpColors.Ink else MpColors.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (toast != null) MpColors.ElectricLime else Color(0xCC14162B))
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

/** Orb + waveform pill: "Say 'Mark' or Tap + to Lock Point", with the mic toggle. */
@Composable
private fun VoicePill(vm: ArTapeViewModel, voiceOn: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val level by vm.voiceLevel.collectAsStateWithLifecycle()
    val strategy by vm.voiceStrategy.collectAsStateWithLifecycle()
    val interaction = rememberPressInteraction()
    Row(
        modifier.fillMaxWidth().clip(CircleShape)
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(MpColors.TwilightOrchid.copy(alpha = 0.55f), Color(0xCC14162B))))
            .border(1.dp, if (voiceOn) MpColors.ElectricLime.copy(alpha = 0.7f) else MpColors.GlassRim, CircleShape)
            .glassClickable(interaction, if (voiceOn) "Turn off voice trigger" else "Turn on voice trigger", onToggle)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IridescentAiOrb(size = 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (voiceOn) "Say \"Mark\" or tap +" else "Tap + or press Volume to Lock",
                style = MpType.Chip.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    !voiceOn -> "TAP TO ENABLE VOICE \"MARK\""
                    strategy == VoiceMarkTrigger.Strategy.KEYWORD -> "LISTENING FOR \"MARK\""
                    strategy == VoiceMarkTrigger.Strategy.SOUND_BURST -> "SOUND TRIGGER · SAY \"MARK!\""
                    else -> "STARTING MIC…"
                },
                style = MpType.LabelSpatial.copy(fontSize = 9.5.sp), color = MpColors.IceWhite.copy(alpha = 0.8f), maxLines = 1,
            )
        }
        if (voiceOn) {
            Box(Modifier.size(width = 40.dp, height = 18.dp), contentAlignment = Alignment.Center) {
                AiWaveformBars(bars = 7, height = (6 + 12 * level).dp, barWidth = 2.5.dp, gap = 2.5.dp)
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (voiceOn) MpColors.ElectricLime else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (voiceOn) Icons.Rounded.Mic else Icons.Rounded.MicOff, null, tint = if (voiceOn) MpColors.Ink else MpColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Mode-specific results: room metrics + close, box presets + fit badge, angle hint. */
@Composable
private fun ModeResultRow(
    vm: ArTapeViewModel,
    session: MeasurementSession,
    system: UnitSystem,
    busy: Boolean,
    onCloseRoom: () -> Unit,
    onCustomBox: () -> Unit,
) {
    when (session.mode) {
        MeasureMode.ROOM_CHAIN -> {
            val frame by vm.frame.collectAsStateWithLifecycle()
            val metrics = frame?.let { session.roomMetrics(it.reticle, it.up) }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SmokedFrostedGlassCard(Modifier.weight(1f), shape = CircleShape, tint = Color(0xCC14162B)) {
                    Text(
                        if (metrics == null) "Walk the corners A → B → C → D"
                        else "${DimensionFormatter.formatLength(metrics.perimeterM, system)} · ${DimensionFormatter.formatArea(metrics.areaSqM, system)} · ${metrics.wallCount}W",
                        style = MpType.MetricValue.copy(fontSize = 12.sp), color = MpColors.TextPrimary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                if (session.canCloseRoom || session.roomClosed) {
                    Spacer(Modifier.width(8.dp))
                    ElectricLimePill(
                        if (busy) "Saving…" else "Finish",
                        icon = Icons.Rounded.CheckCircle,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = if (busy) null else onCloseRoom,
                        onClickLabel = "Close room and open blueprint",
                    )
                }
            }
        }
        MeasureMode.BOX_FIT -> {
            val frame by vm.frame.collectAsStateWithLifecycle()
            val fit = frame?.let { session.boxFit(it.reticle, it.up, it.cameraPosition) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val clearance = fit?.clearanceM
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        clearance == null -> GlassPill(
                            if (session.points.isEmpty()) "Mark both sides of the space" else "Now mark the other side",
                            fill = Color(0xCC14162B), textStyle = MpType.Chip.copy(fontSize = 13.sp),
                        )
                        clearance >= 0 -> ElectricLimePill(
                            "FITS WITH ${clearanceText(clearance, system)} CLEARANCE",
                            icon = Icons.Rounded.CheckCircle,
                            textStyle = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                        )
                        else -> Text(
                            "TOO WIDE BY ${clearanceText(-clearance, system).drop(1)}",
                            style = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                            color = Color.White,
                            modifier = Modifier.clip(CircleShape).background(MpColors.Alert).padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BoxPresets.all, key = { it.name }) { item ->
                        val selected = session.boxItem.name == item.name
                        if (selected) ElectricLimePill(item.name, glow = false, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
                        else GlassPill(item.name, fill = Color(0xCC14162B), textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), onClick = { vm.setBoxItem(item) })
                    }
                    item(key = "custom") {
                        val selected = session.boxItem.custom
                        if (selected) ElectricLimePill("Custom", icon = Icons.Rounded.Tune, glow = false, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold), onClick = onCustomBox)
                        else GlassPill("Custom L×W×H", icon = Icons.Rounded.Tune, fill = Color(0xCC14162B), textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), onClick = onCustomBox)
                    }
                }
            }
        }
        MeasureMode.ANGLE -> {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassPill(
                    when (session.points.size) {
                        0 -> "Mark the first arm, then the corner"
                        1 -> "Now mark the corner (vertex)"
                        2 -> "Aim along the second arm"
                        else -> session.angleDeg(null)
                            ?.let { String.format(java.util.Locale.US, "Angle locked · %.1f°", it) }
                            ?: "Points too close — tap clear and mark again"
                    },
                    fill = Color(0xCC14162B), textStyle = MpType.Chip.copy(fontSize = 13.sp),
                )
            }
        }
        MeasureMode.SINGLE_LINE -> Unit
    }
}

@Composable
private fun CameraPermissionCard(permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    TwilightGradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            SmokedFrostedGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.limeGlow().size(64.dp).clip(CircleShape).background(MpColors.ElectricLime), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = MpColors.Ink, modifier = Modifier.size(32.dp))
                    }
                    Text("Camera access needed", style = MpType.HeadlineItalic.copy(fontSize = 24.sp))
                    Text(
                        "The AR tape measures what your camera sees. Images stay on your phone — nothing is uploaded.",
                        style = MpType.Body, color = MpColors.IceWhite,
                    )
                    ElectricLimePill(
                        if (permanentlyDenied) "Open Settings" else "Allow Camera",
                        onClick = if (permanentlyDenied) onOpenSettings else onRequest,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.weight(1.3f))
        }
    }
}
