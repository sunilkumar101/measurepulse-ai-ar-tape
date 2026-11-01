package com.measurepulse.ar.ui.screens.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.domain.advisor.AdvisorInput
import com.measurepulse.ar.domain.advisor.AdvisorResult
import com.measurepulse.ar.domain.advisor.RoomAdvisor
import com.measurepulse.ar.domain.advisor.SunExposure
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
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
import com.measurepulse.ar.ui.components.StatusBarScrim
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.components.glassClickable
import com.measurepulse.ar.ui.components.rememberPressInteraction
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private val Inner = Color(0x99111324)

/** Screen 8 — "Help With AI" spatial room & renovation advisor. */
@Composable
fun AiRoomAdvisorScreen(onBack: () -> Unit, viewModel: AiRoomAdvisorViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val sys = prefs.unitSystem
    var toast by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is AdvisorEvent.Share -> runCatching { context.startActivity(e.intent) }.onFailure { toast = "No app available to share the PDF" }
                is AdvisorEvent.Message -> toast = e.text
            }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }

    val i = input
    val result = i?.let { RoomAdvisor.analyze(it) }
    val money = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    TwilightGradientBackground {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Row(Modifier.statusBarsPadding().padding(top = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AI Spatial Room Advisor", style = MpType.CardTitle.copy(fontSize = 18.sp), maxLines = 1)
                        Text("SMART ESTIMATES · ON-DEVICE", style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.ElectricLime)
                    }
                    GlassCircleButton(Icons.Rounded.IosShare, "Export shopping list PDF", viewModel::exportPdf)
                }
            }
            item(key = "room") {
                Box {
                    val interaction = rememberPressInteraction()
                    Row(
                        Modifier.fillMaxWidth().clip(CircleShape).background(Color(0xB3141632)).border(1.dp, MpColors.GlassRim, CircleShape)
                            .glassClickable(interaction, "Choose room") { picker = true }.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IridescentAiOrb(size = 34.dp, showStatusDot = true)
                        Spacer(Modifier.width(8.dp))
                        AiWaveformBars(bars = 5, height = 14.dp, barWidth = 2.5.dp, gap = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(i?.roomName ?: "Loading…", style = MpType.Chip.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        i?.let {
                            StatusDot(size = 6.dp); Spacer(Modifier.width(6.dp))
                            Text(
                                DimensionFormatter.formatArea(it.floorAreaSqM, sys) + " (" + DimensionFormatter.formatVolume(it.volumeCuM, sys) + ")",
                                style = MpType.MetricSmall.copy(fontSize = 11.sp), color = MpColors.TextPrimary, maxLines = 1,
                            )
                        }
                        Icon(Icons.Rounded.ExpandMore, null, tint = MpColors.IceWhite, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = picker, onDismissRequest = { picker = false }, containerColor = MpColors.TwilightHorizon) {
                        rooms.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.title} · ${DimensionFormatter.formatArea(r.areaSqM, sys)}", style = MpType.Body.copy(fontSize = 14.sp), color = if (r.id == selected?.id) MpColors.ElectricLime else MpColors.TextPrimary) },
                                onClick = { viewModel.selectRoom(r); picker = false },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom room size…", style = MpType.Body.copy(fontSize = 14.sp), color = MpColors.ElectricLime) },
                            onClick = { viewModel.useCustom(); picker = false },
                        )
                    }
                }
            }
            item(key = "tabs") {
                // All four categories fit a 360 dp phone without clipping.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AdvisorTab.entries.forEach { t -> FilterChipPill(t.label, tab == t, null, onClick = { viewModel.setTab(t) }, compact = true) }
                }
            }
            if (result != null && i != null) {
                if (tab == AdvisorTab.ALL || tab == AdvisorTab.TILES) item(key = "tiles") { TilesCard(result, money, viewModel) }
                if (tab == AdvisorTab.ALL || tab == AdvisorTab.PAINT) item(key = "paint") { PaintCard(result, money, sys) }
                if (tab == AdvisorTab.ALL || tab == AdvisorTab.APPLIANCES) item(key = "appliances") { AppliancesCard(result, sys, viewModel) }
                item(key = "inputs") { InputsCard(i, selected == null, sys, money, viewModel) }
                item(key = "note") {
                    Text(
                        "Estimates from standard trade rules of thumb (not a substitute for a contractor's quote). Edit prices to match your local store.",
                        style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.TextMuted,
                    )
                }
            }
        }

        StatusBarScrim()
        // Solid backdrop so cards never show below or around the floating CTA.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Transparent, 0.3f to MpColors.TwilightNadir.copy(alpha = 0.96f), 1f to MpColors.TwilightNadir))
        )
        FloatingGlassCtaBar(
            label = if (exporting) "Generating PDF…" else "Export Shopping PDF",
            supportingText = result?.let { "Est. ${money.format(it.totalCost)} · tiles + paint" },
            onClick = viewModel::exportPdf,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        )
        toast?.let {
            Text(it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp, start = 24.dp, end = 24.dp)
                    .clip(CircleShape).background(MpColors.ElectricLime).padding(horizontal = 16.dp, vertical = 9.dp))
        }
    }
}

// ---------------------------------------------------------------- cards

@Composable
private fun InsightCard(badge: String, category: String, title: String, subtitle: String, content: @Composable () -> Unit) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ElectricLimePill(badge, glow = false, textStyle = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp))
                Spacer(Modifier.weight(1f).width(8.dp))
                Text(category, style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.IceWhite,
                    modifier = Modifier.clip(CircleShape).background(Color(0xFF27293B)).padding(horizontal = 10.dp, vertical = 5.dp), maxLines = 1)
            }
            Text(title, style = MpType.CardTitle.copy(fontSize = 21.sp, lineHeight = 26.sp))
            Text(subtitle, style = MpType.Body.copy(fontSize = 13.5.sp), color = MpColors.IceWhite)
            content()
        }
    }
}

@Composable
private fun Tile(label: String, value: String, sub: String?, icon: ImageVector?, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Inner).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, tint = MpColors.OrbLilac, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)) }
            Text(label.uppercase(), style = MpType.LabelSpatial.copy(fontSize = 9.5.sp), color = MpColors.IceWhite.copy(alpha = 0.85f), maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MpType.MetricValue.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, maxLines = 1)
        sub?.let { Text(it, style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.TextPrimary, maxLines = 2) }
    }
}

@Composable
private fun TilesCard(r: AdvisorResult, money: NumberFormat, vm: AiRoomAdvisorViewModel) {
    val t = r.tiles
    InsightCard(
        badge = "${t.tiles} Tiles (+10% Margin)", category = "FLOORING",
        title = "${t.tileCm}×${t.tileCm} cm Porcelain Floor Tiles",
        subtitle = "${t.boxes} boxes (${t.perBox} pcs/box) • Adhesive: ${t.adhesiveBags} bags • Est. ${money.format(t.cost)}",
    ) {
        // Wraps instead of hiding the last size past the card edge.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30, 45, 60, 80, 120).forEach { cm ->
                val label = if (cm == 120) "120×60" else "$cm×$cm"
                if (cm == t.tileCm) ElectricLimePill(label, glow = false, textStyle = MpType.MetricValue.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp))
                else GlassPill(label, textStyle = MpType.MetricValue.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp), onClick = { vm.update { it.copy(tileCm = cm) } })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Cutting margin", String.format(Locale.US, "+%.1f tiles", t.wasteTiles), "10% waste factor", Icons.Rounded.GridView, Modifier.weight(1f))
            Tile("Grout · 2 mm", String.format(Locale.US, "%.1f kg", t.groutKg), String.format(Locale.US, "Adhesive %.0f kg", t.adhesiveKg), null, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PaintCard(r: AdvisorResult, money: NumberFormat, sys: UnitSystem) {
    val p = r.paint
    val i = r.input
    InsightCard(
        badge = String.format(Locale.US, "%.1f Liters (2 Coats)", p.litres), category = "PAINT",
        title = "Interior Matte Wall Paint",
        subtitle = "Net wall area: ${DimensionFormatter.formatArea(p.netWallSqM, sys)} (deducted ${i.doors} door${if (i.doors == 1) "" else "s"} & ${i.windows} window${if (i.windows == 1) "" else "s"}) • Est. ${money.format(p.cost)}",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Primer base", String.format(Locale.US, "%.1f L", p.primerLitres), "1 coat", null, Modifier.weight(1f))
            Tile("Coverage", "${RoomAdvisor.PAINT_SQM_PER_L.toInt()} m² / L", String.format(Locale.US, "= %.1f US gal", p.gallons), null, Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(Inner).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFE2E4EC)))
            Spacer(Modifier.width(8.dp))
            Text("Buy: " + p.cans.joinToString(" + ") { "${it.count}×${it.litres.toInt()} L" }.ifEmpty { "—" },
                style = MpType.MetricValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("Velvet matte", style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.IceWhite)
        }
    }
}

@Composable
private fun AppliancesCard(r: AdvisorResult, sys: UnitSystem, vm: AiRoomAdvisorViewModel) {
    val c = r.cooling; val tv = r.tv
    val tons = if (c.tons % 1.0 == 0.0) c.tons.toInt().toString() else String.format(Locale.US, "%.2f", c.tons).trimEnd('0')
    InsightCard(
        badge = "$tons Ton AC • ${tv.recommendedIn}\" TV", category = "ERGONOMICS",
        title = "Optimal Room Appliances",
        subtitle = String.format(Locale.US, "%,d BTU/h inverter split for %s • %d\" 4K ideal at %s viewing distance",
            c.btu, DimensionFormatter.formatVolume(r.input.volumeCuM, sys), tv.recommendedIn,
            DimensionFormatter.formatLength(tv.viewingDistanceM, sys, FractionPrecision.EIGHTH)),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Climate load", String.format(Locale.US, "%,d BTU", c.btu), "$tons ton · need ≈${String.format(Locale.US, "%,d", c.rawBtu)}", Icons.Rounded.AcUnit, Modifier.weight(1f))
            Tile("Sightline 30–36°", String.format(Locale.US, "%.0f–%.0f\"", tv.minDiagonalIn, tv.maxDiagonalIn),
                "Mount @ " + DimensionFormatter.formatLength(tv.mountCentreM, sys, FractionPrecision.EIGHTH) + " ctr", Icons.Rounded.Tv, Modifier.weight(1f))
        }
        Text("SUN EXPOSURE", style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.IceWhite)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SunExposure.entries.forEach { s ->
                if (s == r.input.sun) ElectricLimePill(s.label, glow = false, textStyle = MpType.Chip.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp))
                else GlassPill(s.label, textStyle = MpType.Chip.copy(fontSize = 12.sp), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp), onClick = { vm.update { it.copy(sun = s) } })
            }
        }
    }
}

@Composable
private fun InputsCard(i: AdvisorInput, custom: Boolean, sys: UnitSystem, money: NumberFormat, vm: AiRoomAdvisorViewModel) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, null, tint = MpColors.ElectricLime, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("ADJUST ROOM & PRICES", style = MpType.LabelSpatial.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime)
            }
            fun len(m: Double) = DimensionFormatter.formatLength(m, sys, FractionPrecision.EIGHTH)
            if (custom) {
                Stepper("Room width", len(vm.customWidth), { vm.setCustomSize(vm.customWidth - 0.1, vm.customLength) }, { vm.setCustomSize(vm.customWidth + 0.1, vm.customLength) })
                Stepper("Room length", len(vm.customLength), { vm.setCustomSize(vm.customWidth, vm.customLength - 0.1) }, { vm.setCustomSize(vm.customWidth, vm.customLength + 0.1) })
            }
            Stepper("Ceiling height", len(i.ceilingM), { vm.update { it.copy(ceilingM = (it.ceilingM - 0.05).coerceAtLeast(1.8)) } }, { vm.update { it.copy(ceilingM = (it.ceilingM + 0.05).coerceAtMost(8.0)) } })
            Stepper("Doors", i.doors.toString(), { vm.update { it.copy(doors = (it.doors - 1).coerceAtLeast(0), openingsAreaSqM = null) } }, { vm.update { it.copy(doors = it.doors + 1, openingsAreaSqM = null) } })
            Stepper("Windows", i.windows.toString(), { vm.update { it.copy(windows = (it.windows - 1).coerceAtLeast(0), openingsAreaSqM = null) } }, { vm.update { it.copy(windows = it.windows + 1, openingsAreaSqM = null) } })
            Stepper("Sofa to TV", len(i.viewingDistanceM), { vm.update { it.copy(viewingDistanceM = (it.viewingDistanceM - 0.1).coerceAtLeast(1.0)) } }, { vm.update { it.copy(viewingDistanceM = (it.viewingDistanceM + 0.1).coerceAtMost(8.0)) } })
            Stepper("Occupants", i.occupants.toString(), { vm.update { it.copy(occupants = (it.occupants - 1).coerceAtLeast(1)) } }, { vm.update { it.copy(occupants = it.occupants + 1) } })
            Stepper("Tile box price", money.format(i.tileBoxPrice), { vm.update { it.copy(tileBoxPrice = (it.tileBoxPrice - 1).coerceAtLeast(0.0)) } }, { vm.update { it.copy(tileBoxPrice = it.tileBoxPrice + 1) } })
            Stepper("Paint price / L", money.format(i.paintLitrePrice), { vm.update { it.copy(paintLitrePrice = (it.paintLitrePrice - 0.5).coerceAtLeast(0.0)) } }, { vm.update { it.copy(paintLitrePrice = it.paintLitrePrice + 0.5) } })
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MpType.Body.copy(fontSize = 14.sp), color = MpColors.TextPrimary, modifier = Modifier.weight(1f))
        GlassCircleButton(Icons.Rounded.Remove, "Decrease $label", onMinus, size = 34.dp)
        Text(value, style = MpType.MetricValue.copy(fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
            modifier = Modifier.width(96.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
        GlassCircleButton(Icons.Rounded.Add, "Increase $label", onPlus, size = 34.dp)
    }
}

