package com.nexro.ultra

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { NexroSettingsScreen(this) }
    }
}

@Composable
private fun NexroSettingsScreen(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var whatsapp by remember { mutableStateOf(prefs.getBoolean("pkg_whatsapp", true)) }
    var telegram by remember { mutableStateOf(prefs.getBoolean("pkg_telegram", false)) }
    var instagram by remember { mutableStateOf(prefs.getBoolean("pkg_instagram", false)) }
    var doubleTap by remember { mutableStateOf(prefs.getBoolean("trigger_double_tap", false)) }
    var volumePress by remember { mutableStateOf(prefs.getBoolean("trigger_volume", false)) }
    var statusRefresh by remember { mutableIntStateOf(0) }

    val listenerEnabled = remember(statusRefresh) {
        val expected = ComponentName(activity, NexroNotificationListener::class.java)
        val listeners = Settings.Secure.getString(activity.contentResolver,
            "enabled_notification_listeners").orEmpty()
        listeners.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
    val postPermission = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    fun save(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        NexroNotificationListener.refreshEnabledState(activity)
    }

    MaterialTheme {
        Scaffold { padding ->
            Column(Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("NEXRO ULTRA", style = MaterialTheme.typography.headlineMedium)
                Text("On-device notification privacy", style = MaterialTheme.typography.titleMedium)
                Text("Nexro Ultra can replace selected apps' incoming notifications with a safe notice. Android does not let ordinary apps precisely paint over another app's notification text.")
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Nexro Ultra", style = MaterialTheme.typography.titleLarge)
                        Text(if (enabled && listenerEnabled && postPermission) "Enabled and ready" else if (!enabled) "Off" else "Setup required")
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it; save("enabled", it) })
                }
                Text("Setup status", style = MaterialTheme.typography.titleMedium)
                StatusLine("Notification access", listenerEnabled)
                StatusLine("Post-notification permission", postPermission)
                Button(onClick = { activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()) { Text("Set up notification access") }
                Text("Required to detect and dismiss selected notifications. Nexro Ultra does not upload notification content.")
                HorizontalDivider()
                Text("Supported apps", style = MaterialTheme.typography.titleMedium)
                SettingSwitch("WhatsApp", whatsapp) { whatsapp = it; save("pkg_whatsapp", it) }
                SettingSwitch("Telegram", telegram) { telegram = it; save("pkg_telegram", it) }
                SettingSwitch("Instagram", instagram) { instagram = it; save("pkg_instagram", it) }
                HorizontalDivider()
                Text("Activation methods", style = MaterialTheme.typography.titleMedium)
                SettingSwitch("Double-tap trigger", doubleTap) { doubleTap = it; save("trigger_double_tap", it) }
                Text("System-wide double-tap is not implemented: Android does not provide a general global double-tap event to ordinary apps.")
                SettingSwitch("Double-press volume button", volumePress) { volumePress = it; save("trigger_volume", it) }
                Text("Global volume-button interception is not implemented because Android/device firmware restricts it and it can interfere with normal volume controls.")
                Text("Current implementation", style = MaterialTheme.typography.titleMedium)
                Text("When enabled with notification access, selected app notifications containing message text are dismissed and replaced with a Nexro Ultra notice. Original notification actions/replies may be lost. Notifications that cannot be safely identified are left untouched.")
                Text("Early implementation scaffold — not a verified release build. Test on a spare device before relying on it for privacy.")
                OutlinedButton(onClick = { statusRefresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh permission status") }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(if (ok) "Granted" else "Missing")
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
