package info.blockchain.wallet.ui;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import info.blockchain.api.Access;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.json.JSONException;
import org.json.JSONObject;

import piuk.blockchain.android.R;

public class ManualPairingFragment extends Fragment {

    private EditText edGuid = null;
    private EditText edPassword = null;
    private TextView next = null;

    private ProgressDialog progress = null;

    private boolean waitinForAuth = true;
    private int timer = 0;

    private AppUtil appUtil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_pairing, container, false);

        getActivity().setTitle(getResources().getString(R.string.manual_pairing));

        appUtil = new AppUtil(getActivity());

        edGuid = (EditText) rootView.findViewById(R.id.wallet_id);
        edPassword = (EditText) rootView.findViewById(R.id.wallet_pass);
        next = (TextView) rootView.findViewById(R.id.command_next);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String guid = edGuid.getText().toString();
                final String pw = edPassword.getText().toString();

                if (guid == null || guid.length() == 0) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_guid), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else if (pw == null || pw.length() == 0) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else {
                    pairingThreadManual(guid, new CharSequenceX(pw));
                }
            }
        });

        return rootView;
    }

    @Override
    public void onDestroy() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);

        super.onDestroy();
    }

    // TODO there should only be one place to download and decrypt a wallet
    private void pairingThreadManual(final String guid, final CharSequenceX password) {

        waitinForAuth = true;

        final Handler handler = new Handler();

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(getActivity());
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getActivity().getString(R.string.pairing_wallet));
        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                waitinForAuth = false;
            }
        });
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    Access access = new Access();
                    String sessionId = access.getSessionId(guid);
                    String response = access.getEncryptedPayload(guid, sessionId);

                    if (response.equals(Access.KEY_AUTH_REQUIRED)) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progress.setCancelable(true);
                                progress.setMessage(getResources().getString(R.string.check_email_to_auth_login));
                            }
                        });
                        response = waitForAuthThread(guid, password);
                    }

                    if (response != null && response.equals("Authorization Required")) {
                        ToastCustom.makeText(getActivity(), getString(R.string.auth_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        appUtil.clearCredentialsAndRestart();
                    }

                    JSONObject jsonObj = new JSONObject(response);

                    if (jsonObj != null && jsonObj.has("payload")) {
                        String encrypted_payload = (String) jsonObj.getString("payload");

                        int iterations = PayloadFactory.WalletPbkdf2Iterations;
                        if (jsonObj.has("pbkdf2_iterations")) {
                            iterations = Integer.valueOf(jsonObj.get("pbkdf2_iterations").toString()).intValue();
                        }

                        String decrypted_payload = null;
                        try {
                            decrypted_payload = AESUtil.decrypt(encrypted_payload, password, iterations);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed_decrypt_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            appUtil.clearCredentialsAndRestart();
                        }

                        if (decrypted_payload != null) {
                            JSONObject payloadObj = new JSONObject(decrypted_payload);
                            PrefsUtil prefs = new PrefsUtil(getActivity());

                            if (payloadObj != null && payloadObj.has("sharedKey")) {
                                prefs.setValue(PrefsUtil.KEY_GUID, guid);
                                PayloadFactory.getInstance().setTempPassword(password);

                                String sharedKey = (String) payloadObj.get("sharedKey");
                                appUtil.setSharedKey(sharedKey);

                                PayloadFactory.getInstance().initiatePayload(sharedKey, guid, password, new PayloadFactory.InitiatePayloadListener() {
                                    @Override
                                    public void onInitSuccess() {
                                        prefs.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                                        PayloadFactory.getInstance().setTempPassword(password);
                                        Intent intent = new Intent(getActivity(), PinEntryActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        getActivity().startActivity(intent);
                                    }

                                    @Override
                                    public void onInitPairFail() {
                                        ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }

                                    @Override
                                    public void onInitCreateFail(String s) {
                                        ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }
                                });
                            }

                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    edPassword.setText("");
                                }
                            });
                            ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }

                    }

                } catch (JSONException je) {
                    je.printStackTrace();
                    if (getActivity() != null) {
                        ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (getActivity() != null) {
                        ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                } finally {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
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

    private String waitForAuthThread(final String guid, final CharSequenceX password) {

        final int waitTime = 2;//minutes to wait for auth
        timer = (waitTime * 60);

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (waitinForAuth) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            progress.setMessage(getResources().getString(R.string.check_email_to_auth_login) + " " + timer);
                            timer--;
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (timer <= 0) {
                        ToastCustom.makeText(getActivity(), getString(R.string.pairing_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        waitinForAuth = false;
                        progress.cancel();
                        appUtil.clearCredentialsAndRestart();
                    }
                }
            }
        }).start();

        int sleep = 5;//second
        Access access = new Access();
        String sessionId = null;
        try {
            sessionId = access.getSessionId(guid);

            while (waitinForAuth) {

                try {
                    Thread.sleep(1000 * sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sleep = 1;//after initial sleep, poll every 1 second

                String response = null;
                try {
                    response = access.getEncryptedPayload(guid, sessionId);
                    if (!response.equals(Access.KEY_AUTH_REQUIRED)) {
                        waitinForAuth = false;
                        return response;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            waitinForAuth = false;
            return Access.KEY_AUTH_REQUIRED;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
