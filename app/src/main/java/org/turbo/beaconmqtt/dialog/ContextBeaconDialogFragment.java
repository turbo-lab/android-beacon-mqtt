package org.turbo.beaconmqtt.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beacon.TransactionBeacon;

import java.util.Objects;

public class ContextBeaconDialogFragment extends DialogFragment {
    @SuppressWarnings("unused")
    final protected String TAG = ContextBeaconDialogFragment.class.getName();

    public static ContextBeaconDialogFragment newInstance(String beaconId) {
        ContextBeaconDialogFragment fragment = new ContextBeaconDialogFragment();
        Bundle args = new Bundle();
        args.putString("beaconId", beaconId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String beaconId = Objects.requireNonNull(getArguments()).getString("beaconId");
        final String[] catNamesArray = {getString(R.string.edit_beacon), getString(R.string.delete_beacon)};

        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));

        builder.setItems(catNamesArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (catNamesArray[which].equals(getString(R.string.edit_beacon))) {
                    editBeacon(beaconId);
                } else if (catNamesArray[which].equals(getString(R.string.delete_beacon))) {
                    deleteBeacon(beaconId);
                }
            }
        });

        return builder.create();
    }

    private void deleteBeacon(final String beaconId) {
        DeleteBeaconDialogFragment newFragment = DeleteBeaconDialogFragment
                .newInstance(beaconId);
        newFragment.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
                "DeleteBeaconDialog");
    }

    private void editBeacon(final String beaconId) {
        BeaconApplication application = (BeaconApplication) Objects.requireNonNull(getActivity()).getApplication();

        TransactionBeacon transactionBeacon = new
                TransactionBeacon(application.getBeaconFactory().getBeaconById(beaconId));
        application.getBeaconFactory().setTransactionBeacon(transactionBeacon);

        BaseBeaconDialogFragment newFragment = BaseBeaconDialogFragment
                .newInstance(beaconId);
        newFragment.show(getActivity().getSupportFragmentManager(),
                "EditBeaconDialog");
    }
}
