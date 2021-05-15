package org.turbo.beaconmqtt.beaconFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.beacon.IBeacon;
import org.turbo.beaconmqtt.beacon.WifiBeacon;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.turbo.beaconmqtt.beaconFactory.BeaconFactory.BEACONS_KEY;


@SuppressWarnings("ALL")
@RunWith(AndroidJUnit4ClassRunner.class)
public class loadBeaconsTest {

    private BeaconFactory beaconFactory;

    @Before
    public void before() {
        Context appContext = ApplicationProvider.getApplicationContext();
        BeaconApplication application = (BeaconApplication) appContext;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.clear().commit();

        prefsEditor.putString(BEACONS_KEY, "[{\"iBeaconData\":{\"major\":\"21794\",\"minor\":\"64000\",\"uuid\":\"74278bda-b644-4520-8f0c-720eaf059935\"},\"mac\":\"D8:A9:8B:CB:74:A5\",\"group\":\"home\",\"id\":\"home-ble\",\"tag\":\"ble\",\"type\":\"iBeacon\"}," +
                "{\"wifiBeaconData\":{\"ssid\":\"NTK23-5GHz\"},\"group\":\"work\",\"id\":\"work-wifi\",\"tag\":\"wifi\",\"type\":\"Wi-Fi\"}]");

        prefsEditor.apply();

        beaconFactory = application.getBeaconFactory();
        beaconFactory.reloadBeacons();
    }

    @Test
    public void testCountOfBeacons() {
        assertEquals(2, beaconFactory.getBeaconList().size());
    }

    @Test
    public void testIBeacon() {
        IBeacon beacon = beaconFactory.getIBeaconById("home-ble");

        assertNotNull(beacon);
        assertEquals("home", beacon.getGroup());
        assertEquals("ble", beacon.getTag());
        assertEquals("74278bda-b644-4520-8f0c-720eaf059935", beacon.getUuid());
        assertEquals("21794", beacon.getMajor());
        assertEquals("64000", beacon.getMinor());
        assertEquals("D8:A9:8B:CB:74:A5", beacon.getMacAddress());
    }

    @Test
    public void testWifiBeacon() {
        WifiBeacon beacon = beaconFactory.getWifiBeaconById("work-wifi");

        assertNotNull(beacon);
        assertEquals("NTK23-5GHz", beacon.getSsid());
        assertEquals("work", beacon.getGroup());
        assertEquals("wifi", beacon.getTag());
    }
}
