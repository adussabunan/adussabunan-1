package com.whiterose.clock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.whiterose.clock.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val days   = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
    private val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
                                  "JUL","AUG","SEP","OCT","NOV","DEC")

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while app is open
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Full black immersive for AMOLED
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        requestNotificationPermission()

        // Start background beep service
        binding.btnService.setOnClickListener {
            startBeepService()
        }

        // Stop service
        binding.btnStop.setOnClickListener {
            stopService(Intent(this, BeepService::class.java))
            binding.btnService.isEnabled = true
            binding.btnStop.isEnabled = false
            binding.tvStatus.text = "BEEP SERVICE: OFF"
        }

        // Check if service already running
        updateServiceButtons()
    }

    override fun onResume() {
        super.onResume()
        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val h = now.get(Calendar.HOUR_OF_DAY)
        val m = now.get(Calendar.MINUTE)
        val s = now.get(Calendar.SECOND)
        val d = now.get(Calendar.DAY_OF_WEEK) - 1
        val mo = now.get(Calendar.MONTH)
        val dt = now.get(Calendar.DAY_OF_MONTH)
        val yr = now.get(Calendar.YEAR)

        binding.tvHour.text   = pad(h)
        binding.tvMinute.text = pad(m)
        binding.tvSecond.text = pad(s)

        val pct = s / 59f * 100
        binding.progressSeconds.progress = pct.toInt()

        binding.tvDate.text = "${days[d]} ${pad(dt)} ${months[mo]} $yr"
    }

    private fun startBeepService() {
        val intent = Intent(this, BeepService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.btnService.isEnabled = false
        binding.btnStop.isEnabled = true
        binding.tvStatus.text = "BEEP SERVICE: ON"
    }

    private fun updateServiceButtons() {
        val running = BeepService.isRunning
        binding.btnService.isEnabled = !running
        binding.btnStop.isEnabled = running
        binding.tvStatus.text = if (running) "BEEP SERVICE: ON" else "BEEP SERVICE: OFF"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BeepService.CHANNEL_ID,
                "Whiterose Clock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps beep service running in background"
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')
}
