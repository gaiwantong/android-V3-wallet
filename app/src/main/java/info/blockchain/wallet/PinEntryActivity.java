package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import java.io.IOException;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TimeOutUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class PinEntryActivity extends Activity {

	String userEntered = "";

	final int PIN_LENGTH = 4;
	boolean keyPadLockedFlag = false;

	TextView titleView = null;

	TextView pinBox0 = null;
	TextView pinBox1 = null;
	TextView pinBox2 = null;
	TextView pinBox3 = null;

	TextView[] pinBoxArray = null;

	TextView statusView = null;

	LinearLayout button0 = null;
	LinearLayout button1 = null;
	LinearLayout button2 = null;
	LinearLayout button3 = null;
	LinearLayout button4 = null;
	LinearLayout button5 = null;
	LinearLayout button6 = null;
	LinearLayout button7 = null;
	LinearLayout button8 = null;
	LinearLayout button9 = null;
//	Button buttonForgot = null; //Forgot pin button disabled in new UI
	LinearLayout buttonDeleteBack = null;

	private boolean validating = true;
	private String userInput = null;

	public String strUri = null;

	private ProgressDialog progress = null;

	private String strEmail = null;
	private String strPassword = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_pin_entry);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		boolean isPairing = false;
		Bundle extras = getIntent().getExtras();

		if(extras != null && extras.containsKey("pairing"))	{
			isPairing = extras.getBoolean("pairing");
		}
		else	{
			isPairing = false;
		}
		if(extras != null && extras.containsKey("_email"))	{
			strEmail = extras.getString("_email");
		}
		if(extras != null && extras.containsKey("_pw"))	{
			strPassword = extras.getString("_pw");
		}

		if(isPairing) {
			AppUtil.getInstance(this).restartApp();
		}
		else if(extras!=null){

			//
			// save email here
			//

			// create wallet
			// restart
			try {
				HDPayloadBridge.getInstance(this).createHDWallet(12, "", 1);
				PayloadFactory.getInstance().setTempPassword(new CharSequenceX(strPassword));

				PayloadFactory.getInstance(this).remoteSaveThread();

				AppUtil.getInstance(this).restartApp();
			}
			catch(IOException ioe) {
				Toast.makeText(this, "HD Wallet creation error", Toast.LENGTH_SHORT).show();
				AppUtil.getInstance(this).wipeApp();
			}
			catch(MnemonicException.MnemonicLengthException mle) {
				Toast.makeText(this, "HD Wallet creation error", Toast.LENGTH_SHORT).show();
				AppUtil.getInstance(this).wipeApp();
			}

		}

	    String action = getIntent().getAction();
	    String scheme = getIntent().getScheme();
		if(action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
			strUri = getIntent().getData().toString();
	    }

		/*
		Bundle extras = getIntent().getExtras();
		if(extras != null)	{
		}
		*/

		Typeface typeface = TypefaceUtil.getInstance(this).getRobotoTypeface();

		if(PrefsUtil.getInstance(this).getValue("userInput", "").length() > 0) {
			userInput = PrefsUtil.getInstance(this).getValue("userInput", "");
		}
		else {
			userInput = null;
		}
		userEntered = "";

		/*
		Forgot pin button disabled in new UI
		 */
//		buttonForgot = (Button) findViewById(R.id.buttonForgot);
//		buttonForgot.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//	    		new AlertDialog.Builder(PinEntryActivity.this)
//	    	    .setTitle(R.string.app_name)
//				.setMessage(R.string.ask_you_sure_forget)
//	    	    .setCancelable(false)
//	    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//	    	        public void onClick(DialogInterface dialog, int whichButton) {
//	    	        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.PIN_FAILS);
//	    	        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.PIN_LOOKUP);
//	    	        	validationDialog();
//	    	        }
//	    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//	    	        public void onClick(DialogInterface dialog, int whichButton) {
//	    	        	;
//	    	        }
//	    	    }).show();
//
//			}
//		});
//		buttonForgot.setTypeface(typeface);

		buttonDeleteBack = (LinearLayout) findViewById(R.id.buttonDeleteBack);
		buttonDeleteBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(keyPadLockedFlag == true)	{
					return;
				}

				if(userEntered.length() > 0)	{
					for(int i = 0; i < pinBoxArray.length; i++)	{
						pinBoxArray[i].setBackgroundResource(R.drawable.rounded_view_blue_white_border);//reset pin buttons blank
					}
					userEntered = "";
				}
			}
		});

		titleView = (TextView)findViewById(R.id.titleBox);
		titleView.setTypeface(typeface);

		if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.PIN_LOOKUP, "").length() < 1) {
			validating = false;

			if(userInput == null) {
				titleView.setText(R.string.create_pin);
			}
			else {
				titleView.setText(R.string.confirm_pin);
			}
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

		statusView = (TextView)findViewById(R.id.statusMessage);
		statusView.setTypeface(typeface);

		View.OnClickListener pinButtonHandler = new View.OnClickListener() {
			public void onClick(View v) {

				if(keyPadLockedFlag == true)	{
					return;
				}

				LinearLayout pressedButton = (LinearLayout)v;

				if(userEntered.length() < PIN_LENGTH)	{
					userEntered = userEntered + pressedButton.getTag().toString().substring(0, 1);

					// Update pin boxes
					pinBoxArray[userEntered.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

					if(userEntered.length() == PIN_LENGTH)	{
						if(validating)	{
							PrefsUtil.getInstance(PinEntryActivity.this).setValue("userInput", "");
							validatePIN(userEntered);
						}
						else	{
							if(userInput != null)	{
								PrefsUtil.getInstance(PinEntryActivity.this).setValue("userInput", "");
								if(userInput.equals(userEntered))	{
									createPINThread(userEntered);
								}
								else	{
									Toast.makeText(PinEntryActivity.this, R.string.pin_mismatch_error, Toast.LENGTH_SHORT).show();
					        		Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
					        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					        		startActivity(intent);
								}
							}
							else	{
								if(userEntered.equals("0000"))	{
									Toast.makeText(PinEntryActivity.this, R.string.zero_pin, Toast.LENGTH_SHORT).show();
									PrefsUtil.getInstance(PinEntryActivity.this).setValue("userInput", "");
					        		Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
					        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					        		startActivity(intent);
								}
								else	{
									PrefsUtil.getInstance(PinEntryActivity.this).setValue("userInput", userEntered);
					        		Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
					        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					        		startActivity(intent);
								}
							}
						}

					}
				}
				else	{
					// Roll over

					for(int i = 0; i < pinBoxArray.length; i++)	{
						pinBoxArray[i].setBackgroundResource(R.drawable.rounded_view_blue_white_border);
					}

					userEntered = "";

					statusView.setText("");

					userEntered = userEntered + pressedButton.getTag().toString().substring(0, 1);

					// Update pin boxes
					pinBoxArray[userEntered.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

				}
			}
		};

		button0 = (LinearLayout)findViewById(R.id.button0);
//		button0.setTypeface(typeface);
		button0.setOnClickListener(pinButtonHandler);

		button1 = (LinearLayout)findViewById(R.id.button1);
//		button1.setTypeface(typeface);
		button1.setOnClickListener(pinButtonHandler);

//		SpannableStringBuilder cs = null;
//		float sz = 0.6f;

		button2 = (LinearLayout)findViewById(R.id.button2);
//		button2.setTypeface(typeface);
		button2.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("2 ABC");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button2.setText(cs);

		button3 = (LinearLayout)findViewById(R.id.button3);
//		button3.setTypeface(typeface);
		button3.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("3 DEF");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button3.setText(cs);

		button4 = (LinearLayout)findViewById(R.id.button4);
//		button4.setTypeface(typeface);
		button4.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("4 GHI");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button4.setText(cs);

		button5 = (LinearLayout)findViewById(R.id.button5);
//		button5.setTypeface(typeface);
		button5.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("5 JKL");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button5.setText(cs);

		button6 = (LinearLayout)findViewById(R.id.button6);
//		button6.setTypeface(typeface);
		button6.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("6 MNO");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button6.setText(cs);

		button7 = (LinearLayout)findViewById(R.id.button7);
//		button7.setTypeface(typeface);
		button7.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("7 PQRS");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button7.setText(cs);

		button8 = (LinearLayout)findViewById(R.id.button8);
//		button8.setTypeface(typeface);
		button8.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("8 TUV");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button8.setText(cs);

		button9 = (LinearLayout)findViewById(R.id.button9);
//		button9.setTypeface(typeface);
		button9.setOnClickListener(pinButtonHandler);
//		cs = new SpannableStringBuilder("9 WXYZ");
//		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		button9.setText(cs);

		buttonDeleteBack = (LinearLayout)findViewById(R.id.buttonDeleteBack);
//		buttonDeleteBack.setTypeface(typeface);

		final int colorOff = 0xffffffff;
		final int colorOn = 0xFFe0e0e0;

		button0.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button0.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button0.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button1.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button1.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button2.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button2.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button3.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button3.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button3.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button4.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button4.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button4.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button5.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button5.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button5.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button6.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button6.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button6.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button7.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button7.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button7.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button8.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button8.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button8.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		button9.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					button9.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					button9.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		buttonDeleteBack.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					buttonDeleteBack.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					buttonDeleteBack.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

		/*
		Forgot pin button disabled in new UI
		 */
//		buttonForgot.setOnTouchListener(new OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				switch (event.getAction())	{
//				case android.view.MotionEvent.ACTION_DOWN:
//				case android.view.MotionEvent.ACTION_MOVE:
//					buttonForgot.setBackgroundColor(colorOn);
//					break;
//				case android.view.MotionEvent.ACTION_UP:
//				case android.view.MotionEvent.ACTION_CANCEL:
//					buttonForgot.setBackgroundColor(colorOff);
//					break;
//				}
//
//				return false;
//			}
//		});

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

    	int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.PIN_FAILS, 0);
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

                    AppUtil.getInstance(PinEntryActivity.this).wipeApp();

                }
            }).show();

        }

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

	private class LockKeyPadOperation extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			for(int i = 0; i < 2; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return "Executed";
		}

		@Override
		protected void onPostExecute(String result) {
			statusView.setText("");

			// Roll over
			for(int i = 0; i < pinBoxArray.length; i++)	{
				pinBoxArray[i].setBackgroundResource(R.drawable.rounded_view_blue_white_border);
			}

			userEntered = "";

			keyPadLockedFlag = false;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
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

								/*
								if(!OSUtil.getInstance(PinEntryActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
									startService(new Intent(PinEntryActivity.this, info.blockchain.wallet.service.WebSocketService.class));
								}
								*/

					    		AppUtil.getInstance(PinEntryActivity.this).restartApp("verified", true);
							}
						});

					}
					else {
			        	Toast.makeText(PinEntryActivity.this, "Password error", Toast.LENGTH_SHORT).show();
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
	        	catch(JSONException je) {
	        		je.printStackTrace();
	        	}
	        	catch(IOException ioe) {
	        		ioe.printStackTrace();
	        	}
	        	catch(DecoderException de) {
	        		de.printStackTrace();
	        	}
	        	catch(AddressFormatException afe) {
	        		afe.printStackTrace();
	        	}
	        	catch(MnemonicException.MnemonicLengthException mle) {
	        		mle.printStackTrace();
	        	}
	        	catch(MnemonicException.MnemonicChecksumException mce) {
	        		mce.printStackTrace();
	        	}
	        	catch(MnemonicException.MnemonicWordException mwe) {
	        		mwe.printStackTrace();
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

				if(AccessFactory.getInstance(PinEntryActivity.this).createPÏN(PayloadFactory.getInstance().getTempPassword(), pin)) {

					if(progress != null && progress.isShowing()) {
						progress.dismiss();
						progress = null;
					}

		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.PIN_FAILS, 0);
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

				CharSequenceX password = AccessFactory.getInstance(PinEntryActivity.this).validatePÏN(pin);

				if(password != null) {

		    		if(progress != null && progress.isShowing()) {
		    			progress.dismiss();
		    			progress = null;
		    		}

		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.PIN_FAILS, 0);
					TimeOutUtil.getInstance().updatePin();
					updatePayloadThread(password);
				}
				else {

		    		if(progress != null && progress.isShowing()) {
		    			progress.dismiss();
		    			progress = null;
		    		}

		    		int fails = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.PIN_FAILS, 0);
		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.PIN_FAILS, ++fails);
//		        	PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.LOGGED_IN, false);
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
		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

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
			        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.PIN_FAILS);
			        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.PIN_LOOKUP);

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
	        	catch(JSONException je) {
	        		je.printStackTrace();
	        	}
	        	catch(IOException ioe) {
	        		ioe.printStackTrace();
	        	}
	        	catch(DecoderException de) {
	        		de.printStackTrace();
	        	}
	        	catch(AddressFormatException afe) {
	        		afe.printStackTrace();
	        	}
	        	catch(MnemonicException.MnemonicLengthException mle) {
	        		mle.printStackTrace();
	        	}
	        	catch(MnemonicException.MnemonicChecksumException mce) {
	        		mce.printStackTrace();
	        	}
	        	catch(MnemonicException.MnemonicWordException mwe) {
	        		mwe.printStackTrace();
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

}