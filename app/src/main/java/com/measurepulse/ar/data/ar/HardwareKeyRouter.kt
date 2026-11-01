package com.measurepulse.ar.data.ar

import android.view.KeyEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes the hardware Volume Up/Down keys to the AR tape so a point can be locked without
 * touching the screen. MainActivity forwards every key event here; when no screen has claimed
 * the keys they keep their normal volume behaviour.
 */
@Singleton
class HardwareKeyRouter @Inject constructor() {
    @Volatile private var handler: (() -> Unit)? = null

    fun register(onVolumeKey: () -> Unit) { handler = onVolumeKey }

    fun unregister(onVolumeKey: () -> Unit) {
        if (handler === onVolumeKey) handler = null
    }

    /** Returns true when the event was consumed. */
    fun dispatch(event: KeyEvent): Boolean {
        val h = handler ?: return false
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) h()
        return true // swallow DOWN, repeats and UP so the volume panel never appears
    }
}
