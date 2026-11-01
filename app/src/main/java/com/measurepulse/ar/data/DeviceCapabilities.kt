package com.measurepulse.ar.data

import android.content.Context
import com.google.ar.core.ArCoreApk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ArSupport { CHECKING, SUPPORTED, SUPPORTED_NEEDS_INSTALL, UNSUPPORTED }

/** Tells the UI whether ARCore can run here, so screens show "ARCore Depth" or the sensor fallback honestly. */
@Singleton
class DeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun arSupport(): ArSupport = withContext(Dispatchers.Default) {
        repeat(10) {
            val availability = runCatching { ArCoreApk.getInstance().checkAvailability(context) }.getOrNull()
                ?: return@withContext ArSupport.UNSUPPORTED
            when {
                availability.isTransient -> delay(200)
                availability == ArCoreApk.Availability.SUPPORTED_INSTALLED -> return@withContext ArSupport.SUPPORTED
                availability.isSupported -> return@withContext ArSupport.SUPPORTED_NEEDS_INSTALL
                else -> return@withContext ArSupport.UNSUPPORTED
            }
        }
        ArSupport.UNSUPPORTED
    }
}
