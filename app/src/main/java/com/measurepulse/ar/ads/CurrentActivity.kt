package com.measurepulse.ar.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full-screen ads and the Play purchase sheet need an Activity, but they're requested from ViewModels.
 * This keeps a weak reference to the resumed activity (registered once in the Application).
 */
@Singleton
class CurrentActivity @Inject constructor() : Application.ActivityLifecycleCallbacks {
    private var ref: WeakReference<Activity>? = null

    val activity: Activity?
        get() = ref?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { ref = WeakReference(activity) }
    private val _resumed = MutableSharedFlow<Activity>(extraBufferCapacity = 8)
    /** Every activity resume in this process (including Google's ad screen). */
    val resumed: SharedFlow<Activity> = _resumed

    override fun onActivityResumed(activity: Activity) {
        ref = WeakReference(activity)
        _resumed.tryEmit(activity)
    }
    override fun onActivityDestroyed(activity: Activity) { if (ref?.get() === activity) ref = null }
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
