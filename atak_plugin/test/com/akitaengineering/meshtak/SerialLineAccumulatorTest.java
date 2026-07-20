package com.akitaengineering.meshtak;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SerialLineAccumulatorTest {
    @Test
    public void reconstructsSplitAndCombinedLines() {
        SerialLineAccumulator accumulator = new SerialLineAccumulator(100);
        assertTrue(accumulator.accept("ENC:one".getBytes(StandardCharsets.UTF_8)).isEmpty());
        List<String> lines = accumulator.accept("\nENC:two\r\npart".getBytes(StandardCharsets.UTF_8));
        assertEquals(List.of("ENC:one", "ENC:two"), lines);
        assertEquals(List.of("partial"), accumulator.accept("ial\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void oversizedRecordIsDiscardedThroughItsTerminator() {
        SerialLineAccumulator accumulator = new SerialLineAccumulator(5);
        assertTrue(accumulator.accept("123456tail".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertEquals(List.of("valid"), accumulator.accept("\nvalid\n".getBytes(StandardCharsets.UTF_8)));
    }
}
