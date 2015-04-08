package info.blockchain.wallet;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.nio.charset.Charset;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.EditText;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
//import android.nfc.Tag;
import android.nfc.NfcEvent;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.widget.Toast;
import android.os.Parcelable;
import android.text.InputType;
import android.util.Log;

import org.apache.commons.codec.DecoderException;

import org.json.JSONException;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import net.sourceforge.zbar.Symbol;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.params.MainNetParams;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.TimeOutUtil;
import info.blockchain.wallet.util.WebUtil;

public class MainActivity extends ActionBarActivity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback	{

	private static final int IMPORT_PRIVATE_KEY = 2006;
	private static final int SCAN_URI = 2007;

	private Locale locale = null;

	// toolbar
	private Toolbar toolbar = null;
	
	private boolean wasPaused = false;

	// hamburger menu
	private DrawerLayout drawerLayout = null;
	private ListView listView = null;
	private ActionBarDrawerToggle actionBarDrawerToggle = null;
	private String[] navigationDrawerItems = null;

	private ProgressDialog progress = null;
	
	private NfcAdapter mNfcAdapter = null;
	public static final String MIME_TEXT_PLAIN = "text/plain";
	private static final int MESSAGE_SENT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if(!ConnectivityStatus.hasConnectivity(this)) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);

			final String message = getString(R.string.check_connectivity_exit);

			builder.setMessage(message)
					.setCancelable(false)
					.setPositiveButton(R.string.dialog_continue,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface d, int id) {
									d.dismiss();
									Class c = null;
									if(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.GUID, "").length() < 1) {
										c = Setup0Activity.class;
									}
									else {
										c = PinEntryActivity.class;
									}
									Intent intent = new Intent(MainActivity.this, c);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(intent);
								}
							});

			builder.create().show();
		}
		else {

			exchangeRateThread();

			boolean verified = false;
			Bundle extras = getIntent().getExtras();
			if(extras != null && extras.containsKey("verified"))	{
				verified = extras.getBoolean("verified");
			}

			if(PrefsUtil.getInstance(this).getValue(PrefsUtil.GUID, "").length() < 1) {
				PayloadFactory.getInstance().setTempPassword(new CharSequenceX(""));
				Intent intent = new Intent(MainActivity.this, Setup0Activity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
			else if(PrefsUtil.getInstance(this).getValue(PrefsUtil.PIN_LOOKUP, "").length() < 1) {
				Intent intent = new Intent(MainActivity.this, PinEntryActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
			else if(verified) {
				AccessFactory.getInstance(MainActivity.this).setIsLoggedIn(true);

				TimeOutUtil.getInstance().updatePin();
				Fragment fragment = new BalanceFragment();
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			}
			else if(AccessFactory.getInstance(MainActivity.this).isLoggedIn() && !TimeOutUtil.getInstance().isTimedOut()) {
				TimeOutUtil.getInstance().updatePin();
				Fragment fragment = new BalanceFragment();
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			}
			else {
				Intent intent = new Intent(MainActivity.this, PinEntryActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		}

		setContentView(R.layout.activity_main);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		locale = Locale.getDefault();

		toolbar = (Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

//		getSupportActionBar().setDisplayOptions(getSupportActionBar().getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
		getSupportActionBar().setDisplayOptions(getSupportActionBar().getDisplayOptions() | ActionBar.DISPLAY_SHOW_TITLE);
//		getSupportActionBar().setLogo(R.drawable.masthead);
		getSupportActionBar().setTitle("Blockchain");
    	getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));

		navigationDrawerItems = getResources().getStringArray(R.array.navigation_drawer_items);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		listView = (ListView) findViewById(R.id.left_drawer);

		// set a custom shadow that overlays the main content when the drawer opens
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, navigationDrawerItems));
		listView.setOnItemClickListener(new DrawerItemClickListener());

		actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
		drawerLayout.setDrawerListener(actionBarDrawerToggle);

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(mNfcAdapter == null)   {
//            Toast.makeText(MainActivity.this, "nfcAdapter == null, no NFC adapter exists", Toast.LENGTH_SHORT).show();
        }
        else    {
//            Toast.makeText(MainActivity.this, "Set NFC Callback(s)", Toast.LENGTH_SHORT).show();
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }

		if(savedInstanceState == null) {
//			selectItem(0);
		}

	}

    /* start NFC specific */

	@Override
    protected void onResume() {
        super.onResume();

		if(TimeOutUtil.getInstance().isTimedOut()) {
            Class c = null;
			if(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.GUID, "").length() < 1) {
				c = Setup0Activity.class;
			}
			else {
				c = PinEntryActivity.class;
			}

    		Intent i = new Intent(MainActivity.this, c);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    		startActivity(i);
		}
		else {
			TimeOutUtil.getInstance().updatePin();
		}

        if(Build.VERSION.SDK_INT >= 16){
            Intent intent = getIntent();
            String action = intent.getAction();
            if(mNfcAdapter != null && action != null && action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)){
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage inNdefMessage = (NdefMessage)parcelables[0];
                NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
                NdefRecord NdefRecord_0 = inNdefRecords[0];
                String inMsg = new String(NdefRecord_0.getPayload(), 1, NdefRecord_0.getPayload().length - 1, Charset.forName("US-ASCII"));
//            Toast.makeText(MainActivity.this, inMsg, Toast.LENGTH_SHORT).show();
                doScanInput(inMsg);

            }
        }

    }

	@Override
	public void onStart() {
        super.onStart();

	}
	
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

		if(!OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
			stopService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));
		}

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {

        if(Build.VERSION.SDK_INT < 16){
            return;
        }

        /*
        final String eventString = "onNdefPushComplete\n" + event.toString();

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), eventString, Toast.LENGTH_SHORT).show();
            }
        });
        */

    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {

        if(Build.VERSION.SDK_INT < 16){
            return null;
        }

        NdefRecord rtdUriRecord = NdefRecord.createUri("market://details?id=piuk.blockchain.android");
        NdefMessage ndefMessageout = new NdefMessage(rtdUriRecord);
        return ndefMessageout;
    }

    /* end NFC specific */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_merchant_directory:
	        	Toast.makeText(this, "Merchant directory", Toast.LENGTH_SHORT).show();
	            return true;
	        case R.id.action_qr:
	        	scanURI();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	/* The click listener for ListView in the navigation drawer */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}
	
	private void selectItem(int position) {

		switch(position) {
		case 0:
			addAccount();
			break;
		case 1:
			doExchangeRates();
			break;
		case 2:
			doSettings();
			break;
		case 3:
			scanPrivateKey();
			break;
		default:
			break;
		}
		
		// update selected item and title, then close the drawer
		listView.setItemChecked(position, true);
		setTitle(navigationDrawerItems[position]);
		drawerLayout.closeDrawer(listView);
	}

	@Override
	public void setTitle(CharSequence title) { ; }

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		actionBarDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggle
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if(resultCode == Activity.RESULT_OK && requestCode == SCAN_URI)	{
			if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{
				String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
				doScanInput(strResult);
			}
        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_URI)	{
			;
		}
		else if(resultCode == Activity.RESULT_OK && requestCode == IMPORT_PRIVATE_KEY)	{
			if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{
				try	{
					final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
		        	String format = PrivateKeyFactory.getInstance().getFormat(strResult);
		        	if(format != null)	{
			        	if(!format.equals(PrivateKeyFactory.BIP38))	{
			        		ECKey key = PrivateKeyFactory.getInstance().getKey(format, strResult);
			        		if(key != null && key.hasPrivKey() && !PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString()))	{
				        		LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", "");
				        		/*
				        		 * if double encrypted, save encrypted in payload
				        		 */
				        		if(!PayloadFactory.getInstance().get().isDoubleEncrypted())	{
					        		legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
				        		}
				        		else	{
					        		String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
					        		String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getIterations());
					        		legacyAddress.setEncryptedKey(encrypted2);
				        		}
				        		PayloadFactory.getInstance().get().getLegacyAddresses().add(legacyAddress);
					        	Toast.makeText(MainActivity.this, key.toAddress(MainNetParams.get()).toString(), Toast.LENGTH_SHORT).show();
					    		PayloadFactory.getInstance(MainActivity.this).remoteSaveThread();
			        		}
			        		else	{
					        	Toast.makeText(MainActivity.this, getString(R.string.no_private_key), Toast.LENGTH_SHORT).show();
			        		}
			        	}
			        	else	{

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

			        	    		if(progress != null && progress.isShowing()) {
			        	    			progress.dismiss();
			        	    			progress = null;
			        	    		}
			        	    		progress = new ProgressDialog(MainActivity.this);
			        	    		progress.setTitle(R.string.app_name);
			        	    		progress.setMessage("Please wait...");
			        	    		progress.show();
			        	    		
			        	    		new Thread(new Runnable() {
			        	    			@Override
			        	    			public void run() {
			        	    				
			        	    				Looper.prepare();
			        	    				
					        	            try {
								        		ECKey key = PrivateKeyFactory.getInstance().getKey(PrivateKeyFactory.BIP38, strResult, new CharSequenceX(pw));
								        		if(key != null && key.hasPrivKey() && !PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString()))	{
									        		LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", "");
									        		/*
									        		 * if double encrypted, save encrypted in payload
									        		 */
									        		if(!PayloadFactory.getInstance().get().isDoubleEncrypted())	{
										        		legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
									        		}
									        		else	{
										        		String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
										        		String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getIterations());
										        		legacyAddress.setEncryptedKey(encrypted2);
									        		}
									        		PayloadFactory.getInstance().get().getLegacyAddresses().add(legacyAddress);
										        	Toast.makeText(MainActivity.this, key.toAddress(MainNetParams.get()).toString(), Toast.LENGTH_SHORT).show();
										    		PayloadFactory.getInstance(MainActivity.this).remoteSaveThread();
								        		}
								        		else	{
										        	Toast.makeText(MainActivity.this, "Null key returned (BIP38)", Toast.LENGTH_SHORT).show();
								        		}
					        	            }
					        	            catch(Exception e) {
									        	Toast.makeText(MainActivity.this, "Exception: Password error", Toast.LENGTH_SHORT).show();
					        	            }
					        				finally {
					        					if(progress != null && progress.isShowing()) {
					        						progress.dismiss();
					        						progress = null;
					        					}
					        				}
					        	            
			        	    				Looper.loop();

			        	    			}
			        	    		}).start();

			        	        }
			        	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			        	        public void onClick(DialogInterface dialog, int whichButton) {
			        	        	AppUtil.getInstance(MainActivity.this).restartApp();
			        	        }
			        	    }).show();
			        	}
		        	}
		        	else	{
			        	Toast.makeText(MainActivity.this, "Cannot recognize private key format", Toast.LENGTH_SHORT).show();
		        	}
				}
				catch(Exception e)	{
		        	Toast.makeText(MainActivity.this, "Error processing private key format", Toast.LENGTH_SHORT).show();
				}
			}
        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == IMPORT_PRIVATE_KEY)	{
			;
		}
        else {
        	;
        }

	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 

		if(keyCode == KeyEvent.KEYCODE_BACK) {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.ask_you_sure_exit).setCancelable(false);
			AlertDialog alert = builder.create();

			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {

		    		PayloadFactory.getInstance(MainActivity.this).remoteSaveThread();

					dialog.dismiss();

		    		AccessFactory.getInstance(MainActivity.this).setIsLoggedIn(false);

                    /*
					final Intent relaunch = new Intent(MainActivity.this, Exit.class)
					.addFlags(
							Intent.FLAG_ACTIVITY_NEW_TASK |
							Intent.FLAG_ACTIVITY_CLEAR_TASK |
							Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					startActivity(relaunch);
					*/

                    finish();

				}}); 

			alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}});

			alert.show();
        	
            return true;
        }
        else	{
        	;
        }

        return false;
    }

    private void updatePayloadThread(final CharSequenceX pw) {

		final Handler handler = new Handler();

		if(progress != null && progress.isShowing()) {
			progress.dismiss();
			progress = null;
		}
		progress = new ProgressDialog(MainActivity.this);
		progress.setTitle(R.string.app_name);
		progress.setMessage("Please wait...");
		progress.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Looper.prepare();

					if(HDPayloadBridge.getInstance(MainActivity.this).init(pw)) {
						
						PayloadFactory.getInstance().setTempPassword(pw);
						
						handler.post(new Runnable() {
							@Override
							public void run() {
								
								if(progress != null && progress.isShowing()) {
									progress.dismiss();
									progress = null;
								}
								
								if(!OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
									startService(new Intent(MainActivity.this, info.blockchain.wallet.service.WebSocketService.class));
								}

				        		Fragment fragment = new BalanceFragment();
				        		FragmentManager fragmentManager = getFragmentManager();
				        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
							}
						});

					}
					else {
			        	Toast.makeText(MainActivity.this, "Password error", Toast.LENGTH_SHORT).show();
						if(progress != null && progress.isShowing()) {
							progress.dismiss();
							progress = null;
						}
			        	AppUtil.getInstance(MainActivity.this).restartApp();
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

    private void exchangeRateThread() {

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				String response = null;
				try {
					response = WebUtil.getInstance().getURL(WebUtil.EXCHANGE_URL);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				ExchangeRateFactory.getInstance(MainActivity.this).setData(response);
				ExchangeRateFactory.getInstance(MainActivity.this).parse();

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

    private void addAccount()	{

        if(PayloadFactory.getInstance().get().isDoubleEncrypted()) {
        	
        	if(DoubleEncryptionFactory.getInstance().isActivated()) {

        		String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
	        			PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
	        			PayloadFactory.getInstance().get().getSharedKey(),
	    	        	PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
	        			PayloadFactory.getInstance().get().getIterations());

				try {
    				HD_Wallet hdw = HD_WalletFactory.getInstance(MainActivity.this).restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
    				HD_WalletFactory.getInstance(MainActivity.this).setWatchOnlyWallet(hdw);
	    			HDPayloadBridge.getInstance(MainActivity.this).addAccount();

	        		Fragment fragment = new BalanceFragment();
	        		FragmentManager fragmentManager = getFragmentManager();
	        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
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
	    		final EditText double_encrypt_password = new EditText(MainActivity.this);
	    		double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
	    		
	    		new AlertDialog.Builder(MainActivity.this)
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
	    	    				HD_Wallet hdw = HD_WalletFactory.getInstance(MainActivity.this).restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
	    	    				HD_WalletFactory.getInstance(MainActivity.this).setWatchOnlyWallet(hdw);
	    		    			HDPayloadBridge.getInstance(MainActivity.this).addAccount();

	    		        		Fragment fragment = new BalanceFragment();
	    		        		FragmentManager fragmentManager = getFragmentManager();
	    		        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
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
	                        Toast.makeText(MainActivity.this, R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
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

    		try {
    			
    			HDPayloadBridge.getInstance(MainActivity.this).addAccount();

        		Fragment fragment = new BalanceFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

    		}
    		catch(IOException ioe) {
    			ioe.printStackTrace();
            	Toast.makeText(this, "HD wallet error", Toast.LENGTH_SHORT).show();
    		}
    		catch(MnemonicException.MnemonicLengthException mle) {
    			mle.printStackTrace();
            	Toast.makeText(this, "HD wallet error", Toast.LENGTH_SHORT).show();
    		}

        }

    }

    private void doSettings()	{
		TimeOutUtil.getInstance().updatePin();
    	Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.SettingsActivity.class);
		startActivity(intent);
    }

    private void doExchangeRates()	{
		TimeOutUtil.getInstance().updatePin();
        if(hasZeroBlock())	{
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.phlint.android.zeroblock");
            startActivity(intent);
        }
        else	{
        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.phlint.android.zeroblock"));
        	startActivity(intent);
        }
    }

    private boolean hasZeroBlock()	{
    	PackageManager pm = this.getPackageManager();
    	try	{
    		pm.getPackageInfo("com.phlint.android.zeroblock", 0);
    		return true;
    	}
    	catch(NameNotFoundException nnfe)	{
    		return false;
    	}
    }

	private void scanPrivateKey() {
		
		if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
    		Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
    		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
    		startActivityForResult(intent, IMPORT_PRIVATE_KEY);
		}
		else {
			final EditText double_encrypt_password = new EditText(MainActivity.this);
			double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			
			new AlertDialog.Builder(MainActivity.this)
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

	    	    		Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
	    	    		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
	    	    		startActivityForResult(intent, IMPORT_PRIVATE_KEY);

		        	}
		        	else {
	                    Toast.makeText(MainActivity.this, R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
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

	private void scanURI() {
		Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
		startActivityForResult(intent, SCAN_URI);
	}

/*
    private void doMerchantDirectory()	{
    	if (!application.isGeoEnabled()) {
    		EnableGeo.displayGPSPrompt(this);
    	}
    	else {
    		//
    		// SecurityException fix
    		//
    		Security.removeProvider("SC");
    		TimeOutUtil.getInstance().updatePin();
        	Intent intent = new Intent(MainActivity.this, info.blockchain.merchant.directory.MapActivity.class);
    		startActivityForResult(intent, MERCHANT_ACTIVITY);
    	}
    }
*/

    private void doScanInput(String address)	{

        String btc_address = null;
        String btc_amount = null;

    	if(FormatsUtil.getInstance().isValidBitcoinAddress(address)) {
            btc_address = address;
        }
        else if(FormatsUtil.getInstance().isBitcoinUri(address)) {
            btc_address = FormatsUtil.getInstance().getBitcoinAddress(address);
            btc_amount = FormatsUtil.getInstance().getBitcoinAmount(address);
        }
        else {
    		Toast.makeText(MainActivity.this, "Not processed", Toast.LENGTH_SHORT).show();
    		return;
        }
        
		Fragment fragment = new SendFragment();
		Bundle args = new Bundle();
		args.putString("btc_address", btc_address);
		if(btc_amount != null) {
			try {
		    	NumberFormat btcFormat = NumberFormat.getInstance(locale);
		    	btcFormat.setMaximumFractionDigits(8);
		    	btcFormat.setMinimumFractionDigits(1);
		    	args.putString("btc_amount", btcFormat.format(Double.parseDouble(btc_amount) / 1e8));
			}
			catch(NumberFormatException nfe) {
				;
			}
		}
		fragment.setArguments(args);
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

    }

}
