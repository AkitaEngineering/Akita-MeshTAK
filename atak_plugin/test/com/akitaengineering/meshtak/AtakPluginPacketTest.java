package com.akitaengineering.meshtak;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AtakPluginPacketTest {

    @Test
    public void pliRoundTripPreservesCoordinatesAndIdentity() {
        AtakPluginPacket.Packet packet = AtakPluginPacket.Packet.pli(
                new AtakPluginPacket.Contact("Alpha1", "A1B2C3D4"),
                "Cyan",
                "Team Lead",
                78,
                45.4215d,
                -75.6972d,
                70,
                2,
                90);
        byte[] encoded = AtakPluginPacket.encode(packet);
        assertTrue(encoded.length > 0);
        assertTrue(encoded.length <= AtakPluginPacket.MAX_PAYLOAD_BYTES);
        AtakPluginPacket.Packet decoded = AtakPluginPacket.decode(encoded);
        assertNotNull(decoded);
        assertEquals(AtakPluginPacket.Kind.PLI, decoded.kind);
        assertEquals("Alpha1", decoded.contact.callsign);
        assertEquals("Cyan", decoded.team);
        assertEquals("Team Lead", decoded.role);
        assertEquals(78, decoded.battery);
        assertEquals(45.4215d, decoded.latitude, 0.0000002d);
        assertEquals(-75.6972d, decoded.longitude, 0.0000002d);
        assertEquals(70, decoded.altitude);
        assertEquals(2, decoded.speed);
        assertEquals(90, decoded.course);
    }

    @Test
    public void chatRoundTripPreservesDirectRecipient() {
        AtakPluginPacket.Packet packet = AtakPluginPacket.Packet.chat(
                new AtakPluginPacket.Contact("Alpha1", "Alpha1"),
                "Red",
                "Medic",
                0,
                "Need extract",
                "Bravo1",
                "Bravo1");
        AtakPluginPacket.Packet decoded = AtakPluginPacket.decode(AtakPluginPacket.encode(packet));
        assertNotNull(decoded);
        assertEquals(AtakPluginPacket.Kind.CHAT, decoded.kind);
        assertEquals("Need extract", decoded.chatMessage);
        assertEquals("Bravo1", decoded.chatToCallsign);
        assertEquals("Red", decoded.team);
        assertEquals("Medic", decoded.role);
    }

    @Test
    public void teamAndRoleEnumsMatchMeshtasticTable() {
        assertEquals(10, AtakPluginPacket.teamToEnum("Cyan"));
        assertEquals(8, AtakPluginPacket.teamToEnum("Dark Blue"));
        assertEquals("Forward Observer", AtakPluginPacket.enumToRole(6));
        assertEquals(72, AtakPluginPacket.PORTNUM);
    }
}
