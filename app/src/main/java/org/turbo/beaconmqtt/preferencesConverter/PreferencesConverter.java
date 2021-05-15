package org.turbo.beaconmqtt.preferencesConverter;

import android.content.SharedPreferences;

public class PreferencesConverter {
    private static final String PREFERENCES_REVISION_KEY = "preferences_revision";
    private static final String PREFERENCES_REVISION_CHANGED_KEY = "preferences_revision_changed";
    private static final String PREFERENCES_REVISION_1 = "1";
    private static final String PREFERENCES_REVISION_CURRENT = "2";

    public PreferencesConverter(SharedPreferences defaultSharedPreferences) {
        SharedPreferences.Editor prefsEditor = defaultSharedPreferences.edit();

        if (isPreferencesRevisionV1(defaultSharedPreferences)) {
            PreferencesConverterV1 converter = new PreferencesConverterV1(defaultSharedPreferences);
            if (converter.convert()) {
                prefsEditor.putString(PREFERENCES_REVISION_CHANGED_KEY, "");
                prefsEditor.apply();
            }
        }

        prefsEditor.putString(PREFERENCES_REVISION_KEY, PREFERENCES_REVISION_CURRENT);
        prefsEditor.apply();
    }

    public static void hidePreferencesRevisionChanged(SharedPreferences defaultSharedPreferences) {
        SharedPreferences.Editor prefsEditor = defaultSharedPreferences.edit();
        prefsEditor.remove(PREFERENCES_REVISION_CHANGED_KEY);
        prefsEditor.apply();
    }

    public static boolean isPreferencesRevisionChanged(SharedPreferences defaultSharedPreferences) {
        return defaultSharedPreferences
                .getString(PREFERENCES_REVISION_CHANGED_KEY, null) != null;
    }

    private static boolean isPreferencesRevisionV1(SharedPreferences defaultSharedPreferences) {
        return PREFERENCES_REVISION_1.equals(defaultSharedPreferences
                .getString(PREFERENCES_REVISION_KEY, PREFERENCES_REVISION_1));
    }

    public static boolean isCurrentPreferencesRevision(SharedPreferences defaultSharedPreferences) {
        return PREFERENCES_REVISION_CURRENT.equals(defaultSharedPreferences
                .getString(PREFERENCES_REVISION_KEY, null));
    }
}
