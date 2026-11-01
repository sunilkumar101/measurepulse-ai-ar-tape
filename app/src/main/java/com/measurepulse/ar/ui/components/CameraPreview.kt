package com.measurepulse.ar.ui.components

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

private val Ratio43 = ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
    .build()

/**
 * Back-camera preview (4:3, FILL_CENTER, TextureView for easy snapshots) with optional image analysis.
 * [zoomRatio] is applied through CameraX (optical where the device supports it, digital otherwise).
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    zoomRatio: Float = 1f,
    analyzer: ImageAnalysis.Analyzer? = null,
    onPreviewView: (PreviewView) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(previewView) { onPreviewView(previewView) }
    LaunchedEffect(lifecycleOwner, analyzer) {
        runCatching {
            val p = ProcessCameraProvider.awaitInstance(context)
            provider = p
            val useCases = mutableListOf<UseCase>(
                Preview.Builder().setResolutionSelector(Ratio43).build().also { it.surfaceProvider = previewView.surfaceProvider },
            )
            if (analyzer != null) {
                useCases += ImageAnalysis.Builder()
                    .setResolutionSelector(Ratio43)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { it.setAnalyzer(executor, analyzer) }
            }
            p.unbindAll()
            camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases.toTypedArray())
        }.onFailure { Log.e("CameraPreview", "bind failed", it) }
    }
    LaunchedEffect(camera, zoomRatio) {
        camera?.let { cam ->
            val state = cam.cameraInfo.zoomState.value
            val z = zoomRatio.coerceIn(state?.minZoomRatio ?: 1f, state?.maxZoomRatio ?: zoomRatio)
            cam.cameraControl.setZoomRatio(z)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { provider?.unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())
}

/** True once the CAMERA permission is granted. */
fun hasCameraPermission(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
