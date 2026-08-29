package com.akitaengineering.meshtak;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Commands pushed to the controller after a bearer becomes ready.
 */
public final class FirmwareRuntimeSync {

    private static final String TAG = "FirmwareRuntimeSync";
    private static final long COMMAND_STAGGER_MS = 100L;

    public interface CommandSender {
        boolean send(byte[] commandBytes);
    }

    private FirmwareRuntimeSync() {
    }

    public static List<String> buildCommands(SharedPreferences preferences, long epochSeconds) {
        List<String> commands = new ArrayList<>(5);
        commands.add(Config.CMD_TIME_SYNC_PREFIX + epochSeconds);
        commands.add(Config.CMD_COT_MISSION_PREFIX + OperatorIdentity.getMissionName(preferences));
        commands.add(OperatorIdentity.encodeIdentityCommand(preferences));
        commands.add(OperatorIdentity.encodeStaleCommand(preferences));
        boolean atakPlugin = preferences.getBoolean(MeshtasticMqttCodec.PREF_ATAK_PLUGIN, false);
        commands.add(Config.CMD_MESH_ATAK_PREFIX + (atakPlugin ? "1" : "0"));
        commands.add(Config.CMD_GET_SEC_STATE);
        return commands;
    }

    public static void sendCommands(Handler handler, SharedPreferences preferences, CommandSender sender) {
        if (handler == null || sender == null) {
            return;
        }
        List<String> commands = buildCommands(preferences, System.currentTimeMillis() / 1000L);
        for (int index = 0; index < commands.size(); index++) {
            final String command = commands.get(index);
            handler.postDelayed(() -> sender.send((command + "\n").getBytes(StandardCharsets.UTF_8)),
                    index * COMMAND_STAGGER_MS);
        }
    }

    public static boolean consumeStatus(String line) {
        if (line == null) {
            return false;
        }
        if (line.startsWith(Config.STATUS_TIME_SYNC_PREFIX)
                || line.startsWith(Config.STATUS_COT_MISSION_PREFIX)
                || line.startsWith(Config.STATUS_COT_IDENTITY_PREFIX)
                || line.startsWith(Config.STATUS_COT_STALE_PREFIX)
                || line.startsWith(Config.STATUS_MESH_ATAK_PREFIX)) {
            Log.i(TAG, "Firmware runtime status: " + line);
            return true;
        }
        if (DeviceSecurityState.updateFromStatusLine(line)) {
            Log.i(TAG, "Firmware security state: " + DeviceSecurityState.getKeySummary()
                    + " • " + DeviceSecurityState.getHardwareSummary());
            return true;
        }
        return false;
    }
}
