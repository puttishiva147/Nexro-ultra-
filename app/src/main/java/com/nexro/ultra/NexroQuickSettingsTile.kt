package com.nexro.ultra

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class NexroQuickSettingsTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        prefs.edit().putBoolean("enabled", !enabled).apply()
        syncTile()
    }

    private fun syncTile() {
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Nexro Ultra"
            if (Build.VERSION.SDK_INT >= 29) {
                tile.subtitle = if (enabled) "Protection on" else "Protection off"
            }
            tile.contentDescription = if (enabled) "Turn Nexro Ultra off" else "Turn Nexro Ultra on"
            tile.updateTile()
        }
    }

    companion object {
        fun refresh(context: Context) {
            // Tile updates when Android next calls onStartListening.
        }
    }
}
