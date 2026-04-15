// File: firmware/src/mailbox_escape.cpp
// Description: Shared mailbox payload escape/unescape utilities.

#include "mailbox_escape.h"

String escapeMailboxPayload(const String& payload) {
  String escaped = payload;
  escaped.replace("%", "%25");
  escaped.replace("\r", "%0D");
  escaped.replace("\n", "%0A");
  return escaped;
}

String unescapeMailboxPayload(const String& payload) {
  String result = "";
  result.reserve(payload.length());
  for (int index = 0; index < (int)payload.length(); index++) {
    if (payload.charAt(index) == '%' && index + 2 <= (int)payload.length() - 1) {
      String token = payload.substring(index, index + 3);
      if (token.equalsIgnoreCase("%0A")) {
        result += '\n';
        index += 2;
        continue;
      }
      if (token.equalsIgnoreCase("%0D")) {
        result += '\r';
        index += 2;
        continue;
      }
      if (token.equalsIgnoreCase("%25")) {
        result += '%';
        index += 2;
        continue;
      }
    }
    result += payload.charAt(index);
  }
  return result;
}
