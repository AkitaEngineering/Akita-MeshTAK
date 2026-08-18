#include "hardware_security.h"

#if __has_include("esp_flash_encrypt.h")
#include "esp_flash_encrypt.h"
#define AKITA_HAS_FLASH_ENCRYPT 1
#endif

#if __has_include("esp_secure_boot.h")
#include "esp_secure_boot.h"
#define AKITA_HAS_SECURE_BOOT 1
#endif

bool isFlashEncryptionEnabled() {
#if defined(AKITA_HAS_FLASH_ENCRYPT)
  return esp_flash_encryption_enabled();
#else
  return false;
#endif
}

bool isSecureBootEnabled() {
#if defined(AKITA_HAS_SECURE_BOOT)
  return esp_secure_boot_enabled();
#else
  return false;
#endif
}

String formatHardwareSecurityFields() {
  String out = "flash=";
  out += isFlashEncryptionEnabled() ? "1" : "0";
  out += ":boot=";
  out += isSecureBootEnabled() ? "1" : "0";
  return out;
}
