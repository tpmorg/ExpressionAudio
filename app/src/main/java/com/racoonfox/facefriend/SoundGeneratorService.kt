package com.racoonfox.facefriend

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.IBinder

class SoundGeneratorService : Service() {
    private lateinit var audioTrack: AudioTrack
    private val sampleRate = 44100
    private val durationMs = 500

    override fun onCreate() {
        super.onCreate()
        audioTrack = createAudioTrack()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val frequency = intent?.getDoubleExtra(EXTRA_FREQUENCY, 440.0) ?: 440.0
        generateAndPlayTone(frequency)
        return START_NOT_STICKY
    }

    private fun createAudioTrack(): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun generateSineWave(frequency: Double): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val samples = ShortArray(numSamples)
        val angleIncrement = (2 * Math.PI * frequency / sampleRate).toFloat()
        var currentAngle = 0f

        for (i in 0 until numSamples) {
            val sample = (Math.sin(currentAngle.toDouble()) * Short.MAX_VALUE).toInt().toShort()
            samples[i] = sample
            currentAngle += angleIncrement
        }

        return samples
    }

    private fun generateAndPlayTone(frequency: Double) {
        val samples = generateSineWave(frequency)
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioTrack.release()
    }

    companion object {
        const val EXTRA_FREQUENCY = "extra_frequency"
    }
}