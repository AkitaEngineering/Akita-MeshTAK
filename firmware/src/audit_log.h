// File: firmware/src/audit_log.h
// Description: Audit logging for accountability and security monitoring.
// CRITICAL: For military/law enforcement accountability requirements

#ifndef AUDIT_LOG_H
#define AUDIT_LOG_H

#include <Arduino.h>

// Audit event types
typedef enum {
    AUDIT_EVENT_CONNECTION = 1,
    AUDIT_EVENT_DISCONNECTION,
    AUDIT_EVENT_COMMAND_RECEIVED,
    AUDIT_EVENT_COMMAND_EXECUTED,
    AUDIT_EVENT_DATA_SENT,
    AUDIT_EVENT_DATA_RECEIVED,
    AUDIT_EVENT_SECURITY_VIOLATION,
    AUDIT_EVENT_AUTHENTICATION_FAILURE,
    AUDIT_EVENT_INTEGRITY_FAILURE,
    AUDIT_EVENT_SOS_TRIGGERED,
    AUDIT_EVENT_CONFIGURATION_CHANGE,
    AUDIT_EVENT_ERROR
} AuditEventType;

// Audit log entry structure
typedef struct {
    uint32_t timestamp;
    AuditEventType event_type;
    uint8_t severity;        // 0=Info, 1=Warning, 2=Error, 3=Critical
    char source[16];          // Source identifier (device ID, user, etc.)
    char details[80];         // Redacted event details
    bool success;            // Operation success status
} AuditLogEntry;

// Initialize audit logging
bool initAuditLog();

// Log an audit event
void logAuditEvent(AuditEventType event_type, uint8_t severity,
                   const char* source, const char* details, bool success);

// Get audit log entry count
uint32_t getAuditLogCount();

// Read audit log entry (for retrieval/export)
bool getAuditLogEntry(uint32_t index, AuditLogEntry* entry);

#endif // AUDIT_LOG_H

