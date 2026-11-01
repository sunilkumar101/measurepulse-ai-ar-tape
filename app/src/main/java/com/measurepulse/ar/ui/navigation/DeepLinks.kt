package com.measurepulse.ar.ui.navigation

import android.content.Intent
import com.measurepulse.ar.ui.widgets.WidgetRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Routes requested from outside the app (home-screen widgets). The NavHost consumes them. */
object DeepLinks {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun handle(intent: Intent?) {
        intent?.getStringExtra(WidgetRoutes.EXTRA_ROUTE)?.let { _pending.value = it }
    }

    fun consume() { _pending.value = null }
}
