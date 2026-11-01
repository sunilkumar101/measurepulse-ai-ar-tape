package com.measurepulse.ar.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.measurepulse.ar.data.monetization.ProEntitlement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play Billing Library 7 — the one-time, non-consumable "MeasurePulse AI Pro" lifetime pass.
 *
 * Purchases are acknowledged (otherwise Play refunds them after 3 days) and cached in [ProEntitlement]
 * so Pro keeps working offline. Every launch re-reads owned purchases, which also handles refunds.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val entitlement: ProEntitlement,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectLock = Mutex()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _product = MutableStateFlow<ProductDetails?>(null)
    /** Localized price from Google Play (e.g. "$2.99", "AED 10.99"); null until the product loads. */
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    // Broadcast (not a queue): every visible collector gets each message, so a screen left in the back stack can't swallow it.
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Called once at app start: connect, load the price and re-sync owned purchases silently. */
    fun start() {
        scope.launch {
            if (!ensureConnected()) return@launch
            loadProduct()
            restore(silent = true)
        }
    }

    private suspend fun ensureConnected(): Boolean = connectLock.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingResponseCode.OK) Log.w(TAG, "Billing setup: ${result.responseCode} ${result.debugMessage}")
                    if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                }
                // The next call reconnects through ensureConnected().
                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    private suspend fun loadProduct(): ProductDetails? {
        _product.value?.let { return it }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        val result = client.queryProductDetails(params)
        val details = result.productDetailsList?.firstOrNull { it.productId == PRO_PRODUCT_ID }
        if (details == null) Log.w(TAG, "Product $PRO_PRODUCT_ID not found: ${result.billingResult.responseCode} ${result.billingResult.debugMessage}")
        _product.value = details
        _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        return details
    }

    /** Opens the Google Play purchase sheet for the lifetime pass. */
    fun purchase(activity: Activity) {
        if (_busy.value) return
        scope.launch {
            _busy.value = true
            try {
                if (!ensureConnected()) { say("Google Play isn't reachable right now — check your connection and try again"); return@launch }
                val details = loadProduct()
                if (details == null) {
                    say("Pro isn't on sale yet — it goes live once the app is published on Google Play")
                    return@launch
                }
                val flow = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
                    .build()
                val result = client.launchBillingFlow(activity, flow)
                if (result.responseCode != BillingResponseCode.OK) say(describe(result))
            } finally {
                _busy.value = false
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            when (result.responseCode) {
                BillingResponseCode.OK -> {
                    if (handle(purchases.orEmpty())) say("Welcome to Pro — ads are gone and every export is unlocked")
                }
                BillingResponseCode.USER_CANCELED -> say("Purchase cancelled")
                BillingResponseCode.ITEM_ALREADY_OWNED -> restore(silent = false)
                else -> say(describe(result))
            }
        }
    }

    /** Grants Pro for a completed purchase (acknowledging it first). Returns true if Pro is owned. */
    private suspend fun handle(purchases: List<Purchase>): Boolean {
        val pro = purchases.filter { PRO_PRODUCT_ID in it.products }
        val owned = pro.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (owned != null) {
            if (!owned.isAcknowledged) {
                val ack = client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(owned.purchaseToken).build()
                )
                if (ack.responseCode != BillingResponseCode.OK) Log.w(TAG, "Acknowledge failed: ${ack.responseCode} ${ack.debugMessage}")
            }
            entitlement.setPurchased(true)
            return true
        }
        if (pro.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            say("Payment pending — Pro unlocks automatically when Google Play confirms it")
        }
        return false
    }

    /** Re-reads purchases owned by the Play account ("Restore Purchase"). */
    fun restorePurchases() {
        scope.launch { restore(silent = false) }
    }

    private suspend fun restore(silent: Boolean) {
        if (!ensureConnected()) {
            if (!silent) say("Google Play isn't reachable right now — check your connection and try again")
            return
        }
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            if (!silent) say(describe(result.billingResult))
            return
        }
        val owned = handle(result.purchasesList)
        // Play is the source of truth: a refunded or revoked pass is removed here.
        if (!owned) entitlement.setPurchased(false)
        if (!silent) say(if (owned) "Pro restored — thanks for your support!" else "No Pro purchase found on this Google account")
    }

    private suspend fun say(text: String) { _messages.emit(text) }

    private fun describe(r: BillingResult): String = when (r.responseCode) {
        BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Billing isn't available on this device or account"
        BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.SERVICE_DISCONNECTED,
        BillingResponseCode.NETWORK_ERROR -> "Couldn't reach Google Play — check your connection and try again"
        BillingResponseCode.ITEM_UNAVAILABLE -> "Pro isn't available for purchase in your country yet"
        BillingResponseCode.DEVELOPER_ERROR -> "Pro isn't on sale yet — it goes live once the app is published on Google Play"
        else -> "Google Play error ${r.responseCode}. Please try again"
    }

    companion object {
        const val PRO_PRODUCT_ID = "measurepulse_pro_lifetime"
        private const val TAG = "MP.Billing"
    }
}
