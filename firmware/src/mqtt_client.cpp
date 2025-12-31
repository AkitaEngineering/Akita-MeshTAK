// firmware/src/mqtt_client.cpp
#ifdef ENABLE_MQTT
#include "mqtt_client.h"
#include "config.h"
#include "cot_generation.h"
#include "audit_log.h"        // For audit logging
#include "security.h"         // For security operations
#include "input_validation.h" // For input validation

WiFiClient espClient;
PubSubClient client(espClient);

void callback(char* topic, byte* payload, unsigned int length) {
  // SECURITY: Validate input
  if (topic == nullptr || payload == nullptr || length == 0 || length > MAX_MESSAGE_LENGTH) {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "MQTT", 
                 "Invalid MQTT message received", false);
    return;
  }
  
  // Convert to String for validation
  String message = "";
  for (unsigned int i = 0; i < length && i < MAX_MESSAGE_LENGTH; i++) {
    message += (char)payload[i];
  }
  
  // Validate message content
  ValidationResult validation = validateCommand(message);
  if (validation != VALIDATION_OK) {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "MQTT",
                 "Invalid MQTT message - validation failed", false);
    Serial.printf("SECURITY: MQTT message validation failed: %d\n", validation);
    return;
  }
  
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  Serial.println(message);
  
  logAuditEvent(AUDIT_EVENT_DATA_RECEIVED, 0, "MQTT",
               String("Message from topic: " + String(topic)).c_str(), true);
  
  //  Handle incoming MQTT messages (process commands, CoT data, etc.)
}

void connectWiFi() {
  Serial.print("Connecting to WiFi...");
  // SECURITY: WiFi credentials should be stored securely, not hardcoded
  // In production, use secure storage or provisioning
  const char* ssid = "your_SSID";     // MUST be replaced with secure storage
  const char* password = "your_PASSWORD"; // MUST be replaced with secure storage
  
  WiFi.begin(ssid, password);
  unsigned long startTime = millis();
  const unsigned long timeout = 30000; // 30 second timeout
  
  while (WiFi.status() != WL_CONNECTED && (millis() - startTime) < timeout) {
    delay(500);
    Serial.print(".");
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi connected");
    logAuditEvent(AUDIT_EVENT_CONNECTION, 0, "MQTT", "WiFi connected", true);
  } else {
    Serial.println("WiFi connection timeout");
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "MQTT", "WiFi connection failed", false);
  }
}

void connectMQTT() {
  Serial.print("Connecting to MQTT...");
  // SECURITY: MQTT credentials should be stored securely, not hardcoded
  // In production, use secure storage or certificate-based authentication
  const char* mqttUser = "MQTT_USERNAME";   // MUST be replaced with secure storage
  const char* mqttPass = "MQTT_PASSWORD";   // MUST be replaced with secure storage
  
  unsigned long startTime = millis();
  const unsigned long timeout = 30000; // 30 second timeout
  
  while (!client.connect(DEVICE_ID, mqttUser, mqttPass) && (millis() - startTime) < timeout) {
    Serial.print("failed, rc=");
    Serial.print(client.state());
    Serial.println(" try again in 5 seconds");
    delay(5000);
  }
  
  if (client.connected()) {
    Serial.println("MQTT connected");
    client.subscribe(MQTT_TOPIC_PREFIX "#");
    logAuditEvent(AUDIT_EVENT_CONNECTION, 0, "MQTT", "MQTT broker connected", true);
  } else {
    Serial.println("MQTT connection timeout");
    logAuditEvent(AUDIT_EVENT_ERROR, 2, "MQTT", "MQTT connection failed", false);
  }
}

bool setupMQTT() {
  Serial.println("Initializing MQTT...");
  connectWiFi();
  client.setServer(MQTT_SERVER, MQTT_PORT);
  client.setCallback(callback);
  connectMQTT();
  return true;
}

void loopMQTT() {
  if (!client.connected()) {
    connectMQTT();
  }
  client.loop();
  //  Publish CoT data
  static unsigned long lastPublishTime = 0;
  if (millis() - lastPublishTime > 5000) {
    String currentCot = generateLocationCoT(DEVICE_ID, 0.0, 0.0, 0.0); // Replace with actual data
    publishMQTT(MQTT_TOPIC_PREFIX "cot", currentCot.c_str());
    lastPublishTime = millis();
  }
}

void publishMQTT(const char* topic, const char* payload) {
  // SECURITY: Validate inputs
  if (topic == nullptr || payload == nullptr) {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "MQTT", "Publish failed - null parameters", false);
    return;
  }
  
  String payloadStr = String(payload);
  if (payloadStr.length() > MAX_MESSAGE_LENGTH) {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "MQTT", "Publish failed - payload too long", false);
    return;
  }
  
  // Validate payload content
  ValidationResult validation = validateCoTXml(payloadStr);
  if (validation != VALIDATION_OK && !payloadStr.startsWith("STATUS:")) {
    logAuditEvent(AUDIT_EVENT_SECURITY_VIOLATION, 2, "MQTT",
                 "Invalid payload - validation failed", false);
    return;
  }
  
  char fullTopic[128];
  snprintf(fullTopic, sizeof(fullTopic), "%s%s", MQTT_TOPIC_PREFIX, topic);
  
  if (strlen(fullTopic) >= sizeof(fullTopic)) {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "MQTT", "Publish failed - topic too long", false);
    return;
  }
  
  bool published = client.publish(fullTopic, payload);
  
  if (published) {
    Serial.print("Publishing to: ");
    Serial.print(fullTopic);
    Serial.print(" payload: ");
    Serial.println(payload);
    logAuditEvent(AUDIT_EVENT_DATA_SENT, 0, "MQTT",
                 String("Published to: " + String(fullTopic)).c_str(), true);
  } else {
    logAuditEvent(AUDIT_EVENT_ERROR, 1, "MQTT", "Publish failed - client not connected", false);
  }
}
#endif
