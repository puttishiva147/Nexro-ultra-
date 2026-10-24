package com.nexro.ultra

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore by preferencesDataStore(name = "nexro_privacy_settings")

data class PrivacySettings(
    val triggerEnabled: Boolean = false,
    val trigger: String = "quick_settings"
)

/** DataStore-backed durable settings. Volume-key gestures are marked experimental, not guaranteed. */
class PrivacySettingsStore(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("trigger_enabled")
    private val triggerKey = stringPreferencesKey("trigger")

    val settings: Flow<PrivacySettings> = context.privacyDataStore.data.map { values ->
        PrivacySettings(
            triggerEnabled = values[enabledKey] ?: false,
            trigger = values[triggerKey] ?: "quick_settings"
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.privacyDataStore.edit { it[enabledKey] = enabled }
        context.getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("enabled", enabled).apply()
    }

    suspend fun setTrigger(trigger: String) {
        require(trigger in setOf("quick_settings", "volume_down", "volume_up"))
        context.privacyDataStore.edit { it[triggerKey] = trigger }
        context.getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
            .edit().putString("trigger", trigger).apply()
    }
}
