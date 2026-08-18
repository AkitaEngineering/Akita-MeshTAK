#ifndef HARDWARE_SECURITY_H
#define HARDWARE_SECURITY_H

#include <Arduino.h>

bool isFlashEncryptionEnabled();
bool isSecureBootEnabled();
String formatHardwareSecurityFields();

#endif
