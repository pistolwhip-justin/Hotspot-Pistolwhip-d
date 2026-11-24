# Test and validation plan

## Automated checks
- Compile all Android Kotlin sources.
- Run JVM unit tests.
- Assemble a debug APK from committed source.

## CI result — 2026-11-24
- `testDebugUnitTest`: passed.
- `assembleDebug`: passed.
- APK published as v0.1.0 and its SHA-256 matches the release asset metadata.
- No physical-device install or runtime tests have been performed.

## Phase 1 manual checks (required on two physical Android phones)
1. Install the APK on both devices.
2. Confirm app label in installer, Settings > Apps, launcher and app drawer.
3. Enable hotspot on phone A; start Share; confirm service remains active and UI stays open.
4. Connect phone B through normal Android Wi-Fi settings; start Receive.
5. Confirm receiver discovers the gateway and reaches the SOCKS5 listener.
6. Connect at least two client sessions and confirm they are accepted independently.
7. Stop Receive; verify Activity remains open and Share remains active.
8. Stop Share; verify Activity remains open and Receive remains independently controllable.
9. Test Android notification permission denied/granted.
10. Test both Quick Settings tiles and long-press behavior.

## Not validated by Phase 1
- Full device-wide routing through the sharing phone.
- TCP/UDP packet forwarding through Android VpnService.
- OS-kill restart prompt and auto-receive behavior.
- Samsung-specific Wi-Fi/hotspot control behavior on every supported model.
