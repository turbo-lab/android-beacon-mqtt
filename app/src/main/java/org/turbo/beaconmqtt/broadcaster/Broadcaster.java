package org.turbo.beaconmqtt.broadcaster;

import android.content.Context;
import android.util.Log;

import org.turbo.beaconmqtt.beacon.BaseBeacon;

import java.util.ArrayList;
import java.util.List;

public class Broadcaster {
    private static final String TAG = Broadcaster.class.getName();
    private final List<BaseBroadcaster> broadcasters = new ArrayList<>();

    private final List<BroadcasterChangeListener> listeners = new ArrayList<>();

    public Broadcaster(final Context context) {
        broadcasters.add(new MqttBroadcaster(this, context));
    }

    public void publishState(BaseBeacon beacon) {
        for (BaseBroadcaster broadcaster : broadcasters) {
            broadcaster.state(beacon);
        }
    }

    public void publishEnterMaster(BaseBeacon beacon) {
        Log.i(TAG, "publishEnterMaster");
        for (BaseBroadcaster broadcaster : broadcasters) {
            broadcaster.enterMaster(beacon);
        }
    }

    public void publishExitMaster(BaseBeacon beacon) {
        for (BaseBroadcaster broadcaster : broadcasters) {
            broadcaster.exitMaster(beacon);
        }
    }

    public void publishTrack(BaseBeacon beacon) {
        for (BaseBroadcaster broadcaster : broadcasters) {
            broadcaster.track(beacon);
        }
    }

    public List<BaseBroadcaster> getBroadcasters() {
        return broadcasters;
    }

    public void notifyListeners() {
        for (BroadcasterChangeListener listener : listeners) {
            listener.onChangedBroadcaster(broadcasters);
        }
    }

    public void addChangeListener(BroadcasterChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(BroadcasterChangeListener listener) {
        listeners.remove(listener);
    }
}