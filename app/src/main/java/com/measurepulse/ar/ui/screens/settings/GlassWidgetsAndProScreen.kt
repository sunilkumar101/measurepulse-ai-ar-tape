package com.measurepulse.ar.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.flow.merge
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.components.ElectricLimePill
import com.measurepulse.ar.ui.components.FloatingGlassCtaBar
import com.measurepulse.ar.ui.components.GlassCircleButton
import com.measurepulse.ar.ui.components.GlassPill
import com.measurepulse.ar.ui.components.IridescentAiOrb
import com.measurepulse.ar.ui.components.SmokedFrostedGlassCard
import com.measurepulse.ar.ui.components.StatusDot
import com.measurepulse.ar.ui.components.StatusBarScrim
import com.measurepulse.ar.ui.components.TwilightGradientBackground
import com.measurepulse.ar.ui.theme.MpColors
import com.measurepulse.ar.ui.theme.MpType
import com.measurepulse.ar.ui.widgets.AiQuickConverterWidgetReceiver
import com.measurepulse.ar.ui.widgets.MinimalGlassBarWidgetReceiver
import com.measurepulse.ar.ui.widgets.SpatialGlassHubWidgetReceiver
import kotlinx.coroutines.delay

private val Preview = Color(0xF0161830)

/** Screen 10 — frosted glass home-screen widgets + Pro Studio pass. */
@Composable
fun GlassWidgetsAndProScreen(onBack: () -> Unit, viewModel: GlassWidgetsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val room by viewModel.latestRoom.collectAsStateWithLifecycle()
    val pro by viewModel.pro.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val billingBusy by viewModel.billingBusy.collectAsStateWithLifecycle()
    val privacyRequired by viewModel.privacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { merge(viewModel.messages, viewModel.billingMessages).collect { toast = it } }
    LaunchedEffect(toast) { if (toast != null) { delay(3200); toast = null } }
    val sys = prefs.unitSystem

    TwilightGradientBackground {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero") {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                        Spacer(Modifier.weight(1f))
                        ElectricLimePill("3 GLANCE STYLES", icon = Icons.Rounded.GridView, glow = false,
                            textStyle = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Frosted Glass", style = MpType.HeroItalic.copy(fontSize = 32.sp, lineHeight = 36.sp))
                    Text("Home Widgets", style = MpType.HeroItalic.copy(fontSize = 32.sp, lineHeight = 36.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal), color = MpColors.ElectricLime)
                    Spacer(Modifier.height(8.dp))
                    Text("Your latest room, last measurement and one-tap tools — pinned to your home screen.", style = MpType.Body, color = MpColors.IceWhite)
                }
            }

            // 4×2 hub
            item(key = "hubLabel") { SectionLabel("ACTIVE PREVIEW · 4×2 GRID", "UPDATES ON EVERY SCAN") }
            item(key = "hub") {
                SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Preview).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(size = 7.dp); Spacer(Modifier.width(6.dp))
                                Text("MEASUREPULSE.AI", style = MpType.LabelSpatial.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, modifier = Modifier.weight(1f))
                                Text("◎ Level ${prefs.lastLevelText ?: "—"}", style = MpType.MetricSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime,
                                    modifier = Modifier.clip(CircleShape).background(Color(0x33FFFFFF)).padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IridescentAiOrb(size = 42.dp, showStatusDot = true)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(room?.title ?: "No room scanned yet", style = MpType.Chip.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(room?.let { "${DimensionFormatter.formatArea(it.areaSqM, sys)} • ${it.qualityLabel}" } ?: "Measure your first room", style = MpType.Body.copy(fontSize = 12.sp), maxLines = 1)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                MiniTool(Icons.Rounded.Straighten, "AR Tape", primary = true)
                                MiniTool(Icons.Rounded.GridView, "Floorplan")
                                MiniTool(Icons.Rounded.SquareFoot, "Caliper")
                                MiniTool(Icons.Rounded.AutoAwesome, "AI Advisor")
                            }
                        }
                        ElectricLimePill("Pin 4×2 Glass Widget", icon = Icons.Rounded.Add, textStyle = MpType.Button.copy(fontSize = 16.sp), contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth(), onClick = { viewModel.pin(SpatialGlassHubWidgetReceiver::class.java, "Glass Hub widget") })
                    }
                }
            }

            // 2×2 converter
            item(key = "convLabel") { SectionLabel("MICRO GLANCE · 2×2 GRID", "INSTANT CONVERSION") }
            item(key = "conv") {
                SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val m = prefs.lastMeasurementM
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Preview).padding(14.dp)) {
                            Text("LAST LENGTH", style = MpType.LabelSpatial.copy(fontSize = 10.sp), color = MpColors.TextMuted)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(m?.let { com.measurepulse.ar.ui.widgets.compactMetric(it) } ?: "—", style = MpType.MetricDisplay.copy(fontSize = 30.sp), color = MpColors.ElectricLime)
                                Spacer(Modifier.width(10.dp))
                                Text(m?.let { "= " + DimensionFormatter.formatLength(it, UnitSystem.IMPERIAL, FractionPrecision.SIXTEENTH) } ?: "", style = MpType.MetricValue, color = MpColors.TextPrimary,
                                    modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text(prefs.lastMeasurementLabel ?: "Measure with AR Tape or Caliper", style = MpType.Body.copy(fontSize = 12.sp))
                        }
                        GlassPill("Pin 2×2 Widget", icon = Icons.Rounded.Add, iconTint = MpColors.ElectricLime, modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                            onClick = { viewModel.pin(AiQuickConverterWidgetReceiver::class.java, "Converter widget") })
                    }
                }
            }

            // 4×1 dock
            item(key = "barLabel") { SectionLabel("DOCK BAR · 4×1 STRIP", "ONE TAP TO LAUNCH") }
            item(key = "bar") {
                SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth().clip(CircleShape).background(Preview).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            DockItem(Icons.Rounded.RadioButtonChecked, prefs.lastLevelText ?: "Level", MpColors.ElectricLime)
                            DockItem(Icons.Rounded.SquareFoot, "Caliper", MpColors.TextPrimary)
                            DockItem(Icons.Rounded.Straighten, "AR Tape", MpColors.TextPrimary)
                            DockItem(Icons.Rounded.AutoAwesome, "Help With AI", MpColors.HyperMagenta)
                        }
                        GlassPill("Pin 4×1 Dock Bar", icon = Icons.Rounded.Add, iconTint = MpColors.ElectricLime, modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                            onClick = { viewModel.pin(MinimalGlassBarWidgetReceiver::class.java, "Dock Bar widget") })
                    }
                }
            }

            // Pro Studio
            item(key = "pro") {
                SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), tint = Color(0xB31C1E36)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val shownPrice = price ?: "\$2.99"
                        if (pro.isPro) {
                            ElectricLimePill(if (pro.purchased) "PRO ACTIVE · LIFETIME" else "PRO ACTIVE · DEBUG MODE", icon = Icons.Rounded.Verified, glow = false,
                                textStyle = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp))
                            Text("You're on MeasurePulse AI Pro", style = MpType.HeadlineItalic.copy(fontSize = 28.sp, lineHeight = 32.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal, fontWeight = FontWeight.Bold))
                            Text("Thank you! Every screen is ad-free and every blueprint and shopping list exports instantly.", style = MpType.Body, color = MpColors.IceWhite)
                        } else {
                            ElectricLimePill("LIFETIME PASS · ${shownPrice.uppercase()} ONE-TIME", icon = Icons.Rounded.Verified, glow = false,
                                textStyle = MpType.MetricSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp))
                            Text("No subscription · Keep forever", style = MpType.LabelSpatial.copy(fontSize = 10.5.sp), color = MpColors.IceWhite)
                            Text("Unlock MeasurePulse AI Pro", style = MpType.HeadlineItalic.copy(fontSize = 28.sp, lineHeight = 32.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal, fontWeight = FontWeight.Bold))
                            Text("Remove ads and export every blueprint and shopping list without watching a video.", style = MpType.Body, color = MpColors.IceWhite)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Feature("Ad-free", "Every screen", Modifier.weight(1f))
                            Feature("Unlimited PDFs", "Scaled A4 blueprints", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Feature("Shopping lists", "Tiles, paint, AC, TV", Modifier.weight(1f))
                            Feature("Supports updates", "One-time, forever", Modifier.weight(1f))
                        }
                        if (!pro.isPro) {
                            FloatingGlassCtaBar(
                                label = if (billingBusy) "Opening Google Play…" else "Upgrade to Pro ($shownPrice)",
                                supportingText = "One-time purchase · Google Play", icon = Icons.Rounded.ShoppingBag,
                                onClick = { activity?.let(viewModel::upgrade) },
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPill("Restore Purchase", Modifier.weight(1.2f), icon = Icons.Rounded.Restore, textStyle = MpType.Chip.copy(fontSize = 12.sp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp), onClick = viewModel::restore)
                            if (privacyRequired || viewModel.debugTools) {
                                GlassPill("Privacy & ads", Modifier.weight(1f), icon = Icons.Rounded.PrivacyTip, textStyle = MpType.Chip.copy(fontSize = 12.sp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp), onClick = { activity?.let(viewModel::privacyOptions) })
                            }
                        }
                    }
                }
            }

            if (viewModel.debugTools) {
                item(key = "debug") { DebugMonetizationCard(pro.debugOverride, viewModel) }
            }
        }
        StatusBarScrim()
        toast?.let {
            Text(it, style = MpType.Chip.copy(fontSize = 13.sp), color = MpColors.Ink,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(20.dp)).background(MpColors.ElectricLime).padding(horizontal = 16.dp, vertical = 10.dp))
        }
    }
}

/** Debug builds only: flip Free / Pro and reset consent or ad pacing to test every monetization state. */
@Composable
private fun DebugMonetizationCard(debugPro: Boolean, viewModel: GlassWidgetsViewModel) {
    SmokedFrostedGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), tint = Color(0xB3111324)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, null, tint = MpColors.ElectricLime, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("DEBUG MONETIZATION TOOLS", style = MpType.LabelSpatial.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime)
            }
            // Whole row toggles, not just the switch.
            Row(
                Modifier.fillMaxWidth()
                    .toggleable(value = debugPro, role = Role.Switch, onValueChange = { viewModel.setDebugPro(it) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Debug Pro mode", style = MpType.CardTitle.copy(fontSize = 16.sp), color = MpColors.TextPrimary)
                    Text(if (debugPro) "Pro · 100% ad-free" else "Free · ad-supported (test ads)", style = MpType.Body.copy(fontSize = 12.sp), color = MpColors.IceWhite)
                }
                Switch(
                    checked = debugPro, onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MpColors.Ink, checkedTrackColor = MpColors.ElectricLime,
                        uncheckedThumbColor = MpColors.IceWhite, uncheckedTrackColor = Color(0x33FFFFFF), uncheckedBorderColor = Color(0x55FFFFFF),
                    ),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPill("Reset consent", textStyle = MpType.Chip.copy(fontSize = 13.sp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), onClick = { viewModel.resetConsent() })
                GlassPill("Reset ad cooldown", textStyle = MpType.Chip.copy(fontSize = 13.sp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), onClick = { viewModel.resetAdCooldown() })
            }
            GlassPill("Test interstitial (180 s cap)", textStyle = MpType.Chip.copy(fontSize = 13.sp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp), onClick = { viewModel.testInterstitial() })
            Text("Hidden in release builds. Debug builds always use Google's test ad units.", style = MpType.Body.copy(fontSize = 11.sp), color = MpColors.IceWhite.copy(alpha = 0.7f))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun SectionLabel(left: String, right: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MpType.LabelSpatial.copy(fontSize = 10.5.sp), color = MpColors.IceWhite, modifier = Modifier.weight(1f).padding(end = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(right, style = MpType.LabelSpatial.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = MpColors.ElectricLime, maxLines = 1)
    }
}

@Composable
private fun MiniTool(icon: ImageVector, label: String, primary: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (primary) MpColors.ElectricLime else Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (primary) MpColors.Ink else MpColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MpType.Chip.copy(fontSize = 11.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium), color = if (primary) MpColors.ElectricLime else MpColors.IceWhite)
    }
}

@Composable
private fun DockItem(icon: ImageVector, label: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MpType.MetricSmall.copy(fontSize = 10.sp), color = MpColors.IceWhite, maxLines = 1)
    }
}

@Composable
private fun Feature(title: String, sub: String, modifier: Modifier) {
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x99111324)).border(1.dp, MpColors.Divider, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Icon(Icons.Rounded.CheckCircle, null, tint = MpColors.ElectricLime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MpType.Chip.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = MpColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MpType.Body.copy(fontSize = 11.sp), color = MpColors.IceWhite.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

