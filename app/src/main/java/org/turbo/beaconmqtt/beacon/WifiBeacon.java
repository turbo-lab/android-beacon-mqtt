package org.turbo.beaconmqtt.beacon;

import android.net.wifi.WifiInfo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public final class WifiBeacon extends BaseBeacon {
    public final static String BEACON_WIFI = "Wi-Fi";
    @Expose
    private final WifiBeaconData wifiBeaconData = new WifiBeaconData();

    public WifiBeacon() {
        mType = BEACON_WIFI;
    }

    public WifiBeacon(String id,
                      String group,
                      String tag,
                      String ssid) {
        mId = id;
        mGroup = group;
        mTag = tag;
        mType = BEACON_WIFI;
        wifiBeaconData.mSsid = ssid;
    }

    public WifiBeacon(TransactionBeacon transactionBeacon) {
        mId = transactionBeacon.getId();
        mGroup = transactionBeacon.getGroup();
        mTag = transactionBeacon.getTag();
        mType = BEACON_WIFI;
        wifiBeaconData.mSsid = transactionBeacon.getSsid();
    }

    @Override
    public String getSsid() {
        return wifiBeaconData.mSsid;
    }

    public void setRunningData(WifiInfo wifiInfo) {
        see();
        mRssi = wifiInfo.getRssi();
        changeBeaconNotifyListeners();
    }

    public boolean isValid() {
        boolean isValid = super.isValid();
        isValid &= Helper.validateSsid(wifiBeaconData.mSsid);
        return isValid;
    }

    private class WifiBeaconData {
        @Expose
        @SerializedName("ssid")
        private String mSsid;
    }
}
