package com.chiara.beep

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chiara_beep_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_COLOR         = "color"
        const val KEY_BEEP_COUNT    = "beep_count"
        const val KEY_BEEP_FREQ     = "beep_freq"
        const val KEY_BEEP_DURATION = "beep_duration"
        const val KEY_SHOW_DATE     = "show_date"
        const val KEY_BLINK_COLON   = "blink_colon"
        const val KEY_SILENT_ON     = "silent_on"
        const val KEY_SILENT_START  = "silent_start"
        const val KEY_SILENT_END    = "silent_end"
        const val KEY_DND_RESPECT   = "dnd_respect"
        const val KEY_SERVICE_ON    = "service_on"

        // Color options
        const val COLOR_GREEN  = "green"
        const val COLOR_AMBER  = "amber"
        const val COLOR_BLUE   = "blue"
        const val COLOR_RED    = "red"

        // Frequency options (Hz)
        const val FREQ_LOW    = 800
        const val FREQ_MID    = 1800
        const val FREQ_HIGH   = 3000

        // Duration options (ms)
        const val DUR_SHORT   = 50
        const val DUR_MID     = 100
        const val DUR_LONG    = 200
    }

    var color: String
        get() = prefs.getString(KEY_COLOR, COLOR_GREEN) ?: COLOR_GREEN
        set(v) = prefs.edit().putString(KEY_COLOR, v).apply()

    var beepCount: Int
        get() = prefs.getInt(KEY_BEEP_COUNT, 2)
        set(v) = prefs.edit().putInt(KEY_BEEP_COUNT, v).apply()

    var beepFreq: Int
        get() = prefs.getInt(KEY_BEEP_FREQ, FREQ_MID)
        set(v) = prefs.edit().putInt(KEY_BEEP_FREQ, v).apply()

    var beepDuration: Int
        get() = prefs.getInt(KEY_BEEP_DURATION, DUR_SHORT)
        set(v) = prefs.edit().putInt(KEY_BEEP_DURATION, v).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_DATE, true)
        set(v) = prefs.edit().putBoolean(KEY_SHOW_DATE, v).apply()

    var blinkColon: Boolean
        get() = prefs.getBoolean(KEY_BLINK_COLON, true)
        set(v) = prefs.edit().putBoolean(KEY_BLINK_COLON, v).apply()

    var silentOn: Boolean
        get() = prefs.getBoolean(KEY_SILENT_ON, false)
        set(v) = prefs.edit().putBoolean(KEY_SILENT_ON, v).apply()

    // Format "HH:MM"
    var silentStart: String
        get() = prefs.getString(KEY_SILENT_START, "22:00") ?: "22:00"
        set(v) = prefs.edit().putString(KEY_SILENT_START, v).apply()

    var silentEnd: String
        get() = prefs.getString(KEY_SILENT_END, "06:00") ?: "06:00"
        set(v) = prefs.edit().putString(KEY_SILENT_END, v).apply()

    var dndRespect: Boolean
        get() = prefs.getBoolean(KEY_DND_RESPECT, true)
        set(v) = prefs.edit().putBoolean(KEY_DND_RESPECT, v).apply()

    var serviceOn: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ON, false)
        set(v) = prefs.edit().putBoolean(KEY_SERVICE_ON, v).apply()

    fun getColorHex(): String = when (color) {
        COLOR_AMBER -> "#FFAA00"
        COLOR_BLUE  -> "#00AAFF"
        COLOR_RED   -> "#FF3333"
        else        -> "#00FF41"
    }

    fun getColorDimHex(): String = when (color) {
        COLOR_AMBER -> "#CC7700"
        COLOR_BLUE  -> "#0077CC"
        COLOR_RED   -> "#CC1111"
        else        -> "#00B82E"
    }
}
