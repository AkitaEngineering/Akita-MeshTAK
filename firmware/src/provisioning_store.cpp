#include "provisioning_store.h"

#include "config.h"
#include <Preferences.h>

static const char* NVS_NAMESPACE = "akita-sec";
static const char* NVS_SECRET_KEY = "device-secret";
static const char* NVS_PREV_SECRET_KEY = "prev-secret";
static const char* NVS_KEY_ID_KEY = "key-id";
static bool g_windowEnabled = false;
static unsigned long g_windowOpenedAt = 0;

static bool isValidSecret(const String& secret) {
  if (secret.length() < 16 || secret.length() > 128) {
    return false;
  }
  for (size_t i = 0; i < secret.length(); i++) {
    char c = secret.charAt(i);
    if (!isalnum(c) && c != '-' && c != '_' && c != '.') {
      return false;
    }
  }
  return true;
}

void setupProvisioningStore() {
  pinMode(PROVISION_BUTTON_PIN, INPUT_PULLUP);
  delay(25);
  g_windowEnabled = digitalRead(PROVISION_BUTTON_PIN) == LOW;
  g_windowOpenedAt = millis();
}

bool isProvisioningWindowOpen() {
  return g_windowEnabled && (millis() - g_windowOpenedAt) <= PROVISIONING_WINDOW_MS;
}

bool isPlaintextProvisioningCommandAllowed(const String& command) {
  return isProvisioningWindowOpen() && command.startsWith(CMD_PROVISION_STAGE_PREFIX);
}

bool loadProvisioningSecret(String& secret) {
  ProvisioningMaterial material;
  if (!loadProvisioningMaterial(material)) {
    secret = "";
    return false;
  }
  secret = material.currentSecret;
  return true;
}

bool loadProvisioningMaterial(ProvisioningMaterial& material) {
  Preferences preferences;
  if (!preferences.begin(NVS_NAMESPACE, true)) {
    return false;
  }
  material.currentSecret = preferences.getString(NVS_SECRET_KEY, "");
  material.previousSecret = preferences.getString(NVS_PREV_SECRET_KEY, "");
  material.currentKeyId = preferences.getString(NVS_KEY_ID_KEY, ENCRYPTED_KEY_ID);
  preferences.end();
  if (!isKnownKeyId(material.currentKeyId)) {
    material.currentKeyId = ENCRYPTED_KEY_ID;
  }
  if (!isValidSecret(material.previousSecret)) {
    material.previousSecret = "";
  }
  return isValidSecret(material.currentSecret);
}

bool persistProvisioningSecret(const String& secret) {
  if (!isProvisioningWindowOpen() || !isValidSecret(secret)) {
    return false;
  }

  ProvisioningMaterial existing;
  String previousSecret = "";
  String keyId = ENCRYPTED_KEY_ID;
  if (loadProvisioningMaterial(existing)) {
    if (existing.currentSecret != secret) {
      previousSecret = existing.currentSecret;
      keyId = nextKeyId(existing.currentKeyId);
    } else {
      previousSecret = existing.previousSecret;
      keyId = existing.currentKeyId;
    }
  }

  Preferences preferences;
  if (!preferences.begin(NVS_NAMESPACE, false)) {
    return false;
  }
  size_t written = preferences.putString(NVS_SECRET_KEY, secret);
  if (previousSecret.length() > 0) {
    preferences.putString(NVS_PREV_SECRET_KEY, previousSecret);
  } else {
    preferences.remove(NVS_PREV_SECRET_KEY);
  }
  preferences.putString(NVS_KEY_ID_KEY, keyId);
  preferences.end();
  if (written == 0) {
    return false;
  }
  g_windowEnabled = false;
  return true;
}
