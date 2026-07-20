package com.akitaengineering.meshtak;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small ordered framing protocol used to carry envelopes over BLE attributes. */
public final class TransportFrameCodec {
    private static final String PREFIX = "F1|";
    private static final int HEADER_BUDGET = 32;
    private static final int MAX_PARTS = 128;
    private static final int MAX_REASSEMBLED_BYTES = 2048;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TransportFrameCodec() {
    }

    public static List<byte[]> frame(byte[] payload, int maxFrameBytes) {
        if (payload == null || payload.length == 0 || maxFrameBytes <= HEADER_BUDGET) {
            throw new IllegalArgumentException("Payload and negotiated frame size are required.");
        }
        String value = new String(payload, StandardCharsets.UTF_8);
        int chunkSize = maxFrameBytes - HEADER_BUDGET;
        int count = (value.length() + chunkSize - 1) / chunkSize;
        if (count < 1 || count > MAX_PARTS) {
            throw new IllegalArgumentException("Payload requires too many BLE frames.");
        }

        String messageId = String.format(Locale.US, "%08x", RANDOM.nextInt());
        List<byte[]> frames = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int start = index * chunkSize;
            int end = Math.min(value.length(), start + chunkSize);
            String frame = PREFIX + messageId + "|" + index + "|" + count + "|" + value.substring(start, end);
            byte[] encoded = frame.getBytes(StandardCharsets.UTF_8);
            if (encoded.length > maxFrameBytes) {
                throw new IllegalStateException("Generated BLE frame exceeds negotiated size.");
            }
            frames.add(encoded);
        }
        return frames;
    }

    public static final class Reassembler {
        private String activeMessageId;
        private int expectedIndex;
        private int expectedCount;
        private final StringBuilder payload = new StringBuilder();

        public synchronized String accept(String frame) {
            if (frame == null || !frame.startsWith(PREFIX)) {
                reset();
                return null;
            }
            String[] parts = frame.split("\\|", 5);
            if (parts.length != 5 || parts[1].length() != 8) {
                reset();
                return null;
            }
            int index;
            int count;
            try {
                index = Integer.parseInt(parts[2]);
                count = Integer.parseInt(parts[3]);
            } catch (NumberFormatException exception) {
                reset();
                return null;
            }
            if (count < 1 || count > MAX_PARTS || index < 0 || index >= count) {
                reset();
                return null;
            }
            if (index == 0) {
                reset();
                activeMessageId = parts[1];
                expectedCount = count;
            }
            if (!parts[1].equals(activeMessageId) || count != expectedCount || index != expectedIndex) {
                reset();
                return null;
            }
            payload.append(parts[4]);
            if (payload.length() > MAX_REASSEMBLED_BYTES) {
                reset();
                return null;
            }
            expectedIndex++;
            if (expectedIndex != expectedCount) {
                return null;
            }
            String complete = payload.toString();
            reset();
            return complete;
        }

        public synchronized void reset() {
            activeMessageId = null;
            expectedIndex = 0;
            expectedCount = 0;
            payload.setLength(0);
        }
    }
}
