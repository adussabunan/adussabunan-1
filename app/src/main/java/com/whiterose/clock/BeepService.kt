package com.chiara.beep

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.math.sin

class BeepService : Service() {

    companion object {
        const val CHANNEL_ID      = "chiara_beep_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastBeepMinute = -1
    private lateinit var prefs: PrefsHelper
    private var wakeLock: PowerManager.WakeLock? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            val cal = Calendar.getInstance()
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentSecond = cal.get(Calendar.SECOND)

            if (currentSecond == 0 && currentMinute != lastBeepMinute) {
                lastBeepMinute = currentMinute
                if (shouldBeep(cal)) {
                    Thread { playBeeps() }.start()
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        prefs = PrefsHelper(this)

        // WakeLock agar service tidak dimatikan saat layar mati
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ChiaraBeep::BeepWakeLock"
        ).also { it.acquire(10 * 60 * 60 * 1000L) } // max 10 jam
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service jika di-swipe dari recent apps
        val restartIntent = Intent(applicationContext, BeepService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        wakeLock?.release()
        isRunning = false
        prefs.serviceOn = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun shouldBeep(cal: Calendar): Boolean {
        // Cek DND
        if (prefs.dndRespect) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.isNotificationPolicyAccessGranted) {
                // Tidak punya akses policy, skip cek DND
            } else {
                val filter = nm.currentInterruptionFilter
                if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    filter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
                    return false
                }
            }
        }

        // Cek silent hours
        if (prefs.silentOn) {
            val nowH = cal.get(Calendar.HOUR_OF_DAY)
            val nowM = cal.get(Calendar.MINUTE)
            val nowTotal = nowH * 60 + nowM

            val (startH, startM) = prefs.silentStart.split(":").map { it.toInt() }
            val (endH, endM)     = prefs.silentEnd.split(":").map { it.toInt() }
            val startTotal = startH * 60 + startM
            val endTotal   = endH * 60 + endM

            val inSilent = if (startTotal <= endTotal) {
                nowTotal in startTotal until endTotal
            } else {
                nowTotal >= startTotal || nowTotal < endTotal
            }
            if (inSilent) return false
        }

        return true
    }

    private fun playBeeps() {
        val count    = prefs.beepCount
        val freq     = prefs.beepFreq.toDouble()
        val duration = prefs.beepDuration

        repeat(count) { i ->
            if (i > 0) Thread.sleep(90)
            playTone(freq, duration, 0.7f)
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val samples    = ShortArray(numSamples)
        val fadeLen    = minOf(numSamples / 4, sampleRate / 100)

        for (i in 0 until numSamples) {
            var env = 1.0
            if (i < fadeLen) env = i.toDouble() / fadeLen
            else if (i > numSamples - fadeLen) env = (numSamples - i).toDouble() / fadeLen
            val angle = 2.0 * Math.PI * i * freqHz / sampleRate
            samples[i] = (sin(angle) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
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
            Thread.sleep(durationMs.toLong() + 30)
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
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, BeepService::class.java).apply {
                action = "STOP"
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chiara's Beep")
            .setContentText("Beeping every minute")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
