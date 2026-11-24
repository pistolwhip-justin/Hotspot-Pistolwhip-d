# Development plan

## Phase 1 — clean-room foundation and local-link prototype
Deliver app shell, true-black Material 3 UI, independent Share/Receive toggles, foreground lifecycle, multi-client SOCKS5 listener, receiver Wi-Fi gateway discovery/endpoint probe, initial Quick Settings tiles and build/release automation.

Gate: CI build and automated tests pass; APK is published on a versioned GitHub Release and attached in chat; user can test UI/service/link behavior. Full internet tunneling is explicitly outside this gate.

## Phase 2 — complete traffic tunnel
Deliver Android VpnService integration and independently sourced/implemented TUN-to-SOCKS transport supporting TCP/UDP, routing, DNS, reconnects, failure behavior and traffic tests.

Gate: two-device test proves receiver traffic reaches the internet through the sharing phone.

## Phase 3 — automation and resilient lifecycle
Deliver opt-in auto-receive for previously approved networks, explicit first-time confirmation, Android-stop restart notification/action, Wi-Fi enable prompt/error UX, and verified red/green health state.

Gate: device tests cover radio state, network switch, OS service kill, user stop, reboot and permission denial.

## Phase 4 — One UI polish and distribution readiness
Deliver refined layouts, tile behavior, accessibility, original Pistol Whip Productions identity/assets, app metadata, compatibility matrix and performance work.

Gate: target-device acceptance, accessibility review and reproducible release.

## AI concept backlog (not approved)
Potential on-device "Network Copilot" explains connection state and suggests troubleshooting from local diagnostics only. No traffic payloads, browsing history, credentials or remote AI calls. Requires separate approval and privacy/security review.
