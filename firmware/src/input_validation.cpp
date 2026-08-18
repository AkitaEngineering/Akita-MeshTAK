// File: firmware/src/input_validation.cpp
// Description: Input validation implementation.
// CRITICAL: Prevents injection attacks and malformed data

#include "input_validation.h"
#include "config.h"
#include <string.h>

// Dangerous characters/patterns for injection attacks
static const char* DANGEROUS_PATTERNS[] = {
    "<script",
    "javascript:",
    "onerror=",
    "onload=",
    "eval(",
    "exec(",
    "system(",
    "<?php",
    "${",
    "$(",
    "`",
    NULL
};

ValidationResult validateCommand(const String& command) {
    if (command.length() == 0) {
        return VALIDATION_ERROR_NULL;
    }

    if (command.length() > MAX_COMMAND_LENGTH) {
        return VALIDATION_ERROR_TOO_LONG;
    }

    if (command == CMD_GET_BATT || command == CMD_ALERT_SOS || command == CMD_GET_VERSION
            || command == CMD_GET_SEC_STATE) {
        return VALIDATION_OK;
    }

    if (command.startsWith(CMD_TIME_SYNC_PREFIX)) {
        String value = command.substring(strlen(CMD_TIME_SYNC_PREFIX));
        if (value.length() < 10 || value.length() > 11) {
            return VALIDATION_ERROR_MALFORMED;
        }
        for (size_t i = 0; i < value.length(); i++) {
            if (!isdigit(value.charAt(i))) {
                return VALIDATION_ERROR_INVALID_CHARS;
            }
        }
        return VALIDATION_OK;
    }

    if (command.startsWith(CMD_COT_MISSION_PREFIX)) {
        String mission = command.substring(strlen(CMD_COT_MISSION_PREFIX));
        if (mission.length() > 64) {
            return VALIDATION_ERROR_TOO_LONG;
        }
        for (size_t i = 0; i < mission.length(); i++) {
            char c = mission.charAt(i);
            if (!isalnum(c) && c != '-' && c != '_' && c != ' ' && c != '.') {
                return VALIDATION_ERROR_INVALID_CHARS;
            }
        }
        return VALIDATION_OK;
    }

    if (command.startsWith(CMD_PROVISION_STAGE_PREFIX)) {
        String value = command.substring(strlen(CMD_PROVISION_STAGE_PREFIX));
        int separator = value.lastIndexOf(':');
        if (separator < 16 || separator + 1 >= value.length()) {
            return VALIDATION_ERROR_MALFORMED;
        }
        String secret = value.substring(0, separator);
        String epoch = value.substring(separator + 1);
        if (secret.length() < 16 || secret.length() > 128) {
            return VALIDATION_ERROR_MALFORMED;
        }
        for (size_t i = 0; i < secret.length(); i++) {
            char c = secret.charAt(i);
            if (!isalnum(c) && c != '-' && c != '_' && c != '.') {
                return VALIDATION_ERROR_INVALID_CHARS;
            }
        }
        if (epoch.length() < 10 || epoch.length() > 11) {
            return VALIDATION_ERROR_MALFORMED;
        }
        for (size_t i = 0; i < epoch.length(); i++) {
            if (!isdigit(epoch.charAt(i))) {
                return VALIDATION_ERROR_INVALID_CHARS;
            }
        }
        return VALIDATION_OK;
    }

    if (command.startsWith(CMD_MAILBOX_PUT_PREFIX)) {
        if (command.length() <= strlen(CMD_MAILBOX_PUT_PREFIX)) {
            return VALIDATION_ERROR_MALFORMED;
        }
        for (size_t i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c < 32 || c == 127) {
                return VALIDATION_ERROR_INVALID_CHARS;
            }
        }
        return VALIDATION_OK;
    }

    return VALIDATION_ERROR_MALFORMED;
}

ValidationResult validateDeviceId(const String& deviceId) {
    if (deviceId.length() == 0) {
        return VALIDATION_ERROR_NULL;
    }

    if (deviceId.length() > MAX_DEVICE_ID_LENGTH) {
        return VALIDATION_ERROR_TOO_LONG;
    }

    // Device ID should only contain alphanumeric, dash, underscore
    for (size_t i = 0; i < deviceId.length(); i++) {
        char c = deviceId.charAt(i);
        if (!isalnum(c) && c != '-' && c != '_') {
            return VALIDATION_ERROR_INVALID_CHARS;
        }
    }

    return VALIDATION_OK;
}

ValidationResult validateCallsign(const String& callsign) {
    if (callsign.length() == 0) {
        return VALIDATION_ERROR_NULL;
    }

    if (callsign.length() > MAX_CALLSIGN_LENGTH) {
        return VALIDATION_ERROR_TOO_LONG;
    }

    // Callsign should only contain alphanumeric, dash, underscore, space
    for (size_t i = 0; i < callsign.length(); i++) {
        char c = callsign.charAt(i);
        if (!isalnum(c) && c != '-' && c != '_' && c != ' ') {
            return VALIDATION_ERROR_INVALID_CHARS;
        }
    }

    return VALIDATION_OK;
}

ValidationResult validateCoTXml(const String& cotXml) {
    if (cotXml.length() == 0) {
        return VALIDATION_ERROR_NULL;
    }

    if (cotXml.length() > MAX_MESSAGE_LENGTH) {
        return VALIDATION_ERROR_TOO_LONG;
    }

    // Basic XML structure check
    if (!cotXml.startsWith("<event") || !cotXml.endsWith("</event>")) {
        return VALIDATION_ERROR_MALFORMED;
    }

    // Reject CDATA sections, processing instructions, and DOCTYPE declarations
    // which could be used to embed arbitrary content.
    String lower = cotXml;
    lower.toLowerCase();
    if (lower.indexOf("<![cdata[") >= 0 ||
        lower.indexOf("<!doctype") >= 0 ||
        lower.indexOf("<!entity") >= 0 ||
        lower.indexOf("<?") >= 0) {
        return VALIDATION_ERROR_INJECTION_ATTEMPT;
    }

    // Verify that every '<' has a matching '>' (basic well-formedness).
    int depth = 0;
    bool insideTag = false;
    for (size_t i = 0; i < cotXml.length(); i++) {
        char c = cotXml.charAt(i);
        if (c == '<') {
            if (insideTag) {
                return VALIDATION_ERROR_MALFORMED;  // nested '<'
            }
            insideTag = true;
        } else if (c == '>') {
            if (!insideTag) {
                return VALIDATION_ERROR_MALFORMED;  // unmatched '>'
            }
            insideTag = false;
        }
    }
    if (insideTag) {
        return VALIDATION_ERROR_MALFORMED;  // unclosed '<'
    }

    // Check for injection patterns
    if (containsInjectionPattern(cotXml)) {
        return VALIDATION_ERROR_INJECTION_ATTEMPT;
    }

    return VALIDATION_OK;
}

String sanitizeString(const String& input, size_t max_length) {
    String sanitized = "";
    size_t len = (input.length() < max_length) ? input.length() : max_length;

    for (size_t i = 0; i < len; i++) {
        char c = input.charAt(i);
        // Allow printable ASCII except control characters and dangerous chars
        if (c >= 32 && c <= 126 && c != '<' && c != '>' && c != '&' && c != '"' && c != '\'') {
            sanitized += c;
        }
    }

    return sanitized;
}

bool containsInjectionPattern(const String& input) {
    String lowerInput = input;
    lowerInput.toLowerCase();

    for (int i = 0; DANGEROUS_PATTERNS[i] != NULL; i++) {
        if (lowerInput.indexOf(DANGEROUS_PATTERNS[i]) >= 0) {
            return true;
        }
    }

    return false;
}

bool validateNumericRange(int value, int min, int max) {
    return (value >= min && value <= max);
}

bool validateCoordinate(float coord, bool isLatitude) {
    if (isLatitude) {
        return (coord >= -90.0f && coord <= 90.0f);
    } else {
        return (coord >= -180.0f && coord <= 180.0f);
    }
}

