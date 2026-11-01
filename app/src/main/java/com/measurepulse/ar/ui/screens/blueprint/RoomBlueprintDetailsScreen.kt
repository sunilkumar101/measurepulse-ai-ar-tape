package com.measurepulse.ar.ui.screens.blueprint

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Polyline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.Window
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.domain.advisor.RoomEstimator
import com.measurepulse.ar.domain.floorplan.FloorPlan
import com.measurepulse.ar.domain.floorplan.OpeningType
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.FloatingGlassCtaBar
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.AiWaveformBars
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.TwilightGradient
import com.measurepulse.ar.ui.components.glassClickable
import com.measurepulse.ar.ui.components.rememberPressInteraction
import com.measurepulse.ar.ui.screens.ScanImages
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val SheetTint = Color(28, 33, 58, (0.72f * 255).toInt())
private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
private val CardFill = Color(0xFF232849).copy(alpha = 0.55f)

/** Screen 4 — Room Blueprint Details. */
@Composable
fun RoomBlueprintDetailsScreen(
    onBack: () -> Unit,
    onOpenAdvisor: () -> Unit,
    viewModel: RoomBlueprintViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val system = prefs.unitSystem
    var toast by remember { mutableStateOf<String?>(null) }
    var lengthDialog by remember { mutableStateOf<LengthEdit?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is BlueprintEvent.Share -> runCatching { context.startActivity(e.intent) }.onFailure { toast = "No app available to share the PDF" }
                is BlueprintEvent.Message -> toast = e.text
            }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }

    val s = scan
    val p = plan
    if (s == null || p == null) {
        Box(Modifier.fillMaxSize().background(TwilightGradient), contentAlignment = Alignment.Center) {
            Text(if (notFound) "This project no longer exists." else "Loading blueprint…", style = MpType.Body)
            if (notFound) GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
        }
        return
    }
    val image = ScanImages.model(s.imageKey)

    BoxWithConstraints(Modifier.fillMaxSize().background(MpColors.TwilightNadir)) {
        val density = LocalDensity.current
        val fullH = with(density) { maxHeight.toPx() }
        val heroH = maxHeight * 0.52f
        val collapsedTop = with(density) { (heroH - 32.dp).toPx() }
        val expandedTop = fullH * 0.13f
        val sheetTop = remember { Animatable(collapsedTop) }
        val scope = rememberCoroutineScope()
        // Show the canvas whenever the CAD tab opens.
        LaunchedEffect(tab) { if (tab == BlueprintTab.CAD) sheetTop.animateTo(collapsedTop, spring(stiffness = 500f)) }

        // Frosted backdrop: the room photo, heavily blurred, behind everything (the sheet tints over it).
        AsyncImage(
            model = image, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(40.dp) else Modifier),
        )
        Box(Modifier.fillMaxSize().background(MpColors.TwilightNadir.copy(alpha = 0.55f)))

        // ---------- Top 52%: photo with dimensions, or the CAD editor ----------
        // Fades out as the sheet slides up, so dimension pills don't show through the glass.
        Box(
            Modifier.fillMaxWidth().height(heroH).graphicsLayer {
                alpha = ((sheetTop.value - expandedTop) / (collapsedTop - expandedTop)).coerceIn(0f, 1f)
            }
        ) {
            if (tab == BlueprintTab.CAD) {
                CadPlanCanvas(
                    plan = p, selection = selection, unitSystem = system,
                    onSelect = viewModel::select,
                    onDragStart = viewModel::beginCornerDrag,
                    onDrag = viewModel::dragCorner,
                    modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 56.dp, bottom = 28.dp),
                )
            } else {
                HeroPhoto(image, s, p, system)
            }
        }

        // Header (over the top area)
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Row(
                Modifier.weight(1f).clip(CircleShape).background(Color(0xB3141632)).border(1.dp, MpColors.GlassRim, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(size = 7.dp)
                Spacer(Modifier.width(8.dp))
                Text("Room Details", style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
            }
            GlassCircleButton(
                if (s.isFavorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                if (s.isFavorite) "Unpin room" else "Pin room", viewModel::toggleFavorite,
                iconTint = MpColors.ElectricLime,
            )
            GlassCircleButton(Icons.Rounded.IosShare, "Share PDF blueprint", viewModel::exportPdf)
        }

        // ---------- Sliding frosted glass sheet ----------
        val dragState = rememberDraggableState { delta ->
            scope.launch { sheetTop.snapTo((sheetTop.value + delta).coerceIn(expandedTop, collapsedTop)) }
        }
        val sheetHeightDp = with(density) { (fullH - sheetTop.value).toDp() }
        Column(
            Modifier
                .offset { IntOffset(0, sheetTop.value.roundToInt()) }
                .fillMaxWidth()
                .height(sheetHeightDp)
                .clip(SheetShape)
                .background(SheetTint)
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), endY = 120f), SheetShape),
        ) {
            Column(
                Modifier.draggable(
                    dragState, Orientation.Vertical,
                    onDragStopped = { v ->
                        val mid = (expandedTop + collapsedTop) / 2
                        val target = if (v < -600f || (v < 600f && sheetTop.value < mid)) expandedTop else collapsedTop
                        sheetTop.animateTo(target, spring(stiffness = 500f))
                    },
                ).padding(horizontal = 20.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)))
                }
                SheetHeader(s, p, system)
                Spacer(Modifier.height(14.dp))
                // All three tabs stay fully visible on a 360 dp phone.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BlueprintTab.entries.forEach { t ->
                        FilterChipPill(t.label, tab == t, icon = null, onClick = { viewModel.setTab(t) }, compact = true)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            LazyColumn(
                Modifier.weight(1f),
                // Clear the floating CTA (when shown) and the system navigation bar.
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 6.dp,
                    bottom = (if (tab == BlueprintTab.CAD) 24.dp else 120.dp) +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (tab) {
                    BlueprintTab.DETAILS -> detailsTab(p, system, onOpenAdvisor) { lengthDialog = LengthEdit.Ceiling(p.ceilingM) }
                    BlueprintTab.CAD -> cadTab(p, selection, system, canUndo, viewModel) { lengthDialog = it }
                    BlueprintTab.AI_FIT -> aiFitTab(p, system, onOpenAdvisor)
                }
            }
        }

        // ---------- Floating export CTA (hidden while editing so the CAD tools stay reachable) ----------
        if (tab != BlueprintTab.CAD) Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to MpColors.TwilightNadir.copy(alpha = 0.96f), 1f to MpColors.TwilightNadir))
        )
        if (tab != BlueprintTab.CAD) FloatingGlassCtaBar(
            label = if (exporting) "Generating PDF…" else "Export PDF Blueprint",
            supportingText = "A4 · scaled plan · wall schedule",
            onClick = viewModel::exportPdf,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        )

        toast?.let {
            Text(
                it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp, start = 24.dp, end = 24.dp)
                    .clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }

    lengthDialog?.let { edit ->
        LengthDialog(
            title = when (edit) { is LengthEdit.Wall -> "Wall ${wallName(edit.index, p!!.corners.size)} length"; is LengthEdit.Ceiling -> "Ceiling height" },
            initialM = edit.currentM, system = system,
            onDismiss = { lengthDialog = null },
            onConfirm = { m ->
                when (edit) {
                    is LengthEdit.Wall -> viewModel.setWallLength(edit.index, m)
                    is LengthEdit.Ceiling -> viewModel.setCeiling(m)
                }
                lengthDialog = null
            },
        )
    }
}

sealed interface LengthEdit {
    val currentM: Double
    data class Wall(val index: Int, override val currentM: Double) : LengthEdit
    data class Ceiling(override val currentM: Double) : LengthEdit
}

private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

private fun wallName(i: Int, n: Int) = "${FloorPlan.cornerLabel(i)}→${FloorPlan.cornerLabel((i + 1) % n)}"


/** "24.8 m²" → ("24.8", "m²") so number and unit can be styled separately. */
private fun split(v: String): Pair<String, String> = v.substringBefore(DimensionFormatter.NBSP) to v.substringAfter(DimensionFormatter.NBSP, "")

// ---------------------------------------------------------------- hero

@Composable
private fun HeroPhoto(image: Any, s: RoomScanEntity, p: FloorPlan, system: UnitSystem) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(model = image, contentDescription = s.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0xCC0B0D1E), 0.35f to Color.Transparent, 0.75f to Color.Transparent, 1f to Color(0xE60B0D1E))
            )
        )
        // Glowing lime dimension vectors (width along the floor, ceiling height on the right)
        Canvas(Modifier.fillMaxSize()) {
            val lime = MpColors.ElectricLime
            val y = size.height - 88.dp.toPx()
            val x0 = 28.dp.toPx(); val x1 = size.width - 28.dp.toPx()
            drawLine(lime.copy(alpha = 0.3f), Offset(x0, y), Offset(x1, y), 8.dp.toPx(), StrokeCap.Round)
            drawLine(lime, Offset(x0, y), Offset(x1, y), 2.dp.toPx(), StrokeCap.Round)
            listOf(x0, x1).forEach { drawCircle(lime.copy(alpha = 0.35f), 12.dp.toPx(), Offset(it, y)); drawCircle(lime, 7.dp.toPx(), Offset(it, y)); drawCircle(MpColors.Ink, 3.dp.toPx(), Offset(it, y)) }
            val vx = size.width - 34.dp.toPx(); val vy0 = 128.dp.toPx(); val vy1 = y - 54.dp.toPx()
            drawLine(Brush.verticalGradient(listOf(lime, MpColors.HyperMagenta, lime), vy0, vy1), Offset(vx, vy0), Offset(vx, vy1), 2.dp.toPx(), StrokeCap.Round)
            listOf(vy0, vy1).forEach { drawCircle(lime, 5.dp.toPx(), Offset(vx, it)) }
        }
        DimPill(DimensionFormatter.formatLength(p.widthM, system, FractionPrecision.EIGHTH), Modifier.align(Alignment.BottomCenter).padding(bottom = 74.dp))
        DimPill(
            "↕ " + DimensionFormatter.formatLength(p.ceilingM, system, FractionPrecision.EIGHTH),
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 20.dp),
        )
        p.openings.firstOrNull { it.type == OpeningType.WINDOW }?.let {
            DimPill("Window " + DimensionFormatter.formatLength(it.widthM, system, FractionPrecision.EIGHTH), Modifier.align(Alignment.TopCenter).padding(top = 128.dp))
        }
        Text(
            s.qualityLabel.uppercase(),
            style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp).clip(CircleShape).background(Color(0xCC111324))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DimPill(text: String, modifier: Modifier) {
    Row(
        modifier.clip(CircleShape).background(Color(0xD9111324)).border(1.5.dp, MpColors.ElectricLime, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(size = 6.dp)
        Spacer(Modifier.width(7.dp))
        Text(text, style = MpType.MetricValue.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1)
    }
}

// ---------------------------------------------------------------- sheet header

@Composable
private fun SheetHeader(s: RoomScanEntity, p: FloorPlan, system: UnitSystem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Stacked corner thumbnails
        Box(Modifier.width((26 * minOf(p.corners.size, 4) + 10).dp).height(34.dp)) {
            val shown = minOf(p.corners.size, 3)
            val fills = listOf(Color(0xFF27293B), Color(0xFF323346), MpColors.HyperMagenta.copy(alpha = 0.85f))
            val icons = listOf(Icons.Rounded.Straighten, Icons.Rounded.CropSquare, Icons.Rounded.ViewInAr)
            for (i in 0 until shown) {
                Box(
                    Modifier.offset(x = (i * 24).dp).size(34.dp).clip(CircleShape).background(fills[i]).border(2.dp, SheetTint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(icons[i], null, tint = if (i == 0) MpColors.ElectricLime else MpColors.TextPrimary, modifier = Modifier.size(16.dp)) }
            }
            if (p.corners.size > 3) {
                Box(
                    Modifier.offset(x = (3 * 24).dp).size(34.dp).clip(CircleShape).background(Color(0xFF36384B)).border(2.dp, SheetTint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("+${p.corners.size - 3}", style = MpType.MetricSmall.copy(fontWeight = FontWeight.Bold), color = MpColors.TextPrimary) }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("${p.corners.size} Walls", style = MpType.CardTitle.copy(fontSize = 16.sp), maxLines = 1)
        Spacer(Modifier.weight(1f))
        val primary = DimensionFormatter.formatArea(p.areaSqM, system)
        val secondary = DimensionFormatter.formatArea(p.areaSqM, if (system == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC)
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xCC27293B)).padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(primary, style = MpType.MetricValue.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, maxLines = 1)
            Text("/ $secondary", style = MpType.MetricSmall.copy(fontSize = 10.sp), color = MpColors.IceWhite.copy(alpha = 0.75f), maxLines = 1)
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.title, style = MpType.HeadlineItalic.copy(fontSize = 22.sp, lineHeight = 27.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            s.category.name.replace('_', ' '),
            style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
            modifier = Modifier.clip(CircleShape).background(MpColors.ElectricLime.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
    Spacer(Modifier.height(6.dp))
    Text(
        buildAnnotatedString {
            append("Ceiling ")
            withStyle(SpanStyle(color = MpColors.ElectricLime, fontWeight = FontWeight.SemiBold)) { append(DimensionFormatter.formatLength(p.ceilingM, system, FractionPrecision.EIGHTH)) }
            append("  ·  Perimeter ")
            withStyle(SpanStyle(color = MpColors.ElectricLime, fontWeight = FontWeight.SemiBold)) { append(DimensionFormatter.formatLength(p.perimeterM, system, FractionPrecision.EIGHTH)) }
            append("  ·  ")
            withStyle(SpanStyle(color = MpColors.ElectricLime, fontWeight = FontWeight.SemiBold)) { append(DimensionFormatter.formatVolume(p.volumeCuM, system)) }
        },
        style = MpType.Body.copy(fontSize = 13.sp), color = MpColors.IceWhite, maxLines = 2,
    )
}

// ---------------------------------------------------------------- tab: details

private fun androidx.compose.foundation.lazy.LazyListScope.detailsTab(
    p: FloorPlan, system: UnitSystem, onOpenAdvisor: () -> Unit, onEditCeiling: () -> Unit,
) {
    val est = RoomEstimator.estimate(p)
    item(key = "aboutLabel") { SectionLabel(Icons.Rounded.Polyline, "About room & dimensions") }
    item(key = "about") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardFill).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                buildAnnotatedString {
                    p.walls.take(4).forEachIndexed { i, w ->
                        if (i > 0) append(", ")
                        append(if (i == 0) "Wall " else "wall ")
                        append(wallName(w.index, p.corners.size) + " ")
                        withStyle(SpanStyle(color = MpColors.ElectricLime, fontWeight = FontWeight.SemiBold, fontFamily = com.measurepulse.ar.ui.theme.JetBrainsMono)) {
                            append(DimensionFormatter.formatLength(w.lengthM, system, FractionPrecision.EIGHTH))
                        }
                        p.openingsOn(w.index).forEach { o ->
                            append(" (${if (o.type == OpeningType.DOOR) "Door" else "Window"} ${DimensionFormatter.formatLength(o.widthM, system, FractionPrecision.EIGHTH)})")
                        }
                    }
                    if (p.walls.size > 4) append(", +${p.walls.size - 4} more")
                    append(".")
                },
                style = MpType.Body.copy(fontSize = 14.5.sp, lineHeight = 22.sp), color = MpColors.TextPrimary,
            )
            // Wraps instead of clipping the second chip on narrow phones.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(Icons.Rounded.GridView, "${est.tiles.count} tiles (${est.tiles.sizeCm}×${est.tiles.sizeCm} cm)")
                InfoChip(Icons.Rounded.FormatPaint, String.format(Locale.US, "%.1f L paint", est.paint.litres))
            }
        }
    }
    item(key = "metricsLabel") { SectionLabel(Icons.Rounded.GridView, "Room spatial metrics") }
    item(key = "metrics") {
        val area = split(DimensionFormatter.formatArea(p.areaSqM, system))
        val vol = split(DimensionFormatter.formatVolume(p.volumeCuM, system))
        val per = split(DimensionFormatter.formatLength(p.perimeterM, if (system == UnitSystem.METRIC) com.measurepulse.ar.domain.math.LengthUnit.METERS else com.measurepulse.ar.domain.math.LengthUnit.FEET_DECIMAL))
        val ceil = split(DimensionFormatter.formatLength(p.ceilingM, if (system == UnitSystem.METRIC) com.measurepulse.ar.domain.math.LengthUnit.METERS else com.measurepulse.ar.domain.math.LengthUnit.FEET_DECIMAL))
        val other = if (system == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Floor area", area, DimensionFormatter.formatArea(p.areaSqM, other) + " gross", Icons.Rounded.CropSquare, Modifier.weight(1f))
                MetricCard("Enclosed volume", vol, DimensionFormatter.formatVolume(p.volumeCuM, other) + " air", Icons.Rounded.ViewInAr, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Total perimeter", per, "${p.corners.size} walls enclosed", Icons.Rounded.Straighten, Modifier.weight(1f))
                MetricCard("Ceiling height", ceil, "Tap to edit", Icons.Rounded.Height, Modifier.weight(1f), onClick = onEditCeiling)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Net wall area", split(DimensionFormatter.formatArea(p.netWallAreaSqM, system)), "Doors & windows removed", Icons.Rounded.FormatPaint, Modifier.weight(1f))
                MetricCard("Openings", p.openings.size.toString() to "", plural(p.openings.count { it.type == OpeningType.DOOR }, "door") + " · " + plural(p.openings.count { it.type == OpeningType.WINDOW }, "window"), Icons.Rounded.DoorFront, Modifier.weight(1f))
            }
        }
    }
    item(key = "askAi") { AskAiCard(onOpenAdvisor) }
}

// ---------------------------------------------------------------- tab: CAD

private fun androidx.compose.foundation.lazy.LazyListScope.cadTab(
    p: FloorPlan, selection: CadSelection, system: UnitSystem, canUndo: Boolean,
    vm: RoomBlueprintViewModel, onEditLength: (LengthEdit) -> Unit,
) {
    item(key = "hint") {
        Text(
            "Drag the lime corners · walls snap to 90° · tap a wall to add doors & windows",
            style = MpType.Body.copy(fontSize = 13.sp), color = MpColors.IceWhite.copy(alpha = 0.85f),
        )
    }
    item(key = "tools") {
        // Every CAD tool visible at once (wraps to a second line) instead of hiding past the edge.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canUndo) ToolChip(Icons.AutoMirrored.Rounded.Undo, "Undo", vm::undo)
            ToolChip(Icons.Rounded.CropSquare, "Square 90°", vm::squareUp)
            ToolChip(Icons.Rounded.DoorFront, "+ Door") { vm.addOpening(OpeningType.DOOR) }
            ToolChip(Icons.Rounded.Window, "+ Window") { vm.addOpening(OpeningType.WINDOW) }
            ToolChip(Icons.Rounded.AddCircleOutline, "Add corner", vm::splitSelectedWall)
            ToolChip(Icons.Rounded.RemoveCircleOutline, "Delete corner", vm::deleteSelectedCorner)
        }
    }
    item(key = "selection") {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardFill).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (selection) {
                is CadSelection.Wall -> {
                    val w = p.walls[selection.index]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("WALL ${wallName(w.index, p.corners.size)}", style = MpType.LabelSpatial, color = MpColors.ElectricLime)
                            Text(DimensionFormatter.formatLength(w.lengthM, system), style = MpType.MetricDisplay.copy(fontSize = 22.sp))
                            Text("Net ${DimensionFormatter.formatArea(p.netWallArea(w.index), system)}", style = MpType.MetricSmall, color = MpColors.IceWhite.copy(alpha = 0.75f))
                        }
                        ElectricLimePill("Edit length", icon = Icons.Rounded.Edit, glow = false, onClick = { onEditLength(LengthEdit.Wall(w.index, w.lengthM)) })
                    }
                    val ops = p.openingsOn(w.index)
                    if (ops.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ops.forEach { o ->
                            GlassPill(
                                (if (o.type == OpeningType.DOOR) "Door " else "Window ") + DimensionFormatter.formatLength(o.widthM, system, FractionPrecision.EIGHTH),
                                icon = Icons.Rounded.Close, textStyle = MpType.Chip.copy(fontSize = 12.sp),
                                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                                onClick = { vm.removeOpening(o) },
                            )
                        }
                    }
                }
                is CadSelection.Corner -> {
                    val c = p.corners[selection.index]
                    Text("CORNER ${FloorPlan.cornerLabel(selection.index)}", style = MpType.LabelSpatial, color = MpColors.ElectricLime)
                    Text(
                        "x ${DimensionFormatter.formatLength(c.x, system)} · y ${DimensionFormatter.formatLength(c.y, system)}",
                        style = MpType.MetricValue,
                    )
                }
                CadSelection.None -> Text("Tap a wall or a corner on the plan to edit it.", style = MpType.Body.copy(fontSize = 13.sp))
            }
        }
    }
    item(key = "wallsLabel") { SectionLabel(Icons.Rounded.Straighten, "Walls") }
    items(p.walls, key = { "w${it.index}" }) { w ->
        val selected = selection is CadSelection.Wall && selection.index == w.index
        val interaction = rememberPressInteraction()
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (selected) MpColors.ElectricLime.copy(alpha = 0.14f) else CardFill)
                .border(1.dp, if (selected) MpColors.ElectricLime.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(14.dp))
                .glassClickable(interaction, "Select wall ${wallName(w.index, p.corners.size)}") { vm.select(CadSelection.Wall(w.index)) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(wallName(w.index, p.corners.size), style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, modifier = Modifier.width(56.dp))
            Column(Modifier.weight(1f)) {
                Text(DimensionFormatter.formatLength(w.lengthM, system, FractionPrecision.EIGHTH), style = MpType.MetricValue)
                val ops = p.openingsOn(w.index)
                if (ops.isNotEmpty()) Text(
                    ops.joinToString { if (it.type == OpeningType.DOOR) "Door" else "Window" },
                    style = MpType.MetricSmall, color = MpColors.IceWhite.copy(alpha = 0.7f),
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                    .glassClickable(rememberPressInteraction(), "Edit wall length") { onEditLength(LengthEdit.Wall(w.index, w.lengthM)) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Edit, null, tint = MpColors.TextPrimary, modifier = Modifier.size(18.dp)) }
        }
    }
    item(key = "cadTotals") {
        Text(
            "Area ${DimensionFormatter.formatArea(p.areaSqM, system)} · Perimeter ${DimensionFormatter.formatLength(p.perimeterM, system, FractionPrecision.EIGHTH)} · Net wall ${DimensionFormatter.formatArea(p.netWallAreaSqM, system)}",
            style = MpType.MetricSmall.copy(fontSize = 12.sp), color = MpColors.IceWhite,
        )
    }
}

// ---------------------------------------------------------------- tab: AI fit

private fun androidx.compose.foundation.lazy.LazyListScope.aiFitTab(p: FloorPlan, system: UnitSystem, onOpenAdvisor: () -> Unit) {
    val e = RoomEstimator.estimate(p)
    item(key = "fitLabel") { SectionLabel(Icons.Rounded.AutoAwesome, "Smart fit check") }
    item(key = "estimates") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EstimateRow(Icons.Rounded.GridView, "Floor tiles ${e.tiles.sizeCm}×${e.tiles.sizeCm} cm", "${e.tiles.count} tiles", "≈ ${e.tiles.boxes} boxes incl. 10% waste")
            EstimateRow(Icons.Rounded.FormatPaint, "Wall paint · 2 coats", String.format(Locale.US, "%.1f L", e.paint.litres), "for ${DimensionFormatter.formatArea(e.paint.wallAreaSqM, system)} net wall")
            EstimateRow(Icons.Rounded.AcUnit, "Air conditioner", String.format(Locale.US, "%.2f ton", e.cooling.tons), String.format(Locale.US, "%,d BTU/h for this volume", e.cooling.btu))
            EstimateRow(Icons.Rounded.Tv, "Best TV size", "${e.tv.diagonalIn}\"", "at ~${DimensionFormatter.formatLength(e.tv.viewingDistanceM, system, FractionPrecision.EIGHTH)} viewing distance")
            EstimateRow(Icons.Rounded.Straighten, "Skirting / baseboard", DimensionFormatter.formatLength(e.baseboardM, system, FractionPrecision.EIGHTH), "perimeter minus doors")
        }
    }
    item(key = "furnLabel") { SectionLabel(Icons.Rounded.ViewInAr, "Will it fit? (60 cm walkway)") }
    items(e.furniture, key = { it.name }) { f ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardFill).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (f.fits) Icons.Rounded.CheckCircle else Icons.Rounded.HighlightOff, null, tint = if (f.fits) MpColors.ElectricLime else MpColors.Alert, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(f.name, style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(f.note, style = MpType.MetricSmall.copy(fontSize = 12.sp), color = if (f.fits) MpColors.ElectricLime else MpColors.Alert)
        }
    }
    item(key = "disclaimer") {
        Text(
            "Estimates use standard trade rules of thumb. Check critical numbers with a physical tape before buying.",
            style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.TextMuted,
        )
    }
    item(key = "askAi2") { AskAiCard(onOpenAdvisor) }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun SectionLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MpColors.ElectricLime, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MpType.LabelSpatial.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime)
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF323346)).padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MpColors.OrbLilac, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MpType.MetricSmall.copy(fontSize = 11.5.sp), color = MpColors.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun MetricCard(label: String, value: Pair<String, String>, sub: String, icon: ImageVector, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val interaction = rememberPressInteraction()
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(Color(0x66111324)).border(1.dp, MpColors.GlassRim, RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.glassClickable(interaction, label, onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = MpType.LabelSpatial.copy(fontSize = 10.5.sp), color = MpColors.IceWhite.copy(alpha = 0.85f), maxLines = 1, modifier = Modifier.weight(1f))
            Icon(icon, null, tint = MpColors.ElectricLime, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MpColors.TextPrimary)) { append(value.first) }
                if (value.second.isNotEmpty()) withStyle(SpanStyle(fontSize = 13.sp, color = MpColors.ElectricLime, fontWeight = FontWeight.SemiBold)) { append(" " + value.second) }
            },
            style = MpType.MetricDisplay, maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Text(sub, style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.IceWhite.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EstimateRow(icon: ImageVector, title: String, value: String, sub: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardFill).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MpColors.ElectricLime, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MpType.MetricSmall.copy(fontSize = 11.sp), color = MpColors.IceWhite.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(value, style = MpType.MetricValue.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, maxLines = 1)
    }
}

@Composable
private fun ToolChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    GlassPill(label, icon = icon, iconTint = MpColors.ElectricLime, textStyle = MpType.Chip.copy(fontSize = 13.sp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp), onClick = onClick)
}

@Composable
private fun AskAiCard(onClick: () -> Unit) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), tint = Color(0x8C1C1E36), onClick = onClick, onClickLabel = "Ask the AI room advisor") {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IridescentAiOrb(size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Ask AI Advisor", style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tiles, paint, AC & TV for this room", style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.IceWhite.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AiWaveformBars(bars = 6, height = 14.dp, barWidth = 2.5.dp, gap = 2.5.dp)
            Spacer(Modifier.width(10.dp))
            ElectricLimePill("Ask", glow = false, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp))
        }
    }
}

// ---------------------------------------------------------------- dialog

/** Wall length / ceiling input. Metric: meters ("5.4", "540 cm"). Imperial: feet-inches ("17' 8 5/8\""). */
@Composable
private fun LengthDialog(title: String, initialM: Double, system: UnitSystem, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    val initial = if (system == UnitSystem.METRIC) String.format(Locale.US, "%.2f", initialM)
    else DimensionFormatter.formatFeetInches(initialM, FractionPrecision.EIGHTH).replace(DimensionFormatter.NBSP, ' ')
    // Focused with everything selected, so typing a new value replaces the old one.
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val text = field.text
    val parsed = parseLength(text, system)
    Dialog(onDismissRequest = onDismiss) {
        SmokedFrostedGlassCard(Modifier.fillMaxWidth(), tint = MpColors.TwilightHorizon.copy(alpha = 0.97f)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, style = MpType.HeadlineItalic.copy(fontSize = 22.sp))
                OutlinedTextField(
                    value = field, onValueChange = { field = it }, singleLine = true,
                    label = { Text(if (system == UnitSystem.METRIC) "Meters (or 540 cm)" else "Feet & inches, e.g. 17' 8 5/8\"") },
                    textStyle = MpType.MetricDisplay.copy(fontSize = 22.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = if (system == UnitSystem.METRIC) KeyboardType.Decimal else KeyboardType.Text),
                    isError = parsed == null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MpColors.ElectricLime, unfocusedBorderColor = MpColors.GlassRim,
                        focusedLabelColor = MpColors.ElectricLime, cursorColor = MpColors.ElectricLime,
                        focusedTextColor = MpColors.TextPrimary, unfocusedTextColor = MpColors.TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Text(
                    parsed?.let { "= " + DimensionFormatter.formatLength(it, if (system == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC, FractionPrecision.EIGHTH) }
                        ?: "Enter a length",
                    style = MpType.MetricSmall.copy(fontSize = 12.sp), color = if (parsed == null) MpColors.Alert else MpColors.IceWhite,
                )
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    GlassPill("Cancel", onClick = onDismiss)
                    Spacer(Modifier.width(8.dp))
                    ElectricLimePill("Apply", glow = false, onClick = { parsed?.let(onConfirm) })
                }
            }
        }
    }
}

private fun parseLength(text: String, system: UnitSystem): Double? {
    val t = text.trim().lowercase(Locale.US)
    if (t.isEmpty()) return null
    val metric = when {
        t.endsWith("cm") -> t.removeSuffix("cm").trim().toDoubleOrNull()?.div(100)
        t.endsWith("mm") -> t.removeSuffix("mm").trim().toDoubleOrNull()?.div(1000)
        t.endsWith("m") -> t.removeSuffix("m").trim().toDoubleOrNull()
        else -> null
    }
    val value = metric ?: if (system == UnitSystem.METRIC) t.replace(',', '.').toDoubleOrNull()
    else t.toDoubleOrNull()?.let { it * DimensionFormatter.METERS_PER_FOOT } ?: DimensionFormatter.parseFeetInches(text)
    return value?.takeIf { it in 0.1..50.0 }
}
