#!/usr/bin/env python3
"""Validate Akita MeshTAK CoT fields expected by OpenTAKServer."""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET


ISO8601_Z_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


def build_sample_cot(mission_name: str = "") -> str:
    dest = f"<dest mission='{mission_name}'/>" if mission_name else ""
    return (
        "<event version='2.0' uid='AkitaNode01-123-1' type='a-f-G-U-U' "
        "how='m-g' time='2026-05-31T15:00:00Z' start='2026-05-31T15:00:00Z' "
        "stale='2026-05-31T15:02:00Z'>"
        f"{dest}"
        "<point lat='45.4215000' lon='-75.6972000' hae='70.00' ce='10' le='10'/>"
        "<detail>"
        "<contact callsign='AkitaNode01'/>"
        "<takv device='Heltec V3' platform='Akita MeshTAK' os='ESP32' version='0.2.0'/>"
        "<__group name='Cyan' role='Team Member'/>"
        "<precisionlocation geopointsrc='GPS' altsrc='GPS'/>"
        "</detail>"
        "</event>"
    )


def require(condition: bool, message: str, errors: list[str]) -> None:
    if not condition:
        errors.append(message)


def validate_cot(xml_text: str, expected_mission: str = "") -> list[str]:
    errors: list[str] = []
    try:
        event = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        return [f"CoT XML parse failed: {exc}"]

    require(event.tag == "event", "root element must be event", errors)
    for attr in ("version", "uid", "type", "how", "time", "start", "stale"):
        require(bool(event.attrib.get(attr)), f"missing root event attribute: {attr}", errors)

    require(event.attrib.get("version") == "2.0", "event version must be 2.0", errors)
    require(event.attrib.get("how") == "m-g", "event how should identify machine GPS source", errors)
    for attr in ("time", "start", "stale"):
        require(
            bool(ISO8601_Z_RE.match(event.attrib.get(attr, ""))),
            f"{attr} must be UTC ISO-8601 format ending in Z",
            errors,
        )

    point = event.find("point")
    require(point is not None, "missing point element", errors)
    if point is not None:
        for attr in ("lat", "lon", "hae", "ce", "le"):
            require(bool(point.attrib.get(attr)), f"missing point attribute: {attr}", errors)

    detail = event.find("detail")
    require(detail is not None, "missing detail element", errors)
    if detail is not None:
        require(detail.find("contact") is not None, "missing detail/contact element", errors)
        contact = detail.find("contact")
        if contact is not None:
            require(bool(contact.attrib.get("callsign")), "missing contact callsign", errors)
        require(detail.find("takv") is not None, "missing detail/takv element", errors)
        require(detail.find("__group") is not None, "missing detail/__group element", errors)
        require(
            detail.find("precisionlocation") is not None,
            "missing detail/precisionlocation element",
            errors,
        )

    dest = event.find("dest")
    if expected_mission:
        require(dest is not None, "missing mission dest element", errors)
        if dest is not None:
            require(
                dest.attrib.get("mission") == expected_mission,
                "mission dest does not match expected mission",
                errors,
            )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", help="Validate a CoT XML file instead of the built-in sample.")
    parser.add_argument("--mission", default="Akita Mission", help="Expected mission name.")
    args = parser.parse_args()

    if args.file:
        with open(args.file, "r", encoding="utf-8") as handle:
            xml_text = handle.read().strip()
    else:
        xml_text = build_sample_cot(args.mission)

    errors = validate_cot(xml_text, args.mission)
    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        return 1

    print("OpenTAKServer CoT compatibility check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
