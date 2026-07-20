package com.akitaengineering.meshtak;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransportFrameCodecTest {
    @Test
    public void fragmentedPayloadRoundTripsInOrder() {
        String payload = "ENC:v2:k1:1234567890:0011223344556677:" + "a".repeat(700);
        List<byte[]> frames = TransportFrameCodec.frame(payload.getBytes(StandardCharsets.UTF_8), 180);
        TransportFrameCodec.Reassembler reassembler = new TransportFrameCodec.Reassembler();
        String result = null;
        for (byte[] frame : frames) {
            assertTrue(frame.length <= 180);
            result = reassembler.accept(new String(frame, StandardCharsets.UTF_8));
        }
        assertEquals(payload, result);
    }

    @Test
    public void outOfOrderFrameResetsMessage() {
        List<byte[]> frames = TransportFrameCodec.frame("x".repeat(500).getBytes(StandardCharsets.UTF_8), 100);
        TransportFrameCodec.Reassembler reassembler = new TransportFrameCodec.Reassembler();
        assertNull(reassembler.accept(new String(frames.get(1), StandardCharsets.UTF_8)));
        assertNull(reassembler.accept(new String(frames.get(0), StandardCharsets.UTF_8)));
        assertNull(reassembler.accept(new String(frames.get(2), StandardCharsets.UTF_8)));
    }

    @Test
    public void oversizedReassemblyIsRejected() {
        List<byte[]> frames = TransportFrameCodec.frame("x".repeat(2100).getBytes(StandardCharsets.UTF_8), 100);
        TransportFrameCodec.Reassembler reassembler = new TransportFrameCodec.Reassembler();
        String result = null;
        for (byte[] frame : frames) {
            result = reassembler.accept(new String(frame, StandardCharsets.UTF_8));
        }
        assertNull(result);
    }
}
