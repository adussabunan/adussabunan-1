package com.whiterose.clock

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*

class ScreensaverService : DreamService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvSec: TextView
    private lateinit var container: LinearLayout

    private val days   = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
    private val months = arrayOf("JAN","FEB","MAR","APR","MAY","JUN",
                                  "JUL","AUG","SEP","OCT","NOV","DEC")

    // Slow drift to prevent AMOLED burn-in
    private var driftX = 0f
    private var driftY = 0f
    private var driftDx = 0.3f
    private var driftDy = 0.2f
    private val maxDrift = 30f // pixels

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateClock()
            driftPosition()
            handler.postDelayed(this, 100)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Full screen, no controls
        isFullscreen = true
        isInteractive = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK) // Pure black for AMOLED
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val green = Color.parseColor("#00FF41")
        val greenDim = Color.parseColor("#00B82E")

        // Seconds (small top label)
        tvSec = TextView(this).apply {
            textSize = 11f
            setTextColor(greenDim)
            letterSpacing = 0.3f
            gravity = Gravity.CENTER
        }

        // Main time HH:MM:SS
        tvTime = TextView(this).apply {
            textSize = 58f
            setTextColor(green)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            setShadowLayer(18f, 0f, 0f, Color.parseColor("#8000FF41"))
        }

        // Date line
        tvDate = TextView(this).apply {
            textSize = 11f
            setTextColor(greenDim)
            letterSpacing = 0.25f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        container.addView(tvSec)
        container.addView(tvTime)
        container.addView(tvDate)
        root.addView(container)
        setContentView(root)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.post(tickRunnable)
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        handler.removeCallbacks(tickRunnable)
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

        tvTime.text = "${pad(h)}:${pad(m)}:${pad(s)}"
        tvDate.text = "${days[d]} ${pad(dt)} ${months[mo]} $yr"
        tvSec.text  = "— ${pad(s)} —"
    }

    // Slowly drift the clock position to prevent AMOLED burn-in
    private fun driftPosition() {
        driftX += driftDx
        driftY += driftDy

        if (driftX > maxDrift || driftX < -maxDrift) driftDx = -driftDx
        if (driftY > maxDrift || driftY < -maxDrift) driftDy = -driftDy

        container.translationX = driftX
        container.translationY = driftY
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')
}
