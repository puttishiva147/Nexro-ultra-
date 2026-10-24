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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val Ink = Color(0xFF080A09)
private val Panel = Color(0xFF141815)
private val Raised = Color(0xFF1D241F)
private val Mint = Color(0xFFA4E5BE)
private val Muted = Color(0xFF9AA69E)
private val DividerColor = Color(0xFF29312C)

private data class ProtectedApp(val title: String, val packageName: String, val key: String, val defaultOn: Boolean = false)
private val protectedApps = listOf(
    ProtectedApp("WhatsApp", "com.whatsapp", "pkg_whatsapp", true),
    ProtectedApp("WhatsApp Business", "com.whatsapp.w4b", "pkg_whatsapp_business"),
    ProtectedApp("Telegram", "org.telegram.messenger", "pkg_telegram"),
    ProtectedApp("Instagram", "com.instagram.android", "pkg_instagram")
)

class MainActivity : ComponentActivity() {
    var resumeCount by mutableIntStateOf(0)
        private set

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        resumeCount++
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexroDashboard(this) }
    }

    fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }
}

@Composable
private fun NexroDashboard(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var appStates by remember { mutableStateOf(protectedApps.associate { it.key to prefs.getBoolean(it.key, it.defaultOn) }) }
    var refresh by remember { mutableIntStateOf(0) }
    var showTileHelp by remember { mutableStateOf(false) }
    val resumeCount = activity.resumeCount

    LaunchedEffect(resumeCount) {
        enabled = prefs.getBoolean("enabled", false)
        appStates = protectedApps.associate { it.key to prefs.getBoolean(it.key, it.defaultOn) }
        refresh++
    }

    val notificationAccess = remember(refresh, resumeCount) {
        val expected = ComponentName(activity, NexroNotificationListener::class.java)
        Settings.Secure.getString(activity.contentResolver, "enabled_notification_listeners")
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
    val postPermission = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    val installed = remember(resumeCount) {
        protectedApps.associate { app ->
            app.packageName to try {
                activity.packageManager.getPackageInfo(app.packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    val anySelectedInstalled = protectedApps.any { appStates[it.key] == true && installed[it.packageName] == true }
    val ready = notificationAccess && postPermission && anySelectedInstalled
    val status = when {
        enabled && !notificationAccess -> "Notification access is disabled"
        enabled && !postPermission -> "Nexro notifications are blocked"
        enabled && !anySelectedInstalled -> "Select an installed app"
        enabled -> "Your selected apps are protected"
        !notificationAccess -> "Complete notification access setup"
        !postPermission -> "Allow Nexro replacement notifications"
        !anySelectedInstalled -> "Choose an installed app below"
        else -> "Ready to activate"
    }

    fun persist(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        appStates = appStates + (key to value)
        NexroQuickSettingsTile.refresh(activity)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Ink, background = Ink, surface = Panel,
        onSurface = Color(0xFFF4F7F4), secondary = Muted,
        surfaceVariant = Raised, outline = DividerColor
    )) {
        Scaffold(containerColor = Ink) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Surface(shape = RoundedCornerShape(17.dp), color = Raised, modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("N", color = Mint, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("NEXRO", color = Mint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        Text("ULTRA", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Private by design", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("v0.3.0", color = Muted, style = MaterialTheme.typography.labelSmall)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(26.dp), color = Panel
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("PRIVACY CONTROL", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(if (enabled && ready) "Protection on" else "Protection off", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(status, color = if (enabled && ready) Mint else Muted, style = MaterialTheme.typography.bodyMedium)
                            }
                            Surface(shape = CircleShape, color = if (enabled && ready) Color(0xFF254735) else Raised, modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (enabled && ready) "ON" else "OFF", color = if (enabled && ready) Mint else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Hide message previews", fontWeight = FontWeight.SemiBold)
                                Text("Replaces supported message notifications with a safe notice", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = enabled, enabled = enabled || ready, onCheckedChange = { next ->
                                if (!next || ready) {
                                    enabled = next
                                    prefs.edit().putBoolean("enabled", next).apply()
                                    NexroQuickSettingsTile.refresh(activity)
                                }
                            })
                        }
                        if (!ready) Text("Finish setup below before turning protection on.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionHeading("01", "SETUP")
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SetupRow("Notification access", "Lets Android detect selected app notifications", notificationAccess) {
                            activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                        HorizontalDivider(color = DividerColor)
                        SetupRow("Nexro notifications", "Required to show safe replacement notices", postPermission) {
                            activity.requestPostNotificationPermission()
                        }
                        OutlinedButton(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Refresh permission status")
                        }
                    }
                }

                SectionHeading("02", "APPS TO PROTECT")
                Text("Select apps installed on this phone. Protection applies to supported text-bearing notifications from the whole app, not individual contacts.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        protectedApps.forEachIndexed { index, app ->
                            val isInstalled = installed[app.packageName] == true
                            Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = RoundedCornerShape(13.dp), color = Raised, modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(app.title.first().toString(), color = Mint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(app.title, fontWeight = FontWeight.SemiBold)
                                    Text(if (isInstalled) "Installed" else "Not installed", color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(checked = appStates[app.key] == true, enabled = isInstalled, onCheckedChange = { persist(app.key, it) })
                            }
                            if (index != protectedApps.lastIndex) HorizontalDivider(color = DividerColor)
                        }
                    }
                }

                SectionHeading("03", "QUICK SETTINGS")
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("One-swipe activation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Add the Nexro Ultra tile from Android Quick Settings edit mode. It uses the same saved protection switch.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { showTileHelp = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("How to add tile")
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = Raised, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PRIVACY LIMITS", color = Mint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Replacing a source notification may remove its reply buttons. Notifications without recognizable message text are left untouched. Processing stays on-device; notification contents are not uploaded.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("NEXRO ULTRA  •  PRIVATE BY DESIGN", color = Muted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
            }
        }
        if (showTileHelp) {
            AlertDialog(
                onDismissRequest = { showTileHelp = false },
                title = { Text("Add Nexro Ultra to Quick Settings") },
                text = { Text("1. Swipe down from the top twice.\n2. Tap Edit or the pencil icon.\n3. Find Nexro Ultra in available tiles.\n4. Drag it into active tiles and tap Done.") },
                confirmButton = { TextButton(onClick = { showTileHelp = false }) { Text("Got it") } }
            )
        }
    }
}

@Composable
private fun SectionHeading(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(number, color = Mint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        Text(title, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SetupRow(title: String, description: String, ready: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (ready) "READY" else "NEEDED", color = if (ready) Mint else Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        if (!ready) TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) { Text("Open settings") }
    }
}
