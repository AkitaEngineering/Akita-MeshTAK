// File: firmware/src/audit_log.cpp
// Description: Audit logging implementation for accountability.
// CRITICAL: For military/law enforcement accountability requirements

#include "audit_log.h"
#include "config.h"
#include <string.h>

#define MAX_AUDIT_ENTRIES 1000
#define AUDIT_LOG_MAGIC 0x41554454  // "AUDT"

static AuditLogEntry g_audit_log[MAX_AUDIT_ENTRIES];
static uint32_t g_audit_log_count = 0;
static uint32_t g_audit_log_index = 0;
static bool g_audit_initialized = false;

bool initAuditLog() {
    memset(g_audit_log, 0, sizeof(g_audit_log));
    g_audit_log_count = 0;
    g_audit_log_index = 0;
    g_audit_initialized = true;
    
    // Log initialization
    logAuditEvent(AUDIT_EVENT_CONNECTION, 0, DEVICE_ID, "Audit log initialized", true);
    return true;
}

void logAuditEvent(AuditEventType event_type, uint8_t severity, 
                   const char* source, const char* details, bool success) {
    if (!g_audit_initialized) {
        return;
    }
    
    // Circular buffer - overwrite oldest entries if full
    uint32_t index = g_audit_log_index % MAX_AUDIT_ENTRIES;
    
    g_audit_log[index].timestamp = millis();
    g_audit_log[index].event_type = event_type;
    g_audit_log[index].severity = severity;
    g_audit_log[index].success = success;
    
    if (source != nullptr) {
        strncpy(g_audit_log[index].source, source, sizeof(g_audit_log[index].source) - 1);
        g_audit_log[index].source[sizeof(g_audit_log[index].source) - 1] = '\0';
    } else {
        g_audit_log[index].source[0] = '\0';
    }
    
    if (details != nullptr) {
        strncpy(g_audit_log[index].details, details, sizeof(g_audit_log[index].details) - 1);
        g_audit_log[index].details[sizeof(g_audit_log[index].details) - 1] = '\0';
    } else {
        g_audit_log[index].details[0] = '\0';
    }
    
    g_audit_log_index++;
    if (g_audit_log_count < MAX_AUDIT_ENTRIES) {
        g_audit_log_count++;
    }
    
    // Also output to Serial for immediate monitoring (can be disabled in production)
    Serial.printf("[AUDIT] T:%lu E:%d S:%d Src:%s Det:%s Res:%s\n",
                   g_audit_log[index].timestamp,
                   event_type, severity,
                   g_audit_log[index].source,
                   g_audit_log[index].details,
                   success ? "OK" : "FAIL");
}

uint32_t getAuditLogCount() {
    return g_audit_log_count;
}

bool getAuditLogEntry(uint32_t index, AuditLogEntry* entry) {
    if (entry == nullptr || index >= g_audit_log_count) {
        return false;
    }
    
    // Calculate actual index in circular buffer
    uint32_t actual_index = (g_audit_log_index - g_audit_log_count + index) % MAX_AUDIT_ENTRIES;
    memcpy(entry, &g_audit_log[actual_index], sizeof(AuditLogEntry));
    return true;
}

bool clearAuditLog() {
    // In production, this might require authentication
    memset(g_audit_log, 0, sizeof(g_audit_log));
    g_audit_log_count = 0;
    g_audit_log_index = 0;
    logAuditEvent(AUDIT_EVENT_CONFIGURATION_CHANGE, 1, DEVICE_ID, "Audit log cleared", true);
    return true;
}

void exportAuditLog() {
    Serial.println("=== AUDIT LOG EXPORT ===");
    Serial.printf("Total entries: %lu\n", g_audit_log_count);
    Serial.println("---");
    
    for (uint32_t i = 0; i < g_audit_log_count; i++) {
        AuditLogEntry entry;
        if (getAuditLogEntry(i, &entry)) {
            Serial.printf("[%lu] T:%lu E:%d S:%d Src:%s Det:%s Res:%s\n",
                           i, entry.timestamp, entry.event_type, entry.severity,
                           entry.source, entry.details, entry.success ? "OK" : "FAIL");
        }
    }
    
    Serial.println("=== END AUDIT LOG ===");
}

