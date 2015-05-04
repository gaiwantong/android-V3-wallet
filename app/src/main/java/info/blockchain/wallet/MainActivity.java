package info.blockchain.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import net.sourceforge.zbar.Symbol;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.SSLVerifierUtil;
import info.blockchain.wallet.util.WebUtil;

//import android.nfc.Tag;

public class MainActivity extends ActionBarActivity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback, BalanceFragment.Communicator {

    private static final int SCAN_URI = 2007;

    private static int MERCHANT_ACTIVITY = 1;

    private Locale locale = null;

    // toolbar
    private Toolbar toolbar = null;

    private boolean wasPaused = false;

    public static boolean drawerIsOpen = false;

	RecyclerView recyclerViewDrawer;
	RecyclerView.Adapter mAdapter;
	RecyclerView.LayoutManager mLayoutManager;
	DrawerLayout mDrawerLayout;

	ActionBarDrawerToggle mDrawerToggle;

	private ProgressDialog progress = null;

    private NfcAdapter mNfcAdapter = null;
    public static final String MIME_TEXT_PLAIN = "text/plain";
    private static final int MESSAGE_SENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        AppUtil.getInstance(MainActivity.this).setDEBUG(true);

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
                                    if(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
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

            validSSLThread();

            exchangeRateThread();

            boolean isPinValidated = false;
            Bundle extras = getIntent().getExtras();
            if(extras != null && extras.containsKey("verified"))	{
                isPinValidated = extras.getBoolean("verified");
            }

            if(PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
                PayloadFactory.getInstance().setTempPassword(new CharSequenceX(""));
                Intent intent = new Intent(MainActivity.this, Setup0Activity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else if(PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1) {
                Intent intent = new Intent(MainActivity.this, PinEntryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else if(isPinValidated) {
                AccessFactory.getInstance(MainActivity.this).setIsLoggedIn(true);

                AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
            else if(AccessFactory.getInstance(MainActivity.this).isLoggedIn() && !AppUtil.getInstance(MainActivity.this).isTimedOut()) {
                AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
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

		setToolbar();
		setNavigationDrawer();

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

        AppUtil.getInstance(MainActivity.this).deleteQR();

        if(AppUtil.getInstance(MainActivity.this).isTimedOut()) {
            Class c = null;
            if(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
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
            AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
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

        AppUtil.getInstance(MainActivity.this).deleteQR();

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
                doMerchantDirectory();
                return true;
            case R.id.action_qr:
                scanURI();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
//        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
//        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{
            String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
            doScanInput(strResult);
        }
        else if(resultCode == Activity.RESULT_CANCELED && requestCode == SCAN_URI)	{
            ;
        }
        else {
            ;
        }

    }

	int exitClicked = 0;
	int exitCooldown = 2;//seconds
	@Override
	public void onBackPressed()
	{
		if(drawerIsOpen){
			mDrawerLayout.closeDrawers();
		}else {

			exitClicked++;
			if (exitClicked == 2)
				finish();
			else
				Toast.makeText(this, getResources().getString(R.string.exit_confirm), Toast.LENGTH_SHORT).show();

			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j <= exitCooldown; j++) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (j >= exitCooldown) exitClicked = 0;
					}
				}
			}).start();
		}
	}

    private void updatePayloadThread(final CharSequenceX pw) {

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(MainActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(MainActivity.this.getResources().getString(R.string.please_wait));
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
                        Toast.makeText(MainActivity.this, R.string.invalid_password, Toast.LENGTH_SHORT).show();
                        if(progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }
                        AppUtil.getInstance(MainActivity.this).restartApp();
                    }

                    Looper.loop();

                }
                catch(JSONException | IOException | DecoderException | AddressFormatException
                        | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                        | MnemonicException.MnemonicWordException e) {
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
                ExchangeRateFactory.getInstance(MainActivity.this).updateFxPricesForEnabledCurrencies();

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

    private void validSSLThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if(!SSLVerifierUtil.getInstance().isValid()) {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    final String message = getString(R.string.ssl_hostname_invalid);

                    builder.setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.dialog_continue,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface d, int id) {
                                            d.dismiss();
                                        }
                                    });

                    builder.create().show();

                }

                if(!SSLVerifierUtil.getInstance().isPinned()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    final String message = getString(R.string.ssl_pinning_invalid);

                    builder.setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.dialog_continue,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface d, int id) {
                                            d.dismiss();
                                        }
                                    });

                    builder.create().show();
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

    /*
 * code for adding an account: to be brought back in an upcoming version
 *
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
                Toast.makeText(this, R.string.hd_error, Toast.LENGTH_SHORT).show();
            }
            catch(MnemonicException.MnemonicLengthException mle) {
                mle.printStackTrace();
                Toast.makeText(this, R.string.hd_error, Toast.LENGTH_SHORT).show();
            }

        }

    }
*/
    private void doSettings()	{
        AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
        Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.SettingsActivity.class);
        startActivity(intent);
    }

    private void doExchangeRates()	{
        AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
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

    private void scanURI() {
        Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_URI);
    }

    private void doScanInput(String address)	{

        String btc_address = null;
        String btc_amount = null;

        // check for poorly formed BIP21 URIs
        if(address.startsWith("bitcoin://") && address.length() > 10)	{
            address = "bitcoin:" + address.substring(10);
        }

        if(FormatsUtil.getInstance().isValidBitcoinAddress(address)) {
            btc_address = address;
        }
        else if(FormatsUtil.getInstance().isBitcoinUri(address)) {
            btc_address = FormatsUtil.getInstance().getBitcoinAddress(address);
            btc_amount = FormatsUtil.getInstance().getBitcoinAmount(address);
        }
        else {
            Toast.makeText(MainActivity.this, R.string.scan_not_recognized, Toast.LENGTH_SHORT).show();
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

	public void setToolbar() {

		toolbar = (Toolbar)findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_menu_white_24dp));
		setSupportActionBar(toolbar);
	}

	public void setNavigationDrawer() {

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		// Fix right margin to 56dp (portrait)
		View drawer = findViewById(R.id.scrimInsetsFrameLayout);
		ViewGroup.LayoutParams layoutParams = drawer.getLayoutParams();
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			layoutParams.width = displayMetrics.widthPixels - (56 * Math.round(displayMetrics.density));
		}
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			layoutParams.width = displayMetrics.widthPixels + (20 * Math.round(displayMetrics.density)) - displayMetrics.widthPixels / 2;
		}

		// Setup Drawer Icon
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

			public void onDrawerClosed(View view) {
				drawerIsOpen = false;
			}

			public void onDrawerOpened(View drawerView) {
				drawerIsOpen = true;
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		mDrawerToggle.syncState();

		// statusBar color behind navigation drawer
		TypedValue typedValueStatusBarColor = new TypedValue();
		MainActivity.this.getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValueStatusBarColor, true);
		final int colorStatusBar = typedValueStatusBarColor.data;
		mDrawerLayout.setStatusBarBackgroundColor(colorStatusBar);

		// Setup RecyclerView inside drawer
		recyclerViewDrawer = (RecyclerView) findViewById(R.id.drawer_recycler);
		recyclerViewDrawer.setHasFixedSize(true);
		recyclerViewDrawer.setLayoutManager(new LinearLayoutManager(MainActivity.this));

		ArrayList<DrawerItem> drawerItems = new ArrayList<>();
		final String[] drawerTitles = getResources().getStringArray(R.array.navigation_drawer_items);
		final TypedArray drawerIcons = getResources().obtainTypedArray(R.array.navigation_drawer_icons);
		for (int i = 0; i < drawerTitles.length; i++) {
			drawerItems.add(new DrawerItem(drawerTitles[i], drawerIcons.getDrawable(i)));
		}
		drawerIcons.recycle();
		DrawerAdapter adapterDrawer = new DrawerAdapter(drawerItems);
		recyclerViewDrawer.setAdapter(adapterDrawer);

		//TODO try to get status bar translucent (lollipop) - Fok weet
//		mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(android.R.color.transparent));
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

		recyclerViewDrawer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				for (int i = 0; i < drawerTitles.length; i++) {
					ImageView imageViewDrawerIcon = (ImageView) recyclerViewDrawer.getChildAt(i).findViewById(R.id.drawer_row_icon);
					if (Build.VERSION.SDK_INT > 15) {
						imageViewDrawerIcon.setImageAlpha(255);
					} else {
						imageViewDrawerIcon.setAlpha(255);
					}
				}

				// unregister listener (this is important)
				recyclerViewDrawer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});

		recyclerViewDrawer.addOnItemTouchListener(
				new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
					@Override public void onItemClick(View view, int position) {

						switch (position) {
							case 0:
								doMyAccounts();
								break;
							case 1:
								doExchangeRates();
								break;
							case 2:
								doSettings();
								break;
							case 3:
								doChangePin();
								break;
							case 4:
								doUnpairWallet();
								break;
						}

						mDrawerLayout.closeDrawers();

					}
				})
		);
	}

	private void doChangePin() {

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.alert_change_pin, null);
		dialogBuilder.setView(dialogView);

		final AlertDialog alertDialog = dialogBuilder.create();
		alertDialog.setCanceledOnTouchOutside(false);

		TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
		confirmCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
			}
		});

		TextView confirmChangePin = (TextView) dialogView.findViewById(R.id.confirm_unpair);
		confirmChangePin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				EditText pass = (EditText)dialogView.findViewById(R.id.password_confirm);

				if(pass.getText().toString().equals(PayloadFactory.getInstance(MainActivity.this).getTempPassword().toString())) {

					PrefsUtil.getInstance(MainActivity.this).removeValue(PrefsUtil.KEY_PIN_FAILS);
					PrefsUtil.getInstance(MainActivity.this).removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

					Intent intent = new Intent(MainActivity.this, PinEntryActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					finish();

					alertDialog.dismiss();
				}else{
					Toast.makeText(MainActivity.this,getResources().getString(R.string.invalid_password),Toast.LENGTH_SHORT).show();
				}
			}
		});

		alertDialog.show();
	}

	private void doUnpairWallet(){

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.alert_unpair_wallet, null);
		dialogBuilder.setView(dialogView);

		final AlertDialog alertDialog = dialogBuilder.create();
		alertDialog.setCanceledOnTouchOutside(false);

		TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
		confirmCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
			}
		});

		TextView confirmUnpair = (TextView) dialogView.findViewById(R.id.confirm_unpair);
		confirmUnpair.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PayloadFactory.getInstance().wipe();
				MultiAddrFactory.getInstance().wipe();
				PrefsUtil.getInstance(MainActivity.this).clear();

				AppUtil.getInstance(MainActivity.this).restartApp();

				alertDialog.dismiss();
			}
		});

		alertDialog.show();

	}

    private void doMerchantDirectory()	{

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            EnableGeo.displayGPSPrompt(this);
        }
        else {
            AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
            Intent intent = new Intent(MainActivity.this, info.blockchain.merchant.directory.MapActivity.class);
            startActivityForResult(intent, MERCHANT_ACTIVITY);
        }
    }

    private void doMyAccounts(){

        AppUtil.getInstance(MainActivity.this).updatePinEntryTime();
        Intent intent = new Intent(MainActivity.this, MyAccountsActivity.class);
        startActivity(intent);
    }

}
