package info.blockchain.wallet;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BackupWalletFragment1 extends Fragment {

	TextView tvBackupWallet;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_backup_wallet_1, container, false);

		tvBackupWallet = (TextView)rootView.findViewById(R.id.backup_wallet_action);
		tvBackupWallet.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onBackPressed();
			}
		});
	}
}