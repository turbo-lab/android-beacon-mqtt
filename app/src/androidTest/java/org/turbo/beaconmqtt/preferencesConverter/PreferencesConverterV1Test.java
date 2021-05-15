package org.turbo.beaconmqtt.preferencesConverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_EXIT_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_INACTIVE_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_PAUSE_BETWEEN_SCANS_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_SCAN_DURATION_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_ENTER_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_EXIT_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_TOPIC_KEY;
import static org.turbo.beaconmqtt.beaconFactory.BeaconFactory.BEACONS_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverter.isCurrentPreferencesRevision;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverter.isPreferencesRevisionChanged;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.BEACON_EXIT_REGION_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.BEACON_PERIOD_BETWEEN_SCANS_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.BEACON_SCAN_PERIOD_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.MQTT_ENTER_MESSAGE_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.MQTT_ENTER_TOPIC_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.MQTT_EXIT_MESSAGE_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.MQTT_EXIT_TOPIC_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.REGION_KEY;
import static org.turbo.beaconmqtt.preferencesConverter.PreferencesConverterV1.REGION_LIST_SIZE;

@SuppressWarnings("ALL")
@RunWith(AndroidJUnit4ClassRunner.class)
public class PreferencesConverterV1Test {

    private SharedPreferences sharedPreferences;

    @Before
    public void before() {
        Context appContext = ApplicationProvider.getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.clear().commit();

        prefsEditor.putString(REGION_KEY + "1", "{\"id\":\"home\",\"uuid\":\"74278bda-b644-4520-8f0c-720eaf059935\",\"major\":\"21794\", \"minor\": \"64000\"}");

        prefsEditor.putString(BEACON_PERIOD_BETWEEN_SCANS_KEY, "12000");
        prefsEditor.putString(BEACON_SCAN_PERIOD_KEY, "1000");
        prefsEditor.putString(BEACON_EXIT_REGION_TIMEOUT_KEY, "300000");

        prefsEditor.putString(MQTT_ENTER_TOPIC_KEY, "location/%mac%");
        prefsEditor.putString(MQTT_EXIT_TOPIC_KEY, "location/%mac%");
        prefsEditor.putString(MQTT_ENTER_MESSAGE_KEY, "%beacon%");
        prefsEditor.putString(MQTT_EXIT_MESSAGE_KEY, "not_home");

        prefsEditor.apply();

        @SuppressWarnings("unused")
        PreferencesConverter converter = new PreferencesConverter(sharedPreferences);
    }

    @Test
    public void testPreferencesVersionChanged() {
        assertTrue(isPreferencesRevisionChanged(sharedPreferences));
    }

    @Test
    public void testCurrentPreferencesRevision() {
        assertTrue(isCurrentPreferencesRevision(sharedPreferences));
    }

    @Test
    public void testBeacons() {
        assertEquals("[{\"iBeaconData\":{\"major\":\"21794\",\"minor\":\"64000\",\"uuid\":\"74278bda-b644-4520-8f0c-720eaf059935\"},\"mac\":\"\",\"group\":\"home\",\"id\":\"home\",\"tag\":\"\",\"type\":\"iBeacon\"}]",
                sharedPreferences.getString(BEACONS_KEY, null));
    }

    @Test
    public void testTimings() {
        assertEquals("12000",
                sharedPreferences.getString(BEACON_PAUSE_BETWEEN_SCANS_KEY, null));
        assertEquals("1000",
                sharedPreferences.getString(BEACON_SCAN_DURATION_KEY, null));
        assertEquals("300000",
                sharedPreferences.getString(BEACON_EXIT_TIMEOUT_KEY, null));
        assertEquals("150000",
                sharedPreferences.getString(BEACON_INACTIVE_TIMEOUT_KEY, null));
    }

    @Test
    public void testMqttTemplates() {
        assertEquals("location/%mac%",
                sharedPreferences.getString(MQTT_MASTER_TOPIC_KEY, null));
        assertEquals("%group%",
                sharedPreferences.getString(MQTT_MASTER_ENTER_MESSAGE_KEY, null));
        assertEquals("not_home",
                sharedPreferences.getString(MQTT_MASTER_EXIT_MESSAGE_KEY, null));
    }

    @Test
    public void testInvalidation() {
        for (int index = 0; index < REGION_LIST_SIZE; index++) {
            assertNull(sharedPreferences.getString(REGION_KEY + index, null));
        }

        assertNull(sharedPreferences.getString(BEACON_PERIOD_BETWEEN_SCANS_KEY, null));
        assertNull(sharedPreferences.getString(BEACON_SCAN_PERIOD_KEY, null));
        assertNull(sharedPreferences.getString(BEACON_EXIT_REGION_TIMEOUT_KEY, null));

        assertNull(sharedPreferences.getString(MQTT_ENTER_TOPIC_KEY, null));
        assertNull(sharedPreferences.getString(MQTT_EXIT_TOPIC_KEY, null));
        assertNull(sharedPreferences.getString(MQTT_ENTER_MESSAGE_KEY, null));
        assertNull(sharedPreferences.getString(MQTT_EXIT_MESSAGE_KEY, null));
    }
}
