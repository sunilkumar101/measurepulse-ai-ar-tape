package com.measurepulse.ar.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.measurepulse.ar.R
import com.measurepulse.ar.data.local.ScanCategory
import com.measurepulse.ar.domain.math.DimensionFormatter
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import com.measurepulse.ar.ui.navigation.Routes

// ---------------------------------------------------------------- 4×2 Glass Hub

class SpatialGlassHubWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { HubContent(context, data) }
    }
}

class SpatialGlassHubWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpatialGlassHubWidget()
}

@Composable
private fun HubContent(context: Context, d: WidgetData) {
    val sys = d.prefs.unitSystem
    val room = d.latestRoom
    Column(GlanceModifier.fillMaxSize().glassPanel()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.size(8.dp).background(ImageProvider(R.drawable.widget_btn_lime))) {}
            Spacer(GlanceModifier.width(6.dp))
            Text("MEASUREPULSE.AI", style = W.text(W.Lime, 10.sp, bold = true), modifier = GlanceModifier.defaultWeight())
            Row(
                GlanceModifier.background(ImageProvider(R.drawable.widget_pill_glass)).padding(horizontal = 8.dp, vertical = 3.dp)
                    .clickable(actionStartActivity(WidgetRoutes.intent(context, Routes.LEVEL))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(R.drawable.ic_w_level), "Level", GlanceModifier.size(12.dp), colorFilter = ColorFilter.tint(ColorProvider(W.Lime)))
                Spacer(GlanceModifier.width(4.dp))
                Text(d.prefs.lastLevelText?.let { "Level $it" } ?: "Level", style = W.text(W.Lime, 10.sp, bold = true), maxLines = 1)
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Column(
            GlanceModifier.fillMaxWidth().clickable(actionStartActivity(WidgetRoutes.intent(context, room?.let { Routes.blueprint(it.id) } ?: Routes.AR_TAPE))),
        ) {
            Text(room?.title ?: "No room scanned yet", style = W.text(W.White, 15.sp, bold = true), maxLines = 1)
            Text(
                room?.let { "${DimensionFormatter.formatArea(it.areaSqM, sys)} • ${it.wallCount} walls • ${it.qualityLabel}" } ?: "Tap AR Tape to measure your first room",
                style = W.text(W.Ice, 11.sp), maxLines = 1,
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetTool(context, R.drawable.ic_w_tape, "AR Tape", Routes.AR_TAPE, primary = true, modifier = GlanceModifier.defaultWeight())
            WidgetTool(context, R.drawable.ic_w_plan, "Floorplan", room?.let { Routes.blueprint(it.id) } ?: Routes.PROJECTS, primary = false, modifier = GlanceModifier.defaultWeight())
            WidgetTool(context, R.drawable.ic_w_caliper, "Caliper", Routes.CALIPER, primary = false, modifier = GlanceModifier.defaultWeight())
            WidgetTool(context, R.drawable.ic_w_ai, "AI Advisor", Routes.AI_ADVISOR_BASE, primary = false, modifier = GlanceModifier.defaultWeight())
        }
    }
}

// ---------------------------------------------------------------- 2×2 Quick Converter

class AiQuickConverterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { ConverterContent(context, data) }
    }
}

class AiQuickConverterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AiQuickConverterWidget()
}

@Composable
private fun ConverterContent(context: Context, d: WidgetData) {
    val p = d.prefs
    // Fall back to the latest saved scan's width when nothing has been measured yet.
    val meters = p.lastMeasurementM ?: d.latest?.takeIf { it.category != ScanCategory.LEVEL && it.widthM > 0 }?.widthM
    val label = p.lastMeasurementLabel ?: d.latest?.title
    Column(GlanceModifier.fillMaxSize().glassPanel()) {
        Text("LAST LENGTH", style = W.text(W.Muted, 10.sp, bold = true))
        Spacer(GlanceModifier.height(4.dp))
        Column(GlanceModifier.fillMaxWidth().clickable(actionStartActivity(WidgetRoutes.intent(context, Routes.PROJECTS)))) {
            Text(meters?.let { compactMetric(it) } ?: "—", style = W.text(W.Lime, 24.sp, bold = true), maxLines = 1)
            Text(meters?.let { "= " + DimensionFormatter.formatLength(it, UnitSystem.IMPERIAL, FractionPrecision.SIXTEENTH) } ?: "Measure something first",
                style = W.text(W.White, 12.sp), maxLines = 1)
            Text(label ?: "", style = W.text(W.Ice, 10.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(
            GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_pill_lime)).padding(vertical = 8.dp)
                .clickable(actionStartActivity(WidgetRoutes.intent(context, Routes.AR_TAPE))),
            horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(ImageProvider(R.drawable.ic_w_add), "Scan", GlanceModifier.size(16.dp), colorFilter = ColorFilter.tint(ColorProvider(W.Ink)))
            Spacer(GlanceModifier.width(4.dp))
            Text("Scan", style = W.text(W.Ink, 13.sp, bold = true))
        }
    }
}

// ---------------------------------------------------------------- 4×1 Minimal Dock

class MinimalGlassBarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { BarContent(context, data) }
    }
}

class MinimalGlassBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MinimalGlassBarWidget()
}

@Composable
private fun BarContent(context: Context, d: WidgetData) {
    Row(
        GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bar_bg)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarItem(context, R.drawable.ic_w_level, d.prefs.lastLevelText ?: "Level", Routes.LEVEL, W.Lime, GlanceModifier.defaultWeight())
        BarItem(context, R.drawable.ic_w_caliper, "Caliper", Routes.CALIPER, W.White, GlanceModifier.defaultWeight())
        BarItem(context, R.drawable.ic_w_tape, "AR Tape", Routes.AR_TAPE, W.White, GlanceModifier.defaultWeight())
        BarItem(context, R.drawable.ic_w_ai, "Ask AI", Routes.AI_ADVISOR_BASE, W.Violet, GlanceModifier.defaultWeight())
    }
}

@Composable
private fun BarItem(context: Context, icon: Int, label: String, route: String, tint: androidx.compose.ui.graphics.Color, modifier: GlanceModifier) {
    Row(
        modifier.padding(vertical = 6.dp).clickable(actionStartActivity(WidgetRoutes.intent(context, route))),
        horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(ImageProvider(icon), label, GlanceModifier.size(16.dp), colorFilter = ColorFilter.tint(ColorProvider(tint)))
        Spacer(GlanceModifier.width(4.dp))
        Text(label, style = W.text(W.Ice, 11.sp, bold = tint == W.Lime), maxLines = 1)
    }
}
