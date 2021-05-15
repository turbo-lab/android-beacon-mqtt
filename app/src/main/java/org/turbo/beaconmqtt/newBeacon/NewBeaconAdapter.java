package org.turbo.beaconmqtt.newBeacon;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beacon.Helper;
import org.turbo.beaconmqtt.beacon.IBeacon;
import org.turbo.beaconmqtt.beacon.TransactionBeacon;
import org.turbo.beaconmqtt.dialog.BaseBeaconDialogFragment;

import java.util.Locale;

public class NewBeaconAdapter extends RecyclerView.Adapter<NewBeaconAdapter.BeaconViewHolder> {

    final private Context activityContext;
    final private Context applicationContext;

    private final NewBeaconList beaconList;

    public NewBeaconAdapter(Context activityContext, Context applicationContext) {
        this.activityContext = activityContext;
        this.applicationContext = applicationContext;
        this.beaconList = new NewBeaconList();
    }

    @Override
    public int getItemCount() {
        return beaconList.getSize();
    }

    public void clear() {
        beaconList.clear();
        notifyDataSetChanged();
    }

    public void addBeacon(Beacon beacon) {
        beaconList.addBeacon(beacon);
        notifyDataSetChanged();
    }

    @Override
    public @NonNull
    BeaconViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int position) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_new_beacon_card,
                viewGroup,
                false);
        return new BeaconViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BeaconViewHolder beaconViewHolder, final int position) {

        final Beacon beacon = beaconList.getBeacon(position);

        if (position == 0) {
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) beaconViewHolder.cv.getLayoutParams();
            int marign = layoutParams.getMarginEnd();
            layoutParams.setMargins(marign, marign, marign, marign);
            beaconViewHolder.cv.setLayoutParams(layoutParams);
        }

        beaconViewHolder.beaconType.setText(Helper.getBleBeaconString(beacon));
        beaconViewHolder.distance.setText(String
                .format(Locale.getDefault(), "%.02f", beacon.getDistance()));

        if (IBeacon.BEACON_IBEACON.equals(Helper.getBleBeaconString(beacon))) {
            beaconViewHolder.uuid.setText(activityContext
                    .getString(R.string.card_text_uuid, beacon.getId1().toString()));
            beaconViewHolder.major.setText(activityContext
                    .getString(R.string.card_text_major, beacon.getId2().toString()));
            beaconViewHolder.minor.setText(activityContext
                    .getString(R.string.card_text_minor, beacon.getId3().toString()));
            beaconViewHolder.ibeaconView.setVisibility(View.VISIBLE);
        }

        beaconViewHolder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BeaconApplication application = (BeaconApplication) applicationContext;
                TransactionBeacon transactionBeacon = new TransactionBeacon();

                transactionBeacon.setType(IBeacon.BEACON_IBEACON);
                transactionBeacon.setUuid(beacon.getId1().toString());
                transactionBeacon.setMajor(beacon.getId2().toString());
                transactionBeacon.setMinor(beacon.getId3().toString());
                transactionBeacon.setMacAddress(beacon.getBluetoothAddress());

                application.getBeaconFactory().setTransactionBeacon(transactionBeacon);

                BaseBeaconDialogFragment newFragment = BaseBeaconDialogFragment
                        .newInstance(null);
                newFragment.show(((AppCompatActivity) activityContext).getSupportFragmentManager(),
                        "BaseBeaconDialog");
            }
        });
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class BeaconViewHolder extends RecyclerView.ViewHolder {
        final CardView cv;

        final TextView beaconType;
        final TextView distance;

        final ConstraintLayout ibeaconView;
        final TextView uuid;
        final TextView minor;
        final TextView major;

        BeaconViewHolder(View itemView) {
            super(itemView);

            cv = itemView.findViewById(R.id.cv);

            beaconType = itemView.findViewById(R.id.text_beacon_type);
            distance = itemView.findViewById(R.id.text_distance);

            ibeaconView = itemView.findViewById(R.id.ibeacon_item);
            uuid = itemView.findViewById(R.id.text_uuid);
            major = itemView.findViewById(R.id.text_major);
            minor = itemView.findViewById(R.id.text_minor);
        }
    }
}
