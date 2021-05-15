package org.turbo.beaconmqtt.beaconFactory;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beacon.BaseBeacon;
import org.turbo.beaconmqtt.beacon.BaseBleBeacon;
import org.turbo.beaconmqtt.beacon.WifiBeacon;
import org.turbo.beaconmqtt.dialog.ContextBeaconDialogFragment;

import java.util.List;
import java.util.Locale;

import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_ENTER;
import static org.turbo.beaconmqtt.beacon.Helper.BeaconState.BEACON_STATE_INACTIVE;

public class BeaconFactoryAdapter extends RecyclerView.Adapter<BeaconFactoryAdapter.BeaconViewHolder> {
    @SuppressWarnings("unused")
    private static final String TAG = "BeaconFactoryAdapter";
    final private Context activityContext;

    private final List<BaseBeacon> beaconList;

    public BeaconFactoryAdapter(Context activityContext, List<BaseBeacon> beaconList) {
        this.activityContext = activityContext;
        this.beaconList = beaconList;
    }

    public void updateFactory() {
        notifyDataSetChanged();
    }

    public void updateBeacon(int position) {
        notifyItemChanged(position, "dummy");
    }

    @Override
    public int getItemCount() {
        return beaconList.size();
    }

    @NonNull
    @Override
    public BeaconFactoryAdapter.BeaconViewHolder onCreateViewHolder(ViewGroup viewGroup, final int position) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_beacon_card,
                viewGroup,
                false);
        return new BeaconViewHolder(v);
    }

    private void drawIBeaconCard(@NonNull BeaconFactoryAdapter.BeaconViewHolder beaconViewHolder,
                                 BaseBeacon beacon) {
        beaconViewHolder.beaconIcon.setImageResource(R.drawable.ic_bluetooth_beacon_w_radiation);

        if (beacon.getDistance() > 0.0f) {
            if (beacon.getState() == BEACON_STATE_ENTER || beacon.getState() == BEACON_STATE_INACTIVE) {
                beaconViewHolder.beaconDetails
                        .setText(String.format(Locale.ROOT, "%.1f m", beacon.getDistance()));
            }
        }
    }

    private void drawWifiBeaconCard(@NonNull BeaconFactoryAdapter.BeaconViewHolder beaconViewHolder,
                                    BaseBeacon beacon) {
        beaconViewHolder.beaconIcon.setImageResource(R.drawable.ic_access_point);

        if (beacon.getState() == BEACON_STATE_ENTER || beacon.getState() == BEACON_STATE_INACTIVE) {
            beaconViewHolder.beaconDetails
                    .setText(String.format(Locale.ROOT, "%d dBm", beacon.getRssi()));
        }
    }

    private void drawExitBeaconCard(@NonNull BeaconFactoryAdapter.BeaconViewHolder beaconViewHolder,
                                    BaseBeacon beacon) {
        beaconViewHolder.beaconIcon.setAlpha(0.0f);
        String lastSeen = beacon.getLastSeenString();
        if (lastSeen != null) {
            beaconViewHolder.beaconDetails.setText(activityContext.getString(R.string.beacon_last_seen_format, lastSeen));
        }

        beaconViewHolder.beaconDetails.setAlpha(0.5f);
    }

    private void drawEnterBeaconCard(@NonNull BeaconFactoryAdapter.BeaconViewHolder beaconViewHolder,
                                     BaseBeacon beacon) {
        if (beacon.isMaster()) {
            beaconViewHolder.beaconIcon.setAlpha(1.0f);
        } else {
            beaconViewHolder.beaconIcon.setAlpha(0.5f);
        }
        beaconViewHolder.beaconDetails.setAlpha(1.0f);
    }

    @SuppressWarnings("unused")
    private void drawInactiveBeaconCard(@NonNull BeaconFactoryAdapter.BeaconViewHolder beaconViewHolder,
                                        BaseBeacon beacon) {

        beaconViewHolder.beaconIcon.setAlpha(0.5f);
        beaconViewHolder.beaconDetails.setAlpha(0.5f);
    }

    @Override
    public void onBindViewHolder(@NonNull BeaconFactoryAdapter.BeaconViewHolder beaconViewHolder,
                                 final int position) {
        if (position == 0) {
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) beaconViewHolder.cv.getLayoutParams();
            int marign = layoutParams.getMarginEnd();
            layoutParams.setMargins(marign, marign, marign, marign);
            beaconViewHolder.cv.setLayoutParams(layoutParams);
        }

        beaconViewHolder.cv.setTag(position);
        beaconViewHolder.cv.setOnLongClickListener(new BeaconFactoryAdapter.OnBeaconLongClickListener());

        BaseBeacon beacon = beaconList.get(position);

        beaconViewHolder.beaconIcon.setImageResource(0);

        beaconViewHolder.beaconId.setAlpha(1.0f);
        beaconViewHolder.beaconId.setText(beacon.getId().toUpperCase(Locale.ROOT));
        beaconViewHolder.beaconDetails.setText("");

        if (beacon.isValid()) {
            // Ble-specific case
            if (beacon instanceof BaseBleBeacon) {
                drawIBeaconCard(beaconViewHolder, beacon);
            }
            // Wifi-specific case
            else if (beacon instanceof WifiBeacon) {
                drawWifiBeaconCard(beaconViewHolder, beacon);
            }

            // State-specific cases
            switch (beacon.getState()) {
                case BEACON_STATE_EXIT:
                    drawExitBeaconCard(beaconViewHolder, beacon);
                    break;
                case BEACON_STATE_ENTER:
                    drawEnterBeaconCard(beaconViewHolder, beacon);
                    break;
                case BEACON_STATE_INACTIVE:
                    drawInactiveBeaconCard(beaconViewHolder, beacon);
                    break;
            }
        }
        // Invalid-specific case
        else {
            beaconViewHolder.beaconDetails.setText(R.string.invalid_config);
            beaconViewHolder.beaconDetails.setAlpha(0.5f);
            beaconViewHolder.beaconId.setAlpha(0.5f);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class BeaconViewHolder extends RecyclerView.ViewHolder {
        final CardView cv;

        final ImageView beaconIcon;
        final TextView beaconId;
        final TextView beaconDetails;

        BeaconViewHolder(View itemView) {
            super(itemView);

            cv = itemView.findViewById(R.id.cv);

            beaconIcon = itemView.findViewById(R.id.beacon_icon);
            beaconId = itemView.findViewById(R.id.beacon_id);
            beaconDetails = itemView.findViewById(R.id.beacon_details);
        }
    }

    private class OnBeaconLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            final int position = (int) view.getTag();
            final BaseBeacon beacon = beaconList.get(position);

            ContextBeaconDialogFragment newFragment = ContextBeaconDialogFragment.newInstance(
                    beacon.getId());
            newFragment.show(((AppCompatActivity) activityContext).getSupportFragmentManager(),
                    "ContextBeaconDialog");

            return true;
        }
    }
}
