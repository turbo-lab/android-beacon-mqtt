package org.turbo.beaconmqtt.beacon;

import android.support.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.altbeacon.beacon.Region;

public final class IBeacon extends BaseBleBeacon {
    public final static String BEACON_LAYOUT = "m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    public final static String BEACON_IBEACON = "iBeacon";
    @Expose
    private final IBeaconData iBeaconData = new IBeaconData();

    public IBeacon() {
        mType = BEACON_IBEACON;
    }

    public IBeacon(String id, String group, String tag,
                   String uuid, String major, String minor, String mac) {
        mId = id;
        mGroup = group;
        mTag = tag;
        mType = BEACON_IBEACON;
        iBeaconData.mUuid = uuid;
        iBeaconData.mMajor = major;
        iBeaconData.mMinor = minor;
        mBluetoothAddress = mac.trim();
    }

    public IBeacon(String id, String group, String tag,
                   @NonNull Region region) {
        mId = id;
        mGroup = group;
        mTag = tag;
        mType = BEACON_IBEACON;
        iBeaconData.mUuid = region.getId1().toString();
        iBeaconData.mMajor = region.getId2().toString();
        iBeaconData.mMinor = region.getId3().toString();
        mBluetoothAddress = region.getBluetoothAddress();
    }

    public IBeacon(TransactionBeacon transactionBeacon) {
        mId = transactionBeacon.getId();
        mGroup = transactionBeacon.getGroup();
        mTag = transactionBeacon.getTag();
        mType = BEACON_IBEACON;
        iBeaconData.mUuid = transactionBeacon.getUuid();
        iBeaconData.mMajor = transactionBeacon.getMajor();
        iBeaconData.mMinor = transactionBeacon.getMinor();
        mBluetoothAddress = transactionBeacon.getMacAddress();
    }

    @Override
    public String getUuid() {
        return iBeaconData.mUuid;
    }

    @Override
    public String getMajor() {
        return iBeaconData.mMajor;
    }

    @Override
    public String getMinor() {
        return iBeaconData.mMinor;
    }

    public boolean isValid() {
        boolean isValid = super.isValid();
        isValid &= Helper.validateUuid(iBeaconData.mUuid);
        isValid &= Helper.validateInt(iBeaconData.mMajor, 1, 65535);
        isValid &= Helper.validateInt(iBeaconData.mMinor, 1, 65535);
        isValid &= Helper.validateMacAddress(mBluetoothAddress);
        return isValid;
    }

    private class IBeaconData {
        @Expose
        @SerializedName("uuid")
        private String mUuid;
        @Expose
        @SerializedName("major")
        private String mMajor;
        @Expose
        @SerializedName("minor")
        private String mMinor;
    }
}
