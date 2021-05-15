package org.turbo.beaconmqtt;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.BeaconManager;
import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.Helper;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactory;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactoryAdapter;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactoryChangeListener;
import org.turbo.beaconmqtt.broadcaster.BaseBroadcaster;
import org.turbo.beaconmqtt.broadcaster.Broadcaster;
import org.turbo.beaconmqtt.broadcaster.BroadcasterChangeListener;
import org.turbo.beaconmqtt.preferencesConverter.PreferencesConverter;

import java.util.List;
import java.util.Locale;

import static org.turbo.beaconmqtt.SettingsActivity.NOTIFICATION_SHOW_LOG;

public class MainActivity extends AppCompatActivity
        implements BeaconFactoryChangeListener, BroadcasterChangeListener {
    @SuppressWarnings("unused")
    final private static String TAG = "MainActivity";
    final private static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    final private static int PERMISSION_REQUEST_FINE_LOCATION = 2;
    final private static int PERMISSION_REQUEST_BACKGROUND_LOCATION = 3;
    private BeaconApplication application;
    private BeaconFactory beaconFactory;
    private Broadcaster broadcaster;
    private BeaconFactoryAdapter beaconFactoryAdapter;
    private SharedPreferences defaultSharedPreferences;
    private ScrollView debugLogScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (BeaconApplication) this.getApplicationContext();
        beaconFactory = application.getBeaconFactory();
        broadcaster = application.getBroadcaster();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_main);

        debugLogScrollView = this.findViewById(R.id.debug_log_scroll_view);

        RecyclerView beaconListView = findViewById(R.id.beacon_list);
        beaconListView.setHasFixedSize(true);
        beaconListView.setHasFixedSize(false);
        RecyclerView.LayoutManager beaconLayoutManager = new LinearLayoutManager(this);
        beaconListView.setLayoutManager(beaconLayoutManager);

        beaconFactoryAdapter = new BeaconFactoryAdapter(this,
                beaconFactory.getBeaconList());
        beaconListView.setAdapter(beaconFactoryAdapter);

        verifyBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.coarse_location_access);
                builder.setMessage(R.string.grant_coarse_location_access);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            } else {
                verifyBackgroundLocation();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (MainActivity.this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.coarse_location_access);
                builder.setMessage(R.string.grant_coarse_location_access);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "location permission granted");
            if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        verifyBackgroundLocation();
                    }
                }, 2000);
            }
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.warning);
            builder.setMessage(R.string.coarse_location_access_failed);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                }
            });
            builder.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                this.startActivity(settingsIntent);
                return true;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                this.startActivity(aboutIntent);
                return true;
            case R.id.action_add_beacon:
                if (beaconFactory.getCurrentSize() <
                        beaconFactory.getMaximumSize()) {
                    Intent rangingIntent = new Intent(this, SearchingActivity.class);
                    this.startActivity(rangingIntent);
                } else {
                    String text = getResources()
                            .getQuantityString(R.plurals.beacons_count_limit,
                                    beaconFactory.getMaximumSize(),
                                    beaconFactory.getMaximumSize());
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                }
                return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        application.setMainActivity(this);

        broadcaster.addChangeListener(this);
        beaconFactory.addChangeListener(this);

        updateDebugLog(application.getDebugLog());
        debugLogScrollView.post(new Runnable() {
            public void run() {
                debugLogScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

        updateServers();
        onChangeFactory();

        if (PreferencesConverter.isPreferencesRevisionChanged(defaultSharedPreferences)) {
            showPreferencesRevisionChangedDialog();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        application.setMainActivity(null);
        broadcaster.removeChangeListener(this);
        beaconFactory.removeChangeListener(this);
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.warning);
                builder.setMessage(R.string.bluetooth_disabled);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.warning);
            builder.setMessage(R.string.bluetooth_not_available);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }

    private void verifyBackgroundLocation() {
        if (MainActivity.this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.coarse_location_access);
            builder.setMessage(R.string.grant_coarse_location_access);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @TargetApi(23)
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_BACKGROUND_LOCATION);
                }
            });
            builder.show();
        }
    }

    public void updateDebugLog(final String log) {
        runOnUiThread(new Runnable() {
            public void run() {
                boolean showLog = defaultSharedPreferences
                        .getBoolean(NOTIFICATION_SHOW_LOG, false);
                TextView textView = MainActivity.this
                        .findViewById(R.id.debug_log);
                ScrollView scrollView = MainActivity.this
                        .findViewById(R.id.debug_log_scroll_view);
                if (showLog) {
                    textView.setText(log);
                    scrollView.setVisibility(View.VISIBLE);
                } else {
                    scrollView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateServerState(boolean state, @Nullable String name) {
        ImageView img = findViewById(R.id.server_state_icon);
        TextView textView = findViewById(R.id.server_name);

        if (state) {
            img.setImageResource(R.drawable.ic_server_online);
        } else {
            img.setImageResource(R.drawable.ic_server_offline);
        }

        if (name != null && name.length() > 0) {
            textView.setText(name.toUpperCase(Locale.getDefault()));
        } else {
            textView.setText(getString(R.string.empty_server_string)
                    .toUpperCase(Locale.getDefault()));
        }
    }

    @Override
    public void onChangedBroadcaster(@NonNull List<BaseBroadcaster> broadcasters) {
        // TODO: Update all servers from list
        updateServerState(broadcasters.get(0).getState(), broadcasters.get(0).getName());
    }

    private void updateServers() {
        List<BaseBroadcaster> broadcasters = broadcaster.getBroadcasters();
        onChangedBroadcaster(broadcasters);
    }

    @Override
    public void onChangeFactory() {
        beaconFactoryAdapter.updateFactory();
    }

    @Override
    public void onEnterMaster(@NonNull BaseBeacon beacon) {
        // do nothing
    }

    @Override
    public void onExitMaster(@NonNull BaseBeacon beacon) {
        // do nothing
    }

    @Override
    public void onChangeBeacon(@NonNull BaseBeacon beacon, Helper.BeaconState state) {
        beaconFactoryAdapter.updateBeacon(beacon.getIndex());
    }

    @Override
    public void onTrackBeacon(@NonNull BaseBeacon beacon) {
        // do nothing
    }

    private void showPreferencesRevisionChangedDialog() {
        try {
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.warning));
            builder.setMessage(getString(R.string.preferences_revision_changed, packageInfo.versionName));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    PreferencesConverter.hidePreferencesRevisionChanged(defaultSharedPreferences);
                    Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(settingsIntent);
                }
            });
            builder.show();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
