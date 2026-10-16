package com.chiara.beep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Mainkan beep sekali untuk pengingat
        Thread {
            val prefs = PrefsHelper(context)
            val service = BeepService()
            // Jalankan beep langsung via service intent
            val si = Intent(context, BeepService::class.java).apply {
                action = "REMINDER_BEEP"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(si)
            } else {
                context.startService(si)
            }
        }.start()
    }
}
