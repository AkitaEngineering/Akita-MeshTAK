from pathlib import Path

Import("env")


def has_text(value):
    return value is not None and str(value).strip() != ""


def load_version_name():
    version_file = Path(env["PROJECT_DIR"]).resolve().parent / "version.properties"
    if not version_file.exists():
        return "0.2.1"

    version_name = "0.2.1"
    for line in version_file.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        if key.strip() == "VERSION_NAME":
            version_name = value.strip()
            break
    return version_name


def resolve_env(name):
    value = env['ENV'].get(name)
    if has_text(value):
        return str(value).strip()
    return None


env.Append(CPPDEFINES=[("AKITA_VERSION_NAME", env.StringifyMacro(load_version_name()))])

string_overrides = {
    "DEVICE_ID": "AKITA_DEVICE_ID",
    "BLE_SERVICE_UUID": "AKITA_BLE_SERVICE_UUID",
    "BLE_COT_CHARACTERISTIC_UUID": "AKITA_BLE_COT_CHARACTERISTIC_UUID",
    "BLE_WRITE_CHARACTERISTIC_UUID": "AKITA_BLE_WRITE_CHARACTERISTIC_UUID",
    "MQTT_SERVER": "AKITA_MQTT_SERVER",
    "MQTT_TOPIC_PREFIX": "AKITA_MQTT_TOPIC_PREFIX",
    "MQTT_WIFI_SSID": "AKITA_MQTT_WIFI_SSID",
    "MQTT_WIFI_PASSWORD": "AKITA_MQTT_WIFI_PASSWORD",
    "MQTT_USERNAME": "AKITA_MQTT_USERNAME",
    "MQTT_PASSWORD": "AKITA_MQTT_PASSWORD",
}

for define_name, env_name in string_overrides.items():
    value = resolve_env(env_name)
    if value is not None:
        env.Append(CPPDEFINES=[(define_name, env.StringifyMacro(value))])

port_value = resolve_env("AKITA_MQTT_PORT")
if port_value is not None:
    env.Append(CPPDEFINES=[("MQTT_PORT", int(port_value))])

for define_name, env_name in {
    "MESH_SERIAL_RX_PIN": "AKITA_MESH_SERIAL_RX_PIN",
    "MESH_SERIAL_TX_PIN": "AKITA_MESH_SERIAL_TX_PIN",
}.items():
    value = resolve_env(env_name)
    if value is not None:
        env.Append(CPPDEFINES=[(define_name, int(value))])

allow_placeholder = resolve_env("AKITA_ALLOW_PLACEHOLDER_SECRET")
if allow_placeholder is not None and allow_placeholder.lower() in {"1", "true", "yes", "on"}:
    env.Append(CPPDEFINES=["ALLOW_PLACEHOLDER_SECRET"])

allow_insecure_mqtt = resolve_env("AKITA_ALLOW_INSECURE_MQTT")
if allow_insecure_mqtt is not None and allow_insecure_mqtt.lower() in {"1", "true", "yes", "on"}:
    env.Append(CPPDEFINES=["ALLOW_INSECURE_MQTT"])
