// firmware/src/display_handler.h
#ifndef DISPLAY_HANDLER_H
#define DISPLAY_HANDLER_H

#ifdef ENABLE_DISPLAY
  #include <heltec.h>

  bool setupDisplay();
  void loopDisplay();
  void displayMessage(const String& message);
#endif

#endif
