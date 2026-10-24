# Nexro Ultra notification privacy engine — capability report

## Implemented tier

**Tier A: ordinary Android application.** This repository does not include a checked-out AOSP tree, a privileged/system-signed build, an OEM display integration, or a documented Samsung region-privacy API. The app therefore does not claim Tier B or Tier C capability.

## Public Android behaviour verified from documentation/source

- `NotificationListenerService` receives posted, removed, and ranking events. A posted notification is not evidence that a heads-up popup is actually visible.
- AOSP SystemUI has internal heads-up lifecycle machinery (for example, `HeadsUpManager` in the AOSP source tree). It is not a public third-party API contract; its implementation changes between Android versions.
- `TYPE_APPLICATION_OVERLAY` requires `SYSTEM_ALERT_WINDOW`, is above ordinary application windows, and is explicitly below critical system windows such as the status bar or IME. The system can also change its position, size, or visibility.
- Android graphics documentation describes app/system surfaces being composited by SurfaceFlinger, with Hardware Composer (HWC) handling supported composition work. These public APIs do not expose a generic per-notification optical viewing-angle privacy switch.
- No Samsung proprietary SystemUI/display source or public third-party API for its partial-region optical privacy feature is present in this project. The internal mechanism described in the task is an architectural hypothesis, not an asserted Samsung implementation.

References:
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/src/com/android/systemui/statusbar/policy/HeadsUpManager.java
- https://source.android.com/docs/core/graphics/architecture
- https://source.android.com/docs/core/graphics/implement-hwc

## Source implementation

- `NexroNotificationListener`: tracks post/update/removal and reconciles active notifications without calling cancel, snooze, clear, or repost APIs.
- `NotificationEventRepository`: serializes lifecycle events through a channel and publishes immutable snapshots through `StateFlow`.
- `NotificationPrivacyReducer`: handles post, removal, reconcile, visibility, mask request, mask result, and reveal transitions keyed by `StatusBarNotification.key`.
- `PrivacyEligibilityEngine`: deliberately refuses to represent a posted notification as a confirmed heads-up and reports exact masking unsupported without verified popup visibility and bounds.
- `PrivacySettingsStore`: DataStore persistence for trigger/armed settings, mirrored to legacy preferences for the existing trigger service.
- Dashboard observer: shows real records after notification access is granted. Content is hidden in Nexro's viewer by default.
- Preview: supports simulated masking, update, and multiple mock notifications. It is explicitly labelled simulation, not a device test.
- Volume-key double press is experimental and device-dependent. Quick Settings is the supported user-activated toggle, but it arms/disarms Nexro; it does not magically grant SystemUI access.

## Acceptance status

| Condition | Status | Reason |
|---|---|---|
| Observe a real notification | Implemented; needs user-granted notification access and device verification | Listener callbacks track actual posted records |
| Confirm genuine heads-up visibility | Unsupported on Tier A | Listener API does not guarantee popup visibility |
| Obtain exact rendered bounds | Unsupported on Tier A | No public notification-listener bounds API |
| Trigger detection | Partial | Quick Settings tile is supported; global volume keys are device-dependent |
| Cover the exact SystemUI popup with a black mask | Unsupported/unverified | Overlay permission does not establish correct layer ordering or geometry |
| Keep original notification posted | Implemented by design | No cancel/snooze/repost operations in hiding path |
| Reveal without recreating notification | No OS popup mask exists to remove on Tier A | Viewer reveal only affects Nexro's own screen |
| Multiple notification lifecycle tracking | Implemented in reducer; requires runtime verification | Records are keyed by notification key |
| Rotation/expanded-popup geometry | Unsupported until a system-integrated renderer exists | No genuine geometry to recompute |
| Samsung optical partial-region privacy | Unsupported | Requires OEM hardware/display integration |

## Test reporting

Unit tests cover event state, duplicate posts, removal, unknown-visibility refusal, update preservation, multiple keys, and unsupported geometry. GitHub Actions runs `testDebugUnitTest` before `assembleDebug`.

No physical-device or emulator run is claimed by this report. Real-device verification still needs to be performed on the target Android phone with notification access enabled. A green build does not prove SystemUI masking.
