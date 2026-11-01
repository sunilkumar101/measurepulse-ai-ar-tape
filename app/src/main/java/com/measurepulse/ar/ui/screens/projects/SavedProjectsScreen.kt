package com.measurepulse.ar.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.components.edgeBleed
import com.measurepulse.ar.ui.components.DockDestination
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.FloatingBottomGlassDock
import com.measurepulse.ar.ads.AdMobBannerView
import com.measurepulse.ar.ads.BannerReserve
import com.measurepulse.ar.ads.rememberBannersEnabled
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.LimeCircleButton
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.StatusBarScrim
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.screens.ScanImages
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Screen 9 — saved architectural projects gallery. */
@Composable
fun SavedProjectsScreen(
    onBack: () -> Unit,
    onOpenScan: (RoomScanEntity) -> Unit,
    onNewScan: () -> Unit,
    onHome: () -> Unit,
    onAi: () -> Unit,
    viewModel: SavedProjectsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scans by viewModel.scans.collectAsStateWithLifecycle()
    val bannersOn = rememberBannersEnabled()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val pdfs by viewModel.pdfs.collectAsStateWithLifecycle()
    val busyId by viewModel.busyId.collectAsStateWithLifecycle()
    val count by viewModel.allCount.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var searching by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<RoomScanEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is ProjectsEvent.Launch -> runCatching { context.startActivity(e.intent) }.onFailure { toast = "No app can open this file" }
                is ProjectsEvent.Message -> toast = e.text
            }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2400); toast = null } }

    TwilightGradientBackground {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp + (if (bannersOn) BannerReserve else 0.dp) + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("MeasurePulse AI", style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary)
                            Text("PROJECTS LIBRARY", style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Saved Spatial\nProjects", style = MpType.HeroItalic.copy(fontSize = 32.sp, lineHeight = 36.sp), modifier = Modifier.weight(1f))
                        LimeCircleButton(Icons.Rounded.Add, "New scan", onNewScan, size = 52.dp)
                        Spacer(Modifier.width(10.dp))
                        GlassCircleButton(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, if (searching) "Close search" else "Search projects", {
                            if (searching) viewModel.setQuery("")
                            searching = !searching
                        }, size = 52.dp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(size = 6.dp); Spacer(Modifier.width(8.dp))
                        Text("STORED ON THIS DEVICE · ${pdfs.size} PDF${if (pdfs.size == 1) "" else "S"}", style = MpType.LabelSpatial.copy(fontSize = 10.5.sp), color = MpColors.IceWhite.copy(alpha = 0.8f))
                    }
                    if (searching) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = query, onValueChange = viewModel::setQuery, singleLine = true,
                            placeholder = { Text("Search rooms, readings…", style = MpType.Body) },
                            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MpColors.ElectricLime) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MpColors.ElectricLime, unfocusedBorderColor = MpColors.GlassRim, cursorColor = MpColors.ElectricLime,
                                focusedTextColor = MpColors.TextPrimary, unfocusedTextColor = MpColors.TextPrimary,
                                focusedContainerColor = Color(0x99111324), unfocusedContainerColor = Color(0x99111324),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item(key = "filters") {
                LazyRow(Modifier.edgeBleed(16.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProjectFilter.entries, key = { it.name }) { f ->
                        val label = when (f) { ProjectFilter.ALL -> "${f.label} ($count)"; ProjectFilter.PDFS -> "${f.label} (${pdfs.size})"; else -> f.label }
                        FilterChipPill(label, filter == f, if (f == ProjectFilter.PDFS) Icons.Rounded.PictureAsPdf else if (f == ProjectFilter.ALL) Icons.Rounded.FolderOpen else null, onClick = { viewModel.setFilter(f) })
                    }
                }
            }
            if (filter == ProjectFilter.PDFS) {
                if (pdfs.isEmpty()) item(key = "nopdf") { EmptyCard("No PDFs yet", "Export a blueprint or a shopping list and it will appear here.") }
                items(pdfs, key = { it.absolutePath }) { f -> PdfRow(f, { viewModel.openPdf(f) }, { viewModel.sharePdf(f) }, { viewModel.deletePdf(f) }) }
            } else {
                if (scans.isEmpty()) item(key = "noscan") { EmptyCard(if (query.isBlank()) "Nothing here yet" else "No matches for “$query”", "Measure a room with the AR tape or save a tool reading.") }
                items(scans, key = { it.id }) { s ->
                    ProjectCard(
                        s, prefs.unitSystem, exporting = busyId == s.id,
                        onOpen = { onOpenScan(s) }, onExport = { viewModel.exportBlueprint(s) }, onDelete = { confirmDelete = s },
                    )
                }
            }
        }

        StatusBarScrim()
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(130.dp + if (bannersOn) BannerReserve else 0.dp).background(Brush.verticalGradient(0f to Color.Transparent, 0.25f to MpColors.TwilightNadir.copy(alpha = 0.96f), 1f to MpColors.TwilightNadir)))
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Anchored adaptive banner (free users), kept clear of the dock buttons.
            AdMobBannerView()
            FloatingBottomGlassDock(selected = DockDestination.TOOLS, onHome = onHome, onAi = onAi, onTools = {})
        }
        toast?.let {
            Text(it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp).clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 16.dp, vertical = 9.dp))
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = MpColors.TwilightHorizon,
            title = { Text("Delete project?", style = MpType.CardTitle) },
            text = { Text("“${s.title}” will be removed from this device. Exported PDFs are kept.", style = MpType.Body) },
            confirmButton = { TextButton(onClick = { viewModel.delete(s); confirmDelete = null }) { Text("Delete", color = MpColors.Alert, style = MpType.Chip) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = MpColors.TextPrimary, style = MpType.Chip) } },
        )
    }
}

private val ScanCategory.badge: String
    get() = when (this) {
        ScanCategory.FLOORPLAN -> "2D BLUEPRINT"
        ScanCategory.AR_TAPE -> "AR FIT CHECK"
        ScanCategory.LEVEL -> "LEVEL READING"
        ScanCategory.CALIPER -> "CALIPER"
        ScanCategory.RANGEFINDER -> "RANGEFINDER"
    }

@Composable
private fun ProjectCard(s: RoomScanEntity, sys: UnitSystem, exporting: Boolean, onOpen: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), onClick = onOpen, onClickLabel = "Open ${s.title}") {
        Column {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(model = ScanImages.model(s.imageKey), contentDescription = s.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.3f), 0.5f to Color.Transparent, 1f to MpColors.TwilightNadir.copy(alpha = 0.9f))))
                GlassPill(
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(s.createdAt)),
                    textStyle = MpType.MetricSmall.copy(fontSize = 11.sp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
                ElectricLimePill(s.category.badge, glow = false, textStyle = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
            }
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = MpType.CardTitle.copy(fontSize = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            s.detail ?: "${DimensionFormatter.formatLength(s.widthM, sys, FractionPrecision.EIGHTH)} × ${DimensionFormatter.formatLength(s.lengthM, sys, FractionPrecision.EIGHTH)}",
                            style = MpType.MetricSmall.copy(fontSize = 12.sp), color = MpColors.IceWhite.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    GlassCircleButton(Icons.Rounded.NorthEast, "Open project", onOpen, size = 44.dp)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MpColors.Divider))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isRoom = s.category == ScanCategory.FLOORPLAN || s.category == ScanCategory.AR_TAPE
                    Stat(if (isRoom) DimensionFormatter.formatArea(s.areaSqM, sys) else s.qualityLabel.substringAfter(' ', s.qualityLabel), if (isRoom) "Area" else "Accuracy", Modifier.weight(1f))
                    if (isRoom) Stat(DimensionFormatter.formatLength(s.ceilingM, sys, FractionPrecision.EIGHTH), "Ceiling", Modifier.weight(1f))
                    Text(s.qualityLabel, style = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
                        maxLines = 2, modifier = Modifier.weight(1f))
                    GlassCircleButton(Icons.Rounded.Download, if (exporting) "Exporting PDF" else "Export PDF", onExport, size = 40.dp, iconTint = if (exporting) MpColors.TextMuted else MpColors.TextPrimary)
                    GlassCircleButton(Icons.Rounded.DeleteOutline, "Delete project", onDelete, size = 40.dp, iconTint = MpColors.Alert)
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(
            buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(value) } },
            style = MpType.MetricValue.copy(fontSize = 13.sp), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(label, style = MpType.Body.copy(fontSize = 11.sp), color = MpColors.TextMuted)
    }
}

@Composable
private fun PdfRow(f: File, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), onClick = onOpen, onClickLabel = "Open PDF") {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MpColors.ElectricLime), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PictureAsPdf, null, tint = MpColors.Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.nameWithoutExtension.removePrefix("MeasurePulse_").replace('_', ' '), style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(f.lastModified())) + String.format(Locale.US, " · %.0f KB", f.length() / 1024.0),
                    style = MpType.MetricSmall.copy(fontSize = 11.sp), color = MpColors.IceWhite.copy(alpha = 0.75f),
                )
            }
            GlassCircleButton(Icons.Rounded.Share, "Share PDF", onShare, size = 40.dp)
            GlassCircleButton(Icons.Rounded.DeleteOutline, "Delete PDF", onDelete, size = 40.dp, iconTint = MpColors.Alert)
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MpType.CardTitle)
            Text(body, style = MpType.Body)
        }
    }
}
