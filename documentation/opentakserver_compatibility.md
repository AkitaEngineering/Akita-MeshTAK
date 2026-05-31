# OpenTAKServer Compatibility

Akita MeshTAK is compatible with OpenTAKServer when traffic reaches the server as standard TAK CoT XML through ATAK, WinTAK, iTAK, or an OpenTAKServer TCP/SSL streaming input.

## Supported Transport Path

- Akita device to ATAK plugin over BLE notifications or USB serial.
- ATAK to OpenTAKServer through ATAK's normal TAK server connection.
- OpenTAKServer TCP streaming defaults: `8088`.
- OpenTAKServer SSL streaming defaults: `8089`.
- OpenTAKServer UDP defaults: `8087`.
- OpenTAKServer Marti HTTP/HTTPS API defaults: `8080` and `8443`.

Akita MeshTAK does not currently embed OpenTAKServer account, certificate-enrollment, mission API, data-package, or plugin-repository clients. Those remain handled by ATAK/OpenTAKServer themselves.

## CoT Fields Emitted By Firmware

Firmware-generated location events now include the fields OpenTAKServer parses directly:

- Root `<event>` attributes: `version`, `uid`, `type`, `how`, `time`, `start`, and `stale`.
- `<point>` attributes: `lat`, `lon`, `hae`, `ce`, and `le`.
- `<detail><contact callsign="..."/>` for OpenTAKServer EUD/callsign tracking.
- `<detail><takv .../>` for device, platform, OS, and firmware version.
- `<detail><__group .../>` for team and role association.
- `<detail><precisionlocation geopointsrc="GPS" altsrc="GPS"/>` for location source.
- Optional root-level `<dest mission="..."/>` when an OpenTAKServer mission name is configured in plugin settings.

OpenTAKServer stores these as CoT records and point records, can show the sender as an EUD, and can use the metadata for its web UI and group/mission routing once ATAK forwards the event.

The plugin now synchronizes firmware time from the ATAK device clock after BLE or serial connection. This avoids synthetic fallback timestamps on devices without GPS or network time and improves OpenTAKServer position history and stale-event handling.

## Feature Matrix

| OpenTAKServer capability | Akita MeshTAK status |
| --- | --- |
| Standard CoT ingest | Supported |
| EUD/callsign discovery | Supported through `contact` and `takv` |
| Position history | Supported through root event times and point data |
| Team/group metadata | Supported through `__group` |
| Mission CoT tagging | Supported through configured `<dest mission="..."/>` |
| ATAK server sync | Supported through ATAK's TAK server connection |
| SSL client certificate enrollment | Handled by ATAK/OpenTAKServer, not the Akita plugin |
| Mission API / Data Sync | Handled by ATAK/OpenTAKServer, not the Akita plugin |
| Data packages | Handled by ATAK/OpenTAKServer, not the Akita plugin |
| Native OpenTAKServer Meshtastic MQTT bridge | Not implemented in Akita firmware; Akita uses its own BLE/serial/MQTT paths |

## Validation Checklist

1. Build firmware with `pio run -e heltec_v3_ci`.
2. Pair or connect the Akita device to ATAK using BLE or USB serial.
3. Configure ATAK for the OpenTAKServer streaming endpoint.
4. Confirm received Akita CoT events contain root `uid`, `time`, `start`, `stale`, and `how` attributes.
5. Confirm OpenTAKServer creates or updates the EUD and point records for the Akita callsign.
