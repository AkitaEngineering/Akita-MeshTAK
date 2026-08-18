#include "replay_guard.h"

#include <Preferences.h>

static const char* NVS_NAMESPACE = "akita-rpl";
static const char* NVS_WATERMARK_KEY = "watermark";
static const char* NVS_NONCES_KEY = "nonces";
static const int REPLAY_CACHE_SIZE = 64;
static const unsigned long PERSIST_INTERVAL_MS = 10000UL;

static String g_seenNonces[REPLAY_CACHE_SIZE];
static int g_seenNonceIndex = 0;
static int g_seenNonceCount = 0;
static unsigned long long g_watermark = 0;
static unsigned long g_lastPersistAt = 0;
static int g_unpersistedCount = 0;
static bool g_loaded = false;

static String makeCacheKey(const String& keyId, const String& nonceHex) {
  return keyId + ":" + nonceHex;
}

static void persistLocked(bool force) {
  unsigned long now = millis();
  if (!force && g_unpersistedCount < 8 && (now - g_lastPersistAt) < PERSIST_INTERVAL_MS) {
    return;
  }

  String packed = "";
  packed.reserve(g_seenNonceCount * 20);
  int start = (g_seenNonceCount < REPLAY_CACHE_SIZE)
      ? 0
      : g_seenNonceIndex;
  int count = g_seenNonceCount < REPLAY_CACHE_SIZE ? g_seenNonceCount : REPLAY_CACHE_SIZE;
  for (int i = 0; i < count; i++) {
    int index = (start + i) % REPLAY_CACHE_SIZE;
    if (g_seenNonces[index].length() == 0) {
      continue;
    }
    if (packed.length() > 0) {
      packed += ",";
    }
    packed += g_seenNonces[index];
  }

  Preferences preferences;
  if (!preferences.begin(NVS_NAMESPACE, false)) {
    return;
  }
  preferences.putULong64(NVS_WATERMARK_KEY, g_watermark);
  preferences.putString(NVS_NONCES_KEY, packed);
  preferences.end();
  g_lastPersistAt = now;
  g_unpersistedCount = 0;
}

static void loadIfNeeded() {
  if (g_loaded) {
    return;
  }
  g_loaded = true;

  Preferences preferences;
  if (!preferences.begin(NVS_NAMESPACE, true)) {
    return;
  }
  g_watermark = preferences.getULong64(NVS_WATERMARK_KEY, 0);
  String packed = preferences.getString(NVS_NONCES_KEY, "");
  preferences.end();

  g_seenNonceIndex = 0;
  g_seenNonceCount = 0;
  if (packed.length() == 0) {
    return;
  }

  int start = 0;
  while (start <= packed.length()) {
    int comma = packed.indexOf(',', start);
    if (comma < 0) {
      comma = packed.length();
    }
    String token = packed.substring(start, comma);
    token.trim();
    if (token.length() > 0) {
      g_seenNonces[g_seenNonceIndex] = token;
      g_seenNonceIndex = (g_seenNonceIndex + 1) % REPLAY_CACHE_SIZE;
      if (g_seenNonceCount < REPLAY_CACHE_SIZE) {
        g_seenNonceCount++;
      }
    }
    if (comma >= packed.length()) {
      break;
    }
    start = comma + 1;
  }
}

void setupReplayGuard() {
  g_loaded = false;
  loadIfNeeded();
}

bool replayGuardHasSeen(const String& keyId, const String& nonceHex) {
  loadIfNeeded();
  String cacheKey = makeCacheKey(keyId, nonceHex);
  for (int i = 0; i < REPLAY_CACHE_SIZE; i++) {
    if (g_seenNonces[i] == cacheKey) {
      return true;
    }
  }
  return false;
}

void replayGuardRemember(const String& keyId, const String& nonceHex, unsigned long long timestamp) {
  loadIfNeeded();
  String cacheKey = makeCacheKey(keyId, nonceHex);
  g_seenNonces[g_seenNonceIndex] = cacheKey;
  g_seenNonceIndex = (g_seenNonceIndex + 1) % REPLAY_CACHE_SIZE;
  if (g_seenNonceCount < REPLAY_CACHE_SIZE) {
    g_seenNonceCount++;
  }
  if (timestamp > g_watermark) {
    g_watermark = timestamp;
  }
  g_unpersistedCount++;
  persistLocked(false);
}
