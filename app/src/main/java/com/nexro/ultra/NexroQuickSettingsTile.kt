package com.nexro.ultra

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class NexroQuickSettingsTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        if (!enabled && !isReadyToEnable()) {
            qsTile?.let {
                it.state = Tile.STATE_INACTIVE
                it.label = "Nexro Ultra • setup needed"
                it.updateTile()
            }
            return
        }
        prefs.edit().putBoolean("enabled", !enabled).apply()
        syncTile()
    }

    private fun isReadyToEnable(): Boolean {
        val expected = ComponentName(this, NexroNotificationListener::class.java)
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        val hasAccess = listeners.split(':').any { ComponentName.unflattenFromString(it) == expected }
        val hasPostPermission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val prefs = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
        val hasSelectedApp = prefs.getBoolean("pkg_whatsapp", true) ||
            prefs.getBoolean("pkg_whatsapp_business", false) ||
            prefs.getBoolean("pkg_telegram", false) ||
            prefs.getBoolean("pkg_instagram", false)
        return hasAccess && hasPostPermission && hasSelectedApp
    }

    private fun syncTile() {
        val enabled = getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Nexro Ultra"
            if (Build.VERSION.SDK_INT >= 29) tile.subtitle = if (enabled) "Protection on" else "Protection off"
            tile.contentDescription = if (enabled) "Turn Nexro Ultra off" else "Turn Nexro Ultra on"
            tile.updateTile()
        }
    }

    companion object {
        fun refresh(context: Context) { }
    }
}
