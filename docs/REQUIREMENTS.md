# Product requirements

## Confirmed product requirements

1. Share phone exposes its internet connection through its Wi-Fi hotspot.
2. Receiver joins the hotspot as an ordinary Wi-Fi network; Wi-Fi Direct is not required.
3. Exactly one Share toggle and one Receive toggle control independent roles.
4. Stopping either role must not close the UI or stop the other role.
5. Multiple receiver devices and independent proxy sessions are supported concurrently. Android hotspot DHCP controls client IP allocation; the app must not claim to randomize DHCP leases.
6. If Wi-Fi is off or the expected hotspot is unavailable, guide the user to Android Wi-Fi settings and report the failure. Do not claim the app can silently enable Wi-Fi on every supported Android version.
7. Interface: true-black AMOLED, system-scaled Roboto/system typography, Material 3 dynamic accents, Samsung/One UI-inspired spacing and a scrollable layout for smaller or multi-window screens.
8. First Receive activation is manual and requires Android's VpnService consent. Future auto-start is opt-in and deferred.
9. Persistent service notifications remain available. The Receive health indicator stays red until the SOCKS endpoint can reach an upstream destination and the native VPN tunnel reports running; it is green only when both checks pass.
10. Share health is green only after the SOCKS listener successfully binds and starts accepting connections.
11. Unexpected Android service termination should prompt restart where reliably detectable; force-stop and OEM behavior are documented limitations.
12. Separate Quick Settings tiles for Share and Receive use the app mark and carry their role labels.
13. Tile tap toggles only its corresponding service; it must not launch or foreground the app UI.
14. Long-pressing either Quick Settings tile opens the app through Android's tile settings-activity behavior.
15. Tapping the persistent service notification opens the app.
16. Tapping the separate status indicator does nothing.
17. When the user opens or returns to the app, switches and health indicators reflect the latest persisted service state.
18. Project source, package identifiers, assets and branding are owned by Pistol Whip Productions or explicitly supplied by it.
19. AI integration is deferred and is not a dependency of mass testing.
20. The app must not require root access, Wi-Fi Direct, a remote VPN server or a cloud relay.

## Phase 1 and 1.1 — delivered behavior

- Independent Share/Receive controls and foreground service lifecycle.
- Multi-client SOCKS5 TCP listener and receiver gateway discovery.
- Separate Quick Settings controls, silent tile taps, tile long-press navigation and notification navigation.
- UI remains open when either role is stopped.
- Phase 1.1 is published as v0.1.1. Physical-device acceptance remains distinct from CI results.

## Phase 2 — VPN tunnel acceptance

- Receiver uses Android VpnService with explicit user consent.
- Receiver TUN routes IPv4 and IPv6 default traffic into the native TUN-to-SOCKS5 engine.
- TCP CONNECT and UDP ASSOCIATE are supported end to end.
- DNS requests are routed through the tunnel and resolved through the Share endpoint.
- Share supports multiple concurrent TCP and UDP associations.
- The VPN is bound to the Wi-Fi network connected to the Share phone; the app's own package is excluded to avoid proxy recursion.
- Wi-Fi loss, upstream failure, native startup failure and user stop produce a red/unhealthy state and stop the local tunnel as appropriate.
- README, architecture, decision record and test plan describe actual delivered behavior.

**Phase 2 readiness gate:** automated tests and CI APK build pass, then two physical devices demonstrate that receiving-phone app traffic reaches the internet through the sharing phone over TCP and UDP, with DNS verified. IPv6 is tested when supported by the hotspot/upstream. CI success alone is not the end-to-end acceptance gate.

## Deferred

- Opt-in auto-receive for previously approved networks.
- Refined Wi-Fi enable flow and any Android-supported system prompt.
- Restart prompt/action for service termination where the OS exposes a reliable signal.
- Kill-switch/lockdown behavior; this is not currently implemented or claimed.
- Extended Samsung device compatibility, accessibility and performance validation.
- AI/network intelligence concepts, which require separate approval and privacy/security review.
