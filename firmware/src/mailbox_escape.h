// File: firmware/src/mailbox_escape.h
// Description: Shared mailbox payload escape/unescape utilities.
// Eliminates duplication between meshtastic_setup.cpp and power_management.cpp.

#ifndef MAILBOX_ESCAPE_H
#define MAILBOX_ESCAPE_H

#include <Arduino.h>

// Percent-encode CR, LF, and % characters so payloads survive
// line-oriented transports.
String escapeMailboxPayload(const String& payload);

// Reverse the percent-encoding applied by escapeMailboxPayload.
String unescapeMailboxPayload(const String& payload);

#endif // MAILBOX_ESCAPE_H
