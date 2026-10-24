package com.nexro.ultra

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val NexroDark = androidx.compose.ui.graphics.Color(0xFF0B0D0C)
private val NexroCard = androidx.compose.ui.graphics.Color(0xFF171A18)
private val NexroAccent = androidx.compose.ui.graphics.Color(0xFF9AD9B5)

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
        setContent { NexroApp(this) }
    }

    fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    fun requestQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            TileService.requestAddTileService(
                this,
                ComponentName(this, NexroQuickSettingsTile::class.java),
                "Nexro Ultra",
                Icon.createWithResource(this, android.R.drawable.ic_lock_lock),
                mainExecutor
            ) { }
        } else {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

@Composable
private fun NexroApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var whatsapp by remember { mutableStateOf(prefs.getBoolean("pkg_whatsapp", true)) }
    var whatsappBusiness by remember { mutableStateOf(prefs.getBoolean("pkg_whatsapp_business", false)) }
    var telegram by remember { mutableStateOf(prefs.getBoolean("pkg_telegram", false)) }
    var instagram by remember { mutableStateOf(prefs.getBoolean("pkg_instagram", false)) }
    var refresh by remember { mutableIntStateOf(0) }

    val listenerEnabled = remember(refresh) {
        val expected = ComponentName(activity, NexroNotificationListener::class.java)
        Settings.Secure.getString(activity.contentResolver, "enabled_notification_listeners")
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
    val postPermission = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val selectedAny = whatsapp || whatsappBusiness || telegram || instagram
    val ready = listenerEnabled && postPermission && selectedAny
    val status = when {
        !enabled -> "Protection is off"
        !listenerEnabled -> "Action needed: enable notification access"
        !postPermission -> "Action needed: allow Nexro Ultra notifications"
        !selectedAny -> "Choose at least one supported app"
        else -> "Protection is enabled"
    }

    fun save(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        NexroQuickSettingsTile.refresh(activity)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NexroAccent,
            onPrimary = NexroDark,
            background = NexroDark,
            surface = NexroCard,
            onSurface = androidx.compose.ui.graphics.Color(0xFFF4F6F4),
            secondary = androidx.compose.ui.graphics.Color(0xFFA5ADA7)
        )
    ) {
        Scaffold(containerColor = NexroDark) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NEXRO", style = MaterialTheme.typography.labelLarge,
                            color = NexroAccent, fontWeight = FontWeight.Bold)
                        Text("ULTRA", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Private by design.", color = MaterialTheme.colorScheme.secondary)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = NexroCard) {
                        Text("NU", modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.titleLarge, color = NexroAccent,
                            fontWeight = FontWeight.Bold)
                    }
                }

                Surface(shape = RoundedCornerShape(24.dp), color = NexroCard,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Notification privacy", style = MaterialTheme.typography.titleLarge)
                                Text(status, color = if (enabled && ready) NexroAccent else MaterialTheme.colorScheme.secondary)
                            }
                            Switch(checked = enabled, onCheckedChange = {
                                enabled = it
                                save("enabled", it)
                            })
                        }
                        HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF303632))
                        Text("Selected message notifications are replaced by a safe Nexro notice. Android does not allow a reliable text-only mask on every app layout.")
                    }
                }

                SectionTitle("SETUP")
                Surface(shape = RoundedCornerShape(20.dp), color = NexroCard, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PermissionRow("Notification access", listenerEnabled,
                            "Detects selected app notifications") {
                            activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                        PermissionRow("Nexro notifications", postPermission,
                            "Needed to show safe replacement notices") {
                            activity.requestPostNotificationPermission()
                        }
                        OutlinedButton(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) {
                            Text("Refresh permission status")
                        }
                    }
                }

                SectionTitle("PROTECTED APPS")
                Surface(shape = RoundedCornerShape(20.dp), color = NexroCard, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        AppToggle("WhatsApp", "com.whatsapp", whatsapp) { whatsapp = it; save("pkg_whatsapp", it) }
                        HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF303632))
                        AppToggle("WhatsApp Business", "com.whatsapp.w4b", whatsappBusiness) { whatsappBusiness = it; save("pkg_whatsapp_business", it) }
                        HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF303632))
                        AppToggle("Telegram", "org.telegram.messenger", telegram) { telegram = it; save("pkg_telegram", it) }
                        HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF303632))
                        AppToggle("Instagram", "com.instagram.android", instagram) { instagram = it; save("pkg_instagram", it) }
                    }
                }

                SectionTitle("QUICK ACTIVATION")
                Surface(shape = RoundedCornerShape(20.dp), color = NexroCard, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Quick Settings tile", style = MaterialTheme.typography.titleMedium)
                        Text("Add a Nexro Ultra tile to Android Quick Settings to switch protection on or off without opening the app.")
                        Button(onClick = { activity.requestQuickSettingsTile() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add Quick Settings tile")
                        }
                        Text("Double-tap and volume-button triggers are not advertised as working because Android has no reliable universal API for them.", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionTitle("PRIVACY NOTES")
                Text("• Notification processing stays on-device; message text is not uploaded or logged.\n• The original notification may be dismissed, removing reply buttons and other actions.\n• Notifications that cannot be confidently identified are left untouched.\n• OEM behaviour and notification styles vary; test on your device before relying on this for sensitive messages.",
                    color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text("NEXRO ULTRA  •  0.2.0", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = NexroAccent,
        fontWeight = FontWeight.Bold)
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, explanation: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(explanation, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (granted) "READY" else "SET UP",
                color = if (granted) NexroAccent else MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        if (!granted) TextButton(onClick = onClick) { Text("Open settings") }
    }
}

@Composable
private fun AppToggle(title: String, packageName: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(packageName, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
