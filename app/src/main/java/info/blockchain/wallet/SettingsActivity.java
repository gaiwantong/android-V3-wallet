package info.blockchain.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

//import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

    /**
     * Called when the activity is first created.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        addPreferencesFromResource(R.xml.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final String guid = PayloadFactory.getInstance().get().getGuid();
        Preference guidPref = (Preference) findPreference("guid");
        guidPref.setSummary(guid);

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

				final String[] currencies = ExchangeRateFactory.getInstance(SettingsActivity.this).getCurrencyLabels();
				String strCurrency = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
				int sel = 0;
				for(int i = 0; i < currencies.length; i++) {
					if(currencies[i].endsWith(strCurrency)) {
						sel = i;
						break;
					}
				}

				new AlertDialog.Builder(SettingsActivity.this)
						.setTitle(R.string.select_currency)
//                .setCancelable(false)
						.setSingleChoiceItems(currencies, sel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
//										PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, which);
										PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
										dialog.dismiss();
									}
								}
						).show();

                return true;
            }
        });


        Preference mnemonicPref = (Preference) findPreference("mnemonic");
        mnemonicPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                // Wallet is not double encrypted
                if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                    displayHDSeedAsMnemonic(true);
                }
                // User has already entered double-encryption password
                else if (DoubleEncryptionFactory.getInstance().isActivated()) {
                    displayMnemonicForDoubleEncryptedWallet();
                }
                // Solicit & set double-encryption password, then display
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
                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(double_encrypt_password.getText().toString()));
                                    displayMnemonicForDoubleEncryptedWallet();
                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ;
                        }
                    }).show();
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
    	        		displayHDSeedAsMnemonic(false);
        	        }

        			return true;
        		}
        	});
*/

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

    private void displayMnemonicForDoubleEncryptedWallet() {

        if (PayloadFactory.getInstance().getTempDoubleEncryptPassword() == null || PayloadFactory.getInstance().getTempDoubleEncryptPassword().length() == 0) {
            Toast.makeText(SettingsActivity.this, R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
            return;
        }

        // Decrypt seedHex (which is double encrypted in this case)
        String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
                PayloadFactory.getInstance().get().getSharedKey(),
                PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
                PayloadFactory.getInstance().get().getIterations());

        String mnemonic = null;

        // Try to create a using the decrypted seed hex
        try {
            HD_Wallet hdw = HD_WalletFactory.getInstance(this).get();
            HD_WalletFactory.getInstance(this).restoreWallet(decrypted_hex, "", 1);

            mnemonic = HD_WalletFactory.getInstance(this).get().getMnemonic();
            HD_WalletFactory.getInstance(this).set(hdw);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (DecoderException de) {
            de.printStackTrace();
        } catch (AddressFormatException afe) {
            afe.printStackTrace();
        } catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
        } catch (MnemonicException.MnemonicChecksumException mce) {
            mce.printStackTrace();
        } catch (MnemonicException.MnemonicWordException mwe) {
            mwe.printStackTrace();
        } finally {
            if (mnemonic != null && mnemonic.length() > 0) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(mnemonic)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();

            } else {
                Toast.makeText(this, R.string.double_encryption_password_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void displayHDSeedAsMnemonic(boolean mnemonic) {
        String seed = null;
        try {
            if (mnemonic) {
                seed = HDPayloadBridge.getInstance(SettingsActivity.this).getHDMnemonic();
            } else {
                seed = HDPayloadBridge.getInstance(SettingsActivity.this).getHDSeed();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Toast.makeText(SettingsActivity.this, R.string.hd_error, Toast.LENGTH_SHORT).show();
        } catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            Toast.makeText(SettingsActivity.this, R.string.hd_error, Toast.LENGTH_SHORT).show();
        } finally {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(seed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    private void getUnits() {

        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_BTC_UNITS, 0);

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.select_units)
//                .setCancelable(false)
                .setSingleChoiceItems(units, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_BTC_UNITS, which);
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

    	if(key.equals("backups") && !sp.getString("drawerEmail", "").equals("")) {
    		sendNotifsThread(sp.getBoolean("backups", false), sp.getBoolean("notifs", false));
    	}
    	else {
    		sendNotifsThread(sp.getBoolean("backups", false), sp.getBoolean("notifs", false));
    	}
    	if(key.equals("drawerEmail") && sp.getBoolean("backups", false) == true) {
    		String drawerEmail = sp.getString("drawerEmail", "");
    		if(emailPattern.matcher(drawerEmail).matches()) {
        		sendEmailThread(drawerEmail);
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
    
    private void sendEmailThread(final String drawerEmail) {

    	final MyRemoteWallet remoteWallet = WalletUtil.getInstance(SettingsActivity.this).getRemoteWallet();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				String response = null;
				try {
					response = remoteWallet.updateEmail(drawerEmail);
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

    private void sendNotifsThread(final boolean drawerEmail, final boolean sms) {

    	final MyRemoteWallet remoteWallet = WalletUtil.getInstance(SettingsActivity.this).getRemoteWallet();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				String response = null;
				try {
					response = remoteWallet.updateNotificationsType(drawerEmail, sms);
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
