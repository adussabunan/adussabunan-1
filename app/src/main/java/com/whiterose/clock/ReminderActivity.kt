package com.chiara.beep

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chiara.beep.databinding.ActivityReminderBinding
import java.util.Calendar

class ReminderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderBinding
    private lateinit var prefs: PrefsHelper
    private val reminders = mutableListOf<String>() // format "HH:MM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        prefs = PrefsHelper(this)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadReminders()

        binding.btnAddReminder.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val time = "${pad(h)}:${pad(m)}"
                if (!reminders.contains(time)) {
                    reminders.add(time)
                    reminders.sort()
                    saveAndSchedule()
                    updateList()
                } else {
                    Toast.makeText(this, "Waktu ini sudah ada", Toast.LENGTH_SHORT).show()
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        binding.btnClearAll.setOnClickListener {
            reminders.forEach { cancelAlarm(it) }
            reminders.clear()
            saveAndSchedule()
            updateList()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadReminders() {
        val sp = getSharedPreferences("chiara_reminders", Context.MODE_PRIVATE)
        val raw = sp.getString("list", "") ?: ""
        reminders.clear()
        if (raw.isNotEmpty()) {
            reminders.addAll(raw.split(",").filter { it.isNotEmpty() })
        }
        updateList()
    }

    private fun saveAndSchedule() {
        val sp = getSharedPreferences("chiara_reminders", Context.MODE_PRIVATE)
        sp.edit().putString("list", reminders.joinToString(",")).apply()
        scheduleAll()
    }

    private fun scheduleAll() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reminders.forEachIndexed { i, time ->
            val (h, m) = time.split(":").map { it.toInt() }
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
            val intent = Intent(this, ReminderReceiver::class.java).apply {
                putExtra("time", time)
                putExtra("index", i)
            }
            val pi = PendingIntent.getBroadcast(
                this, i + 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis,
                AlarmManager.INTERVAL_DAY, pi)
        }
    }

    private fun cancelAlarm(time: String) {
        val idx = reminders.indexOf(time)
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            this, idx + 100,
            Intent(this, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { am.cancel(it) }
    }

    private fun updateList() {
        binding.tvReminderList.text = if (reminders.isEmpty()) {
            "Belum ada pengingat"
        } else {
            reminders.joinToString("\n") { "• $it" }
        }
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')
}
