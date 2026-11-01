package com.measurepulse.ar.ui.screens.artape

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.measurepulse.ar.data.ar.SpatialMeasureController
import com.measurepulse.ar.data.ar.engine.ArCoreMeasureEngine
import com.measurepulse.ar.data.ar.engine.MeasureEngine
import com.measurepulse.ar.data.ar.engine.SensorFloorMeasureEngine

/** Engine B: CameraX preview + rotation sensors. Works on every phone. */
@Composable
fun SensorCameraLayer(
    controller: SpatialMeasureController,
    cameraHeightM: Double,
    onEngine: (MeasureEngine) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val engine = remember { SensorFloorMeasureEngine(context, cameraHeightM) }
    LaunchedEffect(cameraHeightM) { engine.heightM = cameraHeightM }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE // TextureView: easy bitmap capture
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    DisposableEffect(engine, lifecycleOwner) {
        controller.attach(engine)
        onEngine(engine)
        engine.bindCamera(lifecycleOwner, previewView)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> engine.startSensors()
                Lifecycle.Event.ON_PAUSE -> engine.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) engine.startSensors()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.stop()
            controller.detach(engine)
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/** Engine A: ARCore session rendered into a GLSurfaceView. Calls [onFailed] if ARCore can't start. */
@Composable
fun ArCoreCameraLayer(
    controller: SpatialMeasureController,
    cameraHeightM: Double,
    onEngine: (MeasureEngine) -> Unit,
    onFailed: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val engine = remember { runCatching { ArCoreMeasureEngine.create(context, cameraHeightM) }.getOrNull() }
    if (engine == null) {
        LaunchedEffect(Unit) { onFailed() }
        return
    }
    LaunchedEffect(cameraHeightM) { engine.fallbackHeightM = cameraHeightM }
    val glView = remember { engine.createSurfaceView() }

    DisposableEffect(engine, lifecycleOwner) {
        controller.attach(engine)
        onEngine(engine)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (!engine.resume(glView)) onFailed()
                Lifecycle.Event.ON_PAUSE -> engine.pause(glView)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !engine.resume(glView)) onFailed()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.pause(glView)
            engine.close()
            controller.detach(engine)
        }
    }
    AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
}
