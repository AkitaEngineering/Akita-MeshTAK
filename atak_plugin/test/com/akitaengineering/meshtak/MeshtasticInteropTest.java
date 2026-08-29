package com.akitaengineering.meshtak;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MeshtasticInteropTest {

    @Test
    public void mqttTopicUsesOpenTakServerLayout() {
        assertEquals("!00ab12ef", MeshtasticMqttCodec.nodeIdToSender("AB12EF"));
        assertEquals(0x00ab12efL, MeshtasticMqttCodec.nodeIdToFrom("!00ab12ef"));
        assertEquals("msh/2/json/LongFast/!00ab12ef",
                MeshtasticMqttCodec.topic("msh/2/json", "LongFast", "AB12EF"));
    }

    @Test
    public void nodeInfoPrefersLongNameAndBuildsPli() {
        MeshtasticNodeInfo node = new MeshtasticNodeInfo(
                "A1B2C3D4", "Alpha One", "ALP", 45.0d, -75.0d, 12.0d, 80, 3, 45);
        assertEquals("Alpha One", node.callsign());
        assertEquals("A1B2C3D4", node.uid());
        AtakPluginPacket.Packet pli = node.toPli("Cyan", "Team Member");
        assertEquals(AtakPluginPacket.Kind.PLI, pli.kind);
        assertEquals(80, pli.battery);
        String cot = node.locationCoT("Cyan", "Team Member", "River", 1_770_000_000_000L, 120);
        assertTrue(cot.contains("callsign='Alpha One'"));
        assertTrue(cot.contains("<status battery='80'/>"));
        assertTrue(cot.contains("speed='3.0'"));
    }

    @Test
    public void dataPackageReferenceIsMeshSafe() {
        DataPackageHandoff.Reference reference = new DataPackageHandoff.Reference(
                "sector-map.zip",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                2048,
                "https://tak.example.local:8443/Marti/sync/content?hash=0123",
                "River Search");
        assertTrue(reference.isComplete());
        String cot = DataPackageHandoff.fileShareCoT(
                reference, "Alpha1", "Cyan", "Team Member", 1.0d, 2.0d, 1_770_000_000_000L, 120);
        assertTrue(cot.contains("type='b-f-t-file'"));
        assertTrue(cot.contains("filename='sector-map.zip'"));
        assertTrue(cot.contains("sha256hash='0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'"));
        assertTrue(DataPackageHandoff.strategySummary().contains("ATAK/OpenTAKServer"));
        assertTrue(DataPackageHandoff.isValidSha256(reference.sha256));
    }
}
