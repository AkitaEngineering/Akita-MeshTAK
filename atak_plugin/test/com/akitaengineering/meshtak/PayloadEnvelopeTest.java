package com.akitaengineering.meshtak;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class PayloadEnvelopeTest {
    private SecurityManager securityManager;

    @Before
    public void setUp() {
        ReplayGuard.resetForTests();
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

    @Test
    public void overlappingKeyIdRemainsReadableAfterRotation() {
        byte[] encoded = PayloadEnvelope.encode(securityManager, "CMD:GET_BATT".getBytes(StandardCharsets.UTF_8));
        String previousEnvelope = new String(encoded, StandardCharsets.UTF_8);

        assertTrue(securityManager.initializeFromProvisioning(
                "AkitaNode01",
                "RotatedDeviceSecret1234567890",
                com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID_K2,
                "UniqueDeviceSecret1234567890",
                com.akitaengineering.meshtak.Config.ENCRYPTED_KEY_ID_K1));

        assertEquals("CMD:GET_BATT", PayloadEnvelope.decode(securityManager, previousEnvelope));
        byte[] rotated = PayloadEnvelope.encode(securityManager, "CMD:GET_VERSION".getBytes(StandardCharsets.UTF_8));
        String rotatedEnvelope = new String(rotated, StandardCharsets.UTF_8);
        assertTrue(rotatedEnvelope.startsWith("ENC:v2:k2:"));
        assertEquals("CMD:GET_VERSION", PayloadEnvelope.decode(securityManager, rotatedEnvelope));
    }

    @Test
    public void persistedReplayCacheSurvivesRestart() throws Exception {
        File stateFile = File.createTempFile("akita-replay", ".json");
        stateFile.deleteOnExit();
        ReplayGuard.attach(stateFile);

        byte[] encoded = PayloadEnvelope.encode(securityManager, "CMD:GET_BATT".getBytes(StandardCharsets.UTF_8));
        String envelope = new String(encoded, StandardCharsets.UTF_8);
        assertEquals("CMD:GET_BATT", PayloadEnvelope.decode(securityManager, envelope));

        ReplayGuard.attach(stateFile);
        assertNull(PayloadEnvelope.decode(securityManager, envelope));
    }
}
