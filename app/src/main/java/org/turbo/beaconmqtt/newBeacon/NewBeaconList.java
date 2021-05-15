package org.turbo.beaconmqtt.newBeacon;

import org.altbeacon.beacon.Beacon;

import java.util.LinkedHashMap;
import java.util.Map;

class NewBeaconList {
    @SuppressWarnings("unused")
    protected static final String TAG = "NewBeaconList";

    private final Map<Integer, Beacon> beaconList;

    public NewBeaconList() {
        beaconList = new LinkedHashMap<>();
    }

    public void addBeacon(Beacon beacon) {
        // todo: we need new beacon object
        beaconList.put(beacon.hashCode(), beacon);
    }

    public int getSize() {
        return beaconList.size();
    }

    public Beacon getBeacon(int position) {
        return (Beacon) beaconList.values().toArray()[position];
    }

    public void clear() {
        beaconList.clear();
    }
}
