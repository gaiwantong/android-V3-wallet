package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class PinEntryActivity extends Activity {

    String userEnteredPIN = "";
	String userEnteredPINConfirm = null;

    final int PIN_LENGTH = 4;

    TextView titleView = null;

    TextView pinBox0 = null;
    TextView pinBox1 = null;
    TextView pinBox2 = null;
    TextView pinBox3 = null;

    TextView[] pinBoxArray = null;

    private ProgressDialog progress = null;

    private String strEmail    = null;
    private String strPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_pin_entry);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().getDecorView().findViewById(android.R.id.content).setFilterTouchesWhenObscured(true);

		//Coming from CreateWalletFragment
		getBundleData();
        if (strPassword != null && strEmail != null) {
			saveLoginAndPassword();
			createWallet();
		}

        // Set title state
		Typeface typeface = TypefaceUtil.getInstance(this).getRobotoTypeface();
        titleView = (TextView)findViewById(R.id.titleBox);
		titleView.setTypeface(typeface);
		if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1) {

			titleView.setText(R.string.create_pin);
		}
		else {
			titleView.setText(R.string.pin_entry);
		}

		pinBox0 = (TextView)findViewById(R.id.pinBox0);
		pinBox1 = (TextView)findViewById(R.id.pinBox1);
		pinBox2 = (TextView)findViewById(R.id.pinBox2);
		pinBox3 = (TextView)findViewById(R.id.pinBox3);

		pinBoxArray = new TextView[PIN_LENGTH];
		pinBoxArray[0] = pinBox0;
		pinBoxArray[1] = pinBox1;
		pinBoxArray[2] = pinBox2;
		pinBoxArray[3] = pinBox3;

		if(!ConnectivityStatus.hasConnectivity(this)) {
	    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);

	        final String message = getString(R.string.check_connectivity_exit);

	        builder.setMessage(message)
	        	.setCancelable(false)
	            .setPositiveButton(R.string.dialog_continue,
	                new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface d, int id) {
	                        d.dismiss();
							Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
	                    }
	            });

	        builder.create().show();
		}

    	int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_FAILS, 0);
    	if(fails >= 3)	{
        	Toast.makeText(PinEntryActivity.this, R.string.pin_3_strikes, Toast.LENGTH_SHORT).show();
//        	validationDialog();

            new AlertDialog.Builder(PinEntryActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.password_or_wipe)
                    .setCancelable(false)
                    .setPositiveButton(R.string.use_password, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            validationDialog();

                        }
                    }).setNegativeButton(R.string.wipe_wallet, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    AppUtil.getInstance(PinEntryActivity.this).clearCredentialsAndRestart();

                }
            }).show();

        }

	}

	private void saveLoginAndPassword() {
		//
		// TODO save drawerEmail here
		//
		PayloadFactory.getInstance().setTempPassword(new CharSequenceX(strPassword));
	}

	private void createWallet() {

		try {
			// create wallet
			// restart

			HDPayloadBridge.getInstance(this).createHDWallet(12, "", 1);

			PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(getResources().getString(R.string.default_wallet_name));

			PayloadFactory.getInstance(this).remoteSaveThread();

			AppUtil.getInstance(this).restartApp();

		} catch (IOException | MnemonicException.MnemonicLengthException e) {
			Toast.makeText(this, "HD Wallet creation error", Toast.LENGTH_SHORT).show();
			AppUtil.getInstance(this).clearCredentialsAndRestart();
		}

	}

	private void getBundleData() {

		Bundle extras = getIntent().getExtras();

		if (extras != null && extras.containsKey("_email")) {
			strEmail = extras.getString("_email");
		}

		if (extras != null && extras.containsKey("_pw")) {
			strPassword = extras.getString("_pw");
		}

		if (extras != null && extras.containsKey(PairingFactory.KEY_EXTRA_IS_PAIRING))
			AppUtil.getInstance(this).restartApp(); // ?
	}

	int exitClicked = 0;
	int exitCooldown = 2;//seconds
	@Override
	public void onBackPressed()
	{
		exitClicked++;
		if(exitClicked==2)
			finish();
		else
			Toast.makeText(this, getResources().getString(R.string.exit_confirm), Toast.LENGTH_SHORT).show();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				for(int j = 0; j <= exitCooldown; j++)
				{
					try{Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
					if(j >= exitCooldown)exitClicked = 0;
				}
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public void validatePIN(final String PIN) {
		validatePINThread(PIN);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

    private void updatePayloadThread(final CharSequenceX pw) {

		final Handler handler = new Handler();

		if(progress != null && progress.isShowing()) {
			progress.dismiss();
			progress = null;
		}
		progress = new ProgressDialog(PinEntryActivity.this);
		progress.setCancelable(false);
		progress.setTitle(R.string.app_name);
		progress.setMessage("Please wait...");
		progress.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Looper.prepare();

					if(HDPayloadBridge.getInstance(PinEntryActivity.this).init(pw)) {

						PayloadFactory.getInstance().setTempPassword(pw);

						handler.post(new Runnable() {
							@Override
							public void run() {

								if(progress != null && progress.isShowing()) {
									progress.dismiss();
									progress = null;
								}

					    		AppUtil.getInstance(PinEntryActivity.this).restartApp("verified", true);
							}
						});

					}
					else {
			        	Toast.makeText(PinEntryActivity.this, R.string.invalid_password, Toast.LENGTH_SHORT).show();
						if(progress != null && progress.isShowing()) {
							progress.dismiss();
							progress = null;
						}

						Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
			    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			    		startActivity(intent);
					}

					Looper.loop();

				}
                catch(JSONException | IOException | DecoderException | AddressFormatException
                        | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException
                        | MnemonicException.MnemonicChecksumException e) {
                    e.printStackTrace();
                }
				finally {
					if(progress != null && progress.isShowing()) {
						progress.dismiss();
						progress = null;
					}
				}

			}
		}).start();
	}

    private void createPINThread(final String pin) {

		final Handler handler = new Handler();

		if(progress != null && progress.isShowing()) {
			progress.dismiss();
			progress = null;
		}
		progress = new ProgressDialog(PinEntryActivity.this);
		progress.setCancelable(false);
		progress.setTitle(R.string.app_name);
		progress.setMessage("Please wait...");
		progress.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();

				if(AccessFactory.getInstance(PinEntryActivity.this).createPIN(PayloadFactory.getInstance().getTempPassword(), pin)) {

					if(progress != null && progress.isShowing()) {
						progress.dismiss();
						progress = null;
					}

		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, 0);
					updatePayloadThread(PayloadFactory.getInstance().getTempPassword());

				}
				else {

					if(progress != null && progress.isShowing()) {
						progress.dismiss();
						progress = null;
					}

		        	Toast.makeText(PinEntryActivity.this, R.string.create_pin_failed, Toast.LENGTH_SHORT).show();
	        		PrefsUtil.getInstance(PinEntryActivity.this).clear();
	        		AppUtil.getInstance(PinEntryActivity.this).restartApp();
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

    private void validatePINThread(final String pin) {

		final Handler handler = new Handler();

		if(progress != null && progress.isShowing()) {
			progress.dismiss();
			progress = null;
		}
		progress = new ProgressDialog(PinEntryActivity.this);
		progress.setCancelable(false);
		progress.setTitle(R.string.app_name);
		progress.setMessage("Please wait...");
		progress.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();

				CharSequenceX password = AccessFactory.getInstance(PinEntryActivity.this).validatePIN(pin);

				if(password != null) {

		    		if(progress != null && progress.isShowing()) {
		    			progress.dismiss();
		    			progress = null;
		    		}

		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                    AppUtil.getInstance(PinEntryActivity.this).updatePinEntryTime();
					updatePayloadThread(password);
				}
				else {

		    		if(progress != null && progress.isShowing()) {
		    			progress.dismiss();
		    			progress = null;
		    		}

		    		int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_FAILS, 0);
		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
//		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.KEY_LOGGED_IN, false);
		        	Toast.makeText(PinEntryActivity.this, R.string.invalid_pin, Toast.LENGTH_SHORT).show();
		    		Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
		    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		    		startActivity(intent);
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

    private void validationDialog()	{

		final EditText password = new EditText(this);
		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		new AlertDialog.Builder(this)
	    .setTitle(R.string.app_name)
	    .setMessage("Please enter password")
	    .setView(password)
	    .setCancelable(false)
	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {

	        	final String pw = password.getText().toString();

	        	if(pw != null && pw.length() > 0) {
		            validatePasswordThread(new CharSequenceX(pw));
	        	}

	        }
	    }).show();

    }

    private void validatePasswordThread(final CharSequenceX pw) {

		final Handler handler = new Handler();

		if(progress != null && progress.isShowing()) {
			progress.dismiss();
			progress = null;
		}
		progress = new ProgressDialog(PinEntryActivity.this);
		progress.setCancelable(false);
		progress.setTitle(R.string.app_name);
		progress.setMessage("Please wait...");
		progress.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Looper.prepare();

					PayloadFactory.getInstance().setTempPassword(new CharSequenceX(""));
					if(HDPayloadBridge.getInstance(PinEntryActivity.this).init(pw)) {

		            	Toast.makeText(PinEntryActivity.this, R.string.pin_3_strikes_password_accepted, Toast.LENGTH_SHORT).show();

						PayloadFactory.getInstance().setTempPassword(pw);
			        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.KEY_PIN_FAILS);
			        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

		        		Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
		        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		        		startActivity(intent);
					}
					else {
			        	Toast.makeText(PinEntryActivity.this, "Password error", Toast.LENGTH_SHORT).show();
						if(progress != null && progress.isShowing()) {
							progress.dismiss();
							progress = null;
						}

						validationDialog();

					}

					Looper.loop();

				}
	        	catch(JSONException | IOException | DecoderException | AddressFormatException
                        | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException
                        | MnemonicException.MnemonicChecksumException e) {
	        		e.printStackTrace();
	        	}
				finally {
					if(progress != null && progress.isShowing()) {
						progress.dismiss();
						progress = null;
					}
				}

			}
		}).start();
	}

	public void padClicked(View view) {

		// Append tapped #
		userEnteredPIN = userEnteredPIN + view.getTag().toString().substring(0, 1);
		pinBoxArray[userEnteredPIN.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

		// Perform appropriate action if PIN_LENGTH has been reached
		if (userEnteredPIN.length() == PIN_LENGTH) {

			// Throw error on '0000' to avoid server-side type issue
			if (userEnteredPIN.equals("0000")) {
				Toast.makeText(PinEntryActivity.this, R.string.zero_pin, Toast.LENGTH_SHORT).show();
				clearPinBoxes();
				userEnteredPIN = "";
				userEnteredPINConfirm = null;
				return;
			}

			// Validate
			if (PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() >= 1) {
				validatePIN(userEnteredPIN);
			}

			if(userEnteredPINConfirm == null)
			{
				//End of Create -  Change to Confirm
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {

						PinEntryActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								titleView.setText(R.string.confirm_pin);
								clearPinBoxes();
								userEnteredPINConfirm = userEnteredPIN;
								userEnteredPIN = "";
							}
						});
					}
				}, 200);

			}else if (userEnteredPINConfirm != null && userEnteredPINConfirm.equals(userEnteredPIN))
			{
				//End of Confirm - Pin is confirmed
				createPINThread(userEnteredPIN); // Pin is confirmed. Save to server.

			} else {

				//End of Confirm - Pin Mismatch
				Toast.makeText(PinEntryActivity.this, R.string.pin_mismatch_error, Toast.LENGTH_SHORT).show();
				clearPinBoxes();
				userEnteredPIN = "";
				userEnteredPINConfirm = null;
				titleView.setText(R.string.create_pin);
			}
		}
	}

	public void cancelClicked(View view) {
		clearPinBoxes();
		userEnteredPIN = "";
	}

	private void clearPinBoxes(){
		if(userEnteredPIN.length() > 0)	{
			for(int i = 0; i < pinBoxArray.length; i++)	{
				pinBoxArray[i].setBackgroundResource(R.drawable.rounded_view_blue_white_border);//reset pin buttons blank
			}
		}
	}
}