package org.turbo.beaconmqtt;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import net.danlew.android.joda.JodaTimeAndroid;

import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.Helper;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactory;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactoryChangeListener;
import org.turbo.beaconmqtt.broadcaster.Broadcaster;
import org.turbo.beaconmqtt.preferencesConverter.PreferencesConverter;

import java.text.DateFormat;
import java.util.Date;

import static org.turbo.beaconmqtt.SettingsActivity.NOTIFICATION_VIBRATE_ON_EVENTS_KEY;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_ENTER;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_EXIT;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_INACTIVE;

public class BeaconApplication extends Application implements BeaconFactoryChangeListener {
    @SuppressWarnings("unused")
    private static final String TAG = "BeaconMQTT";
    private Broadcaster broadcaster;
    private BeaconFactory beaconFactory;
    private MainActivity mainActivity = null;
    private String debugLog = "";
    private SharedPreferences defaultSharedPreferences;
    @SuppressWarnings("FieldCanBeLocal")
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    private boolean vibrateOnEvents;
    private String activeGroup = "";

    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setUpSettingsChangedListener(defaultSharedPreferences);

        @SuppressWarnings("unused")
        PreferencesConverter preferencesConverter = new PreferencesConverter(defaultSharedPreferences);

        beaconFactory = new BeaconFactory(this);
        getBeaconFactory().addChangeListener(this);

        broadcaster = new Broadcaster(this);
        reapplyVibration();
    }

    private void setUpSettingsChangedListener(final SharedPreferences defaultSharedPreferences) {
        onSharedPreferenceChangeListener = new SharedPreferences
                .OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (NOTIFICATION_VIBRATE_ON_EVENTS_KEY.equals(key)) {
                    vibrateOnEvents = defaultSharedPreferences.getBoolean(NOTIFICATION_VIBRATE_ON_EVENTS_KEY, false);
                }
            }
        };
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    @SuppressWarnings("unused")
    public void clearDebugLog() {
        debugLog = "";
    }

    public String getDebugLog() {
        return debugLog;
    }

    private int getDebugLogLinesCount() {
        String[] lines = debugLog.split("\n");
        return lines.length;
    }

    public void debugLog(String line) {
        int start;
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        String currentDateAndTime = df.format(new Date());

        if (debugLog.length() != 0) {
            debugLog += "\n";
        }
        debugLog += (currentDateAndTime + ": " + line);

        while (getDebugLogLinesCount() > 100) {
            if ((start = debugLog.indexOf('\n')) > 0) {
                debugLog = debugLog.substring(start + 1);
            } else {
                break;
            }
        }

        if (this.mainActivity != null) {
            this.mainActivity.updateDebugLog(debugLog);
        }
    }

    private void reapplyVibration() {
        vibrateOnEvents = defaultSharedPreferences.getBoolean(NOTIFICATION_VIBRATE_ON_EVENTS_KEY, false);
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }
    }

    @Override
    public void onChangeFactory() {
        // do nothing
    }

    @Override
    public void onEnterMaster(@NonNull BaseBeacon beacon) {
        debugLog("Master is " + beacon.getId());

        /* Specific case when user didn't assigned the groups */
        if (activeGroup.isEmpty() && beacon.getGroup().isEmpty()) {
            getBroadcaster().publishEnterMaster(beacon);
        }
        /* The app doesn't need to resend the message in the same location. */
        else if (!activeGroup.equals(beacon.getGroup())) {
            activeGroup = beacon.getGroup();
            getBroadcaster().publishEnterMaster(beacon);
        }

        if (vibrateOnEvents) {
            vibrate();
        }
    }

    @Override
    public void onExitMaster(@NonNull BaseBeacon beacon) {
        debugLog("Master " + beacon.getId() + " lost");

        activeGroup = "";

        getBroadcaster().publishExitMaster(beacon);

        if (vibrateOnEvents) {
            vibrate();
        }
    }

    @Override
    public void onChangeBeacon(@NonNull BaseBeacon beacon, Helper.BeaconState state) {
        if (BEACON_STATE_EXIT.equals(state)) {
            debugLog("Exit " + beacon.getId());
            getBroadcaster().publishState(beacon);
        } else if (BEACON_STATE_INACTIVE.equals(state)) {
            debugLog("Timeout " + beacon.getId());
        } else if (BEACON_STATE_ENTER.equals(state)) {
            debugLog("Enter " + beacon.getId());
            getBroadcaster().publishState(beacon);
        }
    }

    @Override
    public void onTrackBeacon(@NonNull BaseBeacon beacon) {
        getBroadcaster().publishTrack(beacon);
    }

    public Broadcaster getBroadcaster() {
        return broadcaster;
    }

    public BeaconFactory getBeaconFactory() {
        return beaconFactory;
    }
}
