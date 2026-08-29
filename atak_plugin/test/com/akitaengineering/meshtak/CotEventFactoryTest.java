package com.akitaengineering.meshtak;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CotEventFactoryTest {

    @Test
    public void locationEventIncludesIdentityMissionAndStale() {
        long start = 1_770_000_000_000L;
        String xml = CotEventFactory.locationEvent(
                "Alpha-1", "Alpha 1", "Cyan", "Team Lead", "River Search",
                45.4215d, -75.6972d, 70.0d, start, 180);
        assertTrue(xml.contains("type='a-f-G-U-U'"));
        assertTrue(xml.contains("callsign='Alpha 1'"));
        assertTrue(xml.contains("<__group name='Cyan' role='Team Lead'/>"));
        assertTrue(xml.contains("<dest mission='River Search'/>"));
        assertEquals(start + 180_000L, CotEventFactory.parseStaleEpochMillis(xml));
        assertEquals("Alpha-1", CotEventFactory.readAttribute(xml, "uid"));
    }

    @Test
    public void geoChatRoomAndDirectUseExpectedType() {
        String room = CotEventFactory.geoChatEvent(
                "Alpha1", "All Chat Rooms", "Status green", false, "River Search",
                1.0d, 2.0d, 1_770_000_000_000L, 120);
        assertTrue(room.contains("type='b-t-f'"));
        assertTrue(room.contains("chatroom='All Chat Rooms'"));
        assertTrue(room.contains("Status green"));
        assertTrue(room.contains("<dest mission='River Search'/>"));

        String direct = CotEventFactory.geoChatEvent(
                "Alpha1", "Bravo1", "Need extract", true, "",
                0d, 0d, 1_770_000_000_000L, 120);
        assertTrue(direct.contains("uid='GeoChat.Alpha1.Bravo1."));
        assertTrue(direct.contains("to='Bravo1'"));
    }

    @Test
    public void mailboxChatRoundTrip() {
        String compact = CotEventFactory.toMailboxChatPayload("All Chat Rooms", "Ping", false);
        CotEventFactory.ChatPayload parsed = CotEventFactory.parseMailboxChat(compact);
        assertNotNull(parsed);
        assertFalse(parsed.directMessage);
        assertEquals("All Chat Rooms", parsed.destination);
        assertEquals("Ping", parsed.message);

        String dm = CotEventFactory.toMailboxChatPayload("Bravo1", "Need extract", true);
        CotEventFactory.ChatPayload parsedDm = CotEventFactory.parseMailboxChat(dm);
        assertNotNull(parsedDm);
        assertTrue(parsedDm.directMessage);
        assertEquals("Bravo1", parsedDm.destination);
    }

    @Test
    public void xmlEscapeAndSingleQuoteAttributes() {
        String xml = CotEventFactory.locationEvent(
                "A&B", "A<B>", "Cyan", "Team Member", "Q",
                0d, 0d, 0d, 1_770_000_000_000L, 120);
        assertTrue(xml.contains("uid='A&amp;B'"));
        assertTrue(xml.contains("callsign='A&lt;B&gt;'"));
        assertEquals("A&amp;B", CotEventFactory.readAttribute(xml, "uid"));
    }
}
