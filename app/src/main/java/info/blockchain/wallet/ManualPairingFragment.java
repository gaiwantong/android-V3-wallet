package info.blockchain.wallet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;

/**
 * Created by riaanvos on 23/03/15.
 */
public class ManualPairingFragment extends Fragment {

	EditText edGuid;
	EditText edPassword;
	TextView next;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_manual_pairing, container, false);

		getActivity().setTitle(getResources().getString(R.string.manual_pairing));

		edGuid = (EditText)rootView.findViewById(R.id.wallet_id);
		edPassword = (EditText)rootView.findViewById(R.id.wallet_pass);
		next = (TextView)rootView.findViewById(R.id.command_next);

		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String guid = edGuid.getText().toString();
				final String pw = edPassword.getText().toString();

				if(guid == null || guid.length() == 0) {
					Toast.makeText(getActivity(), getString(R.string.invalid_guid), Toast.LENGTH_SHORT).show();
				}
				else if(pw == null || pw.length() == 0) {
					Toast.makeText(getActivity(), getString(R.string.invalid_password), Toast.LENGTH_SHORT).show();
				}
				else {
					pairingThreadManual(guid, new CharSequenceX(pw));
				}
			}
		});

		Typeface typeface = TypefaceUtil.getInstance(getActivity()).getRobotoTypeface();
		edGuid.setTypeface(typeface);
		edPassword.setTypeface(typeface);

		return rootView;
	}

	private void pairingThreadManual(final String guid, final CharSequenceX password) {

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();

				try {
					String response = PairingFactory.getInstance(getActivity()).getWalletManualPairing(guid);
					JSONObject jsonObj = new JSONObject(response);

					if(jsonObj != null && jsonObj.has("payload")) {
						String encrypted_payload = (String)jsonObj.getString("payload");

						String decrypted_payload = null;
						try {
							decrypted_payload = AESUtil.decrypt(encrypted_payload, password, AESUtil.PasswordPBKDF2Iterations);
						}
						catch(Exception e) {
							e.printStackTrace();
							Toast.makeText(getActivity(), "Pairing KO", Toast.LENGTH_SHORT).show();
							AppUtil.getInstance(getActivity()).wipeApp();
						}

						if(decrypted_payload != null) {
							JSONObject payloadObj = new JSONObject(decrypted_payload);
							if(payloadObj != null && payloadObj.has("sharedKey")) {
								PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.GUID, guid);
								PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.SHARED_KEY, (String)payloadObj.get("sharedKey"));

								if(HDPayloadBridge.getInstance(getActivity()).init(password)) {
									PayloadFactory.getInstance(getActivity()).setTempPassword(password);
									Toast.makeText(getActivity(), "Pairing OK", Toast.LENGTH_SHORT).show();
									Intent intent = new Intent(getActivity(), Setup2Activity.class);
									intent.putExtra("pairing", true);
									getActivity().startActivity(intent);
									getActivity().finish();
								}
								else {
									Toast.makeText(getActivity(), "Pairing KO", Toast.LENGTH_SHORT).show();
									AppUtil.getInstance(getActivity()).wipeApp();
								}

							}

						}
						else {
							Toast.makeText(getActivity(), "Pairing KO", Toast.LENGTH_SHORT).show();
							AppUtil.getInstance(getActivity()).wipeApp();
						}

					}

				}
				catch(JSONException je) {
					je.printStackTrace();
					Toast.makeText(getActivity(), "Pairing KO", Toast.LENGTH_SHORT).show();
					AppUtil.getInstance(getActivity()).wipeApp();
				}
				catch(Exception e) {
					e.printStackTrace();
					Toast.makeText(getActivity(), "Pairing KO", Toast.LENGTH_SHORT).show();
					AppUtil.getInstance(getActivity()).wipeApp();
				}

				handler.post(new Runnable() {
					@Override
					public void run() {
						;
					}
				});

				Looper.loop();
			}
		}).start();
	}

	@Override
	public void onResume() {
		super.onResume();

		//Select first edittext field and popup softkeyboard
		edGuid.requestFocus();
		InputMethodManager mgr = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.showSoftInput(edGuid, InputMethodManager.SHOW_IMPLICIT);
	}
}
