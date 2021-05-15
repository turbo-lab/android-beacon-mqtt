package org.turbo.beaconmqtt.beacon;

import org.altbeacon.beacon.Beacon;

import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.turbo.beaconmqtt.beacon.EddystoneUidBeacon.BEACON_EDDYSTONE_UID;

public class Helper {
    private static final String STATE_EXIT = "exit";
    private static final String STATE_ENTER = "enter";
    private static final String STATE_UNKNOWN = "unknown";
    private static final int BEACON_TYPE_CODE_IBEACON = 0x4C000215;
    private static final int BEACON_TYPE_CODE_ALTBEACON = 0xBEAC;
    private static final int BEACON_TYPE_CODE_EDDYSTONE_UID = 0x00;
    private static final int BEACON_SERVICE_UUID_EDDYSTONE = 0xFEAA;
    private static final String BEACON_UNKNOWN = "unknown";

    public static String getBleBeaconString(Beacon beacon) {
        if (beacon.getServiceUuid() == BEACON_SERVICE_UUID_EDDYSTONE) {
            if (beacon.getBeaconTypeCode() == BEACON_TYPE_CODE_EDDYSTONE_UID) {
                return BEACON_EDDYSTONE_UID;
            }
        } else {
            if (beacon.getBeaconTypeCode() == BEACON_TYPE_CODE_IBEACON) {
                return IBeacon.BEACON_IBEACON;
            } else if (beacon.getBeaconTypeCode() == BEACON_TYPE_CODE_ALTBEACON) {
                // TODO: Altbeacon
                return BEACON_UNKNOWN;
            }
        }
        return BEACON_UNKNOWN;
    }

    public static String getStateString(BeaconState state) {
        if (state.equals(Helper.BeaconState.BEACON_STATE_EXIT)) {
            return STATE_EXIT;
        } else //noinspection StatementWithEmptyBody
            if (state.equals(Helper.BeaconState.BEACON_STATE_INACTIVE)) {
            } else if (state.equals(Helper.BeaconState.BEACON_STATE_ENTER)) {
                return STATE_ENTER;
            }
        return STATE_UNKNOWN;
    }

    public static boolean validateUuid(String uuid) {
        if (uuid == null) return false;
        final Pattern pattern = Pattern.compile("^[0-9a-zA-Z]{8}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{4}-[0-9a-zA-Z]{12}$");
        return pattern.matcher(uuid).matches();
    }

    public static boolean validateInt(String string, int min, int max) {
        try {
            int value = parseInt(string);

            if (value >= min && value <= max) {
                return true;
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean validateMacAddress(String macAddress) {
        if (macAddress == null) return false;
        if (macAddress.isEmpty()) return true;
        final Pattern pattern = Pattern.compile("^[0-9a-zA-Z]{2}:[0-9a-zA-Z]{2}:[0-9a-zA-Z]{2}:[0-9a-zA-Z]{2}:[0-9a-zA-Z]{2}:[0-9a-zA-Z]{2}$");
        return pattern.matcher(macAddress).matches();
    }

    private static boolean validateString(String string, String regex) {
        if (string == null) return false;
        final Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(string).matches();
    }

    public static boolean validateSsid(String ssid) {
        return validateString(ssid, ".{1,32}$");
    }

    public static boolean validateId(String id) {
        return validateString(id, ".{1,32}$");
    }

    public static boolean validateGroup(String group) {
        return validateString(group, ".{0,32}$");
    }

    public static boolean validateTag(String tag) {
        return validateString(tag, ".{0,32}$");
    }

    public enum BeaconState {
        BEACON_STATE_EXIT,
        BEACON_STATE_INACTIVE,
        BEACON_STATE_ENTER
    }
}
