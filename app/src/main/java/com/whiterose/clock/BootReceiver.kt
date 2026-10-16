package com.chiara.beep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PrefsHelper(context)
        if (prefs.serviceOn) {
            val si = Intent(context, BeepService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(si)
            } else {
                context.startService(si)
            }
        }
    }
}
