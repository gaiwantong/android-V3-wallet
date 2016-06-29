package info.blockchain.wallet.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrefsUtil;

import piuk.blockchain.android.R;

public class BackupWalletFragment1 extends Fragment {

    TextView tvBackupWallet;

    ImageView ivAlert;
    TextView tvHeader;
    TextView tvSubHeader;
    TextView tvLostMnemonic;
    boolean toggle = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_backup_wallet_1, container, false);

        tvHeader = (TextView) rootView.findViewById(R.id.backup_header);
        tvSubHeader = (TextView) rootView.findViewById(R.id.backup_subheader);
        tvLostMnemonic  = (TextView) rootView.findViewById(R.id.backup_lost_mnemonic);
        ivAlert = (ImageView) rootView.findViewById(R.id.ic_alert);

        tvBackupWallet = (TextView) rootView.findViewById(R.id.backup_wallet_action);
        tvBackupWallet.setOnClickListener(v -> {

            // Wallet is double encrypted
            if (PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                final EditText double_encrypt_password = new EditText(getActivity());
                double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.enter_double_encryption_pw)
                        .setView(double_encrypt_password)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, (dialog, whichButton) -> {

                            final String pw = double_encrypt_password.getText().toString();

                            if (DoubleEncryptionFactory.getInstance().validateSecondPassword(
                                    PayloadFactory.getInstance().get().getDoublePasswordHash(),
                                    PayloadFactory.getInstance().get().getSharedKey(),
                                    new CharSequenceX(pw),
                                    PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations())) {

                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));

                                getFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, new BackupWalletFragment2())
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                            }
                        }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
                            ;
                        }).show();
            } else {
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.content_frame, new BackupWalletFragment2())
                        .addToBackStack(null)
                        .commit();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_general);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        int lastBackup = new PrefsUtil(getActivity()).getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0);
        if (lastBackup != 0) {

            String daysAgo = (Math.round(((System.currentTimeMillis() / 1000) - lastBackup) / 86400.0)) + "";
            String day = getResources().getString(R.string.days);
            if (daysAgo.equals("1")) day = getResources().getString(R.string.day);

            String msg = String.format(getResources().getString(R.string.backup_days_ago), daysAgo, day);
            tvHeader.setText(msg);
            tvSubHeader.setText(getResources().getString(R.string.backup_only_need_to_once_reminder));
            ivAlert.setImageResource(R.drawable.ic_thumb_up_white_48dp);
            ivAlert.setColorFilter(getActivity().getResources().getColor(R.color.blockchain_blue), PorterDuff.Mode.SRC_ATOP);
            tvBackupWallet.setText(getString(R.string.backup_funds_again));
            tvLostMnemonic.setVisibility(View.VISIBLE);
        }else{
            tvBackupWallet.setText(getString(R.string.backup_funds));
            tvLostMnemonic.setVisibility(View.GONE);
        }
    }
}