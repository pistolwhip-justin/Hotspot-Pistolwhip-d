# Architecture

## Product topology

The system has two independent roles connected by a local Wi-Fi Direct group:

- **Share:** creates a Wi-Fi Direct group and hosts a multi-client SOCKS5 endpoint on the group-owner interface. Android's conventional Hotspot setting is not the transport.
- **Receive:** associates with the matching group, establishes Android VpnService, then starts the native TUN-to-SOCKS5 engine and routes device traffic to Share.

The actual group SSID is `DIRECT-WHIP-[phonename]`. The naming and full interaction contract are defined in [WIFI_DIRECT_SPEC.md](WIFI_DIRECT_SPEC.md).

## Share role

1. The foreground service requests/uses the Android Wi-Fi Direct manager and creates a group with the required SSID and generated passphrase.
2. Group creation is confirmed through Android's P2P callbacks and group information.
3. The SOCKS5 listener starts only after group creation succeeds and is associated with the P2P group-owner interface.
4. The service exposes independent status for service process, P2P group, listener and receiver peers.
5. The service stops the listener and removes/releases the group on explicit Share stop or unrecoverable failure.
6. The Share UI displays credentials and Android-reported verbose network/radio values. Stats refresh every five seconds and on relevant callbacks.

Do not call conventional hotspot/tethering APIs for this role. A service process or TCP listener is not evidence that the P2P group exists.

## Receive role

1. On activation, request Wi-Fi enablement through the device's available app capability; use an Android system handoff if the operation is unavailable.
2. Wait ten seconds after the enablement step.
3. Read the currently connected Wi-Fi SSID and verify the `DIRECT-WHIP-` prefix.
4. If there is no matching network, present QR scan or manual network entry. The app's visual configuration screen passes the selected credentials/action to Android's supported Wi-Fi association flow.
5. Android performs network association and credential persistence. The app observes the resulting network and does not enumerate or silently manipulate the full saved-network list.
6. After association with the expected P2P group, discover/validate the SOCKS endpoint on the group network.
7. Request Android VPN consent if needed, establish the VpnService interface, and only then start the native TUN-to-SOCKS5 engine.
8. Stop the tunnel and clear healthy status when the group network, endpoint, VPN interface or native engine is lost.

## TUN-to-SOCKS engine

The earlier prototype integrates HevSocks5Tunnel 2.20.0 (MIT) through the JNI contract in `hev.htproxy.TProxyService`. It remains the candidate data plane:

- Android VpnService TUN file descriptor -> native tun2socks -> SOCKS5 endpoint on Share.
- TCP CONNECT and UDP ASSOCIATE transport.
- IPv4/IPv6 route and DNS configuration.
- ABI coverage: arm64-v8a, armeabi-v7a, x86 and x86_64.
- Native libraries are fetched from the pinned upstream release and SHA-256 verified by `scripts/fetch-native-libs.sh`.

This integration is not accepted as working end-to-end until it is bound to the P2P network and passes physical-device traffic tests.

## Layered health reporting

The application must separately track:

1. Share foreground service running.
2. Wi-Fi Direct group creation confirmed.
3. SOCKS5 listener bound to the group-owner interface.
4. Receiver associated with the expected group.
5. VPN permission granted and VPN interface established.
6. Native tunnel engine running.
7. End-to-end traffic probe successful.

Only the final state may be represented as a healthy/ready full tunnel. The UI must not infer later states from earlier ones.

## Sharing credentials

- The QR code contains a standard Wi-Fi configuration payload.
- The Sharesheet uses Android's standard chooser with plain text containing only SSID and passphrase. Do not hard-code an app allowlist; available destinations depend on Android and installed compatible apps.
- Copy is also available.
- No phone number, device identifier, IP address, diagnostic value or other metadata is included in the credential payload.

## Network configuration UI

The app may visually reproduce Android's Wi-Fi selection/configuration experience. It is a presentation layer only: the user's chosen network and action are handed to Android's supported APIs/system UI for association and saving. Do not implement private system internals or claim support for connection semantics Android does not expose.

## Recovery and privacy

Unexpected service failure triggers up to three best-effort restart attempts ten seconds apart where Android permits. After repeated failure, show the user a log-sharing choice. Redact credentials and identifiers. Cancel, Share & Discard, and Share & Save are user-controlled actions. Force-stop and OEM/background restrictions can prevent recovery; do not promise guaranteed restart/dialog display.

No cloud relay, remote VPN server, traffic inspection, automatic log upload, or AI service is part of the approved scope.
