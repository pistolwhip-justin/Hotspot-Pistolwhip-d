# Decision record

## D-001 — Standalone application structure
- Date: 2026-11-24
- Decision: Maintain a standalone Kotlin/Android application with project-owned source, identifiers and distribution identity.
- Rationale: Keep implementation, branding and release ownership clear and maintainable.
- Status: Accepted.

## D-002 — Normal Wi-Fi join replaces Wi-Fi Direct
- Date: 2026-11-24
- Decision: Receiver joins sharing phone's hotspot using Android Wi-Fi UI, then activates Receive.
- Rationale: User reports this is easier and more reliable.
- Status: Accepted.

## D-003 — Concurrent receivers, not app-controlled DHCP
- Date: 2026-11-24
- Decision: Multi-client server with independent sessions. Do not claim to randomize DHCP client IPs.
- Rationale: Android/Samsung hotspot DHCP controls client addressing.
- Status: Accepted.

## D-004 — Link health is not a tunnel claim
- Date: 2026-11-24
- Decision: Until VPN forwarding exists, report endpoint/link health only.
- Rationale: Prevent false green status and misleading "tunnel active" claims.
- Status: Accepted.

## D-005 — Quick Settings and notification navigation
- Date: 2026-11-24
- Decision: Tapping a Share or Receive Quick Settings tile toggles only that service and must not launch, foreground, or otherwise bring the app Activity forward. Long-pressing either tile opens the app through the Android tile settings activity. Tapping the persistent service notification opens the app. Tapping the separate status indicator has no action.
- Rationale: Keep service controls independent from app navigation and avoid accidental UI interruptions.
- Status: Accepted.

## D-006 — AI is deferred
- Date: 2026-11-24
- Decision: Do not implement AI, network copilot, remote model calls, or AI-dependent features during the mass-testing foundation. Record possible concepts only in the future-phase backlog.
- Rationale: Prioritize a small, dependable networking product and get a test build into users' hands.
- Status: Accepted.
