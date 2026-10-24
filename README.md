# Nexro Ultra — Android starter

**Nexro Ultra is an independent project and brand. It is not part of VEIL.**

A standalone Android application scaffold for on-device notification privacy.

## Included
- Dedicated settings screen and independent ON/OFF preference.
- Per-app selection for WhatsApp, Telegram, and Instagram.
- Notification access and Android 13+ notification permission status.
- A NotificationListenerService that detects selected-app notifications with text extras, dismisses the original notification, and posts a safe replacement notice.
- This implementation does not log, store, or upload notification message text.

## Important Android limitation
Ordinary third-party Android apps cannot reliably edit message text inside another app's posted notification or precisely paint a black mask over its private layout. This starter uses a fallback: cancel the original and post a Nexro Ultra replacement. This can remove original actions, reply controls, grouping, or other behavior. It may not cover every notification style and must be tested on target devices.

## Activation triggers
The UI stores separate double-tap and volume-button settings, but global detection is not implemented in this starter. Ordinary apps do not have a universal system-wide double-tap or volume-key interception API.

## Build
Open this project in Android Studio, use JDK 17 and Android SDK 35, and allow Gradle to sync dependencies. Build the app debug variant. No Android/Gradle build has been run in this environment, so build success is not claimed.

## Package and version
- Application ID: com.nexro.ultra
- Version: 0.1.0 (versionCode 1)
