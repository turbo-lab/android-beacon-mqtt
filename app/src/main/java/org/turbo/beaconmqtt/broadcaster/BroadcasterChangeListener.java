package org.turbo.beaconmqtt.broadcaster;

import java.util.List;

public interface BroadcasterChangeListener {
    void onChangedBroadcaster(List<BaseBroadcaster> broadcasters);
}
