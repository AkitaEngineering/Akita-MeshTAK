package com.akitaengineering.meshtak;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Meshtastic ATAK_PLUGIN TAKPacket codec (port 72), matching meshtastic/atak.proto.
 */
public final class AtakPluginPacket {

    public static final int PORTNUM = 72;
    public static final int MAX_PAYLOAD_BYTES = 220;

    public enum Kind {
        PLI,
        CHAT,
        DETAIL
    }

    public static final class Contact {
        public final String callsign;
        public final String deviceCallsign;

        public Contact(String callsign, String deviceCallsign) {
            this.callsign = nullToEmpty(callsign);
            this.deviceCallsign = nullToEmpty(deviceCallsign);
        }
    }

    public static final class Packet {
        public final boolean compressed;
        public final Contact contact;
        public final String team;
        public final String role;
        public final int battery;
        public final Kind kind;
        public final double latitude;
        public final double longitude;
        public final int altitude;
        public final int speed;
        public final int course;
        public final String chatMessage;
        public final String chatTo;
        public final String chatToCallsign;
        public final byte[] detail;

        private Packet(boolean compressed,
                       Contact contact,
                       String team,
                       String role,
                       int battery,
                       Kind kind,
                       double latitude,
                       double longitude,
                       int altitude,
                       int speed,
                       int course,
                       String chatMessage,
                       String chatTo,
                       String chatToCallsign,
                       byte[] detail) {
            this.compressed = compressed;
            this.contact = contact;
            this.team = team;
            this.role = role;
            this.battery = battery;
            this.kind = kind;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.speed = speed;
            this.course = course;
            this.chatMessage = chatMessage;
            this.chatTo = chatTo;
            this.chatToCallsign = chatToCallsign;
            this.detail = detail;
        }

        public static Packet pli(Contact contact,
                                 String team,
                                 String role,
                                 int battery,
                                 double latitude,
                                 double longitude,
                                 int altitude,
                                 int speed,
                                 int course) {
            return new Packet(false, contact, team, role, battery, Kind.PLI,
                    latitude, longitude, altitude, speed, course, "", "", "", new byte[0]);
        }

        public static Packet chat(Contact contact,
                                  String team,
                                  String role,
                                  int battery,
                                  String message,
                                  String to,
                                  String toCallsign) {
            return new Packet(false, contact, team, role, battery, Kind.CHAT,
                    0d, 0d, 0, 0, 0, nullToEmpty(message), nullToEmpty(to), nullToEmpty(toCallsign), new byte[0]);
        }
    }

    private AtakPluginPacket() {
    }

    public static byte[] encode(Packet packet) {
        if (packet == null) {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (packet.compressed) {
            writeVarintKey(output, 1, 0);
            writeVarint(output, 1);
        }
        if (packet.contact != null && (!packet.contact.callsign.isEmpty() || !packet.contact.deviceCallsign.isEmpty())) {
            writeLengthDelimited(output, 2, encodeContact(packet.contact));
        }
        int team = teamToEnum(packet.team);
        int role = roleToEnum(packet.role);
        if (team > 0 || role > 0) {
            ByteArrayOutputStream group = new ByteArrayOutputStream();
            if (role > 0) {
                writeVarintKey(group, 1, 0);
                writeVarint(group, role);
            }
            if (team > 0) {
                writeVarintKey(group, 2, 0);
                writeVarint(group, team);
            }
            writeLengthDelimited(output, 3, group.toByteArray());
        }
        if (packet.battery > 0) {
            ByteArrayOutputStream status = new ByteArrayOutputStream();
            writeVarintKey(status, 1, 0);
            writeVarint(status, Math.min(100, packet.battery));
            writeLengthDelimited(output, 4, status.toByteArray());
        }
        if (packet.kind == Kind.PLI) {
            writeLengthDelimited(output, 5, encodePli(packet));
        } else if (packet.kind == Kind.CHAT) {
            writeLengthDelimited(output, 6, encodeChat(packet));
        } else if (packet.detail != null && packet.detail.length > 0) {
            writeLengthDelimited(output, 7, packet.detail);
        }
        byte[] encoded = output.toByteArray();
        if (encoded.length > MAX_PAYLOAD_BYTES) {
            return Arrays.copyOf(encoded, MAX_PAYLOAD_BYTES);
        }
        return encoded;
    }

    public static Packet decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        Parser parser = new Parser(bytes);
        boolean compressed = false;
        Contact contact = new Contact("", "");
        String team = OperatorIdentity.DEFAULT_TEAM;
        String role = OperatorIdentity.DEFAULT_ROLE;
        int battery = 0;
        Kind kind = Kind.DETAIL;
        double latitude = 0;
        double longitude = 0;
        int altitude = 0;
        int speed = 0;
        int course = 0;
        String chatMessage = "";
        String chatTo = "";
        String chatToCallsign = "";
        byte[] detail = new byte[0];
        while (parser.hasRemaining()) {
            long key = parser.readVarint();
            int field = (int) (key >>> 3);
            int wire = (int) (key & 7);
            if (field == 1 && wire == 0) {
                compressed = parser.readVarint() != 0;
            } else if (field == 2 && wire == 2) {
                contact = decodeContact(parser.readBytes());
            } else if (field == 3 && wire == 2) {
                int[] group = decodeGroup(parser.readBytes());
                role = enumToRole(group[0]);
                team = enumToTeam(group[1]);
            } else if (field == 4 && wire == 2) {
                battery = decodeBattery(parser.readBytes());
            } else if (field == 5 && wire == 2) {
                kind = Kind.PLI;
                int[] pli = decodePli(parser.readBytes());
                latitude = pli[0] * 1e-7d;
                longitude = pli[1] * 1e-7d;
                altitude = pli[2];
                speed = pli[3];
                course = pli[4];
            } else if (field == 6 && wire == 2) {
                kind = Kind.CHAT;
                String[] chat = decodeChat(parser.readBytes());
                chatMessage = chat[0];
                chatTo = chat[1];
                chatToCallsign = chat[2];
            } else if (field == 7 && wire == 2) {
                kind = Kind.DETAIL;
                detail = parser.readBytes();
            } else {
                parser.skip(wire);
            }
        }
        return new Packet(compressed, contact, team, role, battery, kind, latitude, longitude, altitude,
                speed, course, chatMessage, chatTo, chatToCallsign, detail);
    }

    public static int teamToEnum(String team) {
        if (team == null) {
            return 10;
        }
        switch (team.trim().toLowerCase(Locale.US)) {
            case "white":
                return 1;
            case "yellow":
                return 2;
            case "orange":
                return 3;
            case "magenta":
                return 4;
            case "red":
                return 5;
            case "maroon":
                return 6;
            case "purple":
                return 7;
            case "dark blue":
                return 8;
            case "blue":
                return 9;
            case "teal":
                return 11;
            case "green":
                return 12;
            case "dark green":
                return 13;
            case "brown":
                return 14;
            default:
                return 10;
        }
    }

    public static String enumToTeam(int value) {
        switch (value) {
            case 1:
                return "White";
            case 2:
                return "Yellow";
            case 3:
                return "Orange";
            case 4:
                return "Magenta";
            case 5:
                return "Red";
            case 6:
                return "Maroon";
            case 7:
                return "Purple";
            case 8:
                return "Dark Blue";
            case 9:
                return "Blue";
            case 11:
                return "Teal";
            case 12:
                return "Green";
            case 13:
                return "Dark Green";
            case 14:
                return "Brown";
            default:
                return "Cyan";
        }
    }

    public static int roleToEnum(String role) {
        if (role == null) {
            return 1;
        }
        switch (role.trim().toLowerCase(Locale.US)) {
            case "team lead":
                return 2;
            case "hq":
                return 3;
            case "sniper":
                return 4;
            case "medic":
                return 5;
            case "forward observer":
                return 6;
            case "rto":
                return 7;
            case "k9":
                return 8;
            default:
                return 1;
        }
    }

    public static String enumToRole(int value) {
        switch (value) {
            case 2:
                return "Team Lead";
            case 3:
                return "HQ";
            case 4:
                return "Sniper";
            case 5:
                return "Medic";
            case 6:
                return "Forward Observer";
            case 7:
                return "RTO";
            case 8:
                return "K9";
            default:
                return "Team Member";
        }
    }

    public static int latitudeToFixed(double latitude) {
        return (int) Math.round(latitude * 1e7d);
    }

    private static byte[] encodeContact(Contact contact) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!contact.callsign.isEmpty()) {
            writeString(output, 1, contact.callsign);
        }
        if (!contact.deviceCallsign.isEmpty()) {
            writeString(output, 2, contact.deviceCallsign);
        }
        return output.toByteArray();
    }

    private static byte[] encodePli(Packet packet) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeSfixed32(output, 1, latitudeToFixed(packet.latitude));
        writeSfixed32(output, 2, latitudeToFixed(packet.longitude));
        if (packet.altitude != 0) {
            writeVarintKey(output, 3, 0);
            writeVarint(output, packet.altitude & 0xFFFFFFFFL);
        }
        if (packet.speed != 0) {
            writeVarintKey(output, 4, 0);
            writeVarint(output, packet.speed);
        }
        if (packet.course != 0) {
            writeVarintKey(output, 5, 0);
            writeVarint(output, packet.course);
        }
        return output.toByteArray();
    }

    private static byte[] encodeChat(Packet packet) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeString(output, 1, packet.chatMessage);
        if (!packet.chatTo.isEmpty()) {
            writeString(output, 2, packet.chatTo);
        }
        if (!packet.chatToCallsign.isEmpty()) {
            writeString(output, 3, packet.chatToCallsign);
        }
        return output.toByteArray();
    }

    private static Contact decodeContact(byte[] bytes) {
        Parser parser = new Parser(bytes);
        String callsign = "";
        String device = "";
        while (parser.hasRemaining()) {
            long key = parser.readVarint();
            int field = (int) (key >>> 3);
            int wire = (int) (key & 7);
            if (wire != 2) {
                parser.skip(wire);
                continue;
            }
            String value = new String(parser.readBytes(), StandardCharsets.UTF_8);
            if (field == 1) {
                callsign = value;
            } else if (field == 2) {
                device = value;
            }
        }
        return new Contact(callsign, device);
    }

    private static int[] decodeGroup(byte[] bytes) {
        Parser parser = new Parser(bytes);
        int role = 1;
        int team = 10;
        while (parser.hasRemaining()) {
            long key = parser.readVarint();
            int field = (int) (key >>> 3);
            int wire = (int) (key & 7);
            if (wire != 0) {
                parser.skip(wire);
                continue;
            }
            int value = (int) parser.readVarint();
            if (field == 1) {
                role = value;
            } else if (field == 2) {
                team = value;
            }
        }
        return new int[] {role, team};
    }

    private static int decodeBattery(byte[] bytes) {
        Parser parser = new Parser(bytes);
        int battery = 0;
        while (parser.hasRemaining()) {
            long key = parser.readVarint();
            int field = (int) (key >>> 3);
            int wire = (int) (key & 7);
            if (field == 1 && wire == 0) {
                battery = (int) parser.readVarint();
            } else {
                parser.skip(wire);
            }
        }
        return battery;
    }

    private static int[] decodePli(byte[] bytes) {
        Parser parser = new Parser(bytes);
        int lat = 0;
        int lon = 0;
        int alt = 0;
        int speed = 0;
        int course = 0;
        while (parser.hasRemaining()) {
            long key = parser.readVarint();
            int field = (int) (key >>> 3);
            int wire = (int) (key & 7);
            if (field == 1 && wire == 5) {
                lat = parser.readSfixed32();
            } else if (field == 2 && wire == 5) {
                lon = parser.readSfixed32();
            } else if (field == 3 && wire == 0) {
                alt = (int) parser.readVarint();
            } else if (field == 4 && wire == 0) {
                speed = (int) parser.readVarint();
            } else if (field == 5 && wire == 0) {
                course = (int) parser.readVarint();
            } else {
                parser.skip(wire);
            }
        }
        return new int[] {lat, lon, alt, speed, course};
    }

    private static String[] decodeChat(byte[] bytes) {
        Parser parser = new Parser(bytes);
        String message = "";
        String to = "";
        String toCallsign = "";
        while (parser.hasRemaining()) {
            long key = parser.readVarint();
            int field = (int) (key >>> 3);
            int wire = (int) (key & 7);
            if (wire != 2) {
                parser.skip(wire);
                continue;
            }
            String value = new String(parser.readBytes(), StandardCharsets.UTF_8);
            if (field == 1) {
                message = value;
            } else if (field == 2) {
                to = value;
            } else if (field == 3) {
                toCallsign = value;
            }
        }
        return new String[] {message, to, toCallsign};
    }

    private static void writeString(ByteArrayOutputStream output, int field, String value) {
        writeLengthDelimited(output, field, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeLengthDelimited(ByteArrayOutputStream output, int field, byte[] value) {
        writeVarintKey(output, field, 2);
        writeVarint(output, value.length);
        output.write(value, 0, value.length);
    }

    private static void writeSfixed32(ByteArrayOutputStream output, int field, int value) {
        writeVarintKey(output, field, 5);
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 24) & 0xFF);
    }

    private static void writeVarintKey(ByteArrayOutputStream output, int field, int wire) {
        writeVarint(output, ((long) field << 3) | wire);
    }

    private static void writeVarint(ByteArrayOutputStream output, long value) {
        long remaining = value;
        while ((remaining & ~0x7FL) != 0) {
            output.write((int) ((remaining & 0x7F) | 0x80));
            remaining >>>= 7;
        }
        output.write((int) remaining);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Parser {
        private final byte[] data;
        private int offset;

        private Parser(byte[] data) {
            this.data = data;
        }

        private boolean hasRemaining() {
            return offset < data.length;
        }

        private long readVarint() {
            long result = 0;
            int shift = 0;
            while (offset < data.length) {
                int next = data[offset++] & 0xFF;
                result |= (long) (next & 0x7F) << shift;
                if ((next & 0x80) == 0) {
                    return result;
                }
                shift += 7;
                if (shift > 63) {
                    break;
                }
            }
            return result;
        }

        private byte[] readBytes() {
            int length = (int) readVarint();
            if (length < 0 || offset + length > data.length) {
                offset = data.length;
                return new byte[0];
            }
            byte[] slice = Arrays.copyOfRange(data, offset, offset + length);
            offset += length;
            return slice;
        }

        private int readSfixed32() {
            if (offset + 4 > data.length) {
                offset = data.length;
                return 0;
            }
            int value = (data[offset] & 0xFF)
                    | ((data[offset + 1] & 0xFF) << 8)
                    | ((data[offset + 2] & 0xFF) << 16)
                    | ((data[offset + 3] & 0xFF) << 24);
            offset += 4;
            return value;
        }

        private void skip(int wire) {
            if (wire == 0) {
                readVarint();
            } else if (wire == 1) {
                offset = Math.min(data.length, offset + 8);
            } else if (wire == 2) {
                readBytes();
            } else if (wire == 5) {
                offset = Math.min(data.length, offset + 4);
            } else {
                offset = data.length;
            }
        }

    }
}
