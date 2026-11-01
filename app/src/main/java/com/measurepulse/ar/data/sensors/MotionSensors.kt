package com.measurepulse.ar.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.measurepulse.ar.domain.level.LevelMath
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.asin
import kotlin.math.atan

/** Camera orientation as elevation of the optical axis and screen roll, degrees. */
data class Orientation(val pitchDeg: Double, val rollDeg: Double, val rotation: FloatArray)

object MotionSensors {

    /**
     * Fused device orientation: GAME_ROTATION_VECTOR (gyro + accel, no compass drift) →
     * ROTATION_VECTOR → accelerometer + magnetometer. Quaternion is low-pass smoothed.
     */
    fun orientation(context: Context, alpha: Float = 0.2f): Flow<Orientation> = callbackFlow {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rot = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) ?: sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val q = FloatArray(4); var hasQ = false
        val grav = FloatArray(3); val geo = FloatArray(3); var hasG = false; var hasM = false
        val r = FloatArray(9)

        fun emit() {
            // Optical axis = device −Z; its world Z component is sin(elevation).
            val fz = -r[8]
            val pitch = Math.toDegrees(asin(fz.toDouble().coerceIn(-1.0, 1.0)))
            // Screen-right axis (device +X) tipping out of the horizontal plane = roll.
            val roll = Math.toDegrees(asin(r[6].toDouble().coerceIn(-1.0, 1.0)))
            trySend(Orientation(pitch, roll, r.copyOf()))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                        val n = minOf(4, e.values.size)
                        var dot = 0f
                        for (i in 0 until n) dot += q[i] * e.values[i]
                        val s = if (dot < 0) -1f else 1f
                        for (i in 0 until n) q[i] = if (!hasQ) e.values[i] else q[i] + alpha * (s * e.values[i] - q[i])
                        hasQ = true
                        SensorManager.getRotationMatrixFromVector(r, q.copyOf(n))
                        emit()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        LevelMath.lowPass(grav, e.values, hasG, alpha); hasG = true
                        if (hasM && SensorManager.getRotationMatrix(r, null, grav, geo)) emit()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> { LevelMath.lowPass(geo, e.values, hasM, alpha); hasM = true }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (rot != null) sm.registerListener(listener, rot, SensorManager.SENSOR_DELAY_GAME)
        else { acc?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }; mag?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) } }
        awaitClose { sm.unregisterListener(listener) }
    }.conflate()

    /**
     * Gravity vector for the spirit level, low-pass filtered with α = 0.12.
     * Uses the fused TYPE_GRAVITY sensor when present (accelerometer + gyro), else the raw accelerometer.
     */
    fun gravity(context: Context, alpha: Float = LevelMath.ALPHA): Flow<FloatArray> = callbackFlow {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val g = FloatArray(3); var init = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                LevelMath.lowPass(g, e.values, init, alpha); init = true
                trySend(g.copyOf())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm.unregisterListener(listener) }
    }.conflate()

    fun hasGravitySensor(context: Context): Boolean =
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_GRAVITY) != null
}

/** Back-camera intrinsics from Camera2. */
object CameraIntrinsics {
    data class Info(val vfovDeg: Double, val focalMm: Float, val maxZoom: Float)

    fun read(context: Context): Info = runCatching {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.first {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        val ch = cm.getCameraCharacteristics(id)
        val f = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!!.first()
        val size = ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
        val maxZoom = ch.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 4f
        Info(Math.toDegrees(2 * atan(maxOf(size.width, size.height) / 2.0 / f)).coerceIn(40.0, 100.0), f, maxZoom)
    }.getOrElse { Info(66.0, 4.7f, 4f) }
}
