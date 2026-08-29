package com.akitaengineering.meshtak;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class OpenTakMissionApiTest {

    @Test
    public void parseMissionNamesFromDataArray() throws Exception {
        String json = "{\"data\":[{\"name\":\"River Search\"},{\"missionName\":\"Night Watch\"}]}";
        List<String> names = OpenTakMissionApi.parseMissionNames(json);
        assertEquals(2, names.size());
        assertTrue(names.contains("River Search"));
        assertTrue(names.contains("Night Watch"));
    }

    @Test
    public void missionListUrlUsesMartiPath() {
        assertEquals("https://tak.example.local:8443/Marti/api/missions",
                OpenTakMissionApi.missionListUrl("tak.example.local", 8443));
    }
}
