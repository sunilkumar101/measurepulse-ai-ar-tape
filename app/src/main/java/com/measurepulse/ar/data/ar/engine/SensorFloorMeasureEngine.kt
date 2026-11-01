package com.measurepulse.ar.data.ar.engine

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.measurepulse.ar.data.ar.EngineFrame
import com.measurepulse.ar.data.ar.EngineKind
import com.measurepulse.ar.data.ar.HitSource
import com.measurepulse.ar.data.ar.ScreenProjector
import com.measurepulse.ar.data.ar.SensorFloorMath
import com.measurepulse.ar.data.ar.TrackingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan

/**
 * Engine B — works on every Android phone.
 *
 * Shows a CameraX preview and treats the phone as a camera at [heightM] above a flat floor.
 * The centre ray (from the fused rotation sensor) is intersected with the floor; points are
 * fixed in a world frame (X east, Y north, Z up). The user should pivot in place rather than walk
 * between points, because the camera position is assumed constant.
 *
 * Prefers GAME_ROTATION_VECTOR (gyro + accelerometer, no magnetometer) because indoor magnetic
 * interference makes the compass heading wander, which would bend point-to-point lines. Falls back
 * to ROTATION_VECTOR, then accelerometer + magnetometer.
 */
class SensorFloorMeasureEngine(
    private val context: Context,
    heightM: Double,
) : MeasureEngine, SensorEventListener {

    override val kind = EngineKind.SENSOR_FLOOR

    private val _frames = MutableStateFlow<EngineFrame?>(null)
    override val frames: StateFlow<EngineFrame?> = _frames.asStateFlow()

    @Volatile var heightM: Double = heightM

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnet: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val hasMotionSensors: Boolean get() = rotationSensor != null || (accel != null && magnet != null)

    private val smoothedVector = FloatArray(5)
    private var hasVector = false
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnet = false
    private val rotation = FloatArray(9)
    private var lastEmit = 0L

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val vfovDeg: Double = readVerticalFov()

    // ---------- Lifecycle ----------

    fun bindCamera(lifecycleOwner: LifecycleOwner, view: PreviewView) {
        previewView = view
        lifecycleOwner.lifecycleScope.launch {
            runCatching {
                val provider = ProcessCameraProvider.awaitInstance(context)
                cameraProvider = provider
                val preview = Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .build()
                    )
                    .build()
                    .also { it.surfaceProvider = view.surfaceProvider }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }.onFailure { Log.e(TAG, "Camera bind failed", it) }
        }
    }

    fun startSensors() {
        hasVector = false
        val rate = SensorManager.SENSOR_DELAY_GAME
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, rate)
        } else {
            accel?.let { sensorManager.registerListener(this, it, rate) }
            magnet?.let { sensorManager.registerListener(this, it, rate) }
        }
        if (!hasMotionSensors) emit(null, TrackingStatus.PAUSED, "This phone has no motion sensors")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        cameraProvider?.unbindAll()
    }

    // ---------- Sensors ----------

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                val n = minOf(event.values.size, 4)
                // Low-pass the quaternion to calm hand tremor (cheap and lag-free enough at 50 Hz).
                if (!hasVector) {
                    for (i in 0 until n) smoothedVector[i] = event.values[i]
                    hasVector = true
                } else {
                    // Keep the quaternion hemisphere consistent before blending.
                    var dot = 0f
                    for (i in 0 until n) dot += smoothedVector[i] * event.values[i]
                    val sign = if (dot < 0) -1f else 1f
                    for (i in 0 until n) smoothedVector[i] += ALPHA * (sign * event.values[i] - smoothedVector[i])
                }
                SensorManager.getRotationMatrixFromVector(rotation, smoothedVector.copyOf(n))
                publish()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) gravity[i] += if (hasGravity) ALPHA * (event.values[i] - gravity[i]) else event.values[i]
                hasGravity = true
                if (hasMagnet && SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) publish()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) geomagnetic[i] += if (hasMagnet) ALPHA * (event.values[i] - geomagnetic[i]) else event.values[i]
                hasMagnet = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun publish() {
        val now = SystemClock.uptimeMillis()
        if (now - lastEmit < 15) return
        lastEmit = now
        val r = rotation.copyOf()
        val h = heightM
        val hit = SensorFloorMath.floorHit(r, h)
        emit(
            r,
            TrackingStatus.TRACKING,
            if (hit == null) "Tilt down until the reticle sits on the floor" else null,
        )
    }

    private fun emit(r: FloatArray?, status: TrackingStatus, hint: String?) {
        val h = heightM
        val view = previewView
        val w = view?.width ?: 0
        val vh = view?.height ?: 0
        val focal = if (w > 0 && vh > 0) SensorFloorMath.focalPx(w, vh, vfovDeg) else 1f
        val camera = SensorFloorMath.camera(h)
        val projector = if (r == null || w == 0) ScreenProjector { null } else ScreenProjector { world ->
            SensorFloorMath.project(r, camera, world, focal, w / 2f, vh / 2f)
        }
        val hit = r?.let { SensorFloorMath.floorHit(it, h) }
        _frames.value = EngineFrame(
            kind = kind,
            status = status,
            reticle = hit,
            reticleAccuracyM = if (r != null && hit != null) SensorFloorMath.accuracyAt(r, h) else 0.0,
            hitSource = if (hit != null) HitSource.GYRO_FLOOR else HitSource.NONE,
            up = SensorFloorMath.UP,
            cameraPosition = camera,
            projector = projector,
            hint = hint,
            timestampMs = SystemClock.uptimeMillis(),
        )
    }

    // ---------- Capture ----------

    override fun capture(onResult: (Bitmap?) -> Unit) {
        onResult(runCatching { previewView?.bitmap }.getOrNull())
    }

    // ---------- Intrinsics ----------

    /** Long-side field of view of the back camera, from Camera2 characteristics (default 66°). */
    private fun readVerticalFov(): Double = runCatching {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.first {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        val ch = cm.getCameraCharacteristics(id)
        val focal = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!.first()
        val size = ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
        Math.toDegrees(2 * atan(maxOf(size.width, size.height) / 2.0 / focal))
    }.getOrElse { 66.0 }.coerceIn(40.0, 100.0)

    private companion object {
        const val TAG = "SensorFloorEngine"
        const val ALPHA = 0.25f
    }
}
