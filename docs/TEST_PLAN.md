# Test and validation plan

## Phase 1 — automated checks

- SSID construction exactly produces `DIRECT-WHIP-[phonename]` without an extra `IP-` segment and remains within 32 bytes.
- Generated passphrases are 20 characters and satisfy the uppercase/lowercase/digit/special-character policy.
- Wi-Fi QR parser accepts a standard WPA payload for a DIRECT-WHIP network and rejects unrelated or weak credentials.
- QR field escaping round-trips the supported Wi-Fi characters.
- Existing SOCKS5 TCP and UDP codec tests pass.
- Android Kotlin/Java compilation succeeds.
- Native HevSocks5Tunnel ABI checksums pass for all supported ABIs.
- Debug APK assembles from committed source.

## Phase 2 — automated checks

- Receive rejects credentials that do not match the DIRECT-WHIP prefix or passphrase policy.
- Receive requests a local-only Wi-Fi network (removes INTERNET capability) through Android with the selected SSID/password and does not enumerate saved networks.
- Devices without local-only STA concurrency report unavailable and use the Android Wi-Fi settings fallback.
- Receive validates the associated SSID and SOCKS5 upstream before requesting the VPN tunnel.
- VPN/native tunnel startup failure, consent denial and network loss leave the Receive health indicator red.
- The versioned release workflow publishes the expected APK from the exact commit identified by its tag.

## Physical two-device acceptance

Use two real Android phones and the same test APK.

1. Install the APK on both devices. If the signing identity changed, uninstall the previous test build first.
2. On phone A, turn Share on. Confirm Android creates the Wi-Fi Direct group and the displayed SSID is exactly `DIRECT-WHIP-[phonename]`.
3. Confirm the conventional Android Hotspot setting was not enabled by the app.
4. Confirm the Share panel shows the actual passphrase and P2P endpoint only after group/listener readiness.
5. Verify the credential QR scans as a Wi-Fi configuration and can be shared through Android's standard Sharesheet. Confirm no destination is hard-coded and the payload contains only SSID/password.
6. On phone B, use the app's QR scanner and connect flow. Confirm Android presents its network approval/association UI.
7. If Android reports the local-only Wi-Fi request unavailable, record the device/OEM behavior and test the Android Wi-Fi settings fallback.
8. Repeat with manual SSID/password entry.
9. Confirm Receive does not show healthy before the expected group is associated and the Share endpoint is reachable.
10. Approve Android VPN consent. Verify the VPN notification and native engine state.
11. From ordinary apps on phone B, verify TCP web traffic, DNS resolution and UDP-dependent traffic through phone A.
12. Verify IPv6 when the group/upstream network supports it and confirm no unintended IPv6 bypass.
13. Repeat with multiple receivers and independent connections.
14. Stop Share; confirm Receive reports unhealthy and traffic tunnel stops or reports endpoint loss.
15. Restore Share and the connection; verify reconnect behavior.
16. Stop Receive; confirm VPN removal and that the app remains open.
17. Test permission denial, Wi-Fi off, Android association cancellation, invalid QR/manual credentials, screen-off/background operation and Samsung/One UI behavior.

## Results

- v0.1.0 and v0.1.1: historical local-link prototypes.
- v0.2.0: historical conventional-hotspot VPN prototype; it does not implement the current Wi-Fi Direct topology.
- Phase 1/2 source commit e69af99a65031cf7b2238346a37b3356da1d04e6: GitHub Actions run [46099313775](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/actions/runs/46099313775) completed successfully. Pinned native ABI checksum validation passed; `testDebugUnitTest` and `assembleDebug` passed; the APK was assembled and uploaded as artifact 15244827941. Gradle reported 42 actionable tasks. This is a CI result, not a test count or physical-device result.
- v0.3.0 release publication: pending merge to main and the dedicated versioned release workflow.
- Physical two-device Wi-Fi Direct, TCP/UDP/DNS and Samsung background behavior: pending user testing; not inferred from CI.
