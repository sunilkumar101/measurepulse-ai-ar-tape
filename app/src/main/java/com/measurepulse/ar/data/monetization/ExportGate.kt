package com.measurepulse.ar.data.monetization

import com.measurepulse.ar.ads.AdMobManager
import com.measurepulse.ar.ads.RewardOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportKind(val title: String, val body: String) {
    BLUEPRINT(
        "Unlock this PDF blueprint",
        "Watch a short video to export a scaled A4 blueprint of this room — or go Pro for unlimited exports with no ads.",
    ),
    SHOPPING_LIST(
        "Unlock your shopping list",
        "Watch a short video to export the full renovation & material shopping list — or go Pro for unlimited exports with no ads.",
    ),
}

/** Decides whether a premium action (PDF export) may run. */
interface ExportGate {
    /** Returns true when the export may proceed (Pro user, rewarded video watched, or no ad available). */
    suspend fun requestPdfExport(kind: ExportKind): Boolean
}

data class RewardOffer(val kind: ExportKind, val loadingVideo: Boolean = false)

enum class OfferChoice { WATCH_VIDEO, GO_PRO, NOT_NOW }

/**
 * Free users unlock each export with a rewarded video the user chooses to watch (opt-in, as AdMob requires).
 * If no video can be shown (offline, no fill, no consent) the export is unlocked anyway.
 * The dialog itself is drawn by MonetizationHost, which observes [offer].
 */
@Singleton
class RewardedExportGate @Inject constructor(
    private val entitlement: ProEntitlement,
    private val ads: AdMobManager,
) : ExportGate {

    private val _offer = MutableStateFlow<RewardOffer?>(null)
    val offer: StateFlow<RewardOffer?> = _offer.asStateFlow()

    private var choice: CompletableDeferred<OfferChoice>? = null

    private val _notices = Channel<String>(Channel.BUFFERED)
    /** Short confirmations shown as a lime toast by the host. */
    val notices = _notices.receiveAsFlow()

    private val _openPro = Channel<Unit>(Channel.CONFLATED)
    /** Emits when the user picked "Go Pro" — the host navigates to Screen 10. */
    val openPro = _openPro.receiveAsFlow()

    override suspend fun requestPdfExport(kind: ExportKind): Boolean {
        if (entitlement.isProNow()) return true
        choice?.cancel()
        val pending = CompletableDeferred<OfferChoice>().also { choice = it }
        _offer.value = RewardOffer(kind)
        try {
            return when (pending.await()) {
                OfferChoice.NOT_NOW -> false
                OfferChoice.GO_PRO -> { _openPro.send(Unit); false }
                OfferChoice.WATCH_VIDEO -> {
                    _offer.value = RewardOffer(kind, loadingVideo = true)
                    when (ads.showRewarded()) {
                        RewardOutcome.EARNED -> { _notices.send("Unlocked — exporting your PDF"); true }
                        RewardOutcome.DISMISSED -> { _notices.send("Video closed early — export not unlocked"); false }
                        RewardOutcome.UNAVAILABLE -> { _notices.send("No video available right now — this export is on us"); true }
                    }
                }
            }
        } finally {
            if (choice === pending) choice = null
            _offer.value = null
        }
    }

    /** Called by the dialog. */
    fun choose(c: OfferChoice) {
        choice?.complete(c)
    }
}
