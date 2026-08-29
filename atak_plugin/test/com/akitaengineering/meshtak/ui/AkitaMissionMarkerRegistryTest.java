package com.akitaengineering.meshtak.ui;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AkitaMissionMarkerRegistryTest {

    @Before
    public void setUp() {
        AkitaMissionMarkerRegistry.getInstance().resetForTests();
    }

    @Test
    public void staleUsesCotStaleTimestampWhenPresent() {
        AkitaMissionMarkerRegistry registry = AkitaMissionMarkerRegistry.getInstance();
        long now = System.currentTimeMillis();
        registry.recordMarker("fresh", "Fresh", 1d, 2d, "BLE", now + 60_000L);
        registry.recordMarker("stale", "Stale", 3d, 4d, "BLE", now - 1_000L);
        List<AkitaMissionMarkerRegistry.TrackedMarker> stale = registry.getStaleMarkers(5L * 60L * 1000L);
        assertEquals(1, stale.size());
        assertEquals("stale", stale.get(0).uid);
    }

    @Test
    public void missingStaleFallsBackToLastUpdateThreshold() throws Exception {
        AkitaMissionMarkerRegistry registry = AkitaMissionMarkerRegistry.getInstance();
        registry.recordMarker("old", "Old", 1d, 2d, "Serial");
        Thread.sleep(20);
        List<AkitaMissionMarkerRegistry.TrackedMarker> stale = registry.getStaleMarkers(5L);
        assertTrue(stale.size() >= 1);
        assertEquals("old", stale.get(0).uid);
    }
}
