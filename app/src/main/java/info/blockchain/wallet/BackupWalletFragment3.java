package info.blockchain.wallet;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.BackupWalletUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;

public class BackupWalletFragment3 extends Fragment {

    private TextView tvVerify = null;
    private EditText etFirstRequest = null;
    private EditText etSecondRequest = null;
    private EditText etThirdRequest = null;

    private String[] mnemonicRequestHint = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_backup_wallet_3, container, false);

        final List<Pair<Integer, String>> confirmSequence = BackupWalletUtil.getInstance(getActivity()).getConfirmSequence();
        mnemonicRequestHint = getResources().getStringArray(R.array.mnemonic_word_requests);

        etFirstRequest = (EditText)rootView.findViewById(R.id.etFirstRequest);
        etSecondRequest = (EditText)rootView.findViewById(R.id.etSecondRequest);
        etThirdRequest = (EditText)rootView.findViewById(R.id.etThirdRequest);

        etFirstRequest.setHint(mnemonicRequestHint[confirmSequence.get(0).first]);
        etSecondRequest.setHint(mnemonicRequestHint[confirmSequence.get(1).first]);
        etThirdRequest.setHint(mnemonicRequestHint[confirmSequence.get(2).first]);

        tvVerify = (TextView)rootView.findViewById(R.id.verify_action);
        tvVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(etFirstRequest.getText().toString().trim().equals(confirmSequence.get(0).second)
                        && etSecondRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(1).second)
                        && etThirdRequest.getText().toString().trim().equalsIgnoreCase(confirmSequence.get(2).second)) {

                    ToastCustom.makeText(getActivity(), getString(R.string.backup_confirmed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                    PrefsUtil.getInstance(getActivity()).setValue(BackupWalletActivity.BACKUP_DATE_KEY, (int)(System.currentTimeMillis()/1000));

                    getActivity().setResult(Activity.RESULT_OK);
                    getFragmentManager().popBackStack();
                    getFragmentManager().popBackStack();

                }else
                    ToastCustom.makeText(getActivity(), getString(R.string.backup_word_mismatch), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(getActivity()).updatePinEntryTime();

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_general);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }
}