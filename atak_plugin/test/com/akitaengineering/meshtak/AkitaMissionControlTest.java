package com.akitaengineering.meshtak;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AkitaMissionControlTest {

    private Context context;
    private SharedPreferences preferences;
    private File stateFile;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().commit();
        stateFile = new File(context.getNoBackupFilesDir(), "akita-mission-state.json");
        if (stateFile.exists()) {
            stateFile.delete();
        }
        Field instanceField = AkitaMissionControl.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void queueMessageWritesMissionStateToFileAndSignalsPreferences() {
        AkitaMissionControl missionControl = AkitaMissionControl.getInstance(context);

        missionControl.queueMessage("Plain Text", "test payload", AkitaMissionControl.ROUTE_BLE);

        AkitaMissionControl.QueueSnapshot snapshot = missionControl.getQueueSnapshot(true);
        assertEquals(1, snapshot.pendingCount);
        assertTrue(stateFile.exists());
        assertTrue(preferences.getAll().get(AkitaMissionControl.PREF_MAILBOX_RECORDS) instanceof Long);
        assertTrue(preferences.getAll().get(AkitaMissionControl.PREF_REPLAY_EVENTS) instanceof Long);
    }

    @Test
    public void legacyPreferencePayloadsMigrateIntoFileStore() throws Exception {
        JSONObject mailboxRecord = new JSONObject();
        mailboxRecord.put("messageId", "MSGLEGACY");
        mailboxRecord.put("format", "Plain Text");
        mailboxRecord.put("payload", "legacy");
        mailboxRecord.put("preferredRoute", AkitaMissionControl.ROUTE_BLE);
        mailboxRecord.put("lastRoute", "");
        mailboxRecord.put("status", AkitaMissionControl.STATUS_PENDING);
        mailboxRecord.put("attempts", 0);
        mailboxRecord.put("createdAt", 1L);
        mailboxRecord.put("updatedAt", 1L);
        mailboxRecord.put("detail", "legacy import");
        mailboxRecord.put("payloadBytes", 6);

        JSONObject replayEvent = new JSONObject();
        replayEvent.put("timestamp", 1L);
        replayEvent.put("eventType", "MAILBOX_QUEUED");
        replayEvent.put("route", AkitaMissionControl.ROUTE_BLE);
        replayEvent.put("status", AkitaMissionControl.STATUS_PENDING);
        replayEvent.put("format", "Plain Text");
        replayEvent.put("payload", "legacy");
        replayEvent.put("detail", "legacy import");

        preferences.edit()
                .putString(AkitaMissionControl.PREF_MAILBOX_RECORDS, new JSONArray().put(mailboxRecord).toString())
                .putString(AkitaMissionControl.PREF_REPLAY_EVENTS, new JSONArray().put(replayEvent).toString())
                .commit();

        AkitaMissionControl missionControl = AkitaMissionControl.getInstance(context);
        AkitaMissionControl.QueueSnapshot snapshot = missionControl.getQueueSnapshot(true);

        assertEquals(1, snapshot.pendingCount);
        assertEquals(1, missionControl.getReplayTimeline().size());
        assertTrue(stateFile.exists());
    }
}
