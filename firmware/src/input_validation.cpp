// File: firmware/src/input_validation.cpp
// Description: Input validation implementation.
// CRITICAL: Prevents injection attacks and malformed data

#include "input_validation.h"
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
    
    // Check for injection patterns
    if (containsInjectionPattern(command)) {
        return VALIDATION_ERROR_INJECTION_ATTEMPT;
    }
    
    // Commands should start with "CMD:"
    if (!command.startsWith("CMD:")) {
        // Allow status responses and CoT data
        if (!command.startsWith("STATUS:") && !command.startsWith("<event")) {
            return VALIDATION_ERROR_MALFORMED;
        }
    }
    
    return VALIDATION_OK;
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

