package com.nexro.ultra

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NexroQuickSettingsTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        if (!enabled && !isAccessibilityEnabled()) {
            qsTile?.let {
                it.state = Tile.STATE_INACTIVE
                it.label = "Nexro • setup needed"
                it.updateTile()
            }
            startActivityAndCollapse(Intent(this, MainActivity::class.java))
            return
        }
        prefs.edit().putBoolean("enabled", !enabled).apply()
        CoroutineScope(Dispatchers.IO).launch {
            PrivacySettingsStore(this@NexroQuickSettingsTile).setEnabled(!enabled)
        }
        syncTile()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, NexroPrivacyAccessibilityService::class.java)
        return Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun syncTile() {
        val enabled = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Nexro Ultra"
            if (android.os.Build.VERSION.SDK_INT >= 29) tile.subtitle = if (enabled) "Trigger on" else "Trigger off"
            tile.contentDescription = if (enabled) "Turn Nexro Ultra off" else "Turn Nexro Ultra on"
            tile.updateTile()
        }
    }

    companion object {
        fun refresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, NexroQuickSettingsTile::class.java))
            }
        }
    }
}
