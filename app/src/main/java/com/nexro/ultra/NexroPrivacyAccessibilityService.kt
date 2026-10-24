package com.nexro.ultra

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup

class NexroPrivacyAccessibilityService : AccessibilityService() {
    private var lastKeyCode = -1
    private var lastKeyTime = 0L
    private var bar: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val removeBar = Runnable { hideBar() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = hideBar()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        val prefs = getSharedPreferences("nexro_ultra_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return false

        val wanted = if (prefs.getString("trigger", "volume_down") == "volume_up") {
            KeyEvent.KEYCODE_VOLUME_UP
        } else KeyEvent.KEYCODE_VOLUME_DOWN

        if (event.keyCode != wanted) {
            lastKeyCode = -1
            return false
        }

        val now = event.eventTime
        val doublePress = lastKeyCode == wanted && now - lastKeyTime in 1..850
        lastKeyCode = wanted
        lastKeyTime = now
        if (!doublePress) return false

        // Only trigger while Android System UI (such as the notification shade) is active.
        if (rootInActiveWindow?.packageName?.toString() != "com.android.systemui") return false

        showBar()
        return true
    }

    private fun showBar() {
        handler.removeCallbacks(removeBar)
        val existing = bar
        if (existing != null) {
            existing.visibility = View.VISIBLE
            handler.postDelayed(removeBar, 3500)
            return
        }

        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((18 * density).toInt(), 0, (18 * density).toInt(), 0)
            setBackgroundColor(Color.BLACK)
            elevation = 24 * density
        }
        val mark = TextView(this).apply {
            text = "N"
            setTextColor(Color.rgb(164, 229, 190))
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt()).apply {
                marginEnd = (12 * density).toInt()
            }
            setBackgroundColor(Color.rgb(32, 39, 34))
        }
        val textColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textColumn.addView(TextView(this).apply {
            text = "NOTIFICATION HIDDEN"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
        })
        textColumn.addView(TextView(this).apply {
            text = "Message content covered by Nexro"
            setTextColor(Color.LTGRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        })
        root.addView(mark)
        textColumn.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        root.addView(textColumn)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (88 * density).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = (108 * density).toInt()
        }

        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(root, params)
            bar = root
            handler.postDelayed(removeBar, 3500)
        } catch (_: Exception) {
            bar = null
        }
    }

    private fun hideBar() {
        handler.removeCallbacks(removeBar)
        val current = bar ?: return
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(current)
        } catch (_: Exception) { }
        bar = null
    }

    override fun onDestroy() {
        hideBar()
        super.onDestroy()
    }
}
