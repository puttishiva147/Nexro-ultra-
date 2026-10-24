package com.nexro.ultra

import android.app.Notification
import android.service.notification.StatusBarNotification

enum class PrivacyPhase {
    IDLE, NOTIFICATION_OBSERVED, POPUP_VISIBLE, MASK_REQUESTED, MASK_ACTIVE,
    REVEAL_REQUESTED, RESTORED, DISMISSED, UNSUPPORTED, ERROR
}

enum class PopupVisibility { UNKNOWN, CONFIRMED_VISIBLE, CONFIRMED_HIDDEN }

data class NotificationPrivacyRecord(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val category: Int?,
    val importance: Int,
    val title: String?,
    val text: String?,
    val removed: Boolean = false,
    val popupVisibility: PopupVisibility = PopupVisibility.UNKNOWN,
    val phase: PrivacyPhase = PrivacyPhase.NOTIFICATION_OBSERVED,
    val error: String? = null
)

sealed interface NotificationPrivacyEvent {
    data class Posted(val record: NotificationPrivacyRecord) : NotificationPrivacyEvent
    data class Removed(val key: String) : NotificationPrivacyEvent
    data class Reconcile(val records: List<NotificationPrivacyRecord>) : NotificationPrivacyEvent
    data class VisibilityChanged(val key: String, val visibility: PopupVisibility) : NotificationPrivacyEvent
    data class MaskRequested(val key: String) : NotificationPrivacyEvent
    data class MaskFinished(val key: String, val supported: Boolean, val error: String? = null) : NotificationPrivacyEvent
    data class RevealRequested(val key: String) : NotificationPrivacyEvent
}

object NotificationPrivacyReducer {
    fun reduce(
        current: Map<String, NotificationPrivacyRecord>,
        event: NotificationPrivacyEvent
    ): Map<String, NotificationPrivacyRecord> = when (event) {
        is NotificationPrivacyEvent.Posted -> {
            val old = current[event.record.key]
            current + (event.record.key to event.record.copy(
                removed = false,
                popupVisibility = old?.popupVisibility ?: PopupVisibility.UNKNOWN,
                phase = if (old?.phase == PrivacyPhase.MASK_ACTIVE) PrivacyPhase.MASK_ACTIVE
                        else PrivacyPhase.NOTIFICATION_OBSERVED
            ))
        }
        is NotificationPrivacyEvent.Removed -> current[event.key]?.let { record ->
            current + (event.key to record.copy(
                removed = true,
                popupVisibility = PopupVisibility.CONFIRMED_HIDDEN,
                phase = PrivacyPhase.DISMISSED
            ))
        } ?: current
        is NotificationPrivacyEvent.Reconcile -> {
            val live = event.records.associateBy { it.key }
            val merged = live.mapValues { (key, incoming) ->
                val old = current[key]
                incoming.copy(
                    popupVisibility = old?.popupVisibility ?: PopupVisibility.UNKNOWN,
                    phase = if (old?.phase == PrivacyPhase.MASK_ACTIVE) PrivacyPhase.MASK_ACTIVE
                            else PrivacyPhase.NOTIFICATION_OBSERVED
                )
            }
            current.filterValues { it.phase == PrivacyPhase.DISMISSED && it.key !in live } + merged
        }
        is NotificationPrivacyEvent.VisibilityChanged -> current[event.key]?.let { record ->
            current + (event.key to record.copy(
                popupVisibility = event.visibility,
                phase = when (event.visibility) {
                    PopupVisibility.CONFIRMED_VISIBLE -> PrivacyPhase.POPUP_VISIBLE
                    PopupVisibility.CONFIRMED_HIDDEN -> PrivacyPhase.RESTORED
                    PopupVisibility.UNKNOWN -> PrivacyPhase.NOTIFICATION_OBSERVED
                }
            ))
        } ?: current
        is NotificationPrivacyEvent.MaskRequested -> current[event.key]?.let { record ->
            if (record.removed || record.popupVisibility != PopupVisibility.CONFIRMED_VISIBLE) {
                current + (event.key to record.copy(
                    phase = PrivacyPhase.UNSUPPORTED,
                    error = "Heads-up visibility is not confirmed by public Android APIs."
                ))
            } else current + (event.key to record.copy(phase = PrivacyPhase.MASK_REQUESTED, error = null))
        } ?: current
        is NotificationPrivacyEvent.MaskFinished -> current[event.key]?.let { record ->
            if (record.removed) current
            else current + (event.key to record.copy(
                phase = if (event.supported) PrivacyPhase.MASK_ACTIVE else PrivacyPhase.UNSUPPORTED,
                error = event.error
            ))
        } ?: current
        is NotificationPrivacyEvent.RevealRequested -> current[event.key]?.let { record ->
            current + (event.key to record.copy(
                phase = if (record.removed) PrivacyPhase.DISMISSED else PrivacyPhase.RESTORED,
                error = null
            ))
        } ?: current
    }
}

object PrivacyEligibilityEngine {
    data class Decision(val eligibleForTracking: Boolean, val exactMaskSupported: Boolean, val reason: String)

    fun evaluate(record: NotificationPrivacyRecord): Decision {
        if (record.removed) return Decision(false, false, "Notification has been removed.")
        if (record.importance < android.app.NotificationManager.IMPORTANCE_HIGH) {
            return Decision(true, false, "Notification is tracked, but its importance does not indicate heads-up eligibility.")
        }
        if (record.popupVisibility != PopupVisibility.CONFIRMED_VISIBLE) {
            return Decision(true, false, "Posted notification does not prove a visible heads-up popup.")
        }
        return Decision(
            eligibleForTracking = true,
            exactMaskSupported = false,
            reason = "Public NotificationListenerService APIs do not provide the rendered SystemUI heads-up bounds or a guaranteed mask layer."
        )
    }
}

fun StatusBarNotification.toPrivacyRecord(importance: Int): NotificationPrivacyRecord {
    val n = notification
    return NotificationPrivacyRecord(
        key = key,
        packageName = packageName,
        postTime = postTime,
        category = n.category?.let { categoryStringToInt(it) },
        importance = importance,
        title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    )
}

private fun categoryStringToInt(category: String): Int = when (category) {
    Notification.CATEGORY_MESSAGE -> 1
    Notification.CATEGORY_CALL -> 2
    Notification.CATEGORY_EMAIL -> 3
    Notification.CATEGORY_EVENT -> 4
    else -> 0
}
