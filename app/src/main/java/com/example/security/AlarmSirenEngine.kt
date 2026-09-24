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
import com.example.data.AlarmSoundType
import com.example.data.AppPreferences
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-decibel audio synthesizer & alternating frequency siren generator
 * accompanied by distinct emergency vibration patterns.
 * Supports: Police Siren, Dog Barking, Gun Shot, Standard Beep, Custom Sound.
 */
class AlarmSirenEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var sirenJob: Job? = null
    private var vibrationJob: Job? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    var isRinging: Boolean = false
        private set

    fun startSiren(vibrationPattern: String = "EMERGENCY_ALARM", soundType: AlarmSoundType? = null) {
        if (isRinging) return
        isRinging = true

        val selectedSound = soundType ?: AppPreferences.getAlarmSoundType(context)
        startAudioSiren(selectedSound)
        startVibrationPattern(vibrationPattern)
    }

    fun previewSound(soundType: AlarmSoundType, durationMs: Long = 3500L) {
        stopSiren()
        isRinging = true
        startAudioSiren(soundType)
        startVibrationPattern("EMERGENCY_ALARM")
        scope.launch {
            delay(durationMs)
            if (isRinging) {
                stopSiren()
            }
        }
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

    private fun startAudioSiren(soundType: AlarmSoundType) {
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
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let { am ->
                    val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                }

                track.play()
                val buffer = ShortArray(bufferSize)
                var phase = 0.0
                var sampleIndex = 0L

                var currentFreq = 850.0
                var targetFreq = 1650.0
                var sweepUp = true

                while (isActive && isRinging) {
                    when (soundType) {
                        AlarmSoundType.POLICE_SIREN -> {
                            for (i in buffer.indices) {
                                val angle = 2.0 * Math.PI * currentFreq / sampleRate
                                buffer[i] = (sin(phase) * 32767.0).toInt().toShort()
                                phase += angle
                                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                                if (sweepUp) {
                                    currentFreq += 0.04
                                    if (currentFreq >= targetFreq) sweepUp = false
                                } else {
                                    currentFreq -= 0.04
                                    if (currentFreq <= 750.0) sweepUp = true
                                }
                            }
                        }

                        AlarmSoundType.DOG_BARKING -> {
                            // Synthesize rhythmic canine bark bursts (low pitch down-sweep + rough noise harmonic)
                            for (i in buffer.indices) {
                                val timeInCycle = (sampleIndex % (sampleRate * 0.75).toLong()).toDouble() / sampleRate
                                val sample = if (timeInCycle < 0.28) {
                                    // Active bark: descending pitch from 480Hz down to 180Hz
                                    val barkFreq = 480.0 - (timeInCycle / 0.28) * 300.0
                                    val angle = 2.0 * Math.PI * barkFreq / sampleRate
                                    phase += angle
                                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                                    
                                    val env = (1.0 - (timeInCycle / 0.28))
                                    val tone = sin(phase) * 0.65 + sin(phase * 2.3) * 0.25 + (Random.nextDouble(-0.1, 0.1))
                                    (tone * env * 32767.0).toInt().coerceIn(-32767, 32767)
                                } else {
                                    0
                                }
                                buffer[i] = sample.toShort()
                                sampleIndex++
                            }
                        }

                        AlarmSoundType.GUN_SHOT -> {
                            // Synthesize loud tactical gunshot: transient impulse followed by low explosive rumble decay
                            for (i in buffer.indices) {
                                val timeInCycle = (sampleIndex % (sampleRate * 0.9).toLong()).toDouble() / sampleRate
                                val sample = if (timeInCycle < 0.5) {
                                    val decay = exp(-timeInCycle * 9.0)
                                    val blastNoise = Random.nextDouble(-1.0, 1.0)
                                    val lowBoomAngle = 2.0 * Math.PI * 95.0 * timeInCycle
                                    val lowBoom = sin(lowBoomAngle) * 0.5
                                    ((blastNoise * 0.6 + lowBoom) * decay * 32767.0).toInt().coerceIn(-32767, 32767)
                                } else {
                                    0
                                }
                                buffer[i] = sample.toShort()
                                sampleIndex++
                            }
                        }

                        AlarmSoundType.STANDARD_BEEP -> {
                            // Piercing standard alert beep pulses (2600Hz, 120ms ON, 70ms OFF)
                            for (i in buffer.indices) {
                                val timeInCycle = (sampleIndex % (sampleRate * 0.25).toLong()).toDouble() / sampleRate
                                val sample = if (timeInCycle < 0.15) {
                                    val angle = 2.0 * Math.PI * 2700.0 / sampleRate
                                    phase += angle
                                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                                    (sin(phase) * 32767.0).toInt()
                                } else {
                                    0
                                }
                                buffer[i] = sample.toShort()
                                sampleIndex++
                            }
                        }

                        AlarmSoundType.CUSTOM_SOUND -> {
                            // Rapid high-frequency tactical warble alternating between 1400Hz and 2400Hz at 20Hz
                            for (i in buffer.indices) {
                                val t = sampleIndex.toDouble() / sampleRate
                                val warbleRate = 18.0 // 18 times per sec
                                val warbleFreq = if ((t * warbleRate).toInt() % 2 == 0) 1350.0 else 2300.0
                                val angle = 2.0 * Math.PI * warbleFreq / sampleRate
                                phase += angle
                                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                                buffer[i] = (sin(phase) * 32767.0).toInt().toShort()
                                sampleIndex++
                            }
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
