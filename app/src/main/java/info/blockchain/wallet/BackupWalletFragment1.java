package info.blockchain.wallet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrefsUtil;

public class BackupWalletFragment1 extends Fragment {

	TextView tvBackupWallet;

	ImageView ivAlert;
	TextView tvHeader;
	TextView tvSubHeader;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_backup_wallet_1, container, false);

		tvHeader = (TextView)rootView.findViewById(R.id.backup_header);
		tvSubHeader = (TextView)rootView.findViewById(R.id.backup_subheader);
		ivAlert = (ImageView)rootView.findViewById(R.id.ic_alert);

		tvBackupWallet = (TextView)rootView.findViewById(R.id.backup_wallet_action);
		tvBackupWallet.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				// Wallet is double encrypted AND User has not already entered double-encryption password
				if(PayloadFactory.getInstance().get().isDoubleEncrypted() && !DoubleEncryptionFactory.getInstance().isActivated()){
					final EditText double_encrypt_password = new EditText(getActivity());
					double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.app_name)
							.setMessage(R.string.enter_double_encryption_pw)
							.setView(double_encrypt_password)
							.setCancelable(false)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(double_encrypt_password.getText().toString()));

									getFragmentManager().beginTransaction()
											.replace(R.id.content_frame, new BackupWalletFragment2())
											.addToBackStack(null)
											.commit();
								}
							}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							;
						}
					}).show();
				}
				else{
					getFragmentManager().beginTransaction()
							.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
							.replace(R.id.content_frame, new BackupWalletFragment2())
							.addToBackStack(null)
							.commit();
				}
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

		int lastBackup  = PrefsUtil.getInstance(getActivity()).getValue(BackupWalletActivity.BACKUP_DATE_KEY, 0);
		if(lastBackup!=0){

			String daysAgo = (Math.round(((System.currentTimeMillis()/1000) - lastBackup)/86400.0))+"";
			String day = getResources().getString(R.string.days);
			if(daysAgo.equals("1"))day = getResources().getString(R.string.day);

			String msg = getResources().getString(R.string.backup_days_ago).replace("[--time--]", daysAgo).replace("[--day--]", day);
			tvHeader.setText(msg);
			tvSubHeader.setText(getResources().getString(R.string.backup_only_need_to_once_reminder));
			ivAlert.setImageResource(R.drawable.ic_thumb_up_white_48dp);
		}
	}
}