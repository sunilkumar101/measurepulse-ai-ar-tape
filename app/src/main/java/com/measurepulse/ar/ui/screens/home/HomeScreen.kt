package com.measurepulse.ar.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.measurepulse.ar.data.ArSupport
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.LengthUnit
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.components.StatusBarScrim
import com.measurepulse.ar.ui.components.CountBadge
import com.measurepulse.ar.ui.components.DockDestination
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FilterChipPill
import com.measurepulse.ar.ui.components.FloatingBottomGlassDock
import com.measurepulse.ar.ui.components.FloatingGlassCtaBar
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.components.glassClickable
import com.measurepulse.ar.ui.components.rememberPressInteraction
import com.measurepulse.ar.ui.screens.ScanImages
import com.measurepulse.ar.ads.AdMobBannerView
import com.measurepulse.ar.ads.BannerReserve
import com.measurepulse.ar.ads.rememberIsPro
import com.measurepulse.ar.ads.rememberBannersEnabled
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType

/** Screen 2 — Home Spatial Command Hub. */
@Composable
fun HomeScreen(
    onOpenArTape: () -> Unit,
    onOpenScan: (Long) -> Unit,
    onOpenRangefinder: () -> Unit,
    onOpenCaliper: () -> Unit,
    onOpenLevel: () -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenPro: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bannersOn = rememberBannersEnabled()
    val isPro = rememberIsPro()

    TwilightGradientBackground {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 190.dp + if (bannersOn) BannerReserve else 0.dp),
        ) {
            item(key = "header") {
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp)) {
                    HomeHeader(state.arSupport, state.unitSystem.takeUnless { state.loading }, viewModel::setUnitSystem, onOpenPro, isPro)
                    Spacer(Modifier.height(20.dp))
                    Text("Seamless Spatial", style = MpType.HeadlineItalic)
                    Text(
                        "Measurement",
                        style = MpType.HeadlineItalic.copy(
                            brush = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.72f))),
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            item(key = "filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(HomeFilter.entries, key = { it.name }) { f ->
                        FilterChipPill(
                            text = f.label,
                            selected = state.filter == f,
                            icon = f.icon,
                            onClick = { viewModel.setFilter(f) },
                            modifier = Modifier.semantics { selected = state.filter == f },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            item(key = "featuredTitle") {
                SectionHeader(
                    title = "Featured Spatial Scans",
                    badge = if (state.loading) "··· Ready" else "${state.totalCount} Ready",
                    trailing = { TextAction("View All", onOpenProjects) },
                )
            }
            item(key = "featured") {
                val featured = state.featured
                if (featured != null) {
                    FeaturedScanCard(
                        scan = featured,
                        system = state.unitSystem,
                        precision = state.precision,
                        onOpen = { onOpenScan(featured.id) },
                        onFavorite = { viewModel.toggleFavorite(featured.id) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                } else if (state.loading) {
                    // Same footprint as the real card so nothing jumps when data arrives.
                    SmokedFrostedGlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(408.dp)) {}
                } else {
                    EmptyFilterCard(onOpenArTape, Modifier.padding(horizontal = 20.dp))
                }
                Spacer(Modifier.height(26.dp))
            }

            if (state.recent.isNotEmpty()) {
                item(key = "recentTitle") {
                    SectionHeader(
                        title = "Recent Blueprints",
                        badge = "${state.recent.size} Active",
                        trailing = {
                            Text("Swipe →", style = MpType.MetricSmall.copy(fontSize = 12.sp), color = MpColors.TextMuted)
                        },
                    )
                }
                item(key = "recent") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.recent, key = { it.id }) { scan ->
                            RecentScanCard(scan, state.unitSystem, state.precision, onOpen = { onOpenScan(scan.id) })
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                }
            }

            item(key = "toolsTitle") {
                SectionHeader(
                    title = "Precision AR Utilities",
                    trailing = {
                        Text(
                            if (state.arSupport == ArSupport.UNSUPPORTED) "Sensor Mode" else "Sensors Calibrated",
                            style = MpType.MetricSmall.copy(fontSize = 12.sp), color = MpColors.ElectricLime,
                        )
                    },
                )
            }
            item(key = "tools") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToolTile(Icons.Rounded.Straighten, "Live AR Tape", "Point-to-point 3D", MpColors.ElectricLime, onOpenArTape, Modifier.weight(1f))
                        ToolTile(Icons.Rounded.Architecture, "Room Blueprint", "2D CAD + PDF", MpColors.Cyan, { state.featured?.let { onOpenScan(it.id) } ?: onOpenArTape() }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToolTile(Icons.Rounded.Landscape, "Rangefinder", "Height & distance", MpColors.OrbPink, onOpenRangefinder, Modifier.weight(1f))
                        ToolTile(Icons.Rounded.SquareFoot, "Caliper", "Thread pitch gauge", MpColors.OrbLilac, onOpenCaliper, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToolTile(Icons.Rounded.GridOn, "Bubble Level", "Bullseye & laser", MpColors.ElectricLime, onOpenLevel, Modifier.weight(1f))
                        ToolTile(Icons.Rounded.ViewInAr, "Box Fit", "Will the sofa fit?", MpColors.Cyan, onOpenArTape, Modifier.weight(1f))
                    }
                }
            }
        }

        // Status-bar scrim so scrolled content never sits behind the clock/icons.
        StatusBarScrim()
        // Bottom scrim so floating glass reads cleanly over scrolling content.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp + if (bannersOn) BannerReserve else 0.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.2f to MpColors.TwilightNadir.copy(alpha = 0.95f), 1f to MpColors.TwilightNadir))
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Anchored adaptive banner (free users) — above the floating controls, never between them.
            AdMobBannerView(Modifier.padding(bottom = 4.dp))
            FloatingGlassCtaBar(
                label = "Start New Spatial Scan",
                supportingText = "AR tape • room blueprint",
                onClick = onOpenArTape,
            )
            FloatingBottomGlassDock(
                selected = DockDestination.HOME,
                onHome = {},
                onAi = onOpenAiAdvisor,
                onTools = onOpenProjects,
            )
        }
    }
}

private val HomeFilter.icon: ImageVector?
    get() = when (this) {
        HomeFilter.ALL -> null
        HomeFilter.AR_TAPE -> Icons.Rounded.Straighten
        HomeFilter.FLOORPLANS -> Icons.Rounded.Stairs
        HomeFilter.LEVEL_CALIPER -> Icons.Rounded.SquareFoot
        HomeFilter.RANGEFINDER -> Icons.Rounded.Height
    }

@Composable
private fun HomeHeader(
    arSupport: ArSupport,
    unitSystem: UnitSystem?,
    onUnitSystem: (UnitSystem) -> Unit,
    onOpenPro: () -> Unit,
    isPro: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFFA855F7))))
                .border(2.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IridescentAiOrb(size = 44.dp, showStatusDot = true)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (label, dot) = when (arSupport) {
                    ArSupport.CHECKING -> "Checking sensors" to MpColors.TextMuted
                    ArSupport.SUPPORTED, ArSupport.SUPPORTED_NEEDS_INSTALL -> "ARCore Depth Ready" to MpColors.ElectricLime
                    ArSupport.UNSUPPORTED -> "Gyro Sensor Mode" to MpColors.Cyan
                }
                StatusDot(color = dot, size = 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(label.uppercase(), style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.IceWhite.copy(alpha = 0.8f), maxLines = 1)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "MeasurePulse AI", style = MpType.CardTitle.copy(fontSize = 15.sp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                // Free users see an upgrade prompt; the plain "PRO" badge is only for owners.
                ElectricLimePill(
                    if (isPro) "PRO" else "GO PRO",
                    textStyle = MpType.Chip.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    onClick = onOpenPro, onClickLabel = if (isPro) "Pro pass" else "Upgrade to Pro",
                )
            }
        }
        UnitToggle(unitSystem, onUnitSystem)
    }
}

/** Compact metric/imperial switch. Drives every number on screen via the Precision Unit Engine. */
@Composable
private fun UnitToggle(system: UnitSystem?, onChange: (UnitSystem) -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color(0xFF14162B).copy(alpha = 0.6f))
            .border(1.dp, MpColors.GlassRim, CircleShape)
            .padding(3.dp),
    ) {
        listOf(UnitSystem.METRIC to "m", UnitSystem.IMPERIAL to "ft").forEach { (s, label) ->
            val active = s == system
            val interaction = rememberPressInteraction()
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) MpColors.ElectricLime else Color.Transparent)
                    .glassClickable(interaction, if (s == UnitSystem.METRIC) "Use metric units" else "Use imperial units") { onChange(s) }
                    .semantics { selected = active; role = Role.Tab }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = if (active) MpColors.Ink else MpColors.IceWhite,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, badge: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MpType.SectionTitle.copy(fontSize = 17.sp), maxLines = 1)
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            CountBadge(badge)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    val interaction = rememberPressInteraction()
    Row(
        Modifier
            .clip(CircleShape)
            .glassClickable(interaction, label, onClick)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MpType.Chip.copy(fontSize = 14.sp), color = MpColors.IceWhite.copy(alpha = 0.8f), maxLines = 1, softWrap = false)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MpColors.IceWhite.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
    }
}

private val ScanCategory.label: String
    get() = when (this) {
        ScanCategory.AR_TAPE -> "3D AR"
        ScanCategory.FLOORPLAN -> "Floorplan"
        ScanCategory.LEVEL -> "Level"
        ScanCategory.CALIPER -> "Caliper"
        ScanCategory.RANGEFINDER -> "Rangefinder"
    }

private val ScanCategory.icon: ImageVector
    get() = when (this) {
        ScanCategory.AR_TAPE -> Icons.Rounded.ViewInAr
        ScanCategory.FLOORPLAN -> Icons.Rounded.Architecture
        ScanCategory.LEVEL -> Icons.Rounded.GridOn
        ScanCategory.CALIPER -> Icons.Rounded.SquareFoot
        ScanCategory.RANGEFINDER -> Icons.Rounded.Landscape
    }

/** Secondary info line: the scan's own detail, or ceiling + walls. */
private fun RoomScanEntity.secondaryLine(system: UnitSystem, precision: FractionPrecision): String =
    detail ?: buildString {
        append("Ceiling ").append(DimensionFormatter.formatLength(ceilingM, system, precision))
        if (wallCount > 0) append(" • ").append(wallCount).append(" Walls")
    }

/** Saved tool readings (caliper, level, rangefinder) have no floor footprint. */
private val RoomScanEntity.hasFootprint get() = widthM > 0.0 && lengthM > 0.0

/** Width × length for rooms; the tool's own reading for saved tool readings. */
private fun footprint(scan: RoomScanEntity, system: UnitSystem, precision: FractionPrecision) =
    if (scan.hasFootprint) "${DimensionFormatter.formatLength(scan.widthM, system, precision)} × ${DimensionFormatter.formatLength(scan.lengthM, system, precision)}"
    else when (scan.category) {
        ScanCategory.CALIPER -> "Length ${caliperLength(scan.widthM, system)}"
        ScanCategory.RANGEFINDER -> "Height ${DimensionFormatter.formatLength(scan.ceilingM, system, precision)}"
        else -> scan.title
    }

/** Caliper readings are small: mm (metric) or decimal inches (imperial) read better than 0.02 m. */
private fun caliperLength(meters: Double, system: UnitSystem) =
    DimensionFormatter.formatLength(meters, if (system == UnitSystem.METRIC) LengthUnit.MILLIMETERS else LengthUnit.INCHES)

/** Value shown in the lime pill: area for rooms, the reading itself for tools. */
private fun headline(scan: RoomScanEntity, system: UnitSystem, precision: FractionPrecision) =
    if (scan.hasFootprint) DimensionFormatter.formatArea(scan.areaSqM, system)
    else when (scan.category) {
        ScanCategory.CALIPER -> caliperLength(scan.widthM, system)
        ScanCategory.RANGEFINDER -> "↕ " + DimensionFormatter.formatLength(scan.ceilingM, system, precision)
        ScanCategory.LEVEL -> scan.detail?.takeIf { it.length <= 14 } ?: "Level"
        else -> DimensionFormatter.formatArea(scan.areaSqM, system)
    }

@Composable
private fun FeaturedScanCard(
    scan: RoomScanEntity,
    system: UnitSystem,
    precision: FractionPrecision,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmokedFrostedGlassCard(modifier.fillMaxWidth(), onClick = onOpen, onClickLabel = "Open ${scan.title}") {
        Column {
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                AsyncImage(
                    model = ScanImages.model(scan.imageKey),
                    contentDescription = scan.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.45f), 0.4f to Color.Black.copy(alpha = 0.12f),
                            1f to MpColors.TwilightNadir.copy(alpha = 0.95f),
                        )
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassPill(
                        scan.category.label, icon = scan.category.icon, iconTint = MpColors.Cyan,
                        textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                    )
                    ElectricLimePill(
                        headline(scan, system, precision),
                        textStyle = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    GlassCircleButton(
                        if (scan.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (scan.isFavorite) "Remove from favourites" else "Add to favourites",
                        onFavorite, size = 40.dp,
                        iconTint = if (scan.isFavorite) MpColors.Alert else MpColors.TextPrimary,
                    )
                }
            }
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        scan.title, style = MpType.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    GlassCircleButton(Icons.Rounded.NorthEast, "Open blueprint", onOpen, size = 42.dp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Place, null, tint = MpColors.IceWhite.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(footprint(scan, system, precision), style = MpType.MetricValue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    scan.secondaryLine(system, precision), style = MpType.MetricSmall.copy(fontSize = 12.sp),
                    color = MpColors.IceWhite.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MpColors.Divider))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ", style = MpType.MetricSmall.copy(fontSize = 13.sp), color = MpColors.Star)
                    Text(scan.qualityLabel, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.TextPrimary, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    if (scan.wallCount > 0) Text(
                        "${scan.wallCount} Walls", style = MpType.MetricSmall.copy(fontSize = 11.sp),
                        color = MpColors.TextMuted, maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentScanCard(
    scan: RoomScanEntity,
    system: UnitSystem,
    precision: FractionPrecision,
    onOpen: () -> Unit,
) {
    SmokedFrostedGlassCard(
        Modifier.width(220.dp),
        shape = RoundedCornerShape(24.dp),
        onClick = onOpen,
        onClickLabel = "Open ${scan.title}",
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(118.dp)) {
                AsyncImage(
                    model = ScanImages.model(scan.imageKey),
                    contentDescription = scan.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.5f to Color.Transparent, 1f to MpColors.TwilightNadir.copy(alpha = 0.9f))
                    )
                )
                ElectricLimePill(
                    headline(scan, system, precision),
                    textStyle = MpType.MetricValue.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                    glow = false,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                )
                GlassPill(
                    scan.category.label,
                    textStyle = MpType.MetricSmall.copy(fontSize = 10.sp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                )
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(scan.title, style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(footprint(scan, system, precision), style = MpType.MetricSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    scan.secondaryLine(system, precision), style = MpType.MetricSmall,
                    color = MpColors.IceWhite.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MpColors.Divider))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        scan.qualityLabel, style = MpType.MetricSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MpColors.ElectricLime, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Rounded.NorthEast, null, tint = MpColors.IceWhite.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmokedFrostedGlassCard(
        modifier,
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        onClickLabel = "Open $title",
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.NorthEast, null, tint = MpColors.IceWhite.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MpType.MetricSmall.copy(fontSize = 11.sp), color = MpColors.IceWhite.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyFilterCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    SmokedFrostedGlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No scans here yet", style = MpType.CardTitle)
            Text("Measure something with this tool and it will show up here.", style = MpType.Body)
            ElectricLimePill("Start a scan", onClick = onStart, glow = false)
        }
    }
}
