package com.measurepulse.ar.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.measurepulse.ar.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google User Messaging Platform (UMP): GDPR / UK / Swiss consent and US state privacy messages.
 * The messages themselves are configured in AdMob › Privacy & messaging; UMP decides per user
 * whether a form is required and stores the answer (IAB TCF string) for the Ads SDK.
 */
@Singleton
class ConsentManager @Inject constructor(@ApplicationContext context: Context) {

    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True when the user must be able to change their choice later (shown on Screen 10). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Ads may be requested: consent was given, isn't needed here, or was gathered in an earlier session. */
    val canRequestAds: Boolean get() = info.canRequestAds()

    /**
     * Refreshes the consent status and shows the form if this user needs one.
     * [onDone] always runs (also offline / on errors) so the app never gets stuck.
     */
    fun gather(activity: Activity, onDone: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                // Test devices behave as if they were in the EEA, so the GDPR form can be verified anywhere.
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .apply { testDeviceIds().forEach { addTestDeviceHashedId(it) } }
                        .build()
                )
            }
        }.build()

        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.errorCode} ${error.message}")
                    refresh()
                    onDone(canRequestAds)
                }
            },
            { error ->
                Log.w(TAG, "Consent info update failed: ${error.errorCode} ${error.message}")
                refresh()
                onDone(canRequestAds)
            },
        )
    }

    /** Re-opens the consent / "Do not sell" form so the user can change their choice. */
    fun showPrivacyOptions(activity: Activity, onDone: (String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            refresh()
            onDone(error?.message)
        }
    }

    /** Debug only: forget the stored choice so the form shows again on the next launch. */
    fun resetForTesting() {
        if (BuildConfig.DEBUG) { info.reset(); refresh() }
    }

    private fun refresh() {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    init { refresh() }

    companion object {
        private const val TAG = "MP.Consent"
        fun testDeviceIds(): List<String> =
            BuildConfig.ADMOB_TEST_DEVICE_IDS.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
