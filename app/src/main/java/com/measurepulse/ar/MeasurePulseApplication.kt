package com.measurepulse.ar

import android.app.Application
import com.measurepulse.ar.ads.CurrentActivity
import com.measurepulse.ar.billing.BillingManager
import androidx.glance.appwidget.updateAll
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.ui.widgets.AiQuickConverterWidget
import com.measurepulse.ar.ui.widgets.MinimalGlassBarWidget
import com.measurepulse.ar.ui.widgets.SpatialGlassHubWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MeasurePulseApplication : Application() {

    @Inject lateinit var scans: RoomScanRepository
    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject lateinit var currentActivity: CurrentActivity
    @Inject lateinit var billing: BillingManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(currentActivity)
        // Connects to Google Play, loads the Pro price and re-syncs owned purchases (handles refunds too).
        billing.start()
        // Keep home-screen widgets in sync with new scans, readings and unit changes.
        appScope.launch {
            combine(scans.observeScans(), prefs.preferences) { list, p -> list to p }
                .drop(1)
                .debounce(1_000)
                .collect { refreshWidgets() }
        }
    }

    private suspend fun refreshWidgets() = runCatching {
        SpatialGlassHubWidget().updateAll(this)
        AiQuickConverterWidget().updateAll(this)
        MinimalGlassBarWidget().updateAll(this)
    }
}
