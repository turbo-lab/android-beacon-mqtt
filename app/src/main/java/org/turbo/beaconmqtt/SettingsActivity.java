package org.turbo.beaconmqtt;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Locale;
import java.util.Objects;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String BEACON_PAUSE_BETWEEN_SCANS_KEY = "beacon_pause_between_scans";
    public static final String BEACON_SCAN_DURATION_KEY = "beacon_scan_duration";
    public static final String BEACON_INACTIVE_TIMEOUT_KEY = "beacon_inactive_timeout";
    public static final String BEACON_EXIT_TIMEOUT_KEY = "beacon_exit_timeout";
    public static final String BEACON_HYSTERESIS_FACTOR_KEY = "beacon_hysteresis_factor";
    public static final String MQTT_SERVER_KEY = "mqtt_server";
    public static final String MQTT_PORT_KEY = "mqtt_port";
    public static final String MQTT_USER_KEY = "mqtt_user";
    public static final String MQTT_PASS_KEY = "mqtt_pass";
    public static final String MQTT_BEACON_STATE_TOPIC_KEY = "mqtt_beacon_state_topic";
    public static final String MQTT_BEACON_STATE_MESSAGE_KEY = "mqtt_beacon_state_message";
    public static final String MQTT_MASTER_TOPIC_KEY = "mqtt_master_topic";
    public static final String MQTT_MASTER_ENTER_MESSAGE_KEY = "mqtt_master_enter_message";
    public static final String MQTT_MASTER_EXIT_MESSAGE_KEY = "mqtt_master_exit_message";
    public static final String MQTT_TRACK_TOPIC_KEY = "mqtt_track_topic";
    public static final String MQTT_TRACK_MESSAGE_KEY = "mqtt_track_message";
    public static final String NOTIFICATION_SHOW_LOG = "show_log";
    public static final String NOTIFICATION_VIBRATE_ON_EVENTS_KEY = "notification_vibrate_on_events";
    private static final Long BEACON_PAUSE_BETWEEN_SCANS_MIN = 0L;
    private static final Long BEACON_PAUSE_BETWEEN_SCANS_MAX = 55000L;
    private static final Long BEACON_SCAN_DURATION_MIN = 500L;
    private static final Long BEACON_SCAN_DURATION_MAX = 5000L;
    private static final Long BEACON_INACTIVE_TIMEOUT_MIN = 15000L;
    private static final Long BEACON_INACTIVE_TIMEOUT_MAX = 300000L;
    private static final Long BEACON_EXIT_TIMEOUT_MIN = 30000L;
    private static final Long BEACON_EXIT_TIMEOUT_MAX = 600000L;
    private static final Double BEACON_HYSTERESIS_FACTOR_MIN = 1.0d;
    private static final Double BEACON_HYSTERESIS_FACTOR_MAX = 5.0d;
    private static final Long MQTT_PORT_MIN = 1L;
    private static final Long MQTT_PORT_MAX = 65535L;
    private static boolean isActive = false;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (!validatePreference(preference, stringValue)) {
                return false;
            }

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else if (preference.getKey().equals(MQTT_PASS_KEY)) {
                //noinspection ReplaceAllDot
                preference.setSummary(stringValue.replaceAll(".", "•"));
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    public static boolean isActive() {
        return isActive;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setupActionBar();

        bindPreferenceSummaryToValue(findPreference(BEACON_PAUSE_BETWEEN_SCANS_KEY));
        bindPreferenceSummaryToValue(findPreference(BEACON_SCAN_DURATION_KEY));
        bindPreferenceSummaryToValue(findPreference(BEACON_INACTIVE_TIMEOUT_KEY));
        bindPreferenceSummaryToValue(findPreference(BEACON_EXIT_TIMEOUT_KEY));
        bindPreferenceSummaryToValue(findPreference(BEACON_HYSTERESIS_FACTOR_KEY));

        bindPreferenceSummaryToValue(findPreference(MQTT_SERVER_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_PORT_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_USER_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_PASS_KEY));

        bindPreferenceSummaryToValue(findPreference(MQTT_BEACON_STATE_TOPIC_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_BEACON_STATE_MESSAGE_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_MASTER_TOPIC_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_MASTER_ENTER_MESSAGE_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_MASTER_EXIT_MESSAGE_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_TRACK_TOPIC_KEY));
        bindPreferenceSummaryToValue(findPreference(MQTT_TRACK_MESSAGE_KEY));
    }

    private boolean validateLong(String string, Long min, Long max) {
        try {
            Long value = Long.parseLong(string);

            if (value >= min && value <= max) {
                return true;
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(),
                getString(R.string.pref_message_invalid_value,
                        String.format(Locale.getDefault(), "%d ... %d", min, max)),
                Toast.LENGTH_LONG).show();

        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean validateDouble(String string, Double min, Double max) {
        try {
            Double value = Double.parseDouble(string);

            if (value >= min && value <= max) {
                return true;
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(),
                getString(R.string.pref_message_invalid_value,
                        String.format(Locale.getDefault(), "%.1f ... %.1f", min, max)),
                Toast.LENGTH_LONG).show();

        return false;
    }

    private boolean validatePreference(Preference preference, String stringValue) {
        switch (preference.getKey()) {
            case BEACON_PAUSE_BETWEEN_SCANS_KEY:
                return validateLong(stringValue,
                        BEACON_PAUSE_BETWEEN_SCANS_MIN,
                        BEACON_PAUSE_BETWEEN_SCANS_MAX);
            case BEACON_SCAN_DURATION_KEY:
                return validateLong(stringValue,
                        BEACON_SCAN_DURATION_MIN,
                        BEACON_SCAN_DURATION_MAX);
            case BEACON_INACTIVE_TIMEOUT_KEY:
                return validateLong(stringValue,
                        BEACON_INACTIVE_TIMEOUT_MIN,
                        BEACON_INACTIVE_TIMEOUT_MAX);
            case BEACON_EXIT_TIMEOUT_KEY:
                return validateLong(stringValue,
                        BEACON_EXIT_TIMEOUT_MIN,
                        BEACON_EXIT_TIMEOUT_MAX);
            case BEACON_HYSTERESIS_FACTOR_KEY:
                return validateDouble(stringValue,
                        BEACON_HYSTERESIS_FACTOR_MIN,
                        BEACON_HYSTERESIS_FACTOR_MAX);
            case MQTT_PORT_KEY:
                return validateLong(stringValue,
                        MQTT_PORT_MIN,
                        MQTT_PORT_MAX);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isActive = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isActive = false;
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Settings");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                Objects.requireNonNull(PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), "")));
    }


}
