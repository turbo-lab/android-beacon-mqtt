package org.turbo.beaconmqtt.beacon;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.altbeacon.beacon.Beacon;

public abstract class BaseBleBeacon extends BaseBeacon {
    @SuppressWarnings("unused")
    private static final String TAG = "BseBleBeacon";

    @Expose
    @SerializedName("mac")
    String mBluetoothAddress;
    private transient double mDistance;

    public void setRunningData(Beacon beacon) {
        see();
        mDistance = beacon.getDistance();
        mRssi = beacon.getRssi();
        changeBeaconNotifyListeners();
    }

    @Override
    public double getDistance() {
        return mDistance;
    }

    public String getMacAddress() {
        return mBluetoothAddress;
    }
}
