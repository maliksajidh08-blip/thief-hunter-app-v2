package com.example.security

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.AlarmSoundType
import com.example.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Emergency Alarm Engine with 5 Distinct Sounds:
 * 1. Police Siren (Default)
 * 2. Dog Barking
 * 3. Gun Shot
 * 4. Standard Beep
 * 5. Custom Sound
 *
 * Uses native MediaPlayer with synthesized high-decibel WAV audio files
 * backed by robust AudioTrack fallback and synchronized multi-pattern haptics.
 */
class AlarmSirenEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var vibrationJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    var isRinging: Boolean = false
        private set

    companion object {
        private const val TAG = "AlarmSirenEngine"
        private val activeInstances = mutableSetOf<AlarmSirenEngine>()
        private var globalMediaPlayer: MediaPlayer? = null

        /**
         * Global emergency halt: completely silences any ringing siren in the process.
         */
        @Synchronized
        fun haltAllSirens(context: Context) {
            try {
                globalMediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
            } catch (_: Exception) {}
            globalMediaPlayer = null

            activeInstances.toList().forEach { engine ->
                try {
                    engine.stopSiren()
                } catch (_: Exception) {}
            }

            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.cancel()
            } catch (_: Exception) {}
        }
    }

    init {
        synchronized(activeInstances) {
            activeInstances.add(this)
        }
    }

    fun startSiren(vibrationPattern: String = "EMERGENCY_ALARM", soundType: AlarmSoundType? = null) {
        if (isRinging) return
        isRinging = true

        val selectedSound = soundType ?: AppPreferences.getAlarmSoundType(context)
        Log.i(TAG, "Starting siren with sound: ${selectedSound.displayName}")

        // Maximize alarm stream volume
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            }
        } catch (_: Exception) {}

        playMediaSound(selectedSound)
        startVibrationPattern(vibrationPattern)
    }

    fun previewSound(soundType: AlarmSoundType, durationMs: Long = 3500L) {
        stopSiren()
        isRinging = true
        playMediaSound(soundType)
        startVibrationPattern("EMERGENCY_ALARM")
        scope.launch {
            delay(durationMs)
            if (isRinging) {
                stopSiren()
            }
        }
    }

    @Synchronized
    fun stopSiren() {
        isRinging = false
        vibrationJob?.cancel()
        vibrationJob = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

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

    private fun playMediaSound(soundType: AlarmSoundType) {
        scope.launch(Dispatchers.IO) {
            try {
                val wavFile = getOrGenerateWavSound(soundType)
                if (wavFile.exists() && isRinging) {
                    val mp = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                                .build()
                        )
                        setDataSource(wavFile.absolutePath)
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        prepare()
                        start()
                    }
                    synchronized(this@AlarmSirenEngine) {
                        if (isRinging) {
                            mediaPlayer = mp
                            globalMediaPlayer = mp
                        } else {
                            mp.stop()
                            mp.release()
                        }
                    }
                } else if (isRinging) {
                    playAudioTrackFallback(soundType)
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer failed: ${e.message}, falling back to AudioTrack", e)
                if (isRinging) {
                    playAudioTrackFallback(soundType)
                }
            }
        }
    }

    private fun getOrGenerateWavSound(soundType: AlarmSoundType): File {
        val soundDir = File(context.cacheDir, "siren_sounds").apply {
            if (!exists()) mkdirs()
        }
        val file = File(soundDir, "${soundType.id}_v2.wav")
        if (file.exists() && file.length() > 1000) {
            return file
        }

        val sampleRate = 22050
        val durationSeconds = when (soundType) {
            AlarmSoundType.POLICE_SIREN -> 3
            AlarmSoundType.DOG_BARKING -> 3
            AlarmSoundType.GUN_SHOT -> 3
            AlarmSoundType.STANDARD_BEEP -> 2
            AlarmSoundType.CUSTOM_SOUND -> 2
        }
        val totalSamples = sampleRate * durationSeconds
        val pcmData = ShortArray(totalSamples)

        when (soundType) {
            AlarmSoundType.POLICE_SIREN -> {
                // High-low alternating siren wail (800Hz - 1600Hz, 1.5s period)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val cyclePos = (t % 1.5) / 1.5
                    val freq = if (cyclePos < 0.5) {
                        800.0 + (cyclePos * 2.0) * 800.0
                    } else {
                        1600.0 - ((cyclePos - 0.5) * 2.0) * 800.0
                    }
                    val angle = 2.0 * Math.PI * freq / sampleRate
                    phase += angle
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    pcmData[i] = (sin(phase) * 32000.0).toInt().toShort()
                }
            }

            AlarmSoundType.DOG_BARKING -> {
                // Aggressive rhythmic canine bark bursts (low pitch down-sweep + rough noise bursts)
                // Bark pattern: Bark (0.28s), Pause (0.12s), Bark (0.28s), Pause (0.4s)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val cycleTime = t % 1.0 // 1 second cycle
                    val inBark1 = cycleTime < 0.28
                    val inBark2 = cycleTime in 0.40..0.68
                    if (inBark1 || inBark2) {
                        val barkT = if (inBark1) cycleTime else (cycleTime - 0.40)
                        val barkProg = barkT / 0.28
                        // Pitch sweeps down from 480Hz to 160Hz
                        val barkFreq = 480.0 - (barkProg * 320.0)
                        val angle = 2.0 * Math.PI * barkFreq / sampleRate
                        phase += angle
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                        val env = (1.0 - barkProg) * (if (barkProg < 0.1) barkProg * 10.0 else 1.0)
                        val noise = Random.nextDouble(-0.35, 0.35)
                        val tone = sin(phase) * 0.7 + sin(phase * 2.2) * 0.25 + noise
                        pcmData[i] = (tone * env * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                    } else {
                        pcmData[i] = 0
                    }
                }
            }

            AlarmSoundType.GUN_SHOT -> {
                // 3 tactical gunshots per 3-second loop with explosive crack & low boom
                val shotTimes = doubleArrayOf(0.1, 0.9, 1.8)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    var sampleVal = 0.0
                    for (shotStart in shotTimes) {
                        if (t >= shotStart && t < shotStart + 0.6) {
                            val shotT = t - shotStart
                            val decay = exp(-shotT * 12.0)
                            val noise = Random.nextDouble(-1.0, 1.0)
                            val subBoom = sin(2.0 * Math.PI * 90.0 * shotT) * 0.6
                            sampleVal += (noise * 0.7 + subBoom) * decay
                        }
                    }
                    pcmData[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                }
            }

            AlarmSoundType.STANDARD_BEEP -> {
                // High-decibel piercing alarm beep pulses (2650Hz, 120ms on, 80ms off)
                var phase = 0.0
                val angle = 2.0 * Math.PI * 2650.0 / sampleRate
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val cyclePos = t % 0.22
                    if (cyclePos < 0.13) {
                        phase += angle
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                        pcmData[i] = (sin(phase) * 32767.0).toInt().toShort()
                    } else {
                        pcmData[i] = 0
                    }
                }
            }

            AlarmSoundType.CUSTOM_SOUND -> {
                // Tactical oscillating cyber warble (alternating 1250Hz and 2200Hz at 16Hz)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val warbleFreq = if (((t * 16.0).toInt() % 2) == 0) 1250.0 else 2200.0
                    val angle = 2.0 * Math.PI * warbleFreq / sampleRate
                    phase += angle
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    pcmData[i] = (sin(phase) * 32767.0).toInt().toShort()
                }
            }
        }

        writeWavFile(file, pcmData, sampleRate)
        return file
    }

    private fun writeWavFile(file: File, pcm: ShortArray, sampleRate: Int) {
        val totalAudioLen = (pcm.size * 2).toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * channels * 2).toLong()

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen.toInt())
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16) // Subchunk1Size
        buf.putShort(1) // AudioFormat PCM = 1
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate.toInt())
        buf.putShort((channels * 2).toShort())
        buf.putShort(16) // BitsPerSample
        buf.put("data".toByteArray())
        buf.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { out ->
            out.write(header)
            val byteBuf = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) {
                byteBuf.putShort(s)
            }
            out.write(byteBuf.array())
        }
    }

    private fun playAudioTrackFallback(soundType: AlarmSoundType) {
        try {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = (minBuf * 2).coerceAtLeast(4096)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synchronized(this) {
                if (!isRinging) return
                audioTrack = track
            }

            track.play()
            val buffer = ShortArray(bufSize)
            var phase = 0.0

            while (isRinging) {
                for (i in buffer.indices) {
                    val angle = 2.0 * Math.PI * 1400.0 / sampleRate
                    phase += angle
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    buffer[i] = (sin(phase) * 32000.0).toInt().toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
        } catch (_: Exception) {}
    }

    private fun startVibrationPattern(patternType: String) {
        vibrationJob?.cancel()
        vibrationJob = scope.launch {
            val vibrator = getVibrator() ?: return@launch
            val timings = when (patternType) {
                "POCKET_REMOVAL" -> longArrayOf(0, 400, 150, 400, 150, 800)
                "MOTION_DETECTION" -> longArrayOf(0, 250, 100, 250, 100, 250, 400)
                "CHARGER_UNPLUG" -> longArrayOf(0, 500, 200, 500, 200, 500)
                "USB_CONNECTION" -> longArrayOf(0, 150, 80, 150, 80, 300)
                else -> longArrayOf(0, 500, 200, 500, 200, 750, 250)
            }
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)

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
