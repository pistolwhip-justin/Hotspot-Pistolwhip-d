# Wi-Fi Direct product specification

**Status:** Authoritative requirements for the Wi-Fi Direct implementation phase  
**Updated:** 2026-11-25

## Product purpose

Create a direct phone-to-phone Wi-Fi connection using Android Wi-Fi Direct, without enabling Android's conventional Hotspot setting. The sharing phone exposes a local SOCKS5 endpoint over the Wi-Fi Direct group. The receiving phone joins that group and uses an Android VpnService plus the native TUN-to-SOCKS5 engine to route device traffic through the sharing phone.

The Wi-Fi Direct group, receiver VPN, and verified packet forwarding are separate states. A listener starting is not proof that a group exists; a group existing is not proof that a receiver joined; a VPN starting is not proof that traffic is forwarding.

## Naming

The actual Wi-Fi Direct SSID is formed by concatenating:

- Direct group prefix: `DIRECT-WH`
- Friendly network name: `IP-[phonename]`

Result: `DIRECT-WHIP-[phonename]`

The SSID must retain the required `DIRECT-WH` prefix. The phone-name portion is normalized to a valid SSID-safe value and kept within the Wi-Fi SSID byte limit. The group passphrase is generated per Share session, 18–20 characters, containing uppercase and lowercase letters, at least five digits, and at least five characters selected from `!@#$%^&*`. Avoid visually ambiguous characters. Credentials are session secrets and must never be written to logs.

## Share behavior

1. On Share activation, request the platform capabilities/permissions needed for Wi-Fi Direct and the requested device settings changes.
2. Attempt the user's requested Data Saver and Wi-Fi-state preparation only through Android-supported permissions/capabilities available on the device. If Android blocks an operation, report the actual limitation and provide a system handoff; never claim it succeeded.
3. Create a Wi-Fi Direct group with the required SSID and generated passphrase. Do not invoke conventional Android hotspot/tethering APIs.
4. Start the SOCKS5 listener only after group creation succeeds. Bind/advertise the endpoint on the group-owner interface, and verify it is reachable from the P2P network.
5. Expand Share only after the group and listener are established. On failure, show a truthful error and keep the section inactive.
6. Display SSID, passphrase, group/interface details, connected-peer information where exposed by Android, and compact verbose-style radio/network statistics. Refresh visible values every five seconds and supplement with network callbacks.
7. Show a Share action in the expanded section. It opens an overlay with a QR code and a communication/share action.

## Sharing destinations

Use Android's standard Sharesheet/chooser with a plain-text payload containing **only the SSID and passphrase**. Do not hard-code or maintain an app allowlist. Any installed app that Android reports as compatible with the payload may appear, and the user chooses the destination (for example, messaging, email, chat, notes, or nearby-sharing apps when compatible). Do not promise that a particular app will appear on every device. Also provide a QR representation and a copy action. Never send credentials automatically or select a destination on the user's behalf.

The QR payload must use the standard Wi-Fi QR format with WPA/WPA2-compatible credentials. Do not include device phone numbers, hardware identifiers, IP addresses, diagnostic details, or other metadata in the share payload.

## Receive behavior

1. When Receive is enabled, request Wi-Fi enablement through the device's available app permission/capability; if unavailable, use the appropriate Android system handoff.
2. Wait ten seconds after the Wi-Fi enablement step.
3. Read the currently connected Wi-Fi SSID.
4. If the SSID begins `DIRECT-WHIP-`, treat it as a candidate group and validate the active Wi-Fi network/interface and SOCKS endpoint before VPN startup.
5. If there is no connection or the SSID does not match, offer QR scanning or manual network-information entry.
6. Pass the selected network credentials and user action to Android's Wi-Fi connection flow. Android owns actual association and saved-network behavior. Do not enumerate the complete saved-network list or silently select credentials from it.
7. Start the receiver VPN only after Android reports association with the expected group and the endpoint is reachable. The VPN must be authorized and established before the tunnel is reported active.
8. The receiver VPN must not be started while the user is still entering or selecting Wi-Fi credentials.

A saved group may be rejoined automatically by Android itself. The app should observe and validate that connection rather than reimplement Android's saved-network selection.

## VPN/tunnel lifecycle and health states

Expose distinct states for:

1. Share foreground service running.
2. Wi-Fi Direct group creation confirmed.
3. SOCKS5 listener bound to the P2P interface.
4. Receiver associated with the expected group.
5. VPN permission granted and VPN interface established.
6. Native TUN-to-SOCKS engine running.
7. End-to-end traffic probe successful.

Only report the full tunnel as ready after the final checks pass. A failed VPN, network loss, endpoint failure, or native engine exit must immediately clear the healthy state and stop/recover the tunnel as appropriate.

## Network settings interaction

The in-app Receive UI should visually reproduce the relevant Android Wi-Fi network selection/configuration experience. Its controls are a presentation layer: selected SSID/security/password and the user's Connect/Cancel/Save intent are passed to Android's supported Wi-Fi connection UI/API. Android performs the actual connection and credential persistence. Do not attempt to clone private system implementation code or silently bypass system approval.

Where the platform supports user-approved temporary or saved configuration, preserve those semantics. Do not promise a universal "Connect & Forget" operation if Android's public API does not expose it on the target version; document and implement the closest supported behavior without misrepresenting it.

## Recovery and privacy

- Unexpected Share service failure: make up to three best-effort restart attempts, ten seconds apart, when Android permits; then offer the user a log-sharing dialog.
- Log sharing requires explicit user selection. Redact passwords, SSIDs where identifying, device identifiers, phone numbers, and other secrets before presenting logs.
- Actions: Cancel; Share & Discard; Share & Save. Android Sharesheet is used for sharing and the system document picker for saving.
- Force-stop, OEM process management, and Android background-start restrictions can prevent automatic restarts or dialogs. The app must describe recovery as best-effort.
- Never automatically upload logs or connection credentials.

## UI

- Preserve true-black AMOLED styling, system typography/scaling, dynamic accents, compact spacing and independent Share/Receive controls.
- Share is collapsed when inactive and expands automatically after successful group creation.
- Receive is collapsed when inactive and expands when the expected network is connected and the tunnel is being established/active.
- Share network data should use a compact verbose diagnostic layout modeled on the user's supplied Android Wi-Fi verbose-information screenshot.
- Do not display instructional filler inside the active Share panel.

## Explicit exclusions

- Do not enable Android's conventional Hotspot setting or invoke conventional tethering as the Share transport.
- Do not claim a network, receiver, VPN, or tunnel is active based only on a service toggle or listener.
- Do not add cloud relays, remote VPN servers, AI services, traffic inspection, or automatic log uploads.
- Do not require phone-number retrieval to create a group.
- Do not hard-code a list of sharing destinations; use Android's Sharesheet.
