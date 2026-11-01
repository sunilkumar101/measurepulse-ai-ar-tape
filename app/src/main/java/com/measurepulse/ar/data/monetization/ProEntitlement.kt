package com.measurepulse.ar.data.monetization

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.measurepulse.ar.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val Context.monetizationStore by preferencesDataStore(name = "monetization")

data class ProState(
    /** A verified Google Play purchase of the lifetime pass (cached so Pro works offline). */
    val purchased: Boolean = false,
    /** Debug builds only: pretend to be Pro so both Free and Pro can be tested on a phone. */
    val debugOverride: Boolean = false,
) {
    val isPro: Boolean get() = purchased || (BuildConfig.DEBUG && debugOverride)
}

/** Single source of truth for "is this user Pro?" — every ad and export gate checks it. */
@Singleton
class ProEntitlement @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val PURCHASED = booleanPreferencesKey("pro_purchased")
        val DEBUG_PRO = booleanPreferencesKey("debug_pro_override")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val state: Flow<ProState> = context.monetizationStore.data
        .map { ProState(it[Keys.PURCHASED] ?: false, it[Keys.DEBUG_PRO] ?: false) }
        .distinctUntilChanged()

    /** Cold flow without a fake initial value — use it when a wrong first "false" would matter. */
    val proFlow: Flow<Boolean> = state.map { it.isPro }.distinctUntilChanged()

    /** Hot value for synchronous checks. Starts false for one DataStore read at process start. */
    val isPro: StateFlow<Boolean> = proFlow.stateIn(scope, SharingStarted.Eagerly, false)

    val stateNow: StateFlow<ProState> = state.stateIn(scope, SharingStarted.Eagerly, ProState())

    suspend fun isProNow(): Boolean = proFlow.first()

    suspend fun setPurchased(owned: Boolean) {
        context.monetizationStore.edit { it[Keys.PURCHASED] = owned }
    }

    suspend fun setDebugOverride(on: Boolean) {
        if (!BuildConfig.DEBUG) return
        context.monetizationStore.edit { it[Keys.DEBUG_PRO] = on }
    }
}
