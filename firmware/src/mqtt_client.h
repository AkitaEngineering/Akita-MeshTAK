// firmware/src/mqtt_client.h
#ifndef MQTT_CLIENT_H
#define MQTT_CLIENT_H

#ifdef ENABLE_MQTT
  #include <WiFi.h>
  #include <PubSubClient.h>

  extern WiFiClient espClient;
  extern PubSubClient client;

  bool setupMQTT();
  void loopMQTT();
  void publishMQTT(const char* topic, const char* payload);
#endif

#endif
