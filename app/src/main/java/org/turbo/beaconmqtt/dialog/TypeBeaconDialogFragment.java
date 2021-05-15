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
import org.turbo.beaconmqtt.beaconFactory.BeaconFactory;

import java.util.Objects;

public class TypeBeaconDialogFragment extends DialogFragment {
    @SuppressWarnings("unused")
    final protected String TAG = "TypeBeaconDialog";

    public static TypeBeaconDialogFragment newInstance() {
        TypeBeaconDialogFragment fragment = new TypeBeaconDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        final BeaconApplication application = ((BeaconApplication) Objects.requireNonNull(getActivity())
                .getApplicationContext());
        final TransactionBeacon transactionBeacon = application.getBeaconFactory().getTransactionBeacon();

        final String[] types = BeaconFactory.getBeaconTypes();
        transactionBeacon.setType(types[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.type_of_beacon)
                .setSingleChoiceItems(types, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        transactionBeacon.setType(types[which]);
                    }
                })
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BaseBeaconDialogFragment newFragment = BaseBeaconDialogFragment
                                .newInstance(null);
                        newFragment.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
                                "new_beacon_dialog");
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        return dialog;
    }
}
