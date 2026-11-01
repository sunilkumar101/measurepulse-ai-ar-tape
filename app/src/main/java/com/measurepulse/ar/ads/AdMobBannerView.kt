package com.measurepulse.ar.ads

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.measurepulse.ar.BuildConfig
import com.measurepulse.ar.data.monetization.ProEntitlement
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adMob(): AdMobManager
    fun entitlement(): ProEntitlement
}

@Composable
private fun rememberAdMob(): AdMobManager {
    val context = LocalContext.current
    return remember { EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java).adMob() }
}

/** True when this user sees banners (SDK ready, consent OK, not Pro). Screens use it to reserve space. */
@Composable
fun rememberBannersEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val enabled by rememberAdMob().bannersEnabled.collectAsStateWithLifecycle()
    return enabled
}

/** True when the user owns Pro (or debug Pro is on). */
@Composable
fun rememberIsPro(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    val entitlement = remember { EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java).entitlement() }
    val pro by entitlement.isPro.collectAsStateWithLifecycle()
    return pro
}

/** Space a screen should add under scrolling content while a banner is showing. */
val BannerReserve: Dp = 66.dp

/**
 * Anchored adaptive banner sized to the available width. Renders nothing for Pro users,
 * before consent/initialization, or when no ad fills (it only takes space once an ad has loaded).
 */
@Composable
fun AdMobBannerView(modifier: Modifier = Modifier) {
    if (!rememberBannersEnabled()) return
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        var loaded by remember { mutableStateOf(false) }
        val adView = remember(adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = BuildConfig.AD_UNIT_BANNER
                adListener = object : AdListener() {
                    override fun onAdLoaded() { loaded = true }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("MP.Ads", "Banner failed: ${error.code} ${error.message}")
                        loaded = false
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer); adView.destroy() }
        }
        // One view; it only takes space once an ad has filled.
        val height by animateDpAsState(if (loaded) adSize.height.dp else 0.dp, label = "bannerHeight")
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(height))
    }
}
