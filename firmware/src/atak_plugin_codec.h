#ifndef ATAK_PLUGIN_CODEC_H
#define ATAK_PLUGIN_CODEC_H

#include <Arduino.h>
#include <stddef.h>
#include <stdint.h>

void setAtakPluginEnabled(bool enabled);
bool isAtakPluginEnabled();

bool sendAtakPluginPli(const String& callsign, const String& deviceCallsign,
                       float latitude, float longitude, float altitude,
                       uint32_t speed, uint16_t course, uint8_t battery);
bool sendAtakPluginChat(const String& callsign, const String& message,
                        const String& to, const String& toCallsign);

void handleAtakPluginPayload(uint32_t from, uint32_t to, uint8_t channel,
                             const uint8_t* payload, size_t length);

#endif
