package com.measurepulse.ar.data.ar.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix as AndroidMatrix
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.measurepulse.ar.data.ar.EngineFrame
import com.measurepulse.ar.data.ar.EngineKind
import com.measurepulse.ar.data.ar.HitSource
import com.measurepulse.ar.data.ar.ScreenPoint
import com.measurepulse.ar.data.ar.ScreenProjector
import com.measurepulse.ar.data.ar.SpatialMath
import com.measurepulse.ar.data.ar.TrackingStatus
import com.measurepulse.ar.data.ar.Vec3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Engine A — ARCore plane & depth hit-testing.
 *
 * Every frame the centre of the view is hit-tested; hits are ranked Depth > Plane (inside its polygon)
 * > Feature point with a surface normal. When nothing is hit (blank walls, low texture) the centre ray
 * is intersected with an estimated floor (the lowest tracked upward plane, or camera height below the
 * camera) so measuring never stalls — with a wider accuracy estimate.
 *
 * Locked points become ARCore Anchors; their drift-corrected poses are published every frame.
 * ARCore world frame: Y up, metres.
 *
 * Throws from [create] when ARCore cannot start, so the caller can fall back to Engine B.
 */
class ArCoreMeasureEngine private constructor(
    private val context: Context,
    private val session: Session,
    @Volatile var fallbackHeightM: Double,
) : MeasureEngine, GLSurfaceView.Renderer {

    override val kind = EngineKind.ARCORE

    private val _frames = MutableStateFlow<EngineFrame?>(null)
    override val frames: StateFlow<EngineFrame?> = _frames.asStateFlow()

    private val background = ArBackgroundRenderer()
    private val pendingOps = ConcurrentLinkedQueue<(Session) -> Unit>()
    private val anchors = ConcurrentHashMap<Long, Anchor>()
    private var viewWidth = 0
    private var viewHeight = 0
    private var displayRotation = Surface.ROTATION_0
    @Volatile private var captureRequest: ((Bitmap?) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var resumed = false

    /** Creates the GL view that renders the camera and drives this engine. */
    fun createSurfaceView(): GLSurfaceView = GLSurfaceView(context).apply {
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(this@ArCoreMeasureEngine)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        setWillNotDraw(false)
    }

    fun resume(view: GLSurfaceView): Boolean = runCatching {
        session.resume()
        view.onResume()
        resumed = true
    }.onFailure { Log.e(TAG, "ARCore resume failed", it) }.isSuccess

    fun pause(view: GLSurfaceView) {
        resumed = false
        view.onPause()
        runCatching { session.pause() }
    }

    fun close() {
        resumed = false
        runCatching { session.close() }
    }

    // ---------- Anchors ----------

    override fun anchorPoint(id: Long, position: Vec3) {
        pendingOps += { s ->
            runCatching { s.createAnchor(Pose.makeTranslation(position.x.toFloat(), position.y.toFloat(), position.z.toFloat())) }
                .onSuccess { anchors[id] = it }
        }
    }

    override fun removeAnchor(id: Long) {
        anchors.remove(id)?.let { a -> pendingOps += { a.detach() } }
    }

    override fun clearAnchors() {
        val all = anchors.values.toList()
        anchors.clear()
        pendingOps += { all.forEach { it.detach() } }
    }

    override fun capture(onResult: (Bitmap?) -> Unit) {
        captureRequest = onResult
    }

    // ---------- GL ----------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.07f, 0.07f, 0.14f, 1f)
        background.createOnGlThread()
        session.setCameraTextureName(background.textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewWidth = width
        viewHeight = height
        @Suppress("DEPRECATION")
        displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        session.setDisplayGeometry(displayRotation, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (!resumed) return
        while (true) {
            val op = pendingOps.poll() ?: break
            runCatching { op(session) }
        }
        val frame = runCatching { session.update() }.getOrElse {
            Log.w(TAG, "session.update failed", it); return
        }
        background.draw(frame)
        publish(frame)
        captureRequest?.let { cb ->
            captureRequest = null
            val bmp = readPixels()
            mainHandler.post { cb(bmp) }
        }
    }

    private fun publish(frame: Frame) {
        val camera = frame.camera
        val w = viewWidth
        val h = viewHeight
        if (w == 0 || h == 0) return

        val view = FloatArray(16)
        val proj = FloatArray(16)
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, 0.05f, 100f)
        val viewProj = FloatArray(16).also { Matrix.multiplyMM(it, 0, proj, 0, view, 0) }
        val projector = ScreenProjector { p -> projectPoint(viewProj, p, w, h) }
        val camPose = camera.pose
        val camPos = Vec3(camPose.tx().toDouble(), camPose.ty().toDouble(), camPose.tz().toDouble())

        val anchored = anchors.mapNotNull { (id, a) ->
            if (a.trackingState == TrackingState.TRACKING) id to a.pose.toVec3() else null
        }.toMap()

        if (camera.trackingState != TrackingState.TRACKING) {
            _frames.value = EngineFrame(
                kind, if (camera.trackingState == TrackingState.PAUSED) TrackingStatus.SEARCHING else TrackingStatus.PAUSED,
                reticle = null, reticleAccuracyM = 0.0, hitSource = HitSource.NONE, up = UP,
                cameraPosition = camPos, projector = projector, anchoredPositions = anchored,
                hint = "Move the phone slowly to scan the room", timestampMs = SystemClock.uptimeMillis(),
            )
            return
        }

        val best = bestHit(frame.hitTest(w / 2f, h / 2f))
        val reticle: Vec3?
        val source: HitSource
        if (best != null) {
            reticle = best.first.hitPose.toVec3()
            source = best.second
        } else {
            reticle = estimatedFloorHit(camera, camPos)
            source = if (reticle != null) HitSource.ESTIMATED_FLOOR else HitSource.NONE
        }
        val dist = reticle?.distanceTo(camPos) ?: 0.0
        val hasPlanes = session.getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }

        _frames.value = EngineFrame(
            kind = kind,
            status = TrackingStatus.TRACKING,
            reticle = reticle,
            reticleAccuracyM = accuracyFor(source, dist),
            hitSource = source,
            up = UP,
            cameraPosition = camPos,
            projector = projector,
            anchoredPositions = anchored,
            hint = when {
                !hasPlanes -> "Move slowly to detect surfaces"
                source == HitSource.ESTIMATED_FLOOR -> "Low texture — using estimated floor"
                reticle == null -> "Aim at a surface"
                else -> null
            },
            timestampMs = SystemClock.uptimeMillis(),
        )
    }

    private fun bestHit(hits: List<HitResult>): Pair<HitResult, HitSource>? {
        var best: Pair<HitResult, HitSource>? = null
        var bestRank = -1
        for (hit in hits) {
            val (rank, source) = when (val t = hit.trackable) {
                is DepthPoint -> if (t.trackingState == TrackingState.TRACKING) 3 to HitSource.DEPTH else continue
                is Plane -> if (t.trackingState == TrackingState.TRACKING && t.isPoseInPolygon(hit.hitPose)) 2 to HitSource.PLANE else continue
                is Point -> if (t.trackingState == TrackingState.TRACKING && t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL) 1 to HitSource.FEATURE else continue
                else -> continue
            }
            if (rank > bestRank) { best = hit to source; bestRank = rank }
        }
        return best
    }

    /** Centre ray vs. the lowest upward-facing plane (or camera height below the camera). */
    private fun estimatedFloorHit(camera: Camera, camPos: Vec3): Vec3? {
        val floorY = session.getAllTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING && it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            .minOfOrNull { it.centerPose.ty().toDouble() }
            ?: (camPos.y - fallbackHeightM)
        val z = camera.pose.zAxis
        val forward = Vec3(-z[0].toDouble(), -z[1].toDouble(), -z[2].toDouble())
        val hit = SpatialMath.rayPlane(camPos, forward, Vec3(0.0, floorY, 0.0), UP) ?: return null
        return if (hit.distanceTo(camPos) > 15.0) null else hit
    }

    private fun accuracyFor(source: HitSource, distM: Double): Double = when (source) {
        HitSource.DEPTH -> 0.005 + 0.010 * distM
        HitSource.PLANE -> 0.004 + 0.006 * distM
        HitSource.FEATURE -> 0.010 + 0.015 * distM
        HitSource.ESTIMATED_FLOOR -> 0.020 + 0.025 * distM
        else -> 0.0
    }

    private fun readPixels(): Bitmap? = runCatching {
        val w = viewWidth; val h = viewHeight
        val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        buf.rewind()
        bmp.copyPixelsFromBuffer(buf)
        // GL's origin is bottom-left: flip vertically.
        val flip = AndroidMatrix().apply { postScale(1f, -1f, w / 2f, h / 2f) }
        Bitmap.createBitmap(bmp, 0, 0, w, h, flip, false)
    }.getOrNull()

    companion object {
        private const val TAG = "ArCoreEngine"
        val UP = Vec3(0.0, 1.0, 0.0)

        /** Starts an ARCore session. Throws if ARCore is missing, outdated or the camera is unavailable. */
        fun create(context: Context, fallbackHeightM: Double): ArCoreMeasureEngine {
            val session = Session(context)
            val config = Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) depthMode = Config.DepthMode.AUTOMATIC
            }
            session.configure(config)
            return ArCoreMeasureEngine(context, session, fallbackHeightM)
        }

        private fun Pose.toVec3() = Vec3(tx().toDouble(), ty().toDouble(), tz().toDouble())

        private fun projectPoint(viewProj: FloatArray, p: Vec3, w: Int, h: Int): ScreenPoint? {
            val v = floatArrayOf(p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), 1f)
            val out = FloatArray(4)
            Matrix.multiplyMV(out, 0, viewProj, 0, v, 0)
            if (out[3] <= 0.01f) return null
            val nx = out[0] / out[3]
            val ny = out[1] / out[3]
            return ScreenPoint((nx + 1f) / 2f * w, (1f - ny) / 2f * h)
        }
    }
}
