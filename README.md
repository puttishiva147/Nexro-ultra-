# Nexro Ultra — Android

Nexro Ultra is an independent project and brand. It is not part of VEIL.

## Current implementation (0.2.0)
- Dedicated dark Jetpack Compose UI with a custom Nexro vector brand icon and independent ON/OFF control.
- Per-app switches for WhatsApp, WhatsApp Business, Telegram, and Instagram.
- Notification access and Android 13+ notification permission status, refreshed automatically when returning from system settings.
- Android Quick Settings tile for toggling the protection preference; if setup is incomplete, tapping the tile opens the app setup screen. Manual tile placement is explained in-app.
- On-device notification listener that replaces selected message notifications with safe notices and does not copy message bodies into replacement notifications.
- GitHub Actions workflow to build a debug APK and publish it as a workflow artifact.

## Important limitations
Android does not provide ordinary third-party apps with a universal API to rewrite another app's notification text or precisely overlay a black mask on every notification layout. This implementation uses a fallback: cancel the source notification and post a safe replacement. That can remove reply actions, buttons, grouping, or other original behavior. Some notifications may not expose text in standard extras and will be left untouched.

The Quick Settings tile toggles the preference; Android notification access must still be granted separately. Double-tap and global volume-button triggers are not implemented because there is no reliable general API for them on ordinary apps. The UI does not claim they work.

## Privacy
The listener processes notifications locally. It does not upload message text or write it to logs. The sender/title may be included in the replacement title; message body is not copied. Protection applies to text-bearing notifications from each selected app, not individual contacts.

## Build APK
A push to `main` affecting Kotlin, Gradle, XML, or the workflow triggers GitHub Actions. You can also open **Actions → Build Nexro Ultra APK → Run workflow**. After a successful run, download the `nexro-ultra-debug-apk` artifact from the run summary. It is a debug-signed APK, not a production release signature.

## Build locally
Use Android Studio, JDK 17, Android SDK 35, and Gradle 8.9; run `gradle assembleDebug`. No successful build is claimed until the workflow completes successfully.
