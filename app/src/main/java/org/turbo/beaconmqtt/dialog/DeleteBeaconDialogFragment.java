package org.turbo.beaconmqtt.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.R;

import java.util.Objects;

public class DeleteBeaconDialogFragment extends DialogFragment {
    public static DeleteBeaconDialogFragment newInstance(String beaconName) {
        DeleteBeaconDialogFragment fragment = new DeleteBeaconDialogFragment();
        Bundle args = new Bundle();
        args.putString("beaconId", beaconName);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String beaconId = Objects.requireNonNull(getArguments()).getString("beaconId");

        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        builder.setTitle(R.string.delete_beacon);
        builder.setMessage(getString(R.string.are_you_sure_to_delete, beaconId));
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                BeaconApplication application = ((BeaconApplication) Objects.requireNonNull(getActivity())
                        .getApplicationContext());
                application.getBeaconFactory().removeBeacon(beaconId);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }
}
