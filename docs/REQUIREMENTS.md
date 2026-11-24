# Product requirements

## Confirmed
1. Share phone exposes its connection through its Wi-Fi hotspot.
2. Receiver joins the hotspot as an ordinary Wi-Fi network; Wi-Fi Direct is not required.
3. Exactly one Share toggle and one Receive toggle control independent roles.
4. Stopping either role must not close the UI.
5. Multiple receivers are supported concurrently. Android hotspot DHCP controls client IP allocation; the app must not claim to randomize DHCP leases.
6. If Wi-Fi is off, use supported Android system prompts/settings and show an error if rejected or unsuccessful.
7. Interface: true black AMOLED, Roboto/system typography, Material 3 dynamic accents, Samsung/One UI-inspired layout.
8. First Receive activation is manual; future auto-start is opt-in.
9. Active status notification remains. Receiver green status requires verified endpoint and upstream reachability; inactive is red.
10. Unexpected Android service termination should prompt restart where reliably detectable.
11. Separate Quick Settings tiles for Share and Receive; long-press should open app settings/interface where Android supports it.
12. New clean-room implementation; no prior source, binaries, assets, names or proprietary branding.
13. AI integration is a deferred marketing concept, not a core dependency.

## Phase 1 gate
- App installs and launches with derivative identity.
- Dark Material 3 UI has independent Share/Receive switches.
- Share service starts/stops and accepts multiple SOCKS5 handshakes.
- Receiver discovers Wi-Fi gateway and verifies share endpoint.
- UI remains open after either service stops.
- Service state and notification accurately distinguish inactive and active roles.
- README and phase documentation match delivered behavior.

## Deferred
Full-device VPN forwarding, auto-receive preference, OS-stop restart prompt, Wi-Fi enable UX refinement, final tile polish and Samsung device validation.
