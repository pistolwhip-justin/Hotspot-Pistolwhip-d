# Development plan

## Phase 1 — local-link prototype

**Status: v0.1.0 published.** The initial app shell, independent controls, foreground lifecycle, multi-client SOCKS5 TCP listener, receiver Wi-Fi gateway discovery and endpoint probe were delivered. Physical acceptance is tracked separately from CI.

## Phase 1.1 — interaction polish and initial user testing

**Status: v0.1.1 published; APK tested and feedback received.**

- Quick Settings tile taps toggle only their matching service and do not open or foreground the Activity.
- Long-pressing either tile opens the app through its tile settings activity.
- Tapping the persistent service notification opens the app.
- App switches refresh from persisted service state when the app resumes.
- UI feedback is incorporated into the Phase 2 branch: app-name heading, shared app-mark tile icons, tighter margins, larger switch/text separation, smaller system-scaled typography and a scrollable screen.
- AI remains outside the mass-testing implementation and critical path.

## Phase 2 — device-wide VPN tunnel

**Status: v0.2.0 test APK published; CI build and automated tests passed; physical acceptance pending.**

Implementation scope:

- Add Android VpnService consent and receiver-side TUN lifecycle.
- Bind the VPN's underlying network to the Wi-Fi network connected to the Share phone.
- Use HevSocks5Tunnel 2.20.0 (MIT) as a pinned native TUN-to-SOCKS5 engine. Download its Android ABI libraries during build and verify their SHA-256 checksums.
- Add SOCKS5 UDP ASSOCIATE support to the Share service alongside TCP CONNECT.
- Route IPv4, IPv6 and DNS traffic from the receiver through the Share endpoint.
- Exclude the app's own package from its VPN so the proxy transport does not recurse into TUN.
- Keep service health red until the SOCKS upstream probe succeeds and the native tunnel reports running.
- Handle VPN consent denial, Wi-Fi disconnect, endpoint failure, native startup failure and explicit stop.
- Add unit tests for SOCKS5 UDP frame parsing/encoding.
- Update README, architecture, requirements, decision record, test plan and versioned release workflow.

**Automated readiness gate:**
1. CI fetches and verifies each pinned native ABI library.
2. Kotlin/Java compilation succeeds.
3. SOCKS5 UDP codec unit tests and the existing test suite pass.
4. Debug APK assembles from committed source.
5. Release workflow publishes the exact APK from the source commit under a new immutable versioned tag.

**Physical acceptance gate:**
1. Install on two real Android phones.
2. Join the Share phone's normal Wi-Fi hotspot from the Receive phone.
3. Approve Android VPN permission and activate Receive.
4. Verify web/TCP traffic, DNS resolution and UDP-dependent traffic through the Share phone.
5. Verify IPv6 when supported, multiple receiver sessions, Wi-Fi loss/reconnect, Share stop/restart and VPN stop.
6. Record any Samsung/One UI-specific behavior separately from CI.

A CI APK is a test build, not proof of the physical acceptance gate. The project must not claim that a full internet tunnel works until the two-device traffic tests pass.

## Phase 3 — automation and resilient lifecycle

- Opt-in auto-receive for previously approved networks.
- Explicit first-time confirmation and permission-aware behavior.
- Android-stop restart notification/action where the platform reliably exposes the event.
- Wi-Fi enable UX and verified health state.

Gate: device tests cover radio state, network switch, OS service kill, user stop, reboot and permission denial.

## Phase 4 — One UI polish and distribution readiness

- Accessibility and responsive-layout review.
- Samsung device compatibility matrix.
- Performance, battery and background-execution work.
- Final original Pistol Whip Productions identity/assets and distribution metadata.

Gate: target-device acceptance, accessibility review and reproducible release.

## Future AI concepts — deferred, not approved or implemented

AI remains outside the current mass-testing scope. Each concept requires separate approval, requirements review and privacy/security assessment.

- **Network Intelligence:** interpret locally collected latency, packet-loss, throughput and stability measurements without inspecting traffic payloads.
- **Network Map / Radar:** visualize locally discovered devices, link state and connection quality.
- **Network Automation:** translate user-defined rules into transparent, reviewable local automations.
- **Voice troubleshooting assistant:** optional conversational interface for network diagnostics.
- **Performance recommendations:** explain battery, signal and throughput trade-offs using local measurements.

No remote AI service, external model dependency, traffic inspection, browsing-history collection or credential collection is part of the approved scope.
