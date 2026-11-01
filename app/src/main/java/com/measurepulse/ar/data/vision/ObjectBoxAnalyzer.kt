package com.measurepulse.ar.data.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An object box in the (rotated, upright) analysis image's pixel space. */
data class DetectedBox(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val label: String?, val confidence: Float?, val trackingId: Int?,
)

data class DetectionFrame(val boxes: List<DetectedBox>, val imageWidth: Int, val imageHeight: Int) {
    /** Maps an image box onto a FILL_CENTER preview of [viewW]×[viewH] pixels. */
    fun toView(b: DetectedBox, viewW: Int, viewH: Int): FloatArray {
        val scale = maxOf(viewW / imageWidth.toFloat(), viewH / imageHeight.toFloat())
        val ox = (viewW - imageWidth * scale) / 2f
        val oy = (viewH - imageHeight * scale) / 2f
        return floatArrayOf(ox + b.left * scale, oy + b.top * scale, ox + b.right * scale, oy + b.bottom * scale)
    }
}

/**
 * ML Kit on-device object detection (bundled base model, stream mode, multiple objects).
 * The base model localises objects reliably but only classifies coarsely (Home good, Plant, Place…),
 * so the *real-world height* always comes from the user's reference preset.
 */
class ObjectBoxAnalyzer : ImageAnalysis.Analyzer {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val _frames = MutableStateFlow<DetectionFrame?>(null)
    val frames: StateFlow<DetectionFrame?> = _frames.asStateFlow()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null) { proxy.close(); return }
        val rotation = proxy.imageInfo.rotationDegrees
        val w = if (rotation % 180 == 0) proxy.width else proxy.height
        val h = if (rotation % 180 == 0) proxy.height else proxy.width
        detector.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { objects ->
                _frames.value = DetectionFrame(
                    objects.map { o ->
                        val best = o.labels.maxByOrNull { it.confidence }
                        DetectedBox(
                            o.boundingBox.left.toFloat(), o.boundingBox.top.toFloat(),
                            o.boundingBox.right.toFloat(), o.boundingBox.bottom.toFloat(),
                            best?.text, best?.confidence, o.trackingId,
                        )
                    },
                    w, h,
                )
            }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() = detector.close()
}
