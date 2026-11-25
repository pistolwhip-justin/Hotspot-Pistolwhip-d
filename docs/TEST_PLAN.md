# Test and validation plan

## Automated checks

- Compile all Android Kotlin and Java sources.
- Run JVM unit tests.
- Test SSID normalization and the `DIRECT-WHIP-` prefix.
- Test passphrase length and character composition.
- Test layered service/group/listener/association/VPN/tunnel state transitions.
- Verify pinned native library checksums for all supported Android ABIs.
- Assemble a debug APK from committed source.
- Verify release tag points to the exact source commit and release contains the expected APK.

## Historical releases

- v0.1.x and v0.2.0 were built around the earlier conventional-hotspot design.
- v0.2.0 does not create the required Wi-Fi Direct group and must not be treated as a current-architecture test build.
- Earlier conventional-hotspot test instructions are superseded.

## Phase 1 — Wi-Fi Direct Share physical acceptance

1. Install the same build on two physical Android phones.
2. Confirm Android's conventional Hotspot setting is off on the Share phone.
3. Enable Share.
4. Confirm Android reports a created Wi-Fi Direct group.
5. Confirm the actual SSID begins `DIRECT-WHIP-` and includes the normalized `IP-[phonename]` suffix.
6. Confirm passphrase is 18–20 characters, includes uppercase/lowercase, at least five digits and at least five allowed special characters.
7. Confirm the group is visible to the second device and it can associate.
8. Confirm SOCKS5 listener is bound to the P2P interface and accepts only expected P2P peers.
9. Confirm failed group creation never shows a fake active/healthy Share state.
10. Confirm Share expands only after group/listener success and collapses on stop.

## Phase 2 — Receive association

1. Test with Wi-Fi disabled and enabled.
2. Confirm Receive requests the available Wi-Fi enablement capability and waits ten seconds before SSID inspection.
3. Connect to a matching saved group and verify Android's own reconnection is detected.
4. Test QR scan for a new group.
5. Test manual SSID/password entry for a new group.
6. Confirm the in-app visual controls hand off to Android and Android performs actual association/saving.
7. Test wrong password, unrelated SSID, missing group, cancellation and Wi-Fi loss.
8. Confirm the VPN does not start while credentials are being entered or before the expected group is associated.

## Phase 3 — VPN/tunnel acceptance

1. Confirm Android VPN consent is requested when required.
2. Confirm VPN interface establishment precedes native tunnel startup.
3. Confirm the app's VPN does not recurse through itself.
4. Verify end-to-end TCP, UDP and DNS from ordinary receiver apps.
5. Verify IPv6 when supported and ensure no unintended bypass.
6. Test multiple receivers, network loss/reconnect, Share stop/restart and VPN revocation.
7. Confirm all health states clear correctly when a layer fails.

## Phase 4 — sharing and diagnostics

1. Compare displayed SSID, group information, RSSI/link data and other available verbose values with Android's network diagnostics.
2. Confirm statistics refresh every five seconds and on network callbacks.
3. Confirm QR payload is valid and contains only the SSID/passphrase.
4. Confirm copy contains only the SSID/passphrase.
5. Confirm Android Sharesheet offers compatible installed apps and does not hard-code destinations.
6. Confirm no destination is selected or sent to without user action.

## Phase 5 — resilience and privacy

1. Simulate unexpected service termination and verify up to three restart attempts ten seconds apart when OS policy allows.
2. Confirm log dialog appears only after the retry policy exhausts and when the process remains able to show it.
3. Confirm Cancel, Share & Discard and Share & Save behavior.
4. Inspect shared/saved logs for redaction of credentials and identifying values.
5. Confirm no automatic upload.
6. Test screen-off/background and Samsung/One UI behavior; document OS-enforced limitations.

## Current results

- Historical v0.2.0 CI build/unit tests/native checksums passed, but its Share service did not create a Wi-Fi Direct group.
- New Wi-Fi Direct implementation and all physical acceptance checks: pending.
- No end-to-end Wi-Fi Direct/VPN/tunnel success is claimed until the relevant physical gates pass.
