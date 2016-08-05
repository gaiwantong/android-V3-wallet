package info.blockchain.wallet.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityPinEntryBinding;

public class PinEntryActivity extends BaseAuthActivity {

    final int PIN_LENGTH = 4;
    final int maxAttempts = 4;
    String userEnteredPIN = "";
    String userEnteredPINConfirm = null;

    TextView[] pinBoxArray = null;
    boolean allowExit = true;
    int exitClickCount = 0;
    int exitClickCooldown = 2; // in seconds
    private ProgressDialog progress = null;
    private String strEmail = null;
    private String strPassword = null;
    private PrefsUtil prefs;
    private AppUtil appUtil;
    private PayloadManager payloadManager;

    private ActivityPinEntryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_pin_entry);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        prefs = new PrefsUtil(this);
        appUtil = new AppUtil(this);
        payloadManager = PayloadManager.getInstance();

        //Coming from CreateWalletFragment
        getBundleData();
        if (strPassword != null && strEmail != null) {
            allowExit = false;
            saveLoginAndPassword();
            createWallet();
        }

        // Set title state
        if (isCreatingNewPin()) {
            binding.titleBox.setText(R.string.create_pin);
        } else {
            binding.titleBox.setText(R.string.pin_entry);
        }

        pinBoxArray = new TextView[PIN_LENGTH];
        pinBoxArray[0] = binding.pinBox0;
        pinBoxArray[1] = binding.pinBox1;
        pinBoxArray[2] = binding.pinBox2;
        pinBoxArray[3] = binding.pinBox3;

        if (!ConnectivityStatus.hasConnectivity(this)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);

            final String message = getString(R.string.check_connectivity_exit);

            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });

            builder.create().show();
        }

        int fails = prefs.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        if (fails >= maxAttempts) {
            ToastCustom.makeText(getApplicationContext(), getString(R.string.pin_4_strikes), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

            payloadManager.getPayload().stepNumber = 0;

            new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.password_or_wipe)
                    .setCancelable(false)
                    .setPositiveButton(R.string.use_password, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            validationDialog();

                        }
                    }).setNegativeButton(R.string.wipe_wallet, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    appUtil.clearCredentialsAndRestart();

                }
            }).show();
        }
    }

    private boolean isCreatingNewPin() {
        return prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        //Test for screen overlays before user enters PIN
        if(appUtil.detectObscuredWindow(event)){
            return true;//consume event
        }else{
            return super.dispatchTouchEvent(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        userEnteredPIN = "";
        clearPinBoxes();
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    public void onBackPressed() {
        if (allowExit) {
            exitClickCount++;
            if (exitClickCount == 2) {
                AccessState.getInstance().logout(this);
            } else
                ToastCustom.makeText(this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j <= exitClickCooldown; j++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (j >= exitClickCooldown) exitClickCount = 0;
                    }
                }
            }).start();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void saveLoginAndPassword() {
        prefs.setValue(PrefsUtil.KEY_EMAIL, strEmail);
        payloadManager.setEmail(strEmail);
        payloadManager.setTempPassword(new CharSequenceX(strPassword));
    }

    private void createWallet() {
        dismissProgressView();

        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getText(R.string.create_wallet) + "...");
        if(!isFinishing())progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    // New wallet
                    appUtil.setNewlyCreated(true);

                    Payload payload = payloadManager.createHDWallet(strPassword, getString(R.string.default_wallet_name));
                    if (payload != null) {
                        //Successfully created and saved
                        prefs.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                        appUtil.setSharedKey(payload.getSharedKey());
                    } else {
                        ToastCustom.makeText(getApplicationContext(), getApplicationContext().getString(R.string.remote_save_ko),
                                ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }

                }catch(Exception e) {
                    ToastCustom.makeText(getApplicationContext(), getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    appUtil.clearCredentialsAndRestart();
                } finally {
                    dismissProgressView();
                }

                Looper.loop();
            }
        }).start();
    }

    private void getBundleData() {
        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("_email")) {
            strEmail = extras.getString("_email");
        }

        if (extras != null && extras.containsKey("_pw")) {
            strPassword = extras.getString("_pw");
        }
    }

    private void updatePayloadThread(final CharSequenceX pw) {
        final Handler handler = new Handler();

        dismissProgressView();

        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.decrypting_wallet));
        if(!isFinishing())progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    payloadManager.initiatePayload(
                            prefs.getValue(PrefsUtil.KEY_SHARED_KEY, "")
                            , prefs.getValue(PrefsUtil.KEY_GUID, ""),
                            pw,
                            new PayloadManager.InitiatePayloadListener() {
                                @Override
                                public void onInitSuccess() {
                                    payloadManager.setTempPassword(pw);
                                    appUtil.setSharedKey(payloadManager.getPayload().getSharedKey());

                                    double walletVersion = payloadManager.getVersion();

                                    if(walletVersion > PayloadManager.SUPPORTED_ENCRYPTION_VERSION){

                                        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                                                .setTitle(R.string.warning)
                                                .setMessage(String.format(getString(R.string.unsupported_encryption_version), walletVersion))
                                                .setCancelable(false)
                                                .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        AccessState.getInstance().logout(getActivity());
                                                    }
                                                })
                                                .setNegativeButton(R.string.logout, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        appUtil.clearCredentialsAndRestart();
                                                        appUtil.restartApp();
                                                    }
                                                }).show();
                                    }else {

                                        if (appUtil.isNewlyCreated() && payloadManager.getPayload().getHdWallet() != null &&
                                                (payloadManager.getPayload().getHdWallet().getAccounts().get(0).getLabel() == null ||
                                                        payloadManager.getPayload().getHdWallet().getAccounts().get(0).getLabel().isEmpty()))
                                            payloadManager.getPayload().getHdWallet().getAccounts().get(0).setLabel(getResources().getString(R.string.default_wallet_name));

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                dismissProgressView();

                                                try {
                                                    int previousVersionCode = prefs.getValue(PrefsUtil.KEY_CURRENT_APP_VERSION, 0);
                                                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

                                                    //If upgrade detected - reset last reminder so we can warn user again + set new app version in prefs
                                                    if (previousVersionCode != packageInfo.versionCode) {
                                                        prefs.setValue(PrefsUtil.KEY_HD_UPGRADE_LAST_REMINDER, 0L);
                                                        prefs.setValue(PrefsUtil.KEY_CURRENT_APP_VERSION, packageInfo.versionCode);
                                                    }

                                                } catch (PackageManager.NameNotFoundException e) {
                                                    e.printStackTrace();
                                                }

                                                if (prefs.getValue(PrefsUtil.KEY_HD_UPGRADE_LAST_REMINDER, 0L) == 0L && !payloadManager.getPayload().isUpgraded()) {
                                                    Intent intent = new Intent(PinEntryActivity.this, UpgradeWalletActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    startActivity(intent);
                                                } else {
                                                    appUtil.restartApp("verified", true);
                                                }
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onInitPairFail() {
                                    dismissProgressView();
                                    appUtil.clearCredentialsAndRestart();
                                }

                                @Override
                                public void onInitCreateFail(String s) {
                                    dismissProgressView();
                                    appUtil.clearCredentialsAndRestart();
                                }
                            });

                    Looper.loop();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    dismissProgressView();
                }
            }
        }).start();
    }

    private Context getActivity() {
        return this;
    }

    private void createNewPinThread(String pin) {
        final Handler handler = new Handler();
        dismissProgressView();
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.creating_pin));
        if(!isFinishing())progress.show();

        new Thread(() -> {
            Looper.prepare();

            if (AccessState.getInstance().createPIN(payloadManager.getTempPassword(), pin)) {
                prefs.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                updatePayloadThread(payloadManager.getTempPassword());
            } else {
                ToastCustom.makeText(PinEntryActivity.this, getString(R.string.create_pin_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                prefs.clear();
                appUtil.restartApp();
            }

            dismissProgressView();
            handler.post(() -> {
                // No-op
            });

            Looper.loop();
        }).start();
    }

    private void validatePIN(final String PIN) {
        validatePINThread(PIN);
    }

    private void validatePINThread(final String pin) {
        final Handler handler = new Handler();

        dismissProgressView();
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.validating_pin));
        if(!isFinishing())progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                CharSequenceX password;

                try {
                    password = AccessState.getInstance().validatePIN(pin);
                } catch (Exception e) {
                    dismissProgressView();

                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.unexpected_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return;
                }

                if (password != null) {
                    dismissProgressView();

                    prefs.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    updatePayloadThread(password);
                } else {
                    dismissProgressView();

                    incrementFailureCount();
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

    private void validationDialog() {
        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(PinEntryActivity.this.getString(R.string.password_entry))
                .setView(password)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        appUtil.restartApp();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (pw.length() > 0) {
                            validatePasswordThread(new CharSequenceX(pw));
                        } else {
                            incrementFailureCount();
                        }

                    }
                }).show();
    }

    private void validatePasswordThread(final CharSequenceX pw) {
        dismissProgressView();
        progress = new ProgressDialog(PinEntryActivity.this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.validating_password));
        if(!isFinishing())progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    payloadManager.setTempPassword(new CharSequenceX(""));
                    payloadManager.initiatePayload(
                            prefs.getValue(PrefsUtil.KEY_SHARED_KEY, "")
                            , prefs.getValue(PrefsUtil.KEY_GUID, ""), pw, new PayloadManager.InitiatePayloadListener() {
                                @Override
                                public void onInitSuccess() {
                                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.pin_4_strikes_password_accepted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                                    payloadManager.setTempPassword(pw);
                                    prefs.removeValue(PrefsUtil.KEY_PIN_FAILS);
                                    prefs.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }

                                @Override
                                public void onInitPairFail() {
                                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    dismissProgressView();
                                    validationDialog();
                                }

                                @Override
                                public void onInitCreateFail(String s) {
                                    ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    dismissProgressView();
                                    validationDialog();
                                }
                            });

                    Looper.loop();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }
            }
        }).start();
    }

    public void padClicked(View view) {
        if (userEnteredPIN.length() == PIN_LENGTH) {
            return;
        }

        // Append tapped #
        userEnteredPIN = userEnteredPIN + view.getTag().toString().substring(0, 1);
        pinBoxArray[userEnteredPIN.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

        // Perform appropriate action if PIN_LENGTH has been reached
        if (userEnteredPIN.length() == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (userEnteredPIN.equals("0000")) {
                ToastCustom.makeText(PinEntryActivity.this, getString(R.string.zero_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                new Handler().postDelayed(() -> clearPinViewAndReset(), 200);
                return;
            }

            // Only show warning on first entry and if user is creating a new PIN
            if (isCreatingNewPin() && isPinCommon(userEnteredPIN) && userEnteredPINConfirm == null) {
                showCommonPinWarning(new PinWarningCallback() {
                    @Override
                    public void tryAgainClicked() {
                        clearPinViewAndReset();
                    }

                    @Override
                    public void continueClicked() {
                        validateAndConfirmPin();
                    }
                });
            } else {
                validateAndConfirmPin();
            }
        }
    }

    private void clearPinViewAndReset() {
        clearPinBoxes();
        userEnteredPIN = "";
        userEnteredPINConfirm = null;
    }

    private void validateAndConfirmPin() {
        // Validate
        if (prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() >= 1) {
            binding.titleBox.setVisibility(View.INVISIBLE);
            validatePIN(userEnteredPIN);
        } else if (userEnteredPINConfirm == null) {
            // End of Create -  Change to Confirm
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    PinEntryActivity.this.runOnUiThread(() -> {
                        binding.titleBox.setText(R.string.confirm_pin);
                        clearPinBoxes();
                        userEnteredPINConfirm = userEnteredPIN;
                        userEnteredPIN = "";
                    });
                }
            }, 200);

        } else if (userEnteredPINConfirm.equals(userEnteredPIN)) {
            // End of Confirm - Pin is confirmed
            createNewPinThread(userEnteredPIN); // Pin is confirmed. Save to server.

        } else {
            // End of Confirm - Pin Mismatch
            ToastCustom.makeText(PinEntryActivity.this, getString(R.string.pin_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            new Handler().postDelayed(() -> {
                clearPinViewAndReset();
                binding.titleBox.setText(R.string.create_pin);
            }, 200);
        }
    }

    public void deleteClicked(View view) {

        if(userEnteredPIN.length() > 0) {
            //Remove last char from pin string
            userEnteredPIN = userEnteredPIN.substring(0, userEnteredPIN.length() - 1);

            //Clear last box
            pinBoxArray[userEnteredPIN.length()].setBackgroundResource(R.drawable.rounded_view_blue_white_border);
        }
    }

    private void clearPinBoxes() {
        if (userEnteredPIN.length() > 0) {
            for (TextView pinBox : pinBoxArray) {
                // Reset PIN buttons to blank
                pinBox.setBackgroundResource(R.drawable.rounded_view_blue_white_border);
            }
        }
    }

    private void incrementFailureCount() {
        int fails = prefs.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        prefs.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);

        ToastCustom.makeText(PinEntryActivity.this, getString(R.string.invalid_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean isPinCommon(String pin) {
        List<String> commonPins = new ArrayList<String>() {{
            add("1234");
            add("1111");
            add("1212");
            add("7777");
            add("1004");
        }};
        return commonPins.contains(pin);
    }

    private void showCommonPinWarning(PinWarningCallback callback) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.common_pin_dialog_title)
                .setMessage(R.string.common_pin_dialog_message)
                .setPositiveButton(R.string.common_pin_dialog_try_again, (dialogInterface, i) -> callback.tryAgainClicked())
                .setNegativeButton(R.string.common_pin_dialog_continue, (dialogInterface, i) -> callback.continueClicked())
                .setCancelable(false)
                .create();

        dialog.show();
    }

    private interface PinWarningCallback {
        void tryAgainClicked();
        void continueClicked();
    }

    @Override
    protected void onPause() {
        dismissProgressView();
        super.onPause();
    }

    private void dismissProgressView() {
        if (progress != null) {
            progress.dismiss();
            progress = null;
        }
    }
}