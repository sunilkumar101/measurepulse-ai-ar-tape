package com.measurepulse.ar.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.measurepulse.ar.MainActivity
import com.measurepulse.ar.R
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.local.RoomScanEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Widgets aren't Hilt-injected; they reach the repositories through this entry point. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun scans(): RoomScanRepository
    fun prefs(): UserPreferencesRepository
}

data class WidgetData(val latest: RoomScanEntity?, val latestRoom: RoomScanEntity?, val prefs: UserPreferences)

internal suspend fun loadWidgetData(context: Context): WidgetData {
    val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    return WidgetData(ep.scans().latest(), ep.scans().latestRoom(), ep.prefs().preferences.first())
}

/** Deep-link routes understood by MainActivity / the NavHost. */
object WidgetRoutes {
    const val EXTRA_ROUTE = "com.measurepulse.ar.ROUTE"

    fun intent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java)
            // A distinct action per route keeps each button's PendingIntent separate.
            .setAction("com.measurepulse.ar.OPEN.$route")
            .putExtra(EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}

internal object W {
    val Panel = Color(0xF0161830)
    val Glass = Color(0x33FFFFFF)
    val Lime = Color(0xFFDDF83D)
    val Ink = Color(0xFF111322)
    val White = Color(0xFFF3F4FB)
    val Ice = Color(0xFFE2E8F0)
    val Muted = Color(0xFF9E9FB9)
    val Violet = Color(0xFFB845ED)

    fun text(color: Color, size: TextUnit, bold: Boolean = false) =
        TextStyle(color = ColorProvider(color), fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
}

/** Icon + label tool button. [primary] = solid lime with ink content. */
@Composable
internal fun WidgetTool(context: Context, icon: Int, label: String, route: String, primary: Boolean, modifier: GlanceModifier = GlanceModifier) {
    Column(
        modifier.clickable(actionStartActivity(WidgetRoutes.intent(context, route))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            GlanceModifier.size(40.dp).background(ImageProvider(if (primary) R.drawable.widget_btn_lime else R.drawable.widget_btn_glass)),
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(icon), contentDescription = label, modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(ColorProvider(if (primary) W.Ink else W.White)))
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(label, style = W.text(if (primary) W.Lime else W.Ice, 11.sp, bold = primary), maxLines = 1)
    }
}

internal fun GlanceModifier.glassPanel() = this.background(ImageProvider(R.drawable.widget_bg)).padding(12.dp)

/** Short lengths read better in mm/cm on a small widget. */
fun compactMetric(meters: Double): String = when {
    meters < 0.1 -> String.format(java.util.Locale.US, "%.1f mm", meters * 1000)
    meters < 1.0 -> String.format(java.util.Locale.US, "%.1f cm", meters * 100)
    else -> String.format(java.util.Locale.US, "%.2f m", meters)
}
