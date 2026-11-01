package com.measurepulse.ar.data.ar

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Hands-free "Mark" trigger. Two strategies, both offline-capable:
 *
 * 1. **Keyword** — Android's *on-device* speech recognizer (Android 12+) listening for "mark".
 *    Cloud recognizers are never used, so no audio leaves the phone (the privacy policy relies on this).
 * 2. **Sound burst** — if no on-device recognizer exists or it keeps failing, a short loud sound
 *    (saying "Mark!", a click or a clap) over the room's noise floor triggers instead.
 *
 * Requires RECORD_AUDIO (the caller requests it when the user turns the mic on).
 * [level] (0–1) drives the orb waveform so users can see it is listening.
 */
class VoiceMarkTrigger(private val context: Context, private val onMark: () -> Unit) {

    enum class Strategy { OFF, KEYWORD, SOUND_BURST }

    private val _strategy = MutableStateFlow(Strategy.OFF)
    val strategy: StateFlow<Strategy> = _strategy.asStateFlow()
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var consecutiveErrors = 0
    private var lastTrigger = 0L
    @Volatile private var burstThread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        consecutiveErrors = 0
        if (onDeviceRecognitionAvailable()) startKeyword() else startSoundBurst()
    }

    fun stop() {
        running = false
        main.removeCallbacksAndMessages(null)
        recognizer?.run { runCatching { cancel(); destroy() } }
        recognizer = null
        burstThread = null
        _strategy.value = Strategy.OFF
        _level.value = 0f
    }

    private fun fire() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTrigger < 900) return
        lastTrigger = now
        main.post { if (running) onMark() }
    }

    // ---------- Strategy 1: keyword ----------

    private fun startKeyword() {
        _strategy.value = Strategy.KEYWORD
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return startSoundBurst()
        val r = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }.getOrNull()
            ?: return startSoundBurst()
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { consecutiveErrors = 0 }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) { _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) {
                consecutiveErrors++
                if (!running) return
                // No recognizer language/network etc. → switch to the sound-burst trigger.
                if (consecutiveErrors >= 4 || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                ) {
                    recognizer?.destroy(); recognizer = null
                    startSoundBurst()
                } else {
                    main.postDelayed({ listen() }, 250)
                }
            }
            override fun onResults(results: Bundle?) {
                check(results)
                if (running) main.postDelayed({ listen() }, 120)
            }
            override fun onPartialResults(partialResults: Bundle?) = check(partialResults)
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        listen()
    }

    private fun onDeviceRecognitionAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

    private fun listen() {
        val r = recognizer ?: return
        if (!running) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { r.startListening(intent) }.onFailure { startSoundBurst() }
    }

    private fun check(bundle: Bundle?) {
        val words = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        if (words.any { phrase -> KEYWORDS.any { phrase.lowercase().contains(it) } }) fire()
    }

    // ---------- Strategy 2: sound burst ----------

    @SuppressLint("MissingPermission") // caller only starts this after RECORD_AUDIO is granted
    private fun startSoundBurst() {
        _strategy.value = Strategy.SOUND_BURST
        val thread = Thread {
            val rate = 16_000
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf <= 0) return@Thread
            val rec = runCatching {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
            }.getOrNull() ?: return@Thread
            if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return@Thread }
            val buf = ShortArray(rate / 50) // 20 ms frames
            var noise = 300.0
            var loudFrames = 0
            rec.startRecording()
            try {
                while (running && burstThread === Thread.currentThread()) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    var sum = 0.0
                    for (i in 0 until n) sum += buf[i].toDouble() * buf[i]
                    val rms = sqrt(sum / n)
                    _level.value = (rms / 6000.0).toFloat().coerceIn(0f, 1f)
                    val loud = rms > maxOf(noise * 4.0, 1200.0)
                    if (loud) loudFrames++ else {
                        // A word-length burst (80–700 ms) that has just ended = trigger.
                        if (loudFrames in 4..35) fire()
                        loudFrames = 0
                        noise = noise * 0.97 + rms * 0.03
                    }
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
        }
        burstThread = thread
        thread.start()
    }

    private companion object {
        val KEYWORDS = listOf("mark", "marc", "mart", "marque", "drop", "pin")
    }
}
