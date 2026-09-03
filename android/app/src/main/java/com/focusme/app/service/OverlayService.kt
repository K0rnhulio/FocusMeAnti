package com.focusme.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.focusme.app.R

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var pillView: View? = null

    companion object {
        private const val ACTION_SHOW_PILL = "com.focusme.SHOW_PILL"
        private const val ACTION_HIDE_PILL = "com.focusme.HIDE_PILL"
        private const val EXTRA_REMAINING_SECS = "remaining_seconds"

        fun showPill(context: Context, remainingSeconds: Int) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_PILL
                putExtra(EXTRA_REMAINING_SECS, remainingSeconds)
            }
            context.startService(intent)
        }

        fun hidePill(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_HIDE_PILL
            }
            context.startService(intent)
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

        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }

        if (pillView == null) {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = 40
                y = 100
            }

            // Simple programmatic TextView pill
            val textView = TextView(this).apply {
                text = "⏱️ $timeStr left"
                setBackgroundColor(0xDD0F172A.toInt())
                setTextColor(0xFF38BDF8.toInt())
                setPadding(28, 16, 28, 16)
                textSize = 14f
                elevation = 12f
            }

            pillView = textView
            try {
                windowManager?.addView(pillView, params)
            } catch (e: Exception) {}
        } else {
            (pillView as? TextView)?.text = "⏱️ $timeStr left"
        }
    }

    private fun removePill() {
        if (pillView != null && windowManager != null) {
            try {
                windowManager?.removeView(pillView)
            } catch (e: Exception) {}
            pillView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removePill()
    }
}
