package com.measurepulse.ar

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.measurepulse.ar.ads.AdMobManager
import com.measurepulse.ar.ads.ConsentManager
import com.measurepulse.ar.data.monetization.ProEntitlement
import kotlinx.coroutines.launch
import com.measurepulse.ar.data.ar.HardwareKeyRouter
import com.measurepulse.ar.ui.navigation.DeepLinks
import com.measurepulse.ar.ui.navigation.MeasurePulseNavHost
import com.measurepulse.ar.ui.theme.MeasurePulseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Lets the AR tape claim Volume Up/Down as a hands-free "lock point" button. */
    @Inject lateinit var keyRouter: HardwareKeyRouter
    @Inject lateinit var consent: ConsentManager
    @Inject lateinit var ads: AdMobManager
    @Inject lateinit var entitlement: ProEntitlement

    private var consentRequested = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinks.handle(intent)
    }

    /** UMP consent first (GDPR / US states), then Mobile Ads on a background thread. */
    private fun startAds() {
        // Consent from an earlier session lets ads start right away while the status refreshes.
        ads.initializeIfAllowed()
        if (consentRequested) return
        consentRequested = true
        consent.gather(this) { canRequest -> if (canRequest) ads.initializeIfAllowed() }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        keyRouter.dispatch(event) || super.dispatchKeyEvent(event)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the status & navigation bars; every screen applies WindowInsets itself.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) DeepLinks.handle(intent)
        // Ads only for free users: Pro users never see a consent form or load the Ads SDK.
        lifecycleScope.launch {
            entitlement.proFlow.collect { pro -> if (!pro) startAds() }
        }
        setContent {
            MeasurePulseTheme {
                MeasurePulseNavHost()
            }
        }
    }
}
