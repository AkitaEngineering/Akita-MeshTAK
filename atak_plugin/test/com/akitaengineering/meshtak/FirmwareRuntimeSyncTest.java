package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class FirmwareRuntimeSyncTest {

    private SharedPreferences preferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
        preferences.edit()
                .putString(OperatorIdentity.PREF_CALLSIGN, "Alpha1")
                .putString(OperatorIdentity.PREF_TEAM, "Cyan")
                .putString(OperatorIdentity.PREF_ROLE, "Team Lead")
                .putString(OperatorIdentity.PREF_STALE_SECONDS, "180")
                .putString(OperatorIdentity.PREF_OPENTAKSERVER_MISSION_NAME, "River Search")
                .commit();
    }

    @Test
    public void buildCommandsIncludesIdentityAndStale() {
        List<String> commands = FirmwareRuntimeSync.buildCommands(preferences, 1770000000L);
        assertEquals(6, commands.size());
        assertEquals(com.akitaengineering.meshtak.Config.CMD_TIME_SYNC_PREFIX + "1770000000", commands.get(0));
        assertEquals(com.akitaengineering.meshtak.Config.CMD_COT_MISSION_PREFIX + "River Search", commands.get(1));
        assertEquals(com.akitaengineering.meshtak.Config.CMD_COT_IDENTITY_PREFIX + "Alpha1|Cyan|Team Lead", commands.get(2));
        assertEquals(com.akitaengineering.meshtak.Config.CMD_COT_STALE_PREFIX + "180", commands.get(3));
        assertEquals(com.akitaengineering.meshtak.Config.CMD_MESH_ATAK_PREFIX + "0", commands.get(4));
        assertEquals(com.akitaengineering.meshtak.Config.CMD_GET_SEC_STATE, commands.get(5));
    }

    @Test
    public void consumeStatusAcceptsIdentityAndStale() {
        assertTrue(FirmwareRuntimeSync.consumeStatus(com.akitaengineering.meshtak.Config.STATUS_COT_IDENTITY_PREFIX + "Alpha1|Cyan|Team Lead"));
        assertTrue(FirmwareRuntimeSync.consumeStatus(com.akitaengineering.meshtak.Config.STATUS_COT_STALE_PREFIX + "180"));
        assertTrue(FirmwareRuntimeSync.consumeStatus(com.akitaengineering.meshtak.Config.STATUS_TIME_SYNC_PREFIX + "OK:1770000000"));
    }
}
