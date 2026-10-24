package com.nexro.ultra

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/** Observes posted/updated/removed notifications. Never modifies the source notification. */
class NexroNotificationListener : NotificationListenerService() {
    private val repository by lazy { NotificationEventRepository.get(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching {
            repository.reconcile(activeNotifications ?: emptyArray()) { sbn -> currentRankingImportance(sbn) }
        }.onFailure { Log.e(TAG, "Unable to reconcile active notifications", it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        repository.post(sbn.toPrivacyRecord(currentRankingImportance(sbn)))
        Log.d(TAG, "Observed posted notification key=${sbn.key}, package=${sbn.packageName}; heads-up visibility=UNKNOWN")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        repository.remove(sbn.key)
        Log.d(TAG, "Observed removal key=${sbn.key}")
    }

    override fun onNotificationRankingUpdate(ranking: RankingMap) {
        super.onNotificationRankingUpdate(ranking)
        runCatching {
            val active = activeNotifications ?: emptyArray()
            repository.reconcile(active) { sbn ->
                val item = Ranking()
                if (ranking.getRanking(sbn.key, item)) item.importance else currentRankingImportance(sbn)
            }
        }.onFailure { Log.e(TAG, "Unable to reconcile notification ranking", it) }
    }

    private fun currentRankingImportance(sbn: StatusBarNotification): Int {
        val item = Ranking()
        return if (currentRanking.getRanking(sbn.key, item)) item.importance
        else when {
            sbn.notification.priority >= 2 -> android.app.NotificationManager.IMPORTANCE_HIGH
            sbn.notification.priority <= -2 -> android.app.NotificationManager.IMPORTANCE_MIN
            else -> android.app.NotificationManager.IMPORTANCE_DEFAULT
        }
    }

    companion object { private const val TAG = "NexroNotification" }
}
