package com.akitaengineering.meshtak;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DeviceSecurityStateTest {

    @Before
    public void setUp() {
        DeviceSecurityState.resetForTests();
    }

    @Test
    public void parsesControllerSecurityStatus() {
        assertTrue(DeviceSecurityState.updateFromStatusLine("STATUS:SEC_STATE:key=k2:prev=k1:flash=1:boot=0"));
        assertEquals("k2", DeviceSecurityState.getActiveKeyId());
        assertEquals("k1", DeviceSecurityState.getPreviousKeyId());
        assertTrue(DeviceSecurityState.hasFlashEncryption());
        assertFalse(DeviceSecurityState.hasSecureBoot());
        assertTrue(DeviceSecurityState.getKeySummary().contains("k2"));
        assertTrue(DeviceSecurityState.getHardwareSummary().contains("secure boot off"));
    }

    @Test
    public void rejectsMalformedSecurityStatus() {
        assertFalse(DeviceSecurityState.updateFromStatusLine("STATUS:VERSION:0.2.1"));
        assertFalse(DeviceSecurityState.updateFromStatusLine("STATUS:SEC_STATE:flash=1"));
        assertFalse(DeviceSecurityState.hasReport());
    }
}
