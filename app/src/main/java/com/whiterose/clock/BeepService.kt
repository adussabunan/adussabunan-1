package com.whiterose.clock

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlin.math.sin

class BeepService : Service() {

    companion object {
        const val CHANNEL_ID = "whiterose_clock_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBeepMinute = -1

    // Check every 500ms for minute change
    private val checkRunnable = object : Runnable {
        override fun run() {
            val cal = java.util.Calendar.getInstance()
            val currentMinute = cal.get(java.util.Calendar.MINUTE)
            val currentSecond = cal.get(java.util.Calendar.SECOND)

            if (currentSecond == 0 && currentMinute != lastBeepMinute) {
                lastBeepMinute = currentMinute
                Thread { playDoubleBeep() }.start()
            }

            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(checkRunnable)
        return START_STICKY // Restart if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Generate and play double beep using AudioTrack (no file needed)
    private fun playDoubleBeep() {
        playTone(1800.0, 60, 0.6f)
        Thread.sleep(90)
        playTone(1800.0, 60, 0.6f)
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val samples = ShortArray(numSamples)

        // Sine wave with fade in/out to avoid click
        val fadeLen = minOf(numSamples / 4, sampleRate / 100) // 10ms fade
        for (i in 0 until numSamples) {
            var env = 1.0
            if (i < fadeLen) env = i.toDouble() / fadeLen
            else if (i > numSamples - fadeLen) env = (numSamples - i).toDouble() / fadeLen

            val angle = 2.0 * Math.PI * i * freqHz / sampleRate
            samples[i] = (sin(angle) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            samples.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        try {
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Whiterose Clock")
            .setContentText("Beeping every minute — running in background")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
