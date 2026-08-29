from pathlib import Path

Import("env")

HOOK = "AKITA_ATAK_PLUGIN_HOOK"
CALLBACK_DECL = "void (*decoded_packet_callback)"
SETTER = """
void (*decoded_packet_callback)(uint32_t from, uint32_t to, uint8_t channel, uint32_t portnum, const uint8_t *payload, size_t length) = NULL;

void set_decoded_packet_callback(void (*callback)(uint32_t from, uint32_t to, uint8_t channel, uint32_t portnum, const uint8_t *payload, size_t length)) {
  decoded_packet_callback = callback;
}
"""
HANDLER_OLD = """    } else {
      // TODO handle other portnums
      return false;
    }"""
HANDLER_NEW = """    } else if (meshPacket->decoded.portnum == meshtastic_PortNum_ATAK_PLUGIN) {
      // AKITA_ATAK_PLUGIN_HOOK
      if (decoded_packet_callback != NULL)
        decoded_packet_callback(meshPacket->from, meshPacket->to, meshPacket->channel,
            (uint32_t)meshPacket->decoded.portnum,
            meshPacket->decoded.payload.bytes, meshPacket->decoded.payload.size);
    } else {
      // TODO handle other portnums
      return false;
    }"""


def patch_file(path: Path) -> None:
    if not path.is_file():
        return
    text = path.read_text(encoding="utf-8")
    original = text
    if CALLBACK_DECL not in text:
        needle = "void (*text_message_callback)(uint32_t from, uint32_t to,  uint8_t channel, const char* text) = NULL;"
        if needle in text:
            text = text.replace(needle, needle + "\n" + SETTER, 1)
    if HOOK not in text and HANDLER_OLD in text:
        text = text.replace(HANDLER_OLD, HANDLER_NEW, 1)
    if text != original:
        path.write_text(text, encoding="utf-8")
        print("Patched Meshtastic ATAK_PLUGIN receive hook:", path)


libdeps = Path(env.subst("$PROJECT_LIBDEPS_DIR"))
candidates = list(libdeps.glob("*/Meshtastic/src/mt_protocol.cpp"))
if not candidates:
    print("Meshtastic mt_protocol.cpp not found yet; ATAK_PLUGIN receive hook will apply on the next build.")
for candidate in candidates:
    patch_file(candidate)
