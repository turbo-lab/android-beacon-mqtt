package org.turbo.beaconmqtt;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.turbo.beaconmqtt.beacon.TransactionBeacon;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactory;
import org.turbo.beaconmqtt.dialog.TypeBeaconDialogFragment;
import org.turbo.beaconmqtt.newBeacon.NewBeaconAdapter;

import java.util.Collection;

public class SearchingActivity extends AppCompatActivity implements BeaconConsumer {
    private static final String TAG = "SearchingActivity";

    private static final String FAKE_REGION_ID_FOR_RANGING = "myRangingUniqueId";
    private BeaconManager beaconManager;
    private BeaconFactory beaconFactory;
    private NewBeaconAdapter beaconAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconFactory = ((BeaconApplication) getApplication()).getBeaconFactory();

        setContentView(R.layout.activity_ranging);

        setupActionBar();

        RecyclerView beaconRecyclerView = findViewById(R.id.list_beacon);
        beaconRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager beaconLayoutManager = new LinearLayoutManager(this);
        beaconRecyclerView.setLayoutManager(beaconLayoutManager);

        beaconAdapter = new NewBeaconAdapter(this, getApplicationContext());
        beaconRecyclerView.setAdapter(beaconAdapter);

        showEmptyView();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        // todo: https://github.com/AltBeacon/android-beacon-library/issues/614
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region(FAKE_REGION_ID_FOR_RANGING, null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        beaconManager.unbind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        beaconManager.bind(this);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_ranging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_manual:
                beaconFactory.setTransactionBeacon(new TransactionBeacon());
                TypeBeaconDialogFragment newFragment = TypeBeaconDialogFragment.newInstance();
                newFragment.show(getSupportFragmentManager(), "TypeBeaconDialog");
                return true;
            case R.id.action_restart_scan:
                beaconAdapter.clear();
                showEmptyView();
                return true;
        }
        return false;
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect");
        RangeNotifier rangeNotifier = new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                //Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  " + beacons.size());
                for (Beacon beacon : beacons) {
                    if (beaconFactory.getBeaconById(region.getUniqueId()) == null) {
                        beaconAdapter.addBeacon(beacon);
                        hideEmptyView();
                    }
                }
            }
        };
        try {
            beaconManager.startRangingBeaconsInRegion(new Region(FAKE_REGION_ID_FOR_RANGING, null, null, null));
            beaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void hideEmptyView() {
        ImageView progressIcon = findViewById(R.id.image_radiation);
        progressIcon.clearAnimation();

        LinearLayout emptyView = findViewById(R.id.empty_view);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        ImageView progressIcon = findViewById(R.id.image_radiation);
        AlphaAnimation animation1 = new AlphaAnimation(0.0f, 1.0f);
        animation1.setDuration(1000);
        animation1.setRepeatMode(Animation.REVERSE);
        animation1.setRepeatCount(Animation.INFINITE);
        progressIcon.startAnimation(animation1);

        LinearLayout emptyView = findViewById(R.id.empty_view);
        emptyView.setVisibility(View.VISIBLE);
    }
}
