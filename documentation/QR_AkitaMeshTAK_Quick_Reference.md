# QUICK REFERENCE CARD
## AKITA MESHTAK SYSTEM
## QUICK REFERENCE CARD

**Document Number:** QR-AKITA-MESHTAK-001  
**Revision:** 1.0  
**Date:** 2025-12-31  
**Classification:** UNCLASSIFIED

---

## STARTUP PROCEDURE

### BLE Connection
1. ☐ Power on Meshtastic device
2. ☐ Power on Android device
3. ☐ Open ATAK
4. ☐ Verify "Connected" (green) in toolbar

### Serial Connection
1. ☐ Connect USB cable
2. ☐ Power on Meshtastic device
3. ☐ Power on Android device
4. ☐ Grant USB permission
5. ☐ Open ATAK
6. ☐ Verify "Connected" (green) in toolbar

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

---

## DAILY CHECKLIST

- ☐ Verify connection status (green)
- ☐ Check battery level
- ☐ Test message sending
- ☐ Verify location data receiving
- ☐ Review for errors

---

## CONTACT INFORMATION

**Support**: support@akitaengineering.com  
**Emergency**: Follow operational procedures

---

## VERSION INFORMATION

**Firmware**: 0.2.0  
**Plugin**: 0.2.0  
**Document**: QR-AKITA-MESHTAK-001 Rev 1.0

---

**Copyright (C) 2025 Akita Engineering. All Rights Reserved.**

