package org.turbo.beaconmqtt.beaconFactory;

import android.support.annotation.NonNull;

import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.Helper;

public interface BeaconFactoryChangeListener {
    void onChangeFactory();

    void onChangeBeacon(@NonNull BaseBeacon beacon, Helper.BeaconState state);

    void onTrackBeacon(@NonNull BaseBeacon beacon);

    void onEnterMaster(@NonNull BaseBeacon beacon);

    void onExitMaster(@NonNull BaseBeacon beacon);
}
