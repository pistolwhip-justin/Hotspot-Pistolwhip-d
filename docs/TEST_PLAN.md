# Test and validation plan

## Automated checks
- Compile all Android Kotlin sources.
- Run JVM unit tests.
- Assemble a debug APK from committed source.

## CI result — 2026-11-24
- `testDebugUnitTest`: passed for v0.1.0.
- `assembleDebug`: passed for v0.1.0.
- APK published as v0.1.0 and its SHA-256 matches the release asset metadata.
- No physical-device install or runtime tests have been performed.

## Phase 1.1 interaction checks
1. Install the new APK over v0.1.0 and confirm the app identity remains the same.
2. Add both Share and Receive tiles to Quick Settings.
3. Tap Share while the app is closed; verify Share starts and the app does not open.
4. Tap Share again; verify Share stops and the app does not open.
5. Repeat for Receive.
6. With either role active, long-press its tile and verify Android opens the app.
7. Tap the persistent service notification and verify the app opens.
8. Return to the app after toggling a service from Quick Settings; verify its switch reflects the latest state.
9. Stop either role from inside the app; verify the Activity remains open and the other role is unaffected.
10. Verify the status indicator has no tap action, if/when that indicator is implemented.

## Phase 2 two-device checks
1. Install on two physical Android phones.
2. Start Share on phone A and join its hotspot normally from phone B.
3. Start Receive on phone B.
4. Connect at least two receiver clients and verify independent SOCKS5 sessions.
5. Verify the current phase's explicitly supported traffic behavior and document failures.

## Not validated by current CI
- Samsung-specific long-press presentation and Quick Settings behavior.
- Notification tap behavior on physical One UI devices.
- Full device-wide routing through the sharing phone.
- TCP/UDP packet forwarding through Android VpnService.
- OS-kill restart prompt and auto-receive behavior.
- Samsung Wi-Fi/hotspot control behavior.
