package org.turbo.beaconmqtt.preferencesConverter;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.IBeacon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.turbo.beaconmqtt.SettingsActivity.BEACON_EXIT_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_INACTIVE_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_PAUSE_BETWEEN_SCANS_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_SCAN_DURATION_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_ENTER_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_EXIT_MESSAGE_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.MQTT_MASTER_TOPIC_KEY;
import static org.turbo.beaconmqtt.beaconFactory.BeaconFactory.BEACONS_KEY;

/*
 * PreferencesConverterV1 shared preference format
 *
 * beacon_period_between_scans -> beacon_pause_between_scans
 * beacon_scan_period -> beacon_scan_duration
 * beacon_exit_region_timeout -> beacon_exit_timeout
 * beacon_exit_region_timeout / 2 -> beacon_inactive_timeout
 *
 * mqtt_server -> no change
 * mqtt_port -> no change
 * mqtt_user -> no change
 * mqtt_pass -> no change
 *
 * mqtt_exit_topic == mqtt_enter_topic -> mqtt_master_topic
 * mqtt_enter_message -> mqtt_master_enter_message
 * mqtt_exit_message -> mqtt_master_exit_message
 *
 * regionObject0...3 -> beaconList
 */

class PreferencesConverterV1 {
    static final int REGION_LIST_SIZE = 4;
    static final String REGION_KEY = "regionObject";
    static final String BEACON_PERIOD_BETWEEN_SCANS_KEY = "beacon_period_between_scans";
    static final String BEACON_SCAN_PERIOD_KEY = "beacon_scan_period";
    static final String BEACON_EXIT_REGION_TIMEOUT_KEY = "beacon_exit_region_timeout";
    static final String MQTT_ENTER_TOPIC_KEY = "mqtt_enter_topic";
    static final String MQTT_EXIT_TOPIC_KEY = "mqtt_exit_topic";
    static final String MQTT_ENTER_MESSAGE_KEY = "mqtt_enter_message";
    static final String MQTT_EXIT_MESSAGE_KEY = "mqtt_exit_message";
    private final SharedPreferences defaultSharedPreferences;
    private final List<BaseBeacon> beaconList = new ArrayList<>();

    public PreferencesConverterV1(SharedPreferences defaultSharedPreferences) {
        this.defaultSharedPreferences = defaultSharedPreferences;
    }

    public boolean convert() {
        boolean changed = false;

        for (int index = 0; index < REGION_LIST_SIZE; index++) {
            String json = defaultSharedPreferences.getString(REGION_KEY + index, null);
            if (json != null) {
                changed = true;
                try {
                    Gson gson = new Gson();
                    LegacyBeacon legacyBeacon = gson.fromJson(json, LegacyBeacon.class);

                    IBeacon iBeacon = new IBeacon(legacyBeacon.id, legacyBeacon.id, "",
                            legacyBeacon.uuid, legacyBeacon.major, legacyBeacon.minor, "");

                    beaconList.add(iBeacon);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        String backgroundBetweenScanPeriod = defaultSharedPreferences
                .getString(BEACON_PERIOD_BETWEEN_SCANS_KEY, null);
        String backgroundScanPeriod = defaultSharedPreferences
                .getString(BEACON_SCAN_PERIOD_KEY, null);
        String regionExitPeriod = defaultSharedPreferences
                .getString(BEACON_EXIT_REGION_TIMEOUT_KEY, null);

        String mqttEnterTopic = defaultSharedPreferences
                .getString(MQTT_ENTER_TOPIC_KEY, null);
        String mqttExitTopic = defaultSharedPreferences
                .getString(MQTT_EXIT_TOPIC_KEY, null);
        String mqttEnterMessage = defaultSharedPreferences
                .getString(MQTT_ENTER_MESSAGE_KEY, null);
        String mqttExitMessage = defaultSharedPreferences
                .getString(MQTT_EXIT_MESSAGE_KEY, null);

        SharedPreferences.Editor prefsEditor = defaultSharedPreferences.edit();
        invalidate(prefsEditor);

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        String json = gson.toJson(beaconList);
        prefsEditor.putString(BEACONS_KEY, json);

        if (backgroundBetweenScanPeriod != null) {
            changed = true;
            prefsEditor.putString(BEACON_PAUSE_BETWEEN_SCANS_KEY, backgroundBetweenScanPeriod);
        }

        if (backgroundScanPeriod != null) {
            changed = true;
            prefsEditor.putString(BEACON_SCAN_DURATION_KEY, backgroundScanPeriod);
        }

        if (regionExitPeriod != null) {
            changed = true;
            prefsEditor.putString(BEACON_EXIT_TIMEOUT_KEY, regionExitPeriod);
            try {
                long regionExitPeriodValue = Long.parseLong(regionExitPeriod);
                prefsEditor.putString(BEACON_INACTIVE_TIMEOUT_KEY,
                        String.format(Locale.getDefault(), "%d", regionExitPeriodValue / 2));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mqttEnterTopic != null && mqttExitTopic != null) {
            changed = true;
            if (mqttEnterTopic.equals(mqttExitTopic)) {
                prefsEditor.putString(MQTT_MASTER_TOPIC_KEY, mqttEnterTopic);
                prefsEditor.putString(MQTT_MASTER_EXIT_MESSAGE_KEY, mqttExitMessage);
                if (mqttEnterMessage != null && mqttEnterMessage.equals("%beacon%")) {
                    prefsEditor.putString(MQTT_MASTER_ENTER_MESSAGE_KEY, "%group%");
                } else {
                    prefsEditor.putString(MQTT_MASTER_ENTER_MESSAGE_KEY, mqttEnterMessage);
                }
            }
        }

        prefsEditor.apply();
        return changed;
    }

    private void invalidate(SharedPreferences.Editor prefsEditor) {
        for (int index = 0; index < REGION_LIST_SIZE; index++) {
            prefsEditor.remove(REGION_KEY + index);
        }

        prefsEditor.remove(BEACON_PERIOD_BETWEEN_SCANS_KEY);
        prefsEditor.remove(BEACON_SCAN_PERIOD_KEY);
        prefsEditor.remove(BEACON_EXIT_REGION_TIMEOUT_KEY);

        prefsEditor.remove(MQTT_ENTER_TOPIC_KEY);
        prefsEditor.remove(MQTT_EXIT_TOPIC_KEY);
        prefsEditor.remove(MQTT_ENTER_MESSAGE_KEY);
        prefsEditor.remove(MQTT_EXIT_MESSAGE_KEY);

        prefsEditor.apply();
    }

    @SuppressWarnings("unused")
    private class LegacyBeacon {
        String id;
        String uuid;
        String major;
        String minor;
        transient String state;
        transient String timestamp;
    }
}
