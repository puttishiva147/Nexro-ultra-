package com.nexro.ultra

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.launch

private val Ink = Color(0xFF080A09)
private val Panel = Color(0xFF141815)
private val Raised = Color(0xFF202722)
private val Mint = Color(0xFFA4E5BE)
private val Muted = Color(0xFF9AA69E)
private val Line = Color(0xFF29312C)

class MainActivity : ComponentActivity() {
    var resumeCount by mutableIntStateOf(0)
        private set

    override fun onResume() {
        super.onResume()
        resumeCount++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexroDashboard(this) }
    }
}

@Composable
private fun NexroDashboard(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("nexro_ultra_prefs", Context.MODE_PRIVATE) }
    val settingsStore = remember { PrivacySettingsStore(activity) }
    val settings by settingsStore.settings.collectAsState(initial = PrivacySettings())
    val coroutineScope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var trigger by remember { mutableStateOf(prefs.getString("trigger", "quick_settings") ?: "quick_settings") }
    var previewHidden by remember { mutableStateOf(false) }
    val refresh = activity.resumeCount
    val observedRecords by NotificationEventRepository.get(activity).records.collectAsState()
    val accessibilityEnabled = remember(refresh) {
        val expected = ComponentName(activity, NexroPrivacyAccessibilityService::class.java)
        Settings.Secure.getString(activity.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
    val notificationAccessEnabled = remember(refresh) {
        val expected = ComponentName(activity, NexroNotificationListener::class.java)
        Settings.Secure.getString(activity.contentResolver, "enabled_notification_listeners")
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
    val overlayCapability = remember(refresh) { OverlayCapabilityChecker.inspect(activity) }

    LaunchedEffect(settings) {
        enabled = settings.triggerEnabled
        trigger = settings.trigger
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, onPrimary = Ink, background = Ink, surface = Panel,
        onSurface = Color(0xFFF4F7F4), secondary = Muted,
        surfaceVariant = Raised, outline = Line
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
                        Text("Instant notification privacy", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("v0.5.0", color = Muted, style = MaterialTheme.typography.labelSmall)
                }

                Surface(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(26.dp), color = Panel) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("PRIVACY TRIGGER", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(if (enabled) "Trigger enabled" else "Trigger disabled", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(if (accessibilityEnabled) "Accessibility service connected" else "Turn on Nexro accessibility to detect the key press", color = if (accessibilityEnabled) Mint else Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(shape = CircleShape, color = if (enabled) Color(0xFF254735) else Raised, modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (enabled) "ON" else "OFF", color = if (enabled) Mint else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        HorizontalDivider(color = Line)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Enable privacy trigger", fontWeight = FontWeight.SemiBold)
                                Text("Quick Settings tile is the supported tap trigger. Volume-key double-press is experimental and device-dependent.", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = enabled, onCheckedChange = {
                                enabled = it
                                prefs.edit().putBoolean("enabled", it).apply()
                                coroutineScope.launch { settingsStore.setEnabled(it) }
                                NexroQuickSettingsTile.refresh(activity)
                            })
                        }
                    }
                }

                SectionHeading("01", "CHOOSE YOUR TRIGGER")
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pick the button you want to double-press. No app-by-app selection.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        TriggerOption("Quick Settings tile", "Tap the Nexro tile in Android Quick Settings", trigger == "quick_settings") {
                            trigger = "quick_settings"
                            prefs.edit().putString("trigger", trigger).apply()
                            coroutineScope.launch { settingsStore.setTrigger(trigger) }
                        }
                        HorizontalDivider(color = Line)
                        TriggerOption("Volume Down × 2 (experimental)", "Android may not deliver global volume keys to accessibility services", trigger == "volume_down") {
                            trigger = "volume_down"
                            prefs.edit().putString("trigger", trigger).apply()
                            coroutineScope.launch { settingsStore.setTrigger(trigger) }
                        }
                        HorizontalDivider(color = Line)
                        TriggerOption("Volume Up × 2 (experimental)", "Device-dependent; not a guaranteed system shortcut", trigger == "volume_up") {
                            trigger = "volume_up"
                            prefs.edit().putString("trigger", trigger).apply()
                            coroutineScope.launch { settingsStore.setTrigger(trigger) }
                        }
                    }
                }

                SectionHeading("02", "TEST PREVIEW")
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Example notification", color = Muted, style = MaterialTheme.typography.labelMedium)
                        if (!previewHidden) {
                            Surface(shape = RoundedCornerShape(16.dp), color = Raised, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("WhatsApp  •  now", color = Muted, style = MaterialTheme.typography.labelSmall)
                                    Text("Aarav", fontWeight = FontWeight.Bold)
                                    Text("Hey, are we still meeting today?", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(16.dp), color = Color.Black, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(shape = CircleShape, color = Raised, modifier = Modifier.size(34.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text("N", color = Mint, fontWeight = FontWeight.Bold) }
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("NOTIFICATION HIDDEN", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text("Message content is covered", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("•••", color = Color.White)
                                }
                            }
                        }
                        Button(onClick = { previewHidden = !previewHidden }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text(if (previewHidden) "Show normal notification" else "Test black hide bar")
                        }
                        Text("This is a visual demo only. It does not alter a real notification.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionHeading("03", "REQUIRED ACCESS")
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (accessibilityEnabled) "Nexro accessibility is ON" else "Enable Nexro accessibility", fontWeight = FontWeight.SemiBold)
                        Text("Notification access lets Nexro observe posted, updated and removed notifications. Accessibility is only for supported trigger experiments; volume-key delivery varies by device.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text(if (notificationAccessEnabled) "Notification access settings" else "Grant notification access")
                        }
                        Button(onClick = { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text(if (accessibilityEnabled) "Accessibility settings" else "Enable accessibility trigger (optional)")
                        }
                        Button(onClick = { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.packageName))) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text(if (overlayCapability.drawOverlaysGranted) "Overlay permission settings" else "Grant overlay permission")
                        }
                        Text(overlayCapability.explanation, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionHeading("04", "LIVE NOTIFICATION OBSERVER")
                Surface(shape = RoundedCornerShape(22.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${observedRecords.values.count { !it.removed }} active records observed", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("A notification record is not proof that its heads-up popup is visible. Exact SystemUI bounds are unavailable to this ordinary app.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        if (observedRecords.isEmpty()) {
                            Text("No notification events observed in this process yet. Grant notification access, then wait for a new notification.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        } else {
                            observedRecords.values.filter { !it.removed }.takeLast(5).reversed().forEach { item ->
                                HorizontalDivider(color = Line)
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(item.title?.takeIf { it.isNotBlank() } ?: item.packageName, fontWeight = FontWeight.SemiBold)
                                    Text(item.text.orEmpty().take(140), color = Muted, style = MaterialTheme.typography.bodySmall)
                                    Text("State: ${item.phase} • Heads-up visibility: ${item.popupVisibility} • Exact mask: unsupported", color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = Raised, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("HONEST CAPABILITY STATUS", color = Mint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Nexro tracks real notification lifecycle events and never cancels or replaces source notifications. Android's public app APIs do not expose genuine heads-up rendered bounds or guarantee that an app overlay is above SystemUI. The preview is simulated; exact heads-up masking is currently unsupported/unverified on this tier.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("NEXRO ULTRA  •  PRIVATE BY DESIGN", color = Muted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TriggerOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (selected) Raised else Panel, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected = selected, onClick = onClick)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
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
