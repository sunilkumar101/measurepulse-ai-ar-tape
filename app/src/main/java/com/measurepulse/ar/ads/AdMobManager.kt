package com.measurepulse.ar.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.measurepulse.ar.BuildConfig
import com.measurepulse.ar.data.monetization.ProEntitlement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class RewardOutcome {
    /** The user watched to the end — unlock. */
    EARNED,
    /** The user closed the video before the reward — don't unlock. */
    DISMISSED,
    /** No ad could be shown (offline, no fill, no consent) — callers unlock anyway so nobody is stuck. */
    UNAVAILABLE,
}

/**
 * Google Mobile Ads: initialization, rewarded + interstitial preloading and showing, frequency capping.
 * Nothing loads or shows for Pro users or before consent allows ad requests.
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consent: ConsentManager,
    private val entitlement: ProEntitlement,
    private val activities: CurrentActivity,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val started = AtomicBoolean(false)
    private val initialized = MutableStateFlow(false)

    private val rewarded = MutableStateFlow<RewardedAd?>(null)
    private var rewardedLoading = false
    private val interstitial = MutableStateFlow<InterstitialAd?>(null)
    private var interstitialLoading = false

    private val pacingPrefs = context.getSharedPreferences("ad_pacing", Context.MODE_PRIVATE)

    /** Banners render only when the SDK is up and the user isn't Pro. */
    val bannersEnabled: StateFlow<Boolean> = combine(initialized, entitlement.isPro) { ready, pro -> ready && !pro }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        // Going Pro drops any preloaded full-screen ads immediately.
        scope.launch {
            entitlement.isPro.collect { pro ->
                if (pro) { rewarded.value = null; interstitial.value = null } else if (initialized.value) preload()
            }
        }
    }

    /** Starts the SDK once consent allows it. Safe to call repeatedly. */
    fun initializeIfAllowed() {
        if (!consent.canRequestAds || !started.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            val testIds = ConsentManager.testDeviceIds()
            if (BuildConfig.DEBUG && testIds.isNotEmpty()) {
                MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(testIds).build())
            }
            // Google recommends initializing on a background thread to keep app start smooth.
            MobileAds.initialize(context) { status ->
                Log.i(TAG, "Mobile Ads ready: ${status.adapterStatusMap.keys}")
                scope.launch { initialized.value = true; preload() }
            }
        }
    }

    private fun canServe() = initialized.value && consent.canRequestAds && !entitlement.isPro.value

    private fun preload() {
        loadRewarded()
        loadInterstitial()
    }

    // ------------------------------------------------------------------ Rewarded

    private fun loadRewarded() {
        if (!canServe() || rewarded.value != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(context, BuildConfig.AD_UNIT_REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedLoading = false; rewarded.value = ad }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedLoading = false
                Log.w(TAG, "Rewarded failed to load: ${error.code} ${error.message}")
            }
        })
    }

    /**
     * Shows a rewarded video. Waits up to [waitMs] for one to load if none is cached;
     * returns [RewardOutcome.UNAVAILABLE] instead of hanging when offline or unfilled.
     */
    suspend fun showRewarded(waitMs: Long = 6_000): RewardOutcome = withContext(Dispatchers.Main.immediate) {
        if (entitlement.isPro.value) return@withContext RewardOutcome.EARNED
        if (!consent.canRequestAds) return@withContext RewardOutcome.UNAVAILABLE
        val ad = rewarded.value ?: withTimeoutOrNull(waitMs) {
            initialized.first { it }
            loadRewarded()
            rewarded.filterNotNull().first()
        } ?: return@withContext RewardOutcome.UNAVAILABLE
        val activity = activities.activity ?: return@withContext RewardOutcome.UNAVAILABLE
        rewarded.value = null

        val result = CompletableDeferred<RewardOutcome>()
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = markFullScreenShown()
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
                result.complete(if (earned) RewardOutcome.EARNED else RewardOutcome.DISMISSED)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${error.code} ${error.message}")
                loadRewarded()
                result.complete(RewardOutcome.UNAVAILABLE)
            }
        }
        ad.show(activity) { earned = true }
        awaitAdClosed(result) {
            loadRewarded()
            if (earned) RewardOutcome.EARNED else RewardOutcome.DISMISSED
        }
    }

    // ------------------------------------------------------------------ Interstitial

    private fun loadInterstitial() {
        if (!canServe() || interstitial.value != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(context, BuildConfig.AD_UNIT_INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialLoading = false; interstitial.value = ad }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialLoading = false
                Log.w(TAG, "Interstitial failed to load: ${error.code} ${error.message}")
            }
        })
    }

    /**
     * Shows an interstitial at a natural break (after a finished save) — only if one is already loaded
     * and the 180 s cooldown since the last full-screen ad has passed. Suspends until it's closed.
     * Returns true if an ad was shown.
     */
    suspend fun showInterstitialIfEligible(placement: String): Boolean = withContext(Dispatchers.Main.immediate) {
        if (!canServe()) return@withContext false
        val now = System.currentTimeMillis()
        val last = pacingPrefs.getLong(KEY_LAST_FULLSCREEN, 0L)
        if (!AdPacing.interstitialAllowed(now, last)) {
            Log.i(TAG, "Interstitial [$placement] skipped: cooldown ${AdPacing.secondsUntilAllowed(now, last)} s left")
            return@withContext false
        }
        val ad = interstitial.value
        val activity = activities.activity
        if (ad == null || activity == null) {
            Log.i(TAG, "Interstitial [$placement] skipped: none loaded")
            loadInterstitial()
            return@withContext false
        }
        interstitial.value = null
        Log.i(TAG, "Interstitial [$placement] showing")
        val result = CompletableDeferred<Boolean>()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = markFullScreenShown()
            override fun onAdDismissedFullScreenContent() { loadInterstitial(); result.complete(true) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { loadInterstitial(); result.complete(false) }
        }
        ad.show(activity)
        awaitAdClosed(result) { loadInterstitial(); true }
    }

    /**
     * Waits for the ad's close callback. Safety net: if one of our own screens is resumed but the callback
     * still hasn't come [CLOSE_GRACE_MS] later (e.g. a widget or deep link opened the app on top of the ad),
     * stop waiting so the caller — and the "Loading video…" dialog — never hang.
     */
    private suspend fun <T> awaitAdClosed(result: CompletableDeferred<T>, fallback: () -> T): T = coroutineScope {
        val watchdog = launch {
            activities.resumed.first { !it.javaClass.name.startsWith(ADS_PACKAGE) }
            delay(CLOSE_GRACE_MS)
            if (result.complete(fallback())) Log.w(TAG, "Ad close callback missing — continuing without it")
        }
        try { result.await() } finally { watchdog.cancel() }
    }

    private fun markFullScreenShown() {
        pacingPrefs.edit().putLong(KEY_LAST_FULLSCREEN, System.currentTimeMillis()).apply()
    }

    /** Debug only: clears the cooldown so the interstitial can be tested repeatedly. */
    fun resetPacingForTesting() {
        if (BuildConfig.DEBUG) pacingPrefs.edit().remove(KEY_LAST_FULLSCREEN).apply()
    }

    companion object {
        private const val TAG = "MP.Ads"
        private const val KEY_LAST_FULLSCREEN = "last_fullscreen_ms"
        private const val ADS_PACKAGE = "com.google.android.gms.ads"
        private const val CLOSE_GRACE_MS = 2_000L
    }
}
