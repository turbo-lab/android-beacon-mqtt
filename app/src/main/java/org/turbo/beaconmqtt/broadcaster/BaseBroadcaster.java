package org.turbo.beaconmqtt.broadcaster;

import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.Helper;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class BaseBroadcaster {
    private final transient String mMacAddress;
    String mName;
    transient boolean mState;
    @SuppressWarnings("unused")
    private int mPort;

    BaseBroadcaster() {
        mMacAddress = getMacAddress();
    }

    private static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X", b));
                }

                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "020000000000";
    }

    public boolean getState() {
        return mState;
    }

    public String getName() {
        return mName;
    }

    @SuppressWarnings("unused")
    public int getPort() {
        return mPort;
    }

    public abstract void state(BaseBeacon beacon);

    public abstract void enterMaster(BaseBeacon beacon);

    public abstract void exitMaster(BaseBeacon beacon);

    public abstract void track(BaseBeacon beacon);

    String parseStaticTokens(String string, BaseBeacon beacon) {
        string = string.replaceAll("%beacon%", beacon.getId());
        string = string.replaceAll("%group%", beacon.getGroup());
        string = string.replaceAll("%tag%", beacon.getTag());
        string = string.replaceAll("%state%", Helper.getStateString(beacon.getState()));
        string = string.replaceAll("%mac%", mMacAddress.toLowerCase(Locale.ROOT));
        return string;
    }

    String parseDynamicTokens(String string, BaseBeacon beacon) {
        string = string.replaceAll("%rssi%",
                String.format(Locale.getDefault(), "%d", beacon.getRssi()));
        string = string.replaceAll("%distance%",
                String.format(Locale.getDefault(), "%.1f", beacon.getDistance()));
        return string;
    }
}