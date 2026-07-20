#include "provisioning_store.h"

#include "config.h"
#include <Preferences.h>

static const char* NVS_NAMESPACE = "akita-sec";
static const char* NVS_SECRET_KEY = "device-secret";
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
  Preferences preferences;
  if (!preferences.begin(NVS_NAMESPACE, true)) {
    return false;
  }
  secret = preferences.getString(NVS_SECRET_KEY, "");
  preferences.end();
  return isValidSecret(secret);
}

bool persistProvisioningSecret(const String& secret) {
  if (!isProvisioningWindowOpen() || !isValidSecret(secret)) {
    return false;
  }
  Preferences preferences;
  if (!preferences.begin(NVS_NAMESPACE, false)) {
    return false;
  }
  size_t written = preferences.putString(NVS_SECRET_KEY, secret);
  preferences.end();
  if (written == 0) {
    return false;
  }
  g_windowEnabled = false;
  return true;
}
