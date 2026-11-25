# Test and validation plan

## Automated checks

- Compile all Android Kotlin and Java sources.
- Run JVM unit tests.
- Verify the pinned native library checksums for arm64-v8a, armeabi-v7a, x86 and x86_64.
- Assemble a debug APK from committed source.
- Verify that the expected APK is published from the exact commit named by its release tag.

## Existing release and feedback

- v0.1.0: CI unit tests and debug assembly passed; APK published.
- v0.1.1: interaction-polish APK published. User tested the APK and supplied follow-up feedback, which is being incorporated into the Phase 2 implementation.
- No claim is made that Phase 2 has passed CI or physical-device testing until the relevant results are recorded below.

## Phase 2 unit tests

- SOCKS5 UDP decoder accepts IPv4 destination headers.
- SOCKS5 UDP decoder accepts domain-name destination headers.
- SOCKS5 UDP encoder emits IPv6 source-address headers and preserves port/payload.
- Malformed and fragmented UDP frames are rejected.
- Existing SOCKS5 server port test remains passing.

## Phase 2 physical two-device acceptance

Use two Android phones on the same test build.

1. Install the Phase 2 APK on both phones.
2. On phone A, enable the normal Android Wi-Fi hotspot and activate Share.
3. On phone B, join phone A's hotspot through Android Wi-Fi settings.
4. Activate Receive and approve Android's VPN consent dialog.
5. Verify the Receive VPN notification reports a running tunnel and the Receive indicator turns green only after endpoint/upstream and native-engine checks pass.
6. Open multiple websites and apps on phone B; confirm TCP traffic works.
7. Verify DNS resolution from ordinary apps on phone B.
8. Verify UDP-dependent traffic (for example, QUIC/HTTP3 where available) and repeat with a UDP diagnostic test.
9. Verify IPv6 when the hotspot and upstream connection offer IPv6; confirm no unintended IPv6 bypass.
10. Keep multiple receiver devices connected and verify independent TCP sessions and UDP associations.
11. Stop Share; confirm Receive reports unhealthy and the tunnel stops or reports the unavailable endpoint.
12. Restore Share and Wi-Fi; verify the receiver can recover after reconnect.
13. Stop Receive; verify the VPN is removed and the app remains open.
14. Deny VPN consent and confirm the app reports that Receive cannot start without consent.
15. Repeat with screen off/background operation and record Samsung/One UI restrictions, battery use and service stability.

## Failure and privacy checks

- Wi-Fi loss stops the tunnel and clears the healthy status.
- An unavailable SOCKS endpoint or failed upstream CONNECT probe keeps the indicator red.
- Native tunnel startup failure does not show a green status.
- Stopping either role does not stop the other role or close the Activity.
- No traffic payload, browsing history or credentials are written to logs or uploaded.
- The app does not silently enable Wi-Fi or modify hotspot DHCP leases.
- No kill-switch/lockdown behavior is currently implemented; if the VPN service exits, Android may restore the device's normal network route. This limitation must remain explicit.

## CI and device results

- Phase 2 workflow: passed (run [45529698909](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/actions/runs/45529698909)).
- Phase 2 automated unit tests: passed.
- Phase 2 native library checksum validation: passed for all four Android ABIs.
- Phase 2 APK assembly and release: passed; [v0.2.0 test APK](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.2.0) published.
- Physical two-device TCP/UDP/DNS routing: not yet verified; user testing is the next acceptance step.
- Physical Samsung/One UI background, Quick Settings and notification behavior: not yet verified.
