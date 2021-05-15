package org.turbo.beaconmqtt.wifi;

import android.net.wifi.WifiInfo;

public interface WifiChangeListener {
    void onWifiConnected(String ssid);

    void onWifiUpdated(String ssid, WifiInfo wifiInfo);

    void onWifiDisconnected(String ssid);
}
