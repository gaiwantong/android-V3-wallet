package info.blockchain.wallet.view;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupWallet1Binding;

public class BackupWalletFragment1 extends Fragment {

    private PayloadManager payloadManager;
    private FragmentBackupWallet1Binding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_wallet_1, container, false);

        binding.backupWalletAction.setOnClickListener(v -> {

            payloadManager = PayloadManager.getInstance();

            // Wallet is double encrypted
            if (payloadManager.getPayload().isDoubleEncrypted()) {
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
                                    payloadManager.getPayload().getDoublePasswordHash(),
                                    payloadManager.getPayload().getSharedKey(),
                                    new CharSequenceX(pw),
                                    payloadManager.getPayload().getDoubleEncryptionPbkdf2Iterations())) {

                                payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(pw));

                                getFragmentManager().beginTransaction()
                                        .replace(R.id.content_frame, new BackupWalletFragment2())
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));
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

        return binding.getRoot();
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
            binding.backupHeader.setText(msg);
            binding.backupSubheader.setText(getResources().getString(R.string.backup_only_need_to_once_reminder));
            binding.icAlert.setImageResource(R.drawable.ic_thumb_up_white_48dp);
            binding.icAlert.setColorFilter(getActivity().getResources().getColor(R.color.blockchain_blue), PorterDuff.Mode.SRC_ATOP);
            binding.backupWalletAction.setText(getString(R.string.backup_funds_again));
            binding.backupLostMnemonic.setVisibility(View.VISIBLE);
        }else{
            binding.backupWalletAction.setText(getString(R.string.backup_funds));
            binding.backupLostMnemonic.setVisibility(View.GONE);
        }
    }
}