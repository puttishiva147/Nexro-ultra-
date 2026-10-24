package com.nexro.ultra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Local-only fallback implementation. Android does not expose an API to rewrite another
 * app's notification layout. This service replaces supported message notifications with
 * a safe notice; the original app's actions may be lost.
 */
class NexroNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        if ((sbn.notification.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT)) != 0) return

        val selected = when (sbn.packageName) {
            "com.whatsapp" -> prefs.getBoolean("pkg_whatsapp", true)
            "com.whatsapp.w4b" -> prefs.getBoolean("pkg_whatsapp_business", false)
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
        if (!hasMessageText) return
        // Skip silent notifications and already-cancelled entries; leave unsupported layouts alone.
        if (sbn.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0 && text.isNullOrBlank()) return

        try {
            cancelNotification(sbn.key)
            postSafeReplacement(sbn.packageName, title?.toString().orEmpty(), sbn.id)
        } catch (_: SecurityException) {
            // Permission may have been revoked while the service was running.
        } catch (_: Exception) {
            // Never log or persist private notification content.
        }
    }

    private fun postSafeReplacement(sourcePackage: String, originalTitle: String, sourceId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nexro_private_notices"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                channelId, "Private notices", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications with message content hidden by Nexro Ultra" })
        }
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sourcePackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) { "An app" }
        // Preserve the sender/title only; never copy the message body into the replacement.
        val safeTitle = if (originalTitle.isBlank()) appName else "$appName • $originalTitle"
        val notice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(safeTitle)
            .setContentText("New notification hidden by Nexro Ultra")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        val id = (sourcePackage.hashCode() * 31 + sourceId) and 0x7fffffff
        manager.notify(if (id == 0) 1 else id, notice)
    }
}
