# QUICK REFERENCE CARD
## AKITA MESHTAK SYSTEM
## QUICK REFERENCE CARD

**Document Number:** QR-AKITA-MESHTAK-001  
**Revision:** 1.4  
**Date:** 2026-04-14  
**Classification:** UNCLASSIFIED

---

## STARTUP PROCEDURE

### BLE Connection
1. ☐ Power on Meshtastic device
2. ☐ Power on Android device
3. ☐ Open ATAK
4. ☐ Verify "Connected" (green) in toolbar
5. ☐ Verify Mission Assurance shows deployment-ready security posture

### Serial Connection
1. ☐ Connect USB cable
2. ☐ Power on Meshtastic device
3. ☐ Power on Android device
4. ☐ Grant USB permission
5. ☐ Open ATAK
6. ☐ Verify "Connected" (green) in toolbar
7. ☐ Verify Secure Route and Security indicators are operational

---

## STATUS INDICATORS

| Indicator | Color | Meaning | Action |
|-----------|-------|----------|--------|
| **Connected** | 🟢 Green | System ready | Normal operations |
| **Connecting** | 🟡 Yellow | Establishing connection | Wait |
| **Disconnected** | 🔴 Red | Connection failed | Troubleshoot |
| **Battery > 50%** | 🟢 Green | Good | Normal operations |
| **Battery 20-50%** | 🟡 Yellow | Low | Charge soon |
| **Battery < 20%** | 🔴 Red | Critical | Charge immediately |

---

## COMMON COMMANDS

| Command | Purpose |
|---------|---------|
| `CMD:GET_BATT` | Request battery status |
| `CMD:GET_VERSION` | Request firmware version |
| `CMD:ALERT:SOS` | Send emergency alert |
| `CMD:MAILBOX:PUT:<id>:<format>:<payload>` | Queue guaranteed-delivery mission traffic |
| `CMD:PROV:STAGE:<secret>` | Runtime-stage provisioning on a trusted local bearer |

---

## EMERGENCY PROCEDURES

### Send SOS Alert
1. Tap **SOS** button in toolbar
2. Confirm alert sent
3. Alert broadcasts network-wide
4. Event logged (CRITICAL)

**⚠️ WARNING**: SOS alerts are logged and cannot be undone. Use only in genuine emergencies.

---

## TROUBLESHOOTING

### Cannot Connect (BLE)
- ☐ Verify BLE enabled
- ☐ Check device within range (10-50m)
- ☐ Verify device name matches
- ☐ Restart both devices

### Cannot Connect (Serial)
- ☐ Verify USB cable connected
- ☐ Grant USB permission
- ☐ Try different USB cable
- ☐ Restart both devices

### No Location Data
- ☐ Verify connection active
- ☐ Check Meshtastic network
- ☐ Verify other devices on network
- ☐ Restart ATAK

### Message Stuck In Flight
- ☐ Confirm peer node is reachable on the mesh
- ☐ Wait for peer mailbox acknowledgement to return
- ☐ Review Guaranteed Delivery Mailbox for pending or failed frames
- ☐ Use Retry Queue or enable Auto Bearer Failover

---

## DAILY CHECKLIST

- ☐ Verify connection status (green)
- ☐ Verify mission profile and role pack
- ☐ Review Mission Assurance
- ☐ Review Guaranteed Delivery Mailbox for pending or in-flight frames
- ☐ Check battery level
- ☐ Test message sending
- ☐ Verify location data receiving
- ☐ Review tactical overlay for stale-marker or route-health warnings
- ☐ Confirm failover posture matches the mission plan
- ☐ Review for errors

---

## SECURITY QUICK CHECK

- ☐ Confirm active provisioning secret is configured for this deployment and is not placeholder material
- ☐ Confirm firmware/plugin encrypted metadata match (`v1`, `k1`)
- ☐ Confirm firmware encryption enabled (default: on)
- ☐ Confirm **Enable Encrypted Transport** remains enabled in plugin settings
- ☐ If rotating in the field, verify the air-gapped bundle matches the deployment and stage only over a trusted local bearer
- ☐ Confirm encrypted transport operational (envelope format: `ENC:v1:k1:<hex>`)
- ☐ Export audit log after mission or exercise if required by SOP
- ☐ If encrypted payloads are rejected, verify version/key-id alignment before field use

---

## CONTACT INFORMATION

**Support**: support@akitaengineering.com  
**Emergency**: Follow operational procedures

---

## VERSION INFORMATION

**Firmware**: 0.2.0  
**Plugin**: 0.2.0  
**Document**: QR-AKITA-MESHTAK-001 Rev 1.4

---

**Copyright (C) 2025-2026 Akita Engineering. All Rights Reserved.**

