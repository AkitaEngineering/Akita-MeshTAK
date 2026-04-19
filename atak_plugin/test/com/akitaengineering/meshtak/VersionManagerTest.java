package com.akitaengineering.meshtak;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class VersionManagerTest {

    @Test
    public void compareVersionsHandlesDifferentSegmentLengths() {
        assertEquals(0, VersionManager.compareVersions("0.2", "0.2.0"));
        assertTrue(VersionManager.compareVersions("0.2.1", "0.2.0") > 0);
        assertTrue(VersionManager.compareVersions("0.1.9", "0.2.0") < 0);
    }

    @Test
    public void firmwareCompatibilityUsesConfiguredBounds() {
        assertTrue(VersionManager.isFirmwareCompatible(BuildConfig.MIN_FIRMWARE_VERSION));
        assertFalse(VersionManager.isFirmwareCompatible("0.0.1"));
        assertFalse(VersionManager.isFirmwareCompatible("9.0.0"));
    }
}
