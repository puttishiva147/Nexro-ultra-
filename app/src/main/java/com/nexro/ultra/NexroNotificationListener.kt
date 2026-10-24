package com.nexro.ultra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Privacy-first MVP: watches selected packages, replaces selected notifications,
 * and never stores or uploads message text.
 * Source-app actions may be lost; precise text-only masking is not supported here.
 */
class NexroNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val selected = when (sbn.packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> prefs.getBoolean("pkg_whatsapp", true)
            "org.telegram.messenger", "org.telegram.messenger.web" -> prefs.getBoolean("pkg_telegram", false)
            "com.instagram.android" -> prefs.getBoolean("pkg_instagram", false)
            else -> false
        }
        if (!selected) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val hasMessageText = !text.isNullOrBlank() || !bigText.isNullOrBlank() ||
            (lines?.any { !it.isNullOrBlank() } == true)
        if (!hasMessageText || (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        try {
            cancelNotification(sbn.key)
            postSafeReplacement(sbn.packageName, title?.toString().orEmpty())
        } catch (_: Exception) {
            // Never log private notification content.
        }
    }

    private fun postSafeReplacement(sourcePackage: String, originalTitle: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nexro_private_notices"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                channelId, "Private notices", NotificationManager.IMPORTANCE_HIGH))
        }
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sourcePackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) { "An app" }
        val safeTitle = if (originalTitle.isBlank()) appName else "$appName • $originalTitle"
        val notice = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(safeTitle)
            .setContentText("New notification hidden by Nexro Ultra")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        val id = (sourcePackage.hashCode() and 0x7fffffff).coerceAtLeast(1)
        manager.notify(id, notice)
    }

    companion object {
        fun refreshEnabledState(context: Context) {
            // Android controls listener binding through Notification Access settings.
        }
    }
}
