package info.blockchain.wallet;

import java.io.IOException;

import org.apache.commons.codec.DecoderException;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
//import android.util.Log;

import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

//public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener	{
public class SettingsActivity extends PreferenceActivity	{
	
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(R.string.app_name);
        	addPreferencesFromResource(R.xml.settings);
    	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        	PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        	final String guid = PayloadFactory.getInstance().get().getGuid();
        	Preference guidPref = (Preference) findPreference("guid");
            guidPref.setSummary(guid);
        	guidPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {

          			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)SettingsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
          		    android.content.ClipData clip = android.content.ClipData.newPlainText("Blockchain identifier", guid);
          		    clipboard.setPrimaryClip(clip);
         			Toast.makeText(SettingsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();

        			return true;
        		}
        	});

            Preference unitsPref = (Preference) findPreference("units");
            unitsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                    getUnits();
                    return true;
            }
        });

            Preference fiatPref = (Preference) findPreference("fiat");
            	fiatPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
        	        	Intent intent = new Intent(SettingsActivity.this, CurrencySelector.class);
        		    	startActivity(intent);
        			    return true;
               		}
        	});

        	Preference mnemonicPref = (Preference) findPreference("mnemonic");
        	mnemonicPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {

        	        if(PayloadFactory.getInstance().get().isDoubleEncrypted()) {

        	        	if(DoubleEncryptionFactory.getInstance().isActivated()) {

        	        		String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
    	    	        			PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
    	    	        			PayloadFactory.getInstance().get().getSharedKey(),
    			    	        	PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
    	    	        			PayloadFactory.getInstance().get().getIterations());
        	        		
	    					try {
    	    	        		HD_Wallet hdw = HD_WalletFactory.getInstance(SettingsActivity.this).get();
    	    	        		HD_WalletFactory.getInstance(SettingsActivity.this).restoreWallet(decrypted_hex, "", 1);
    	    	        		String mnemonic = HD_WalletFactory.getInstance(SettingsActivity.this).get().getMnemonic();
    	    	        		HD_WalletFactory.getInstance(SettingsActivity.this).set(hdw);
            	            	Toast.makeText(SettingsActivity.this, mnemonic, Toast.LENGTH_SHORT).show();
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
	    						;
	    					}

        	        	}
        	        	else {
            	    		final EditText double_encrypt_password = new EditText(SettingsActivity.this);
            	    		double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            	    		
            	    		new AlertDialog.Builder(SettingsActivity.this)
            	    	    .setTitle(R.string.app_name)
            				.setMessage("Please enter double encryption password")
            	    	    .setView(double_encrypt_password)
            	    	    .setCancelable(false)
            	    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            	    	        public void onClick(DialogInterface dialog, int whichButton) {

            	    	        	String pw2 = double_encrypt_password.getText().toString();

            	    	        	if(pw2 != null && pw2.length() > 0 && DoubleEncryptionFactory.getInstance().validateSecondPassword(
            	    	        			PayloadFactory.getInstance().get().getDoublePasswordHash(),
            	    	        			PayloadFactory.getInstance().get().getSharedKey(),
            	    	        			new CharSequenceX(pw2),
            	    	        			PayloadFactory.getInstance().get().getIterations()
            	    	        			)) {

        			    	        	PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw2));

            	    	        		String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
            	    	        			PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
            	    	        			PayloadFactory.getInstance().get().getSharedKey(),
            	    	        			pw2,
            	    	        			PayloadFactory.getInstance().get().getIterations());

            	    					try {
                	    	        		HD_Wallet hdw = HD_WalletFactory.getInstance(SettingsActivity.this).get();
                	    	        		HD_WalletFactory.getInstance(SettingsActivity.this).restoreWallet(decrypted_hex, "", 1);
                	    	        		String mnemonic = HD_WalletFactory.getInstance(SettingsActivity.this).get().getMnemonic();
                	    	        		HD_WalletFactory.getInstance(SettingsActivity.this).set(hdw);
                        	            	Toast.makeText(SettingsActivity.this, mnemonic, Toast.LENGTH_SHORT).show();
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
            	    						;
            	    					}

            	    	        	}
            	    	        	else {
    			                        Toast.makeText(SettingsActivity.this, R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
    			    	        		PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
            	    	        	}

            	    	        }
            	    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            	    	        public void onClick(DialogInterface dialog, int whichButton) {
            	    	        	;
            	    	        }
            	    	    }).show();
        	        	}
        	        			
	    	        }
        	        else {
    	        		getHDSeed(true);
        	        }

        			return true;
        		}
        	});
/*
        	Preference hexseedPref = (Preference) findPreference("hexseed");
        	hexseedPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {

        	        if(PayloadFactory.getInstance().get().isDoubleEncrypted()) {

        	        	if(DoubleEncryptionFactory.getInstance().isActivated()) {

        	        		String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
    	    	        			PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
    	    	        			PayloadFactory.getInstance().get().getSharedKey(),
    			    	        	PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
    	    	        			PayloadFactory.getInstance().get().getIterations());

        	            	Toast.makeText(SettingsActivity.this, decrypted_hex, Toast.LENGTH_SHORT).show();

        	        	}
        	        	else {
            	    		final EditText double_encrypt_password = new EditText(SettingsActivity.this);
            	    		double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            	    		
            	    		new AlertDialog.Builder(SettingsActivity.this)
            	    	    .setTitle(R.string.app_name)
            				.setMessage(R.string.enter_double_encryption_pw)
            	    	    .setView(double_encrypt_password)
            	    	    .setCancelable(false)
            	    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            	    	        public void onClick(DialogInterface dialog, int whichButton) {

            	    	        	String pw2 = double_encrypt_password.getText().toString();

            	    	        	if(pw2 != null && pw2.length() > 0 && DoubleEncryptionFactory.getInstance().validateSecondPassword(
            	    	        			PayloadFactory.getInstance().get().getDoublePasswordHash(),
            	    	        			PayloadFactory.getInstance().get().getSharedKey(),
            	    	        			new CharSequenceX(pw2),
            	    	        			PayloadFactory.getInstance().get().getIterations()
            	    	        			)) {

        			    	        	PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw2));

            	    	        		String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
            	    	        			PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
            	    	        			PayloadFactory.getInstance().get().getSharedKey(),
            	    	        			pw2,
            	    	        			PayloadFactory.getInstance().get().getIterations());

                    	            	Toast.makeText(SettingsActivity.this, decrypted_hex, Toast.LENGTH_SHORT).show();

            	    	        	}
            	    	        	else {
    			                        Toast.makeText(SettingsActivity.this, R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
    			    	        		PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
            	    	        	}

            	    	        }
            	    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            	    	        public void onClick(DialogInterface dialog, int whichButton) {
            	    	        	;
            	    	        }
            	    	    }).show();
        	        	}
        	        			
	    	        }
        	        else {
    	        		getHDSeed(false);
        	        }

        			return true;
        		}
        	});
*/
        	/*
        	Preference passphrasePref = (Preference) findPreference("passphrase");
        	passphrasePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
        			getPassphrase();
        			return true;
        		}
        	});
        	*/

        	Preference unpairPref = (Preference) findPreference("unpair");
        	unpairPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
					builder.setMessage(R.string.ask_you_sure_unpair)
					.setCancelable(false);

					AlertDialog alert = builder.create();

					alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

							PayloadFactory.getInstance().wipe();
							MultiAddrFactory.getInstance().wipe();
							PrefsUtil.getInstance(SettingsActivity.this).clear();
							
							AppUtil.getInstance(SettingsActivity.this).restartApp();

							dialog.dismiss();
						}}); 

					alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							
							dialog.dismiss();
						}});

					alert.show();
        			
        			return true;
        		}
        	});

            Preference aboutPref = (Preference) findPreference("about");
            aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                    startActivity(intent);

                    return true;
                }
            });

            Preference tosPref = (Preference) findPreference("tos");
            tosPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://blockchain.info/Resources/TermsofServicePolicy.pdf"));
                    startActivity(intent);

                    return true;
                }
            });

            Preference privacyPref = (Preference) findPreference("privacy");
            privacyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://blockchain.info/Resources/PrivacyPolicy.pdf"));
                startActivity(intent);

                return true;
                }
            });

    }
    
    private void getHDSeed(boolean mnemonic)	{
    	String seed = null;
		try {
			if(mnemonic)	{
				seed = HDPayloadBridge.getInstance(SettingsActivity.this).getHDMnemonic();
	        	Toast.makeText(SettingsActivity.this, seed, Toast.LENGTH_SHORT).show();
			}
			else	{
				seed = HDPayloadBridge.getInstance(SettingsActivity.this).getHDSeed();
	        	Toast.makeText(SettingsActivity.this, seed, Toast.LENGTH_SHORT).show();
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
        	Toast.makeText(SettingsActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
		}
		catch(MnemonicException.MnemonicLengthException mle) {
			mle.printStackTrace();
        	Toast.makeText(SettingsActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
		}
		
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager)this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
		android.content.ClipData clip = null;
	    clip = android.content.ClipData.newPlainText(mnemonic ? "Mnemonic" : "Hex seed", seed);
		clipboard.setPrimaryClip(clip);

    }

    private void getPassphrase()	{
    	String passphrase = null;
		try {
			passphrase = HDPayloadBridge.getInstance(SettingsActivity.this).getHDPassphrase();
        	Toast.makeText(SettingsActivity.this, passphrase, Toast.LENGTH_SHORT).show();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
        	Toast.makeText(SettingsActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
		}
		catch(MnemonicException.MnemonicLengthException mle) {
			mle.printStackTrace();
        	Toast.makeText(SettingsActivity.this, "HD wallet error", Toast.LENGTH_SHORT).show();
		}
		
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager)this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
		android.content.ClipData clip = null;
	    clip = android.content.ClipData.newPlainText("Passphrase", passphrase);
		clipboard.setPrimaryClip(clip);

    }

    private void getUnits()	{

        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.BTC_UNITS, 0);

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.select_units)
//                .setCancelable(false)
                .setSingleChoiceItems(units, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.BTC_UNITS, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

/*
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    	Pattern emailPattern = Patterns.EMAIL_ADDRESS;
    	Pattern phonePattern = Pattern.compile("(\\+[1-9]{1}[0-9]{1,2}+|00[1-9]{1}[0-9]{1,2}+)[\\(\\)\\.\\-\\s\\d]{6,16}");

    	SharedPreferences sp = getPreferenceScreen().getSharedPreferences();

    	if(key.equals("backups") && !sp.getString("email", "").equals("")) {
    		sendNotifsThread(sp.getBoolean("backups", false), sp.getBoolean("notifs", false));
    	}
    	else {
    		sendNotifsThread(sp.getBoolean("backups", false), sp.getBoolean("notifs", false));
    	}
    	if(key.equals("email") && sp.getBoolean("backups", false) == true) {
    		String email = sp.getString("email", "");
    		if(emailPattern.matcher(email).matches()) {
        		sendEmailThread(email);
    		}
    		else {
   				Toast.makeText(SettingsActivity.this, R.string.invalid_email, Toast.LENGTH_LONG).show();
    		}
    	}

    	if(key.equals("notifs") && !sp.getString("mobile", "").equals("")) {
    		sendNotifsThread(sp.getBoolean("backups", false), sp.getBoolean("notifs", false));
    	}
    	else {
    		sendNotifsThread(sp.getBoolean("backups", false), sp.getBoolean("notifs", false));
    	}
    	if(key.equals("mobile") && sp.getBoolean("notifs", false) == true) {
    		String mobile = sp.getString("mobile", "");
    		if(phonePattern.matcher(mobile).matches()) {
        		sendSMSThread(mobile);
    		}
    		else {
   				Toast.makeText(SettingsActivity.this, R.string.invalid_mobile, Toast.LENGTH_LONG).show();
    		}
    	}

    }
    
    private void sendEmailThread(final String email) {

    	final MyRemoteWallet remoteWallet = WalletUtil.getInstance(SettingsActivity.this).getRemoteWallet();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				String response = null;
				try {
					response = remoteWallet.updateEmail(email);
				}
				catch(Exception e) {
					e.printStackTrace();
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

    private void sendSMSThread(final String smsNumber) {

    	final MyRemoteWallet remoteWallet = WalletUtil.getInstance(SettingsActivity.this).getRemoteWallet();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				String response = null;
				try {
					response = remoteWallet.updateSMS(smsNumber);
				}
				catch(Exception e) {
					e.printStackTrace();
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

    private void sendNotifsThread(final boolean email, final boolean sms) {

    	final MyRemoteWallet remoteWallet = WalletUtil.getInstance(SettingsActivity.this).getRemoteWallet();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				String response = null;
				try {
					response = remoteWallet.updateNotificationsType(email, sms);
				}
				catch(Exception e) {
					e.printStackTrace();
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
*/
}
