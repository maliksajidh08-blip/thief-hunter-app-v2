package com.example.security

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * High-decibel audio synthesizer & alternating frequency siren generator
 * accompanied by distinct emergency vibration patterns.
 */
class AlarmSirenEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var sirenJob: Job? = null
    private var vibrationJob: Job? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    var isRinging: Boolean = false
        private set

    fun startSiren(vibrationPattern: String = "EMERGENCY_ALARM") {
        if (isRinging) return
        isRinging = true

        startAudioSiren()
        startVibrationPattern(vibrationPattern)
    }

    fun stopSiren() {
        isRinging = false
        sirenJob?.cancel()
        sirenJob = null
        vibrationJob?.cancel()
        vibrationJob = null

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null

        stopVibrations()
    }

    private fun startAudioSiren() {
        sirenJob = scope.launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            try {
                // Ensure audio is loud
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let { am ->
                    val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                }

                track.play()
                val buffer = ShortArray(bufferSize)
                var phase = 0.0

                var currentFreq = 850.0
                var targetFreq = 1650.0
                var sweepUp = true

                while (isActive && isRinging) {
                    for (i in buffer.indices) {
                        val angle = 2.0 * Math.PI * currentFreq / sampleRate
                        buffer[i] = (sin(phase) * 32767.0).toInt().toShort()
                        phase += angle
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }

                        // Sweep frequency for emergency siren effect
                        if (sweepUp) {
                            currentFreq += 0.04
                            if (currentFreq >= targetFreq) sweepUp = false
                        } else {
                            currentFreq -= 0.04
                            if (currentFreq <= 750.0) sweepUp = true
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun startVibrationPattern(patternType: String) {
        vibrationJob = scope.launch {
            val vibrator = getVibrator() ?: return@launch
            val timings = when (patternType) {
                "POCKET_REMOVAL" -> longArrayOf(0, 400, 150, 400, 150, 800)
                "MOTION_DETECTION" -> longArrayOf(0, 250, 100, 250, 100, 250, 400)
                "CHARGER_UNPLUG" -> longArrayOf(0, 500, 200, 500, 200, 500)
                "USB_CONNECTION" -> longArrayOf(0, 150, 80, 150, 80, 300)
                else -> longArrayOf(0, 500, 200, 500, 200, 750, 250) // High urgency
            }
            val amplitudes = when (patternType) {
                "POCKET_REMOVAL" -> intArrayOf(0, 255, 0, 255, 0, 255)
                else -> intArrayOf(0, 255, 0, 255, 0, 255, 0)
            }

            while (isActive && isRinging) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(timings, amplitudes.take(timings.size).toIntArray(), -1)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(timings, -1)
                    }
                } catch (_: Exception) {}
                delay(1200)
            }
        }
    }

    private fun stopVibrations() {
        try {
            getVibrator()?.cancel()
        } catch (_: Exception) {}
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
