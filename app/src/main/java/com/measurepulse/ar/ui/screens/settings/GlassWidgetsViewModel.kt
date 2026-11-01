package com.measurepulse.ar.ui.screens.settings

import android.app.Activity
import android.content.Context
import com.measurepulse.ar.BuildConfig
import com.measurepulse.ar.ads.AdMobManager
import com.measurepulse.ar.ads.ConsentManager
import com.measurepulse.ar.billing.BillingManager
import com.measurepulse.ar.data.monetization.ProEntitlement
import com.measurepulse.ar.data.monetization.ProState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.measurepulse.ar.data.DEFAULT_CAMERA_HEIGHT_M
import com.measurepulse.ar.data.RoomScanRepository
import com.measurepulse.ar.data.UserPreferences
import com.measurepulse.ar.data.UserPreferencesRepository
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.isRoom
import com.measurepulse.ar.domain.math.FractionPrecision
import com.measurepulse.ar.domain.math.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlassWidgetsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    repo: RoomScanRepository,
    prefsRepo: UserPreferencesRepository,
    private val billing: BillingManager,
    private val entitlement: ProEntitlement,
    private val consent: ConsentManager,
    private val ads: AdMobManager,
) : ViewModel() {

    val pro: StateFlow<ProState> = entitlement.stateNow
    /** Localized Google Play price; null until Play returns it. */
    val price: StateFlow<String?> = billing.price
    val billingBusy: StateFlow<Boolean> = billing.busy
    val privacyOptionsRequired: StateFlow<Boolean> = consent.privacyOptionsRequired
    val debugTools: Boolean = BuildConfig.DEBUG

    val prefs: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly, UserPreferences(true, UnitSystem.METRIC, FractionPrecision.SIXTEENTH, DEFAULT_CAMERA_HEIGHT_M),
    )
    val latestRoom: StateFlow<RoomScanEntity?> = repo.observeScans()
        .map { list -> list.filter { it.isRoom }.maxByOrNull { it.createdAt } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** Asks the launcher to pin a widget (Android 8+). Some launchers don't support pinning. */
    fun <T : GlanceAppWidgetReceiver> pin(receiver: Class<T>, name: String) = viewModelScope.launch {
        val ok = runCatching { GlanceAppWidgetManager(context).requestPinGlanceAppWidget(receiver) }.getOrDefault(false)
        _messages.send(
            if (ok) "Confirm on your home screen to add the $name"
            else "Your launcher can't pin widgets from apps — long-press the home screen › Widgets › MeasurePulse"
        )
    }

    /** Google Play results; collected by the visible screen only. */
    val billingMessages: SharedFlow<String> = billing.messages

    fun upgrade(activity: Activity) = billing.purchase(activity)

    fun restore() = billing.restorePurchases()

    fun privacyOptions(activity: Activity) = consent.showPrivacyOptions(activity) { error ->
        if (error != null) viewModelScope.launch { _messages.send("Privacy options unavailable: $error") }
    }

    // ---- Debug-build tools (hidden in release) ----

    fun setDebugPro(on: Boolean) = viewModelScope.launch {
        entitlement.setDebugOverride(on)
        _messages.send(if (on) "Debug: Pro mode ON — all ads hidden" else "Debug: Free mode — ads are back")
    }

    fun resetConsent() = viewModelScope.launch {
        consent.resetForTesting()
        _messages.send("Consent reset — the form shows on the next app launch")
    }

    /** Runs the real interstitial path (eligibility + 180 s cooldown) used after a room/audit save. */
    fun testInterstitial() = viewModelScope.launch {
        val shown = ads.showInterstitialIfEligible("debug_test")
        if (!shown) _messages.send("No interstitial: Pro, cooldown active, or none loaded yet")
    }

    fun resetAdCooldown() = viewModelScope.launch {
        ads.resetPacingForTesting()
        _messages.send("Interstitial cooldown cleared")
    }
}
