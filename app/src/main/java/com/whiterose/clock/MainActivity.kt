package com.chiara.beep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chiara.beep.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsHelper
    private val handler = Handler(Looper.getMainLooper())
    private val days   = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
    private val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
                                  "JUL","AUG","SEP","OCT","NOV","DEC")
    private var colonVisible = true

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 100)
        }
    }

    private val colonRunnable = object : Runnable {
        override fun run() {
            if (prefs.blinkColon) {
                colonVisible = !colonVisible
                binding.tvColon1.visibility = if (colonVisible) View.VISIBLE else View.INVISIBLE
                binding.tvColon2.visibility = if (colonVisible) View.VISIBLE else View.INVISIBLE
            } else {
                binding.tvColon1.visibility = View.VISIBLE
                binding.tvColon2.visibility = View.VISIBLE
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        prefs = PrefsHelper(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        requestPermissions()
        applyColor()
        updateServiceButtons()

        binding.btnStart.setOnClickListener { startBeepService() }
        binding.btnStop.setOnClickListener  { stopBeepService() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnReminder.setOnClickListener {
            startActivity(Intent(this, ReminderActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        applyColor()
        updateServiceButtons()
        handler.post(tickRunnable)
        handler.post(colonRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(colonRunnable)
    }

    private fun applyColor() {
        val hex    = prefs.getColorHex()
        val dimHex = prefs.getColorDimHex()
        val c      = Color.parseColor(hex)
        val cd     = Color.parseColor(dimHex)

        binding.tvHour.setTextColor(c)
        binding.tvColon1.setTextColor(c)
        binding.tvMinute.setTextColor(c)
        binding.tvColon2.setTextColor(c)
        binding.tvSecond.setTextColor(cd)
        binding.tvDate.setTextColor(cd)
        binding.tvStatus.setTextColor(cd)
        binding.progressSeconds.progressTintList =
            android.content.res.ColorStateList.valueOf(c)
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val h   = now.get(Calendar.HOUR_OF_DAY)
        val m   = now.get(Calendar.MINUTE)
        val s   = now.get(Calendar.SECOND)
        val d   = now.get(Calendar.DAY_OF_WEEK) - 1
        val mo  = now.get(Calendar.MONTH)
        val dt  = now.get(Calendar.DAY_OF_MONTH)
        val yr  = now.get(Calendar.YEAR)

        binding.tvHour.text   = pad(h)
        binding.tvMinute.text = pad(m)
        binding.tvSecond.text = pad(s)
        binding.progressSeconds.progress = (s * 100 / 59)

        if (prefs.showDate) {
            binding.tvDate.visibility = View.VISIBLE
            binding.tvDate.text = "${days[d]} ${pad(dt)} ${months[mo]} $yr"
        } else {
            binding.tvDate.visibility = View.GONE
        }
    }

    private fun startBeepService() {
        val intent = Intent(this, BeepService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        prefs.serviceOn = true
        updateServiceButtons()
        requestBatteryOptimization()
    }

    private fun stopBeepService() {
        stopService(Intent(this, BeepService::class.java))
        prefs.serviceOn = false
        updateServiceButtons()
    }

    private fun updateServiceButtons() {
        val running = BeepService.isRunning
        binding.btnStart.isEnabled = !running
        binding.btnStop.isEnabled  = running
        binding.tvStatus.text = if (running) "● BEEP SERVICE: ON" else "○ BEEP SERVICE: OFF"
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this,
                        "Buka Settings → Battery → Chiara's Beep → No restrictions",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BeepService.CHANNEL_ID,
                "Chiara's Beep Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps beep running in background"
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')
}
