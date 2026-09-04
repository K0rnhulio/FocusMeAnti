package com.focusme.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var pillView: View? = null
    private var timerTextView: TextView? = null
    private var pillBackgroundDrawable: GradientDrawable? = null
    private var isCurrentlyLowTime: Boolean = false

    companion object {
        private const val ACTION_SHOW_PILL = "com.focusme.SHOW_PILL"
        private const val ACTION_HIDE_PILL = "com.focusme.HIDE_PILL"
        private const val EXTRA_REMAINING_SECS = "remaining_seconds"

        fun showPill(context: Context, remainingSeconds: Int) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_PILL
                putExtra(EXTRA_REMAINING_SECS, remainingSeconds)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {}
        }

        fun hidePill(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_HIDE_PILL
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) return START_NOT_STICKY

        when (intent?.action) {
            ACTION_SHOW_PILL -> {
                val remaining = intent.getIntExtra(EXTRA_REMAINING_SECS, 300)
                displayOrUpdatePill(remaining)
            }
            ACTION_HIDE_PILL -> {
                removePill()
            }
        }
        return START_NOT_STICKY
    }

    private fun displayOrUpdatePill(remainingSecs: Int) {
        val mins = remainingSecs / 60
        val secs = remainingSecs % 60
        val timeStr = String.format("%02d:%02d", mins, secs)
        val isLowTime = remainingSecs <= 60

        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }

        val density = resources.displayMetrics.density

        if (pillView == null) {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val marginEndPx = (18 * density).toInt()
            val marginBottomPx = (56 * density).toInt()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = marginEndPx
                y = marginBottomPx
            }

            // Create pill container
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padH = (14 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                elevation = 16f
            }

            // Stable GradientDrawable (created ONCE to prevent overlay redraw flickering)
            val cornerRadius = 30f * density
            val strokeWidth = (1.5f * density).toInt()
            val strokeColor = if (isLowTime) Color.parseColor("#88F43F5E") else Color.parseColor("#6638BDF8")
            val bgColor = Color.parseColor("#EE0B1220")

            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = cornerRadius
                setColor(bgColor)
                setStroke(strokeWidth, strokeColor)
            }
            container.background = bgDrawable
            pillBackgroundDrawable = bgDrawable
            isCurrentlyLowTime = isLowTime

            // Timer text
            val tv = TextView(this).apply {
                text = "⏱️ $timeStr"
                setTextColor(if (isLowTime) Color.parseColor("#F43F5E") else Color.parseColor("#38BDF8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            container.addView(tv)
            timerTextView = tv
            pillView = container

            try {
                windowManager?.addView(pillView, params)
            } catch (e: Exception) {}
        } else {
            // Update existing view smoothly without recreating drawables or invalidating layout
            timerTextView?.text = "⏱️ $timeStr"

            // Only update colors if low-time state changed
            if (isCurrentlyLowTime != isLowTime) {
                isCurrentlyLowTime = isLowTime
                val strokeWidth = (1.5f * density).toInt()
                val strokeColor = if (isLowTime) Color.parseColor("#88F43F5E") else Color.parseColor("#6638BDF8")
                pillBackgroundDrawable?.setStroke(strokeWidth, strokeColor)
                timerTextView?.setTextColor(if (isLowTime) Color.parseColor("#F43F5E") else Color.parseColor("#38BDF8"))
            }
        }
    }

    private fun removePill() {
        if (pillView != null && windowManager != null) {
            try {
                windowManager?.removeView(pillView)
            } catch (e: Exception) {}
            pillView = null
            timerTextView = null
            pillBackgroundDrawable = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removePill()
    }
}
