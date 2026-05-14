package com.pomo.ui

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class LockdownOverlay(private val activity: Activity) {

    private var overlayView: View? = null

    fun showOverlay(taskName: String) {
        activity.runOnUiThread {
            if (overlayView != null) return@runOnUiThread

            val textView = TextView(activity).apply {
                text = """
                    🍅 KORI POMODORO - FOCUS MODE 🍅
                    
                    Task: $taskName
                    
                    ⚠️ Please don't switch apps!
                    Only answer emergency calls.
                    
                    Every app switch is logged.
                """.trimIndent()
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.argb(220, 26, 26, 46))
                setPadding(40, 40, 40, 40)
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                android.graphics.PixelFormat.TRANSLUCENT
            )

            try {
                val windowManager = activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
                windowManager.addView(textView, params)
                overlayView = textView
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hideOverlay() {
        activity.runOnUiThread {
            try {
                overlayView?.let {
                    val windowManager = activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
                    windowManager.removeView(it)
                }
                overlayView = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
