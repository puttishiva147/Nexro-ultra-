package com.nexro.ultra

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Experimental volume-key gesture detector only. It does not claim to mask a heads-up popup:
 * Android does not provide this ordinary app with reliable popup bounds or overlay ordering.
 */
class NexroPrivacyAccessibilityService : AccessibilityService() {
    private var lastKeyCode = -1
    private var lastKeyTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        val prefs = getSharedPreferences("nexro_ultra_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return false
        val wanted = when (prefs.getString("trigger", "quick_settings")) {
            "volume_up" -> KeyEvent.KEYCODE_VOLUME_UP
            "volume_down" -> KeyEvent.KEYCODE_VOLUME_DOWN
            else -> return false
        }
        if (event.keyCode != wanted) {
            lastKeyCode = -1
            return false
        }
        val now = event.eventTime
        val doublePress = lastKeyCode == wanted && now - lastKeyTime in 1..850
        lastKeyCode = wanted
        lastKeyTime = now
        if (doublePress) {
            prefs.edit().putLong("last_volume_trigger_time", System.currentTimeMillis()).apply()
            Toast.makeText(this, "Nexro: trigger received; exact heads-up masking is unsupported", Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
