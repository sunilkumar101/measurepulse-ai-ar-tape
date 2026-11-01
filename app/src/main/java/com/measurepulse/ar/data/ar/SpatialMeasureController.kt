package com.measurepulse.ar.data.ar

import android.os.SystemClock
import com.measurepulse.ar.data.ar.engine.MeasureEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Dual-engine spatial measurement controller.
 *
 * Owns the [MeasurementSession] and connects it to whichever [MeasureEngine] is attached
 * (ARCore or gyro-floor). Each engine frame's reticle is smoothed and recorded for anti-shake
 * locking; ARCore anchor corrections are folded back into the session.
 */
class SpatialMeasureController(private val scope: CoroutineScope) {

    sealed interface LockResult {
        data class Locked(val point: MeasurePoint) : LockResult
        data object NoSurface : LockResult
        data object NotAllowed : LockResult
    }

    private val _session = MutableStateFlow(MeasurementSession())
    val session: StateFlow<MeasurementSession> = _session.asStateFlow()

    private val _frame = MutableStateFlow<EngineFrame?>(null)
    /** Latest engine frame with a smoothed reticle. */
    val frame: StateFlow<EngineFrame?> = _frame.asStateFlow()

    private var engine: MeasureEngine? = null
    private var engineJob: Job? = null
    private val sampler = StabilizedSampler()
    private val smoother = PointSmoother()
    private val liveJitter = JitterFilter(0.001)
    private var nextId = 1L

    val attachedEngine: MeasureEngine? get() = engine

    fun attach(newEngine: MeasureEngine) {
        if (engine === newEngine) return
        val switching = engine != null && engine?.kind != newEngine.kind
        engineJob?.cancel()
        engine = newEngine
        sampler.clear()
        // Different engines use different world frames: points can't carry over.
        if (switching) _session.value = _session.value.clear()
        engineJob = scope.launch { newEngine.frames.filterNotNull().collect(::onFrame) }
    }

    fun detach(old: MeasureEngine) {
        if (engine !== old) return
        engineJob?.cancel()
        engine = null
        _frame.value = null
    }

    private fun onFrame(f: EngineFrame) {
        val smoothed = smoother.smooth(f.reticle)
        if (smoothed != null) sampler.add(f.timestampMs, smoothed, f.reticleAccuracyM)
        if (f.anchoredPositions.isNotEmpty()) _session.value = _session.value.withAnchoredPositions(f.anchoredPositions)
        _frame.value = f.copy(reticle = smoothed)
    }

    /** Locks the steadiest recent reticle point (anti-shake). */
    fun lockPoint(triggerTimeMs: Long = SystemClock.uptimeMillis()): LockResult {
        val s = _session.value
        if (!s.canAddPoint) return LockResult.NotAllowed
        val f = _frame.value
        if (f == null || f.reticle == null || f.status != TrackingStatus.TRACKING) return LockResult.NoSurface
        val locked = sampler.lock(triggerTimeMs) ?: return LockResult.NoSurface
        val point = MeasurePoint(
            id = nextId++,
            position = locked.point,
            // A shaky hold widens the error estimate.
            accuracyM = maxOf(locked.accuracyM, locked.spreadM / 2.0),
        )
        engine?.anchorPoint(point.id, point.position)
        _session.value = s.add(point)
        liveJitter.reset()
        return LockResult.Locked(point)
    }

    fun undo() {
        val s = _session.value
        if (!s.roomClosed) s.points.lastOrNull()?.let { engine?.removeAnchor(it.id) }
        _session.value = s.undo()
    }

    fun clear() {
        engine?.clearAnchors()
        _session.value = _session.value.clear()
    }

    fun setMode(mode: MeasureMode) {
        if (mode == _session.value.mode) return
        engine?.clearAnchors()
        _session.value = _session.value.withMode(mode)
    }

    fun closeRoom() { _session.value = _session.value.closeRoom() }

    fun setBoxItem(item: BoxItem) { _session.value = _session.value.withBoxItem(item) }

    /** Live segment length, held steady to 1 mm so digits don't flicker. */
    fun filteredLength(raw: Double): Double = liveJitter.filter(raw)
}
