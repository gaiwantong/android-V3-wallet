package info.blockchain.wallet;

import java.io.IOException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DeviceUtil;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TimeOutUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class PinEntryActivity extends Activity {

	String userEntered = "";

	final int PIN_LENGTH = 4;
	boolean keyPadLockedFlag = false;
	Context context = null;

	TextView titleView = null;

	TextView pinBox0 = null;
	TextView pinBox1 = null;
	TextView pinBox2 = null;
	TextView pinBox3 = null;

	TextView[] pinBoxArray = null;

	TextView statusView = null;

	Button button0 = null;
	Button button1 = null;
	Button button2 = null;
	Button button3 = null;
	Button button4 = null;
	Button button5 = null;
	Button button6 = null;
	Button button7 = null;
	Button button8 = null;
	Button button9 = null;
	Button buttonForgot = null;
	Button buttonDeleteBack = null;

	private boolean validating = true;
	private String userInput = null;

	public String strUri = null;

	private ProgressDialog progress = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if(!DeviceUtil.getInstance(this).isSmallScreen()) {
			setContentView(R.layout.activity_pin_entry);
		}
		else {
			setContentView(R.layout.activity_pin_entry_small);
		}
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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

		buttonForgot = (Button) findViewById(R.id.buttonForgot);
		buttonForgot.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
	    		new AlertDialog.Builder(PinEntryActivity.this)
	    	    .setTitle(R.string.app_name)
				.setMessage(R.string.ask_you_sure_forget)
	    	    .setCancelable(false)
	    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.PIN_FAILS);
	    	        	PrefsUtil.getInstance(PinEntryActivity.this).removeValue(PrefsUtil.PIN_LOOKUP);
	    	        	validationDialog();
	    	        }
	    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	;
	    	        }
	    	    }).show();

			}
		});
		buttonForgot.setTypeface(typeface);

		buttonDeleteBack = (Button) findViewById(R.id.buttonDeleteBack);
		buttonDeleteBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(keyPadLockedFlag == true)	{
					return;
				}

				if(userEntered.length() > 0)	{
					for(int i = 0; i < pinBoxArray.length; i++)	{
						pinBoxArray[i].setText("");
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

				Button pressedButton = (Button)v;

				if(userEntered.length() < PIN_LENGTH)	{
					userEntered = userEntered + pressedButton.getText().toString().substring(0, 1);

					// Update pin boxes
					pinBoxArray[userEntered.length() - 1].setText("8");

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
					pinBoxArray[0].setText("");
					pinBoxArray[1].setText("");
					pinBoxArray[2].setText("");
					pinBoxArray[3].setText("");

					userEntered = "";

					statusView.setText("");

					userEntered = userEntered + pressedButton.getText().toString().substring(0, 1);

					// Update pin boxes
					pinBoxArray[userEntered.length() - 1].setText("8");

				}
			}
		};

		button0 = (Button)findViewById(R.id.button0);
		button0.setTypeface(typeface);
		button0.setOnClickListener(pinButtonHandler);

		button1 = (Button)findViewById(R.id.button1);
		button1.setTypeface(typeface);
		button1.setOnClickListener(pinButtonHandler);

		SpannableStringBuilder cs = null;
		float sz = 0.6f;

		button2 = (Button)findViewById(R.id.button2);
		button2.setTypeface(typeface);
		button2.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("2 ABC");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button2.setText(cs);

		button3 = (Button)findViewById(R.id.button3);
		button3.setTypeface(typeface);
		button3.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("3 DEF");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button3.setText(cs);

		button4 = (Button)findViewById(R.id.button4);
		button4.setTypeface(typeface);
		button4.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("4 GHI");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button4.setText(cs);

		button5 = (Button)findViewById(R.id.button5);
		button5.setTypeface(typeface);
		button5.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("5 JKL");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button5.setText(cs);

		button6 = (Button)findViewById(R.id.button6);
		button6.setTypeface(typeface);
		button6.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("6 MNO");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button6.setText(cs);

		button7 = (Button)findViewById(R.id.button7);
		button7.setTypeface(typeface);
		button7.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("7 PQRS");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button7.setText(cs);

		button8 = (Button)findViewById(R.id.button8);
		button8.setTypeface(typeface);
		button8.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("8 TUV");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button8.setText(cs);

		button9 = (Button)findViewById(R.id.button9);
		button9.setTypeface(typeface);
		button9.setOnClickListener(pinButtonHandler);
		cs = new SpannableStringBuilder("9 WXYZ");
		cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button9.setText(cs);

		buttonDeleteBack = (Button)findViewById(R.id.buttonDeleteBack);
		buttonDeleteBack.setTypeface(typeface);

		final int colorOff = 0xff333333;
		final int colorOn = 0xff1a87c6;

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

		buttonForgot.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction())	{
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_MOVE:
					buttonForgot.setBackgroundColor(colorOn);
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					buttonForgot.setBackgroundColor(colorOff);
					break;
				}

				return false;
			}
		});

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
        	validationDialog();
    	}

	}

	@Override
	public void onBackPressed() {
		return;
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
			pinBoxArray[0].setText("");
			pinBoxArray[1].setText("");
			pinBoxArray[2].setText("");
			pinBoxArray[3].setText("");

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