package com.chiara.beep

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.chiara.beep.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        prefs = PrefsHelper(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        // Color
        when (prefs.color) {
            PrefsHelper.COLOR_GREEN -> binding.rgColor.check(R.id.rbGreen)
            PrefsHelper.COLOR_AMBER -> binding.rgColor.check(R.id.rbAmber)
            PrefsHelper.COLOR_BLUE  -> binding.rgColor.check(R.id.rbBlue)
            PrefsHelper.COLOR_RED   -> binding.rgColor.check(R.id.rbRed)
        }

        // Beep count
        when (prefs.beepCount) {
            1 -> binding.rgBeepCount.check(R.id.rb1x)
            2 -> binding.rgBeepCount.check(R.id.rb2x)
            3 -> binding.rgBeepCount.check(R.id.rb3x)
        }

        // Frequency
        when (prefs.beepFreq) {
            PrefsHelper.FREQ_LOW  -> binding.rgFreq.check(R.id.rbFreqLow)
            PrefsHelper.FREQ_MID  -> binding.rgFreq.check(R.id.rbFreqMid)
            PrefsHelper.FREQ_HIGH -> binding.rgFreq.check(R.id.rbFreqHigh)
        }

        // Duration
        when (prefs.beepDuration) {
            PrefsHelper.DUR_SHORT -> binding.rgDuration.check(R.id.rbDurShort)
            PrefsHelper.DUR_MID   -> binding.rgDuration.check(R.id.rbDurMid)
            PrefsHelper.DUR_LONG  -> binding.rgDuration.check(R.id.rbDurLong)
        }

        // Toggles
        binding.swShowDate.isChecked   = prefs.showDate
        binding.swBlinkColon.isChecked = prefs.blinkColon
        binding.swDnd.isChecked        = prefs.dndRespect

        // Silent hours
        binding.swSilent.isChecked = prefs.silentOn
        binding.tvSilentStart.text = prefs.silentStart
        binding.tvSilentEnd.text   = prefs.silentEnd
        updateSilentVisibility()
    }

    private fun setupListeners() {
        // Color
        binding.rgColor.setOnCheckedChangeListener { _, id ->
            prefs.color = when (id) {
                R.id.rbAmber -> PrefsHelper.COLOR_AMBER
                R.id.rbBlue  -> PrefsHelper.COLOR_BLUE
                R.id.rbRed   -> PrefsHelper.COLOR_RED
                else         -> PrefsHelper.COLOR_GREEN
            }
        }

        // Beep count
        binding.rgBeepCount.setOnCheckedChangeListener { _, id ->
            prefs.beepCount = when (id) {
                R.id.rb1x -> 1
                R.id.rb3x -> 3
                else      -> 2
            }
        }

        // Frequency
        binding.rgFreq.setOnCheckedChangeListener { _, id ->
            prefs.beepFreq = when (id) {
                R.id.rbFreqLow  -> PrefsHelper.FREQ_LOW
                R.id.rbFreqHigh -> PrefsHelper.FREQ_HIGH
                else            -> PrefsHelper.FREQ_MID
            }
        }

        // Duration
        binding.rgDuration.setOnCheckedChangeListener { _, id ->
            prefs.beepDuration = when (id) {
                R.id.rbDurMid  -> PrefsHelper.DUR_MID
                R.id.rbDurLong -> PrefsHelper.DUR_LONG
                else           -> PrefsHelper.DUR_SHORT
            }
        }

        // Toggles
        binding.swShowDate.setOnCheckedChangeListener   { _, v -> prefs.showDate   = v }
        binding.swBlinkColon.setOnCheckedChangeListener { _, v -> prefs.blinkColon = v }
        binding.swDnd.setOnCheckedChangeListener        { _, v -> prefs.dndRespect = v }

        // Silent hours
        binding.swSilent.setOnCheckedChangeListener { _, v ->
            prefs.silentOn = v
            updateSilentVisibility()
        }

        binding.tvSilentStart.setOnClickListener {
            val (h, m) = prefs.silentStart.split(":").map { it.toInt() }
            TimePickerDialog(this, { _, hh, mm ->
                val t = "${pad(hh)}:${pad(mm)}"
                prefs.silentStart = t
                binding.tvSilentStart.text = t
            }, h, m, true).show()
        }

        binding.tvSilentEnd.setOnClickListener {
            val (h, m) = prefs.silentEnd.split(":").map { it.toInt() }
            TimePickerDialog(this, { _, hh, mm ->
                val t = "${pad(hh)}:${pad(mm)}"
                prefs.silentEnd = t
                binding.tvSilentEnd.text = t
            }, h, m, true).show()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun updateSilentVisibility() {
        val vis = if (prefs.silentOn) View.VISIBLE else View.GONE
        binding.layoutSilentTimes.visibility = vis
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')
}
