package org.turbo.beaconmqtt.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.METValidator;

import org.turbo.beaconmqtt.BeaconApplication;
import org.turbo.beaconmqtt.R;
import org.turbo.beaconmqtt.beacon.Helper;
import org.turbo.beaconmqtt.beacon.IBeacon;
import org.turbo.beaconmqtt.beacon.TransactionBeacon;
import org.turbo.beaconmqtt.beacon.WifiBeacon;

import java.util.Objects;


public class BaseBeaconDialogFragment extends DialogFragment {
    @SuppressWarnings("unused")
    final protected String TAG = "BaseBeaconDialog";

    private AlertDialog dialog;

    private MaterialEditText idView;
    private MaterialEditText groupView;
    private MaterialEditText tagView;

    public static BaseBeaconDialogFragment newInstance(String beaconId) {
        BaseBeaconDialogFragment fragment = new BaseBeaconDialogFragment();
        Bundle args = new Bundle();
        args.putString("beaconId", beaconId);
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
                .inflate(R.layout.dialog_base_beacon, null, false);
        final InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        final String beaconId = Objects.requireNonNull(getArguments()).getString("beaconId");
        final TransactionBeacon transactionBeacon = application.getBeaconFactory().getTransactionBeacon();
        final String beaconType = transactionBeacon.getType();

        idView = dialogLayout
                .findViewById(R.id.text_beacon_id);
        groupView = dialogLayout
                .findViewById(R.id.text_beacon_group);
        tagView = dialogLayout
                .findViewById(R.id.text_beacon_tag);

        idView.setAutoValidate(true);
        groupView.setAutoValidate(true);
        tagView.setAutoValidate(true);

        idView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateId(text.toString());
            }
        });
        groupView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateGroup(text.toString());
            }
        });
        tagView.addValidator(new METValidator(getString(R.string.invalid_value)) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                return Helper.validateTag(text.toString());
            }
        });

        idView.setText(transactionBeacon.getId());
        groupView.setText(transactionBeacon.getGroup());
        tagView.setText(transactionBeacon.getTag());

        builder.setView(dialogLayout)
                .setTitle(R.string.base_beacon_parameters)
                .setNeutralButton(R.string.advanced, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        transactionBeacon.setId(Objects.requireNonNull(idView.getText()).toString());
                        transactionBeacon.setGroup(Objects.requireNonNull(groupView.getText()).toString());
                        transactionBeacon.setTag(Objects.requireNonNull(tagView.getText()).toString());

                        if (IBeacon.BEACON_IBEACON.equals(beaconType)) {
                            IBeaconDialogFragment newFragment = IBeaconDialogFragment
                                    .newInstance(beaconId);
                            newFragment.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
                                    "IBeaconDialog");
                        } else if (WifiBeacon.BEACON_WIFI.equals(beaconType)) {
                            WifiBeaconDialogFragment newFragment = WifiBeaconDialogFragment
                                    .newInstance(beaconId);
                            newFragment.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(),
                                    "WifiBeaconDialog");
                        }
                    }
                })
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        transactionBeacon.setId(Objects.requireNonNull(idView.getText()).toString());
                        transactionBeacon.setGroup(Objects.requireNonNull(groupView.getText()).toString());
                        transactionBeacon.setTag(Objects.requireNonNull(tagView.getText()).toString());

                        // We wanna take (change) name, but the name is already taken
                        if (!transactionBeacon.getId().equals(beaconId) &&
                                application.getBeaconFactory()
                                        .getBeaconById(transactionBeacon.getId()) != null) {
                            final AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                            builder.setTitle(getString(R.string.error));
                            builder.setMessage(getString(R.string.beacon_is_already_exists,
                                    transactionBeacon.getId()));
                            builder.setPositiveButton(R.string.close, null);
                            builder.show();
                        } else if (IBeacon.BEACON_IBEACON.equals(beaconType)) {
                            IBeacon counterpart = application.getBeaconFactory()
                                    .getIBeaconByUMMM(transactionBeacon.getUuid(),
                                            transactionBeacon.getMajor(),
                                            transactionBeacon.getMinor(),
                                            transactionBeacon.getMacAddress());

                            // The same beacon
                            if (counterpart != null && counterpart.getId().equals(beaconId)) {
                                counterpart = null;
                            }

                            // Hmm... We have counterpart!
                            if (counterpart != null) {
                                final AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                                builder.setTitle(getString(R.string.error));
                                builder.setMessage(getString(R.string.beacon_with_the_same_parameters_already_exists,
                                        counterpart.getId()));
                                builder.setPositiveButton(R.string.close, null);
                                builder.show();
                            } else if (beaconId == null) {
                                application.getBeaconFactory().insertBeaconFromTransaction();
                                Objects.requireNonNull(getActivity()).finish();
                            } else {
                                application.getBeaconFactory().updateBeaconFromTransaction(beaconId);
                            }
                        } else if (WifiBeacon.BEACON_WIFI.equals(beaconType)) {
                            WifiBeacon counterpart = application.getBeaconFactory()
                                    .getWifiBeaconBySsid(transactionBeacon.getSsid());

                            // The same beacon
                            if (counterpart != null && counterpart.getId().equals(beaconId)) {
                                counterpart = null;
                            }

                            // Hmm... We have counterpart!
                            if (counterpart != null) {
                                final AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                                builder.setTitle(getString(R.string.error));
                                builder.setMessage(getString(R.string.beacon_with_the_same_parameters_already_exists,
                                        counterpart.getId()));
                                builder.setPositiveButton(R.string.close, null);
                                builder.show();
                            } else if (beaconId == null) {
                                application.getBeaconFactory().insertBeaconFromTransaction();
                                Objects.requireNonNull(getActivity()).finish();
                            } else {
                                application.getBeaconFactory().updateBeaconFromTransaction(beaconId);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        dialog = builder.create();
        dialog.show();

        idView.postDelayed(new Runnable() {
            @Override
            public void run() {
                idView.requestFocus();
                idView.setSelection(Objects.requireNonNull(idView.getText()).length());
                imm.showSoftInput(idView, 0);
            }
        }, 100);

        imm.showSoftInput(getView(), InputMethodManager.SHOW_IMPLICIT);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(idView.validate());

        idView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!transactionBeacon.isValid()) {
                    Button button = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                    AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
                    animation.setDuration(200);
                    animation.setRepeatMode(Animation.REVERSE);
                    animation.setRepeatCount(10);
                    button.startAnimation(animation);
                }
            }
        }, 2000);


        idView.addTextChangedListener(new TextViewListener());

        return dialog;
    }

    private class TextViewListener implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(idView.validate());
        }
    }
}
