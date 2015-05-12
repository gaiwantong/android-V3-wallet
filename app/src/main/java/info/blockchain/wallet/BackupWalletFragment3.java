package info.blockchain.wallet;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import info.blockchain.wallet.util.BackupWalletUtil;

public class BackupWalletFragment3 extends Fragment {

	private TextView tvVerify = null;
	private EditText etFirstRequest = null;
	private EditText etSecondRequest = null;
	private EditText etThirdRequest = null;

	private String[] mnemonicRequestHint = null;

	private String[] mnemonic = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_backup_wallet_3, container, false);

		mnemonic = BackupWalletUtil.getInstance(getActivity()).getMnemonic();
		mnemonicRequestHint = getResources().getStringArray(R.array.mnemonic_word_requests);

		etFirstRequest = (EditText)rootView.findViewById(R.id.etFirstRequest);
		etSecondRequest = (EditText)rootView.findViewById(R.id.etSecondRequest);
		etThirdRequest = (EditText)rootView.findViewById(R.id.etThirdRequest);

		Bundle args = getArguments();
		final int mnemonicIndex1 = args.getInt("random1");
		final int mnemonicIndex2 = args.getInt("random2");
		final int mnemonicIndex3 = args.getInt("random3");

		etFirstRequest.setHint(mnemonicRequestHint[mnemonicIndex1]);
		etSecondRequest.setHint(mnemonicRequestHint[mnemonicIndex2]);
		etThirdRequest.setHint(mnemonicRequestHint[mnemonicIndex3]);

		tvVerify = (TextView)rootView.findViewById(R.id.verify_action);
		tvVerify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if(etFirstRequest.getText().toString().equalsIgnoreCase(mnemonic[mnemonicIndex1] )
						&& etSecondRequest.getText().toString().equalsIgnoreCase(mnemonic[mnemonicIndex2])
						&& etThirdRequest.getText().toString().equalsIgnoreCase(mnemonic[mnemonicIndex3])) {

					getActivity().setResult(Activity.RESULT_OK);
					getActivity().finish();

				}else
					Toast.makeText(getActivity(),"Nope",Toast.LENGTH_SHORT).show();

			}
		});

		return rootView;
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
}