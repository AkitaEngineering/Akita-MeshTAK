// File: firmware/src/power_management.h
// Description: Declares power management, battery reading, and command processing functions.
#ifndef POWER_MANAGEMENT_H
#define POWER_MANAGEMENT_H

#include <Arduino.h> // Include for String

bool setupPowerManagement();
void loopPowerManagement();
float readBatteryVoltage(); 
void processIncomingCommand(const String& cmd); // Declaration

#endif
