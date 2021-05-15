package org.turbo.beaconmqtt.beacon;

import static org.turbo.beaconmqtt.beacon.IBeacon.BEACON_IBEACON;
import static org.turbo.beaconmqtt.beacon.WifiBeacon.BEACON_WIFI;

public class TransactionBeacon extends BaseBeacon {
    private String mUuid;
    private String mMajor;
    private String mMinor;
    private String mBluetoothAddress;
    private String mSsid;

    public TransactionBeacon() {
    }

    public TransactionBeacon(BaseBeacon beacon) {
        try {
            mType = beacon.getType();
            mId = beacon.getId();
            mGroup = beacon.getGroup();
            mTag = beacon.getTag();
            if (beacon instanceof IBeacon) {
                mUuid = beacon.getUuid();
                mMajor = beacon.getMajor();
                mMinor = beacon.getMinor();
                mBluetoothAddress = beacon.getMacAddress();
            } else if (beacon instanceof WifiBeacon) {
                mSsid = beacon.getSsid();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        this.mUuid = uuid;
    }

    @Override
    public String getMajor() {
        return mMajor;
    }

    public void setMajor(String major) {
        this.mMajor = major;
    }

    @Override
    public String getMinor() {
        return mMinor;
    }

    public void setMinor(String minor) {
        this.mMinor = minor;
    }

    @Override
    public String getMacAddress() {
        return mBluetoothAddress;
    }

    public void setMacAddress(String bluetoothAddress) {
        this.mBluetoothAddress = bluetoothAddress.trim();
    }

    @Override
    public String getSsid() {
        return mSsid;
    }

    public void setSsid(String ssid) {
        this.mSsid = ssid;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public boolean isValid() {
        boolean isValid = true;

        if (BEACON_IBEACON.equals(mType)) {
            isValid = Helper.validateUuid(mUuid);
            isValid &= Helper.validateInt(mMajor, 1, 65535);
            isValid &= Helper.validateInt(mMinor, 1, 65535);
            isValid &= Helper.validateMacAddress(mBluetoothAddress);
        } else if (BEACON_WIFI.equals(mType)) {
            isValid = Helper.validateSsid(mSsid);
        }

        return isValid;
    }
}
