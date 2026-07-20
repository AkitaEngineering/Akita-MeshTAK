package com.akitaengineering.meshtak;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Incrementally reconstructs newline-delimited UTF-8 protocol records. */
public final class SerialLineAccumulator {
    private final int maximumLineLength;
    private final StringBuilder buffer = new StringBuilder();
    private boolean discardingOversizedLine;

    public SerialLineAccumulator(int maximumLineLength) {
        this.maximumLineLength = maximumLineLength;
    }

    public synchronized List<String> accept(byte[] bytes) {
        List<String> lines = new ArrayList<>();
        if (bytes == null) return lines;
        String chunk = new String(bytes, StandardCharsets.UTF_8);
        for (int index = 0; index < chunk.length(); index++) {
            char c = chunk.charAt(index);
            if (c == '\n') {
                if (discardingOversizedLine) {
                    discardingOversizedLine = false;
                    buffer.setLength(0);
                    continue;
                }
                String line = buffer.toString();
                buffer.setLength(0);
                if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
                if (!line.isEmpty()) lines.add(line);
            } else if (discardingOversizedLine) {
                continue;
            } else if (buffer.length() < maximumLineLength) {
                buffer.append(c);
            } else {
                // Drop the entire oversized record instead of accepting a suffix.
                buffer.setLength(0);
                discardingOversizedLine = true;
            }
        }
        return lines;
    }

    public synchronized void reset() {
        buffer.setLength(0);
        discardingOversizedLine = false;
    }
}
