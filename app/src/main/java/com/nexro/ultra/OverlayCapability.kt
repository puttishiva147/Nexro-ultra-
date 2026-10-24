package com.nexro.ultra

import android.content.Context
import android.os.Build
import android.provider.Settings

data class OverlayCapability(
    val drawOverlaysGranted: Boolean,
    val applicationOverlayTypeAvailable: Boolean,
    val canVerifySystemUiHeadsUpLayer: Boolean = false,
    val explanation: String
)

object OverlayCapabilityChecker {
    fun inspect(context: Context): OverlayCapability {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        val typeAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        val message = when {
            !granted -> "Overlay permission is missing. Grant it before running an app-window overlay test."
            !typeAvailable -> "This implementation requires Android 8.0+ TYPE_APPLICATION_OVERLAY."
            else -> "App overlay permission is available. This does not verify coverage of SystemUI heads-up notifications; public APIs do not expose their exact rendered bounds."
        }
        return OverlayCapability(granted, typeAvailable, false, message)
    }
}
