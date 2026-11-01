package com.measurepulse.ar.data.ar.engine

import android.graphics.Bitmap
import com.measurepulse.ar.data.ar.EngineFrame
import com.measurepulse.ar.data.ar.EngineKind
import com.measurepulse.ar.data.ar.Vec3
import kotlinx.coroutines.flow.StateFlow

/**
 * A source of spatial frames for the AR tape.
 * - Engine A [ArCoreMeasureEngine]: ARCore plane/depth hit-tests with drift-corrected anchors.
 * - Engine B [SensorFloorMeasureEngine]: CameraX preview + rotation-vector trigonometry (any phone).
 */
interface MeasureEngine {
    val kind: EngineKind
    val frames: StateFlow<EngineFrame?>

    /** Ask the engine to track a locked point (ARCore creates an Anchor). No-op for engines without anchors. */
    fun anchorPoint(id: Long, position: Vec3) {}
    fun removeAnchor(id: Long) {}
    fun clearAnchors() {}

    /** Grabs the current camera image (same size as the view), delivered on the main thread. */
    fun capture(onResult: (Bitmap?) -> Unit)
}
