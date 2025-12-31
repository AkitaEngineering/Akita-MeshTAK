// File: firmware/src/input_validation.h
// Description: Input validation and sanitization utilities.
// CRITICAL: Prevents injection attacks and malformed data

#ifndef INPUT_VALIDATION_H
#define INPUT_VALIDATION_H

#include <Arduino.h>

// Maximum lengths
#define MAX_COMMAND_LENGTH 256
#define MAX_DEVICE_ID_LENGTH 64
#define MAX_CALLSIGN_LENGTH 32
#define MAX_MESSAGE_LENGTH 512

// Validation result
typedef enum {
    VALIDATION_OK = 0,
    VALIDATION_ERROR_NULL,
    VALIDATION_ERROR_TOO_LONG,
    VALIDATION_ERROR_INVALID_CHARS,
    VALIDATION_ERROR_MALFORMED,
    VALIDATION_ERROR_INJECTION_ATTEMPT
} ValidationResult;

// Validate command string
ValidationResult validateCommand(const String& command);

// Validate device ID
ValidationResult validateDeviceId(const String& deviceId);

// Validate callsign
ValidationResult validateCallsign(const String& callsign);

// Validate CoT XML
ValidationResult validateCoTXml(const String& cotXml);

// Sanitize string (remove dangerous characters)
String sanitizeString(const String& input, size_t max_length);

// Check for injection patterns
bool containsInjectionPattern(const String& input);

// Validate numeric range
bool validateNumericRange(int value, int min, int max);

// Validate coordinate
bool validateCoordinate(float coord, bool isLatitude);

#endif // INPUT_VALIDATION_H

