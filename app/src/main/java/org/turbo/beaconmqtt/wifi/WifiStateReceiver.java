package org.turbo.beaconmqtt.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiStateReceiver extends BroadcastReceiver {
    @SuppressWarnings("unused")
    private static final String TAG = "WifiStateReceiver";
    private static final String INVALID_SSID = "invalid_ssid_string";
    private final Context context;
    private final List<WifiChangeListener> listeners = new ArrayList<>();
    private final Handler handler = new Handler();
    private String activeSsid;
    private String lastSsid;
    private final Runnable disconnectRunnable = new Runnable() {
        public void run() {
            for (WifiChangeListener listener : listeners) {
                listener.onWifiDisconnected(lastSsid);
            }
            lastSsid = INVALID_SSID;
        }
    };
    private long exitTimeout;

    public WifiStateReceiver() {
        this.context = null;
    }

    public WifiStateReceiver(Context context) {
        this.context = context;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(this, intentFilter);

        lastSsid = activeSsid = INVALID_SSID;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (Objects.requireNonNull(action).equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    String ssid = wifiInfo.getSSID();

                    try {
                        Pattern p = Pattern.compile("\"([^\"]*)\"");
                        Matcher m = p.matcher(ssid);
                        //noinspection ResultOfMethodCallIgnored
                        m.find();

                        notifyWifiConnected(m.group(1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // wifi connection was lost
                notifyWifiDisconnected();
            }
        }
    }

    public void setExitTimeout(long exitTimeout) {
        this.exitTimeout = exitTimeout;
    }

    private void notifyWifiConnected(String ssid) {
        if (lastSsid.equals(INVALID_SSID)) {
            // if disconnected long time ago
            // just send connected
            for (WifiChangeListener listener : listeners) {
                listener.onWifiConnected(ssid);
            }
        } else //noinspection StatementWithEmptyBody
            if (lastSsid.equals(ssid)) {
                // if disconnected within DISCONNECT_PERIOD from the same ssid
                // do nothing
            } else {
                // if disconnected within DISCONNECT_PERIOD but from other ssid
                // first send disconnected from last
                // and after that send connected connected to new
                for (WifiChangeListener listener : listeners) {
                    listener.onWifiDisconnected(lastSsid);
                    listener.onWifiConnected(ssid);
                }
            }

        if (!lastSsid.equals(ssid)) {
            for (WifiChangeListener listener : listeners) {
                listener.onWifiConnected(ssid);
            }
        }
        lastSsid = activeSsid = ssid;
        handler.removeCallbacks(disconnectRunnable);
    }

    private void notifyWifiDisconnected() {
        //BeaconApplication application = (BeaconApplication) context.getApplicationContext();
        //application.debugLog("notifyWifiDisconnected");
        if (!activeSsid.equals(INVALID_SSID)) {
            activeSsid = INVALID_SSID;
            handler.postDelayed(disconnectRunnable, exitTimeout);
        }
    }

    public void touch() {
        if (!activeSsid.equals(INVALID_SSID)) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            for (WifiChangeListener listener : listeners) {
                listener.onWifiUpdated(activeSsid, wifiInfo);
            }
        }
    }

    public void addChangeListener(WifiChangeListener listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("unused")
    public void removeChangeListener(WifiChangeListener listener) {
        listeners.remove(listener);
    }
}
