package org.turbo.beaconmqtt.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.METValidator;

import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beacon.Helper;
import org.turbo.beaconmqtt.beacon.TransactionBeacon;

import java.util.Locale;
import java.util.Objects;

public class IBeaconDialogFragment extends DialogFragment {
    @SuppressWarnings("unused")
    final protected String TAG = "IBeaconDialog";

    public static IBeaconDialogFragment newInstance(String beaconId) {
        IBeaconDialogFragment fragment = new IBeaconDialogFragment();
        Bundle args = new Bundle();
        if (beaconId != null) {
            args.putString("beaconId", beaconId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        final BeaconApplication application = ((BeaconApplication) Objects.requireNonNull(getActivity())
                .getApplicationContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        @SuppressLint("InflateParams") final View dialogLayout = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_i_beacon, null);
        final InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        final String beaconId = Objects.requireNonNull(getArguments()).getString("beaconId");
        final TransactionBeacon transactionBeacon = application.getBeaconFactory()
                .getTransactionBeacon();

        final MaterialEditText uuidView = dialogLayout
                .findViewById(R.id.text_beacon_uuid);
        final MaterialEditText majorView = dialogLayout
                .findViewById(R.id.text_beacon_major);
        final MaterialEditText minorView = dialogLayout
                .findViewById(R.id.text_beacon_minor);
        final MaterialEditText macView = dialogLayout
                .findViewById(R.id.text_beacon_mac);

        uuidView.setAutoValidate(true);
        majorView.setAutoValidate(true);
        minorView.setAutoValidate(true);
        macView.setAutoValidate(true);

        uuidView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateUuid(text.toString());
            }
        });
        majorView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateInt(text.toString(), 1, 65535);
            }
        });
        minorView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateInt(text.toString(), 1, 65535);
            }
        });
        macView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                if (isEmpty) {
                    return true;
                } else {
                    return Helper.validateMacAddress(text.toString());
                }
            }
        });

        uuidView.setText(transactionBeacon.getUuid());
        majorView.setText(transactionBeacon.getMajor());
        minorView.setText(transactionBeacon.getMinor());
        macView.setText(transactionBeacon.getMacAddress());

        builder.setView(dialogLayout)
                .setTitle(R.string.advanced_parameters)
                .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        transactionBeacon.setUuid(Objects.requireNonNull(uuidView.getText())
                                .toString().toLowerCase(Locale.getDefault()));
                        transactionBeacon.setMajor(Objects.requireNonNull(majorView.getText()).toString());
                        transactionBeacon.setMinor(Objects.requireNonNull(minorView.getText()).toString());
                        transactionBeacon.setMacAddress(Objects.requireNonNull(macView.getText())
                                .toString().toUpperCase(Locale.getDefault()));

                        BaseBeaconDialogFragment newFragment = BaseBeaconDialogFragment
                                .newInstance(beaconId);
                        newFragment.show(Objects.requireNonNull(getFragmentManager()),
                                "BaseBeaconDialog");
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        uuidView.postDelayed(new Runnable() {
            @Override
            public void run() {
                uuidView.requestFocus();
                uuidView.setSelection(Objects.requireNonNull(uuidView.getText()).length());
                imm.showSoftInput(uuidView, 0);
            }
        }, 100);

        return dialog;
    }
}
