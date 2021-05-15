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

import java.util.Objects;

public class WifiBeaconDialogFragment extends DialogFragment {
    @SuppressWarnings("unused")
    final protected String TAG = "WifiBeaconDialog";

    public static WifiBeaconDialogFragment newInstance(String beaconId) {
        WifiBeaconDialogFragment fragment = new WifiBeaconDialogFragment();
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
                .inflate(R.layout.dialog_wifi_beacon, null);
        final InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        final String beaconId = Objects.requireNonNull(getArguments()).getString("beaconId");
        final TransactionBeacon transactionBeacon = application.getBeaconFactory()
                .getTransactionBeacon();


        final MaterialEditText ssidView = dialogLayout
                .findViewById(R.id.text_beacon_ssid);

        ssidView.setAutoValidate(true);

        ssidView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateSsid(text.toString());
            }
        });

        ssidView.setText(transactionBeacon.getSsid());

        builder.setView(dialogLayout)
                .setTitle(R.string.advanced_parameters)
                .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        transactionBeacon.setSsid(Objects.requireNonNull(ssidView.getText()).toString());

                        BaseBeaconDialogFragment newFragment = BaseBeaconDialogFragment
                                .newInstance(beaconId);
                        newFragment.show(Objects.requireNonNull(getFragmentManager()),
                                "BaseBeaconDialog");
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        ssidView.postDelayed(new Runnable() {
            @Override
            public void run() {
                ssidView.requestFocus();
                ssidView.setSelection(Objects.requireNonNull(ssidView.getText()).length());
                imm.showSoftInput(ssidView, 0);
            }
        }, 100);

        return dialog;
    }
}
