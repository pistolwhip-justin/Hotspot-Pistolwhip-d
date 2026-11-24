# Development plan

## Phase 1 — clean-room foundation and local-link prototype

**Status: CI build and unit tests passed; v0.1.0 test APK published. Physical two-device acceptance remains pending.**

Deliver app shell, true-black Material 3 UI, independent Share/Receive toggles, foreground lifecycle, multi-client SOCKS5 listener, receiver Wi-Fi gateway discovery/endpoint probe, initial Quick Settings tiles and build/release automation.

Gate: CI build and automated tests pass; APK is published on a versioned GitHub Release and attached in chat; user can test UI/service/link behavior. Full internet tunneling is explicitly outside this gate.

## Phase 1.1 — mass-testing interaction and lifecycle polish

**Status: In development.**

- Quick Settings tile taps toggle only their matching service. They must not open, foreground, or otherwise bring the Activity to the front.
- Long-pressing either tile opens the app using Android's tile settings-activity affordance.
- Tapping the persistent service notification opens the app.
- Tapping the separate status indicator performs no action.
- When the user later opens or returns to the app, the displayed Share/Receive switches refresh from persisted service state.
- Keep AI out of the implementation and critical path.

Gate: automated build/tests pass; tile and notification interactions are exercised on Samsung/Android devices; a new immutable APK release is published. No physical-device verification is claimed until actually performed.

## Phase 2 — complete traffic tunnel

Deliver Android VpnService integration and independently sourced/implemented TUN-to-SOCKS transport supporting TCP/UDP, routing, DNS, reconnects, failure behavior and traffic tests.

Gate: two-device test proves receiver traffic reaches the internet through the sharing phone.

## Phase 3 — automation and resilient lifecycle

Deliver opt-in auto-receive for previously approved networks, explicit first-time confirmation, Android-stop restart notification/action, Wi-Fi enable UX and verified health state. The status indicator itself remains non-interactive.

Gate: device tests cover radio state, network switch, OS service kill, user stop, reboot and permission denial.

## Phase 4 — One UI polish and distribution readiness

Deliver refined layouts, tile behavior, accessibility, original Pistol Whip Productions identity/assets, app metadata, compatibility matrix and performance work.

Gate: target-device acceptance, accessibility review and reproducible release.

## Future AI concepts — deferred, not approved or implemented

AI is explicitly outside the current mass-testing scope. These are ideas only; each requires a separate approval, requirements review, privacy/security assessment and a decision on whether it can run locally.

- **Network Intelligence:** interpret locally collected latency, packet-loss, throughput and stability measurements; explain likely causes without inspecting traffic payloads.
- **Network Map / Radar:** visualize locally discovered devices, link state and connection quality.
- **Network Automation:** translate user-defined rules into transparent, reviewable local automations.
- **Voice troubleshooting assistant:** optional conversational interface for network diagnostics.
- **Performance recommendations:** explain battery, signal and throughput trade-offs using local measurements.

No remote AI service, external model dependency, traffic inspection, browsing-history collection or credential collection is part of the approved scope.
