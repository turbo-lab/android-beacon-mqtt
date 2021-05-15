package org.turbo.beaconmqtt.beacon;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beaconFactory.BeaconFactory;

import java.util.Locale;

import static org.turbo.beaconmqtt.beacon.Helper.BeaconState;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_ENTER;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_EXIT;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_INACTIVE;

public abstract class BaseBeacon {
    @SuppressWarnings("unused")
    private static final String TAG = BaseBeacon.class.getName();
    @Expose
    @SerializedName("id")
    String mId;
    @Expose
    @SerializedName("group")
    String mGroup;
    @Expose
    @SerializedName("tag")
    String mTag;
    @Expose
    @SerializedName("type")
    String mType;
    int mRssi;
    private BeaconFactory beaconFactory = null;
    private int mIndex;
    private boolean mMaster;
    private BeaconState mState = BEACON_STATE_EXIT;
    private DateTime mLastSeenTimeStamp;
    private String mLastSeenString;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        if (!id.equals(mId)) {
            this.mId = id;
            changeBeaconNotifyListeners();
        }
    }

    public String getGroup() {
        return mGroup;
    }

    public void setGroup(String group) {
        if (!group.equals(mGroup)) {
            this.mGroup = group;
            changeBeaconNotifyListeners();
        }
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        if (!tag.equals(mTag)) {
            this.mTag = tag;
            changeBeaconNotifyListeners();
        }
    }

    public String getType() {
        return mType;
    }

    public String getUuid() {
        return "";
    }

    public String getMajor() {
        return "";
    }

    public String getMinor() {
        return "";
    }

    public String getMacAddress() {
        return "";
    }

    public String getSsid() {
        return "";
    }

    public double getDistance() {
        // TODO
        return 1000.0d;
    }

    public int getRssi() {
        return mRssi;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public String getSortOrder() {
        return mGroup + mId;
    }

    public BeaconState getState() {
        return mState;
    }

    public void setState(BeaconState state) {
        if (mState != state) {
            if (state == BEACON_STATE_EXIT) {
                mMaster = false;
            }
            mState = state;
            changeBeaconNotifyListeners(state);
        }
    }

    private int getTimeout() {
        if (mLastSeenTimeStamp != null) {
            DateTime now = new DateTime();
            return Seconds.secondsBetween(mLastSeenTimeStamp, now).getSeconds();
        }

        return -1;
    }

    public String getLastSeenString() {
        return mLastSeenString;
    }

    private void updateLastSeenString() {
        if (mLastSeenTimeStamp != null) {
            Context context = beaconFactory.getApplicationContext();
            DateTime now = new DateTime();

            String format = context.getString(R.string.beacon_last_seen_time_format);
            String day = context.getString(R.string.beacon_last_seen_day);
            String days = context.getString(R.string.beacon_last_seen_days);
            String hour = context.getString(R.string.beacon_last_seen_hour);
            String hours = context.getString(R.string.beacon_last_seen_hours);
            String minute = context.getString(R.string.beacon_last_seen_minute);
            String minutes = context.getString(R.string.beacon_last_seen_minutes);
            String justNow = context.getString(R.string.beacon_last_seen_just_now);
            String newLastSeenString;

            int d = Days.daysBetween(mLastSeenTimeStamp, now).getDays();
            int h = Hours.hoursBetween(mLastSeenTimeStamp, now).getHours();
            int m = Minutes.minutesBetween(mLastSeenTimeStamp, now).getMinutes();

            if (d > 0) {
                newLastSeenString = String.format(Locale.getDefault(), format, d, d > 1 ? days : day);
            } else if (h > 0) {
                newLastSeenString = String.format(Locale.getDefault(), format, h, h > 1 ? hours : hour);
            } else if (m > 0) {
                newLastSeenString = String.format(Locale.getDefault(), format, m, m > 1 ? minutes : minute);
            } else {
                newLastSeenString = justNow;
            }

            if (!newLastSeenString.equals(mLastSeenString)) {
                mLastSeenString = newLastSeenString;
                changeBeaconNotifyListeners();
            }
        }
    }

    public boolean isMaster() {
        return mMaster;
    }

    public void setMaster(boolean master) {
        if (this.mMaster != master) {
            this.mMaster = master;
            changeBeaconNotifyListeners();
        }
    }

    void see() {
        this.mLastSeenTimeStamp = new DateTime();
        setState(BEACON_STATE_ENTER);
    }

    public void expire() {
        if (beaconFactory != null) {
            long inactiveTimeout = beaconFactory.getInactiveTimeout() / 1000;
            if (mState == BEACON_STATE_ENTER &&
                    getTimeout() > inactiveTimeout) {
                setState(BEACON_STATE_INACTIVE);
            } else if (mState == BEACON_STATE_EXIT) {
                updateLastSeenString();
            }
        }
    }

    public void setBeaconFactory(BeaconFactory beaconFactory) {
        this.beaconFactory = beaconFactory;
    }

    void changeBeaconNotifyListeners() {
        if (beaconFactory != null) {
            beaconFactory.changeBeaconNotifyListeners(this, null);
        }
    }

    private void changeBeaconNotifyListeners(BeaconState state) {
        if (beaconFactory != null) {
            beaconFactory.changeBeaconNotifyListeners(this, state);
        }
    }

    public boolean isValid() {
        boolean isValid = Helper.validateId(mId);
        isValid &= Helper.validateGroup(mGroup);
        isValid &= Helper.validateTag(mTag);
        return isValid;
    }
}


