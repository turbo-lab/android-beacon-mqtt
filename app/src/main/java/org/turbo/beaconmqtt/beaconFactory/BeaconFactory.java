package org.turbo.beaconmqtt.beaconFactory;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.turbo.beaconmqtt.BuildConfig;
import org.turbo.beaconmqtt.MainActivity;
import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.BaseBleBeacon;
import org.turbo.beaconmqtt.beacon.IBeacon;
import org.turbo.beaconmqtt.beacon.TransactionBeacon;
import org.turbo.beaconmqtt.beacon.WifiBeacon;
import org.turbo.beaconmqtt.wifi.WifiChangeListener;
import org.turbo.beaconmqtt.wifi.WifiStateReceiver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static org.turbo.beaconmqtt.SettingsActivity.BEACON_EXIT_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_HYSTERESIS_FACTOR_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_INACTIVE_TIMEOUT_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_PAUSE_BETWEEN_SCANS_KEY;
import static org.turbo.beaconmqtt.SettingsActivity.BEACON_SCAN_DURATION_KEY;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_ENTER;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_EXIT;

public class BeaconFactory implements BootstrapNotifier, WifiChangeListener {
    public static final String BEACONS_KEY = "beaconList";
    private static final String TAG = "BeaconFactory";
    private static final int BEACON_LIST_SIZE = 10;
    private static final int BEACON_LIST_SIZE_PRO = 32;
    private final Context context;
    private final SharedPreferences defaultSharedPreferences;
    private final WifiStateReceiver wifiStateReceiver;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final BackgroundPowerSaver backgroundPowerSaver;
    private final Handler mHandler = new Handler();
    private final BeaconManager beaconManager;
    private final List<BaseBeacon> beaconList = new ArrayList<>();
    private final List<BeaconFactoryChangeListener> listeners = new ArrayList<>();
    private int activeActivityCount = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private RegionBootstrap regionBootstrap;
    private long scanDuration;
    private long backgroundPauseBetweenScans;
    private long inactiveTimeout;
    private long exitTimeout;
    private double hysteresisFactor;
    private final Runnable foregroundUpdaterRunnable = new Runnable() {
        public void run() {
            Log.i(TAG, "called foregroundUpdaterRunnable()");
            mHandler.postDelayed(this, scanDuration);
            touch();
        }
    };
    private final Runnable backgroundUpdaterRunnable = new Runnable() {
        public void run() {
            Log.i(TAG, "called backgroundUpdaterRunnable()");
            mHandler.postDelayed(this, scanDuration + backgroundPauseBetweenScans);
            touch();
        }
    };
    private TransactionBeacon transactionBeacon;

    public BeaconFactory(Context context) {
        this.context = context;
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(context);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        registerSettingsChangeListener();

        loadBeacons();
        startBleMonitoring();

        wifiStateReceiver = new WifiStateReceiver(context);
        wifiStateReceiver.addChangeListener(this);
        wifiStateReceiver.setExitTimeout(exitTimeout);

        transactionBeacon = new TransactionBeacon();

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
        backgroundPowerSaver = new BackgroundPowerSaver(context) {

            @Override
            public void onActivityResumed(Activity activity) {
                super.onActivityResumed(activity);

                if (activeActivityCount == 0) {
                    mHandler.removeCallbacks(backgroundUpdaterRunnable);
                    mHandler.postDelayed(foregroundUpdaterRunnable,
                            scanDuration);
                }

                activeActivityCount++;
                Log.i(TAG, "called onActivityCreated() for " + activity.getLocalClassName() + " " + activeActivityCount);
            }


            @Override
            public void onActivityPaused(Activity activity) {
                super.onActivityPaused(activity);
                activeActivityCount--;

                if (activeActivityCount == 0) {
                    mHandler.removeCallbacks(foregroundUpdaterRunnable);
                    mHandler.postDelayed(backgroundUpdaterRunnable,
                            scanDuration + backgroundPauseBetweenScans);
                }

                Log.i(TAG, "called onActivityDestroyed() for " + activity.getLocalClassName() + " " + activeActivityCount);
            }
        };

        mHandler.postDelayed(backgroundUpdaterRunnable,
                scanDuration + backgroundPauseBetweenScans);
    }

    public static String[] getBeaconTypes() {
        return new String[]{IBeacon.BEACON_IBEACON, WifiBeacon.BEACON_WIFI};
    }

    public Context getApplicationContext() {
        return context;
    }

    private void addBeacon(BaseBeacon beacon, boolean startWatching, boolean save) {
        if (getCurrentSize() >= getMaximumSize())
            return;

        if (beacon instanceof IBeacon) {
            IBeacon iBeacon = new IBeacon(beacon.getId(), beacon.getGroup(), beacon.getTag(),
                    beacon.getUuid(), beacon.getMajor(), beacon.getMinor(), beacon.getMacAddress());
            iBeacon.setBeaconFactory(this);
            beaconList.add(iBeacon);
            if (startWatching && iBeacon.isValid()) {
                startWatchingForBleBeacon(iBeacon);
            }
        } else if (beacon instanceof WifiBeacon) {
            WifiBeacon wifiBeacon = new WifiBeacon(beacon.getId(), beacon.getGroup(), beacon.getTag(),
                    beacon.getSsid());
            wifiBeacon.setBeaconFactory(this);
            beaconList.add(wifiBeacon);
        }

        sortFactory();
        changeFactoryNotifyListeners();

        if (save) {
            saveFactory();
        }
    }

    private void changeBeacon(String id, BaseBeacon beacon) {
        boolean changed = false;
        ListIterator<BaseBeacon> listIterator = beaconList.listIterator();

        while (listIterator.hasNext()) {
            BaseBeacon baseBeacon = listIterator.next();
            if (baseBeacon.getId().equals(id)) {
                if (baseBeacon instanceof IBeacon) {
                    if (baseBeacon.isValid()) {
                        stopWatchingForBleBeacon(baseBeacon);
                    }

                    IBeacon iBeacon = new IBeacon(beacon.getId(), beacon.getGroup(), beacon.getTag(),
                            beacon.getUuid(), beacon.getMajor(), beacon.getMinor(), beacon.getMacAddress());
                    iBeacon.setBeaconFactory(this);
                    listIterator.set(iBeacon);

                    if (iBeacon.isValid()) {
                        startWatchingForBleBeacon(iBeacon);
                    }

                } else if (baseBeacon instanceof WifiBeacon) {
                    WifiBeacon wifiBeacon = new WifiBeacon(beacon.getId(), beacon.getGroup(),
                            beacon.getTag(), beacon.getSsid());
                    wifiBeacon.setBeaconFactory(this);
                    listIterator.set(wifiBeacon);
                }
                changed = true;
            }
        }

        if (changed) {
            sortFactory();
            saveFactory();
            changeFactoryNotifyListeners();
        }
    }

    public void removeBeacon(String id) {
        boolean changed = false;
        ListIterator listIterator = beaconList.listIterator();

        while (listIterator.hasNext()) {
            BaseBeacon beacon = (BaseBeacon) listIterator.next();
            if (beacon.getId().equals(id)) {
                if (beacon.isValid()) {
                    stopWatchingForBleBeacon(beacon);
                }

                listIterator.remove();
                changed = true;
            }
        }

        if (changed) {
            sortFactory();
            saveFactory();
            changeFactoryNotifyListeners();
        }
    }

    private void loadBeacons() {
        String json = defaultSharedPreferences.getString(BEACONS_KEY, null);

        if (json != null) {
            Type beaconListType = new TypeToken<ArrayList<BaseBeacon>>() {
            }.getType();

            RuntimeTypeAdapterFactory<BaseBeacon> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                    .of(BaseBeacon.class, "type")
                    .registerSubtype(IBeacon.class, IBeacon.BEACON_IBEACON)
                    .registerSubtype(WifiBeacon.class, WifiBeacon.BEACON_WIFI);

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapterFactory(runtimeTypeAdapterFactory)
                    .create();

            List<BaseBeacon> list;
            list = gson.fromJson(json, beaconListType);
            for (BaseBeacon beacon : list) {
                addBeacon(beacon, false, false);
            }
            Log.i(TAG, "beaconList was deserialized with " + beaconList.size() + " beacons");
        }
    }

    /* For testing purpose only */
    public void reloadBeacons() {
        beaconList.clear();
        loadBeacons();
    }

    private void saveFactory() {
        SharedPreferences.Editor prefsEditor = defaultSharedPreferences.edit();

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        String json = gson.toJson(beaconList);

        prefsEditor.putString(BEACONS_KEY, json);
        prefsEditor.apply();
    }

    private void sortFactory() {
        Collections.sort(beaconList, new Comparator<BaseBeacon>() {
            @Override
            public int compare(BaseBeacon b1, BaseBeacon b2) {
                return b1.getSortOrder().compareTo(b2.getSortOrder());
            }
        });

        int index = 0;
        for (BaseBeacon baseBeacon : beaconList) {
            baseBeacon.setIndex(index++);
        }
    }

    public int getMaximumSize() {
        if (BuildConfig.PRO) {
            return BEACON_LIST_SIZE_PRO;
        }

        if (BuildConfig.DEBUG) {
            return BEACON_LIST_SIZE_PRO;
        }

        return BEACON_LIST_SIZE;
    }

    public int getCurrentSize() {
        return beaconList.size();
    }

    public List<BaseBeacon> getBeaconList() {
        return beaconList;
    }

    public IBeacon getIBeaconByUMMM(String uuid, String major, String minor, String macAddress) {
        for (BaseBeacon beacon : beaconList) {
            if (beacon instanceof IBeacon && beacon.isValid()) {
                if (beacon.getUuid().equals(uuid) &&
                        beacon.getMajor().equals(major) &&
                        beacon.getMinor().equals(minor) &&
                        beacon.getMacAddress().equals(macAddress)) {
                    return (IBeacon) beacon;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public IBeacon getIBeaconById(String id) {
        for (BaseBeacon beacon : beaconList) {
            if (beacon instanceof IBeacon) {
                if (beacon.getId().equals(id)) {
                    return (IBeacon) beacon;
                }
            }
        }
        return null;
    }

    public BaseBeacon getBeaconById(String id) {
        for (BaseBeacon beacon : beaconList) {
            if (beacon.getId().equals(id)) {
                return beacon;
            }
        }
        return null;
    }

    private BaseBleBeacon getBleBeaconById(String id) {
        return (BaseBleBeacon) getBeaconById(id);
    }

    @SuppressWarnings("unused")
    public WifiBeacon getWifiBeaconById(String id) {
        for (BaseBeacon beacon : beaconList) {
            if (beacon instanceof WifiBeacon) {
                if (beacon.getId().equals(id)) {
                    return (WifiBeacon) beacon;
                }
            }
        }
        return null;
    }

    public WifiBeacon getWifiBeaconBySsid(String ssid) {
        for (BaseBeacon beacon : beaconList) {
            if (beacon instanceof WifiBeacon && beacon.isValid()) {
                if (beacon.getSsid().equals(ssid)) {
                    return (WifiBeacon) beacon;
                }
            }
        }
        return null;
    }

    private List<BaseBeacon> getActiveBeaconList() {
        List<BaseBeacon> activeList = new ArrayList<>();
        for (BaseBeacon beacon : beaconList) {
            if (beacon.getState() != BEACON_STATE_EXIT) {
                activeList.add(beacon);
            }
        }
        return activeList;
    }

    private BaseBeacon getMasterBeacon() {
        for (BaseBeacon beacon : beaconList) {
            if (beacon.isMaster() && beacon.getState() == BEACON_STATE_ENTER) {
                return beacon;
            }
        }
        return null;
    }

    private BaseBeacon getClosestBeacon() {
        double closedDistance = 10000.0f;
        BaseBeacon closestBeacon = null;

        for (BaseBeacon beacon : beaconList) {
            if (beacon.getState() == BEACON_STATE_ENTER) {
                if (beacon.getDistance() < closedDistance) {
                    closedDistance = beacon.getDistance();
                    closestBeacon = beacon;
                }
            }
        }

        return closestBeacon;
    }

    private void startBleMonitoring() throws IllegalStateException {
        ArrayList<Region> regions = new ArrayList<>();

        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(IBeacon.BEACON_LAYOUT));
        // TODO: Add other layouts here

        if (BuildConfig.DEBUG) {
            BeaconManager.setDebug(true);
        } else {
            BeaconManager.setDebug(false);
        }

        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle("Searching for beacons");
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("BaseBeacon MQTT notifications",
                    "BaseBeacon MQTT notifications", NotificationManager.IMPORTANCE_DEFAULT);
            //channel.setDescription("My Notification Channel Description");
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }

        beaconManager.setRegionStatePersistenceEnabled(false);
        beaconManager.enableForegroundServiceScanning(builder.build(), 1);
        beaconManager.setEnableScheduledScanJobs(false);

        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "Called didRangeBeaconsInRegion() for " + region.getUniqueId());
                BaseBleBeacon bleBeacon = getBleBeaconById(region.getUniqueId());

                if (bleBeacon != null) {
                    if (beacons.size() > 0) {
                        for (Beacon beacon : beacons) {
                            Log.d(TAG, "Calling setRunningData()");
                            bleBeacon.setRunningData(beacon);
                            trackBeaconNotifyListeners(bleBeacon);
                        }
                    } else {
                        Log.d(TAG, "There are no active beacons");
                    }
                }
            }
        });

        try {
            scanDuration = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                    .getString(BEACON_SCAN_DURATION_KEY, "")));
            beaconManager.setForegroundScanPeriod(scanDuration);
            beaconManager.setBackgroundScanPeriod(scanDuration);

            backgroundPauseBetweenScans = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                    .getString(BEACON_PAUSE_BETWEEN_SCANS_KEY, "")));
            beaconManager.setBackgroundBetweenScanPeriod(backgroundPauseBetweenScans);

            exitTimeout = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                    .getString(BEACON_EXIT_TIMEOUT_KEY, "")));
            BeaconManager.setRegionExitPeriod(exitTimeout);

            inactiveTimeout = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                    .getString(BEACON_INACTIVE_TIMEOUT_KEY, "")));

            hysteresisFactor = Double.parseDouble(Objects.requireNonNull(defaultSharedPreferences
                    .getString(BEACON_HYSTERESIS_FACTOR_KEY, "")));

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        for (BaseBeacon beacon : beaconList) {
            try {
                if (beacon instanceof IBeacon) {
                    Log.i(TAG, "Region id=" + beacon.getId() + " uuid=" + beacon.getUuid() + ", major=" + beacon.getMajor() + ", minor=" + beacon.getMinor());
                    List<Identifier> ids = new ArrayList<>();

                    ids.add(Identifier.parse(beacon.getUuid()));
                    ids.add(Identifier.parse(beacon.getMajor()));
                    ids.add(Identifier.parse(beacon.getMinor()));

                    if (beacon.getMacAddress().length() > 0) {
                        Region region = new Region(beacon.getId(),
                                ids,
                                beacon.getMacAddress());
                        regions.add(region);
                    } else {
                        Region region = new Region(beacon.getId(),
                                ids);
                        regions.add(region);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            regionBootstrap = new RegionBootstrap(this, regions);
        } catch (IllegalStateException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    JobScheduler jobScheduler =
                        (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                List<JobInfo> pendingJobs = jobScheduler.getAllPendingJobs();

                StringBuilder message = new StringBuilder("pendingJobsList");

                if (pendingJobs.isEmpty()) {
                    message.append(" is empty");
                } else {
                    message.append(" contains ").append(pendingJobs.size()).append(" entries:");
                    for (JobInfo job : pendingJobs) {
                        message.append("\n").append(job.toString());
                    }
                }

                StringWriter stackTraceWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTraceWriter));
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onWifiConnected(String ssid) {
        WifiBeacon beacon = getWifiBeaconBySsid(ssid);
        if (beacon != null) {
            beacon.setState(BEACON_STATE_ENTER);
        }
    }

    @Override
    public void onWifiUpdated(String ssid, WifiInfo wifiInfo) {
        WifiBeacon beacon = getWifiBeaconBySsid(ssid);
        if (beacon != null) {
            beacon.setRunningData(wifiInfo);
            trackBeaconNotifyListeners(beacon);
        }
    }

    @Override
    public void onWifiDisconnected(String ssid) {
        WifiBeacon exMaster = getWifiBeaconBySsid(ssid);

        // TODO: check it
        if (exMaster != null) {
            exMaster.setState(BEACON_STATE_EXIT);

            if (getActiveBeaconList().size() == 0) {
                exitMasterNotifyListeners(exMaster);
            }
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "Called didEnterRegion() for " + region.getUniqueId());
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG, "Called didExitRegion() for " + region.getUniqueId());
        BaseBeacon exMaster = getBeaconById(region.getUniqueId());

        if (exMaster != null && getActiveBeaconList().size() == 0) {
            exitMasterNotifyListeners(exMaster);
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG, "Called didDetermineStateForRegion() for " + region.getUniqueId() + " with state " + state);

        BaseBeacon beacon = getBeaconById(region.getUniqueId());

        if (beacon != null) {
            if (state == INSIDE) {
                beacon.setState(BEACON_STATE_ENTER);
                try {
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (state == OUTSIDE) {
                beacon.setState(BEACON_STATE_EXIT);
                try {
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public TransactionBeacon getTransactionBeacon() {
        return transactionBeacon;
    }

    public void setTransactionBeacon(TransactionBeacon transactionBeacon) {
        this.transactionBeacon = transactionBeacon;
    }

    private void touch() {
        wifiStateReceiver.touch();
        for (BaseBeacon beacon : beaconList) {
            beacon.expire();
        }
        selectMaster();
    }

    private void registerSettingsChangeListener() {
        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                try {
                    if (BEACON_SCAN_DURATION_KEY.equals(key)) {
                        scanDuration = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                                .getString(BEACON_SCAN_DURATION_KEY, "")));
                        beaconManager.setForegroundScanPeriod(scanDuration);
                        beaconManager.setBackgroundScanPeriod(scanDuration);
                    } else if (BEACON_PAUSE_BETWEEN_SCANS_KEY.equals(key)) {
                        backgroundPauseBetweenScans = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                                .getString(BEACON_PAUSE_BETWEEN_SCANS_KEY, "")));
                        beaconManager.setBackgroundBetweenScanPeriod(backgroundPauseBetweenScans);
                    } else if (BEACON_EXIT_TIMEOUT_KEY.equals(key)) {
                        long exitTimeout = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                                .getString(BEACON_EXIT_TIMEOUT_KEY, "")));
                        BeaconManager.setRegionExitPeriod(exitTimeout);
                        wifiStateReceiver.setExitTimeout(exitTimeout);
                    } else if (BEACON_INACTIVE_TIMEOUT_KEY.equals(key)) {
                        inactiveTimeout = Long.parseLong(Objects.requireNonNull(defaultSharedPreferences
                                .getString(BEACON_INACTIVE_TIMEOUT_KEY, "")));
                    } else if (BEACON_HYSTERESIS_FACTOR_KEY.equals(key)) {
                        hysteresisFactor = Double.parseDouble(Objects.requireNonNull(defaultSharedPreferences
                                .getString(BEACON_HYSTERESIS_FACTOR_KEY, "")));
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        };
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void selectMaster() {
        BaseBeacon masterBeacon = getMasterBeacon();
        BaseBeacon closestBeacon = getClosestBeacon();

        if (closestBeacon == null) {
            Log.d(TAG, "Oops... We have no closest beacon in range");
        } else if (masterBeacon == null) { // Let's become the master
            Log.i(TAG, "We are alone. Let's become the master!");
            closestBeacon.setMaster(true);
            enterMasterNotifyListeners(closestBeacon);
        } else if (masterBeacon.getId().equals(closestBeacon.getId())) { // We are master already
            Log.d(TAG, "The master is the same");
        } else if (closestBeacon.getDistance() * hysteresisFactor < masterBeacon.getDistance()) {
            Log.i(TAG, "New master");
            masterBeacon.setMaster(false);
            closestBeacon.setMaster(true);
            enterMasterNotifyListeners(closestBeacon);
        }
    }

    private void enterMasterNotifyListeners(@NonNull BaseBeacon master) {
        for (BeaconFactoryChangeListener listener : listeners) {
            listener.onEnterMaster(master);
        }
    }

    private void exitMasterNotifyListeners(@NonNull BaseBeacon exMaster) {
        for (BeaconFactoryChangeListener listener : listeners) {
            listener.onExitMaster(exMaster);
        }
    }

    private void changeFactoryNotifyListeners() {
        for (BeaconFactoryChangeListener listener : listeners) {
            listener.onChangeFactory();
        }
    }

    public void changeBeaconNotifyListeners(@NonNull BaseBeacon beacon, BeaconState state) {
        for (BeaconFactoryChangeListener listener : listeners) {
            listener.onChangeBeacon(beacon, state);
        }
    }

    private void trackBeaconNotifyListeners(@NonNull BaseBeacon beacon) {
        for (BeaconFactoryChangeListener listener : listeners) {
            listener.onTrackBeacon(beacon);
        }
    }

    public void addChangeListener(BeaconFactoryChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(BeaconFactoryChangeListener listener) {
        listeners.remove(listener);
    }

    public void insertBeaconFromTransaction() {
        if (transactionBeacon.getType().equals(IBeacon.BEACON_IBEACON)) {
            addBeacon(new IBeacon(transactionBeacon), true, true);
        } else if (transactionBeacon.getType().equals(WifiBeacon.BEACON_WIFI)) {
            addBeacon(new WifiBeacon(transactionBeacon), true, true);
        }
    }

    public void updateBeaconFromTransaction(String id) {
        if (transactionBeacon.getType().equals(IBeacon.BEACON_IBEACON)) {
            IBeacon beacon = new IBeacon(transactionBeacon);
            changeBeacon(id, beacon);
        } else if (transactionBeacon.getType().equals(WifiBeacon.BEACON_WIFI)) {
            WifiBeacon beacon = new WifiBeacon(transactionBeacon);
            changeBeacon(id, beacon);
        }
    }

    public long getInactiveTimeout() {
        return inactiveTimeout;
    }

    private void stopWatchingForBleBeacon(BaseBeacon beacon) {
        try {
            Region region;
            List<Identifier> ids = new ArrayList<>();

            // TODO: if (beacon instanceof ZZZ)
            ids.add(Identifier.parse(beacon.getUuid()));
            ids.add(Identifier.parse(beacon.getMajor()));
            ids.add(Identifier.parse(beacon.getMinor()));

            if (beacon.getMacAddress().length() > 0) {
                region = new Region(beacon.getId(), ids, beacon.getMacAddress());
            } else {
                region = new Region(beacon.getId(), ids);
            }

            beaconManager.stopRangingBeaconsInRegion(region);
            beaconManager.stopMonitoringBeaconsInRegion(region);
        } catch (IllegalArgumentException | RemoteException e) {
            e.printStackTrace();
        }

        beacon.setState(BEACON_STATE_EXIT);

        if (getActiveBeaconList().size() == 0) {
            exitMasterNotifyListeners(beacon);
        }
    }

    private void startWatchingForBleBeacon(BaseBeacon beacon) {
        try {
            Region region;
            List<Identifier> ids = new ArrayList<>();

            // TODO: if (beacon instanceof ZZZ)
            ids.add(Identifier.parse(beacon.getUuid()));
            ids.add(Identifier.parse(beacon.getMajor()));
            ids.add(Identifier.parse(beacon.getMinor()));

            if (beacon.getMacAddress().length() > 0) {
                region = new Region(beacon.getId(), ids, beacon.getMacAddress());
            } else {
                region = new Region(beacon.getId(), ids);
            }

            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (IllegalArgumentException | RemoteException e) {
            e.printStackTrace();
        }
    }
}
