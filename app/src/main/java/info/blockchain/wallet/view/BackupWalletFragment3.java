package info.blockchain.wallet.view;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.BackupWalletUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupWallet3Binding;

public class BackupWalletFragment3 extends Fragment {

    private String[] mnemonicRequestHint = null;
    private FragmentBackupWallet3Binding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_wallet_3, container, false);

        final List<Pair<Integer, String>> confirmSequence = new BackupWalletUtil(getActivity()).getConfirmSequence();
        mnemonicRequestHint = getResources().getStringArray(R.array.mnemonic_word_requests);

        binding.etFirstRequest.setHint(mnemonicRequestHint[confirmSequence.get(0).first]);
        binding.etSecondRequest.setHint(mnemonicRequestHint[confirmSequence.get(1).first]);
        binding.etThirdRequest.setHint(mnemonicRequestHint[confirmSequence.get(2).first]);

        binding.verifyAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (binding.etFirstRequest.getText().toString().trim().equals(confirmSequence.get(0).second)
                        && binding.etSecondRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(1).second)
                        && binding.etThirdRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(2).second)) {

                    PayloadManager.getInstance().getPayload().getHdWallet().mnemonic_verified(true);
                    new PrefsUtil(getActivity()).setValue(BackupWalletActivity.BACKUP_DATE_KEY, (int) (System.currentTimeMillis() / 1000));
                    PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                        @Override
                        public void onSaveSuccess() {
                            ToastCustom.makeText(getActivity(), getString(R.string.backup_confirmed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                            getActivity().setResult(Activity.RESULT_OK);
                            getFragmentManager().popBackStack();
                            getFragmentManager().popBackStack();
                        }

                        @Override
                        public void onSaveFail() {
                            ToastCustom.makeText(getActivity(), getActivity().getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            getActivity().setResult(Activity.RESULT_CANCELED);
                            getFragmentManager().popBackStack();
                            getFragmentManager().popBackStack();
                        }
                    });

                } else
                    ToastCustom.makeText(getActivity(), getString(R.string.backup_word_mismatch), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

            }
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_general);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}