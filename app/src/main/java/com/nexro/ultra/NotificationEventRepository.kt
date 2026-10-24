package com.nexro.ultra

import android.content.Context
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Serializes notification lifecycle events. It observes posted notifications only;
 * it never cancels, snoozes, clears, or reposts them.
 */
class NotificationEventRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val events = Channel<NotificationPrivacyEvent>(Channel.UNLIMITED)
    private val _records = MutableStateFlow<Map<String, NotificationPrivacyRecord>>(emptyMap())
    val records: StateFlow<Map<String, NotificationPrivacyRecord>> = _records.asStateFlow()

    init {
        scope.launch {
            for (event in events) {
                _records.value = NotificationPrivacyReducer.reduce(_records.value, event)
            }
        }
    }

    fun post(record: NotificationPrivacyRecord) { events.trySend(NotificationPrivacyEvent.Posted(record)) }
    fun remove(key: String) { events.trySend(NotificationPrivacyEvent.Removed(key)) }
    fun visibility(key: String, visibility: PopupVisibility) {
        events.trySend(NotificationPrivacyEvent.VisibilityChanged(key, visibility))
    }
    fun requestMask(key: String) { events.trySend(NotificationPrivacyEvent.MaskRequested(key)) }
    fun maskFinished(key: String, supported: Boolean, error: String? = null) {
        events.trySend(NotificationPrivacyEvent.MaskFinished(key, supported, error))
    }
    fun reveal(key: String) { events.trySend(NotificationPrivacyEvent.RevealRequested(key)) }

    fun reconcile(active: Array<StatusBarNotification>, importance: (StatusBarNotification) -> Int) {
        val snapshot = active.map { it.toPrivacyRecord(importance(it)) }
        events.trySend(NotificationPrivacyEvent.Reconcile(snapshot))
    }

    companion object {
        @Volatile private var instance: NotificationEventRepository? = null
        fun get(context: Context): NotificationEventRepository =
            instance ?: synchronized(this) {
                instance ?: NotificationEventRepository(context).also { instance = it }
            }
    }
}
