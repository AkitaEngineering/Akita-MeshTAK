package com.akitaengineering.meshtak;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PayloadEnvelopeTest {
    private SecurityManager securityManager;

    @Before
    public void setUp() {
        securityManager = SecurityManager.getInstance();
        securityManager.reset();
        securityManager.initializeFromProvisioning("AkitaNode01", "UniqueDeviceSecret1234567890");
        securityManager.setEncryptionEnabled(true);
    }

    @Test
    public void roundTripRejectsReplayAndPlaintext() {
        byte[] encoded = PayloadEnvelope.encode(securityManager, "CMD:GET_BATT".getBytes(StandardCharsets.UTF_8));
        String envelope = new String(encoded, StandardCharsets.UTF_8);
        assertEquals("CMD:GET_BATT", PayloadEnvelope.decode(securityManager, envelope));
        assertNull(PayloadEnvelope.decode(securityManager, envelope));
        assertNull(PayloadEnvelope.decode(securityManager, "CMD:GET_BATT"));
    }

    @Test
    public void invalidAuthenticationDoesNotConsumeReplayNonce() {
        byte[] encoded = PayloadEnvelope.encode(securityManager, "CMD:GET_VERSION".getBytes(StandardCharsets.UTF_8));
        String envelope = new String(encoded, StandardCharsets.UTF_8);
        char replacement = envelope.endsWith("0") ? '1' : '0';
        String tampered = envelope.substring(0, envelope.length() - 1) + replacement;
        assertNull(PayloadEnvelope.decode(securityManager, tampered));
        assertEquals("CMD:GET_VERSION", PayloadEnvelope.decode(securityManager, envelope));
    }

    @Test
    public void legacyV1EnvelopeIsRejected() {
        assertNull(PayloadEnvelope.decode(securityManager, "ENC:v1:k1:00"));
    }
}
