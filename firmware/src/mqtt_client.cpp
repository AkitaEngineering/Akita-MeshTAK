// firmware/src/mqtt_client.cpp
#ifdef ENABLE_MQTT
#include "mqtt_client.h"
#include "config.h"
#include "cot_generation.h"

WiFiClient espClient;
PubSubClient client(espClient);

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
  //  Handle incoming MQTT messages
}

void connectWiFi() {
  Serial.print("Connecting to WiFi...");
  WiFi.begin("your_SSID", "your_PASSWORD"); // Replace
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("WiFi connected");
}

void connectMQTT() {
  Serial.print("Connecting to MQTT...");
  while (!client.connect(DEVICE_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
    Serial.print("failed, rc=");
    Serial.print(client.state());
    Serial.println(" try again in 5 seconds");
    delay(5000);
  }
  Serial.println("MQTT connected");
  client.subscribe(MQTT_TOPIC_PREFIX "#");
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
  char fullTopic[128];
  snprintf(fullTopic, sizeof(fullTopic), "%s%s", MQTT_TOPIC_PREFIX, topic);
  Serial.print("Publishing to: ");
  Serial.print(fullTopic);
  Serial.print(" payload: ");
  Serial.println(payload);
  client.publish(fullTopic, payload);
}
#endif
