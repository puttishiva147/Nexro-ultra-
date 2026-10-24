package com.nexro.ultra

import android.service.notification.NotificationListenerService

/**
 * Intentionally does not cancel or replace notifications.
 * Nexro's privacy trigger is handled by NexroPrivacyAccessibilityService.
 */
class NexroNotificationListener : NotificationListenerService()
