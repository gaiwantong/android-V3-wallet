package info.blockchain.wallet.ui;

import com.google.zxing.client.android.CaptureActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.helpers.EnableGeo;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.viewModel.MainViewModel;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements BalanceFragment.Communicator, MainViewModel.DataListener {

    public static final int SCAN_URI = 2007;
    private static final int REQUEST_BACKUP = 2225;
    public static boolean drawerIsOpen = false;
    public static Fragment currentFragment;
    private static int MERCHANT_ACTIVITY = 1;
    private static String SUPPORT_URI = "http://support.blockchain.com/";

    // toolbar
    private Toolbar toolbar = null;
    private AlertDialog rootedAlertDialog;

    private MainViewModel mainViewModel;//MainActivity logic
    private ActivityMainBinding binding;
    private ProgressDialog fetchTransactionsProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainViewModel = new MainViewModel(this, this);
        binding.setViewModel(mainViewModel);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(MainActivity.this).stopLogoutTimer();
        AppUtil.getInstance(MainActivity.this).deleteQR();

        if (!OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            startService(new Intent(MainActivity.this, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (rootedAlertDialog != null) {
            rootedAlertDialog.dismiss();
            rootedAlertDialog = null;
        }

        if (!OSUtil.getInstance(MainActivity.this).isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            stopService(new Intent(MainActivity.this, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                binding.drawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
                }else{
                    startScanActivity();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);

            if(currentFragment instanceof SendFragment){
                currentFragment.onActivityResult(requestCode, resultCode, data);
            }else{
                doScanInput(strResult);
            }

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetNavigationDrawer();
        }
    }

    @Override
    public void onBackPressed() {

        if (drawerIsOpen) {
            binding.drawerLayout.closeDrawers();
        } else if (currentFragment instanceof BalanceFragment) {

            mainViewModel.onBackPressed();

        } else if (currentFragment instanceof ReceiveFragment && ((ReceiveFragment) currentFragment).customKeypad != null && ((ReceiveFragment) currentFragment).customKeypad.isVisible()) {
            ((ReceiveFragment) currentFragment).onKeypadClose();

        } else if (currentFragment instanceof SendFragment && ((SendFragment) currentFragment).customKeypad != null && ((SendFragment) currentFragment).customKeypad.isVisible()) {
            ((SendFragment) currentFragment).onKeypadClose();

        } else {
            Fragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    private void startScanActivity() {
        if (!AppUtil.getInstance(MainActivity.this).isCameraOpen()) {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_URI);
        } else {
            ToastCustom.makeText(MainActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void doScanInput(String strResult) {

        Fragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString("scan_data", strResult);
        fragment.setArguments(args);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public void setNavigationDrawerToggleEnabled(boolean enabled) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            toolbar.getChildAt(i).setEnabled(enabled);
            toolbar.getChildAt(i).setClickable(enabled);
        }

        if (enabled)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        else
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void selectDrawerItem(MenuItem menuItem) {

        switch(menuItem.getItemId()) {
            case R.id.nav_backup:
                startActivityForResult(new Intent(MainActivity.this, BackupWalletActivity.class), REQUEST_BACKUP);
                break;
            case R.id.nav_addresses:
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                break;
            case R.id.nav_upgrade:
                startActivity(new Intent(MainActivity.this, UpgradeWalletActivity.class));
                break;
            case R.id.nav_map:
                startMerchantActivity();
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_support:
//                startActivity(new Intent(MainActivity.this, SupportActivity.class));
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URI)));
                break;
            case R.id.nav_logout:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.unpair_wallet)
                        .setMessage(R.string.ask_you_sure_unpair)
                        .setPositiveButton(R.string.unpair, (dialog, which) -> {
                            mainViewModel.unpair();
                        })
                        .setNegativeButton(R.string.cancel,null)
                        .show();
                break;
        }
        binding.drawerLayout.closeDrawers();
    }

    @Override
    public void resetNavigationDrawer() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_menu_white_24dp));
        setSupportActionBar(toolbar);

        View headerLayout = binding.nvView.getHeaderView(0);//TODO - future use for account selection
        MenuItem backUpMenuItem = binding.nvView.getMenu().findItem(R.id.nav_backup);
        MenuItem upgradeMenuItem = binding.nvView.getMenu().findItem(R.id.nav_upgrade);

        if (AppUtil.getInstance(this).isNotUpgraded()) {
            //Legacy
            upgradeMenuItem.setVisible(true);
            backUpMenuItem.setVisible(false);
        } else {
            //HD
            upgradeMenuItem.setVisible(false);
            backUpMenuItem.setVisible(true);
        }

        MenuItem backUpView = binding.nvView.getMenu().findItem(R.id.nav_backup);
        Drawable drawable = backUpView.getIcon();
        drawable.mutate();
        if (PayloadFactory.getInstance().get() != null &&
                PayloadFactory.getInstance().get().getHdWallet() != null &&
                !PayloadFactory.getInstance().get().getHdWallet().isMnemonicVerified()) {
            //Not backed up
            drawable.setColorFilter(getResources().getColor(R.color.blockchain_send_red), PorterDuff.Mode.SRC_ATOP);
            startActivityForResult(new Intent(MainActivity.this, BackupWalletActivity.class), REQUEST_BACKUP);
        } else {
            //Backed up
            drawable.setColorFilter(getResources().getColor(R.color.alert_green), PorterDuff.Mode.SRC_ATOP);
        }

        binding.nvView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });
    }

    private void startMerchantActivity() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestLocationPermissionFromActivity(binding.getRoot(), this);
        }else{
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!enabled) {
                EnableGeo.displayGPSPrompt(this);
            } else {
                Intent intent = new Intent(MainActivity.this, info.blockchain.merchant.directory.MapActivity.class);
                startActivityForResult(intent, MERCHANT_ACTIVITY);
            }
        }
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(MainActivity.this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity();
            } else {
                // Permission request was denied.
            }
        } if (requestCode == PermissionUtil.PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startMerchantActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onRooted() {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.device_rooted))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            (d, id) -> {
                                d.dismiss();
                            });
            rootedAlertDialog = builder.create();
            rootedAlertDialog.show();
    }

    @Override
    public void onConnectivityFail() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String message = getString(R.string.check_connectivity_exit);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue,
                        (d, id) -> {
                            d.dismiss();
                            Class c = null;
                            if (PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
                                c = LandingActivity.class;
                            } else {
                                c = PinEntryActivity.class;
                            }
                            startSingleActivity(c);
                        });

        builder.create().show();
    }

    @Override
    public void onNoGUID() {
        startSingleActivity(LandingActivity.class);
    }

    @Override
    public void onRequestPin() {
        startSingleActivity(PinEntryActivity.class);
    }

    @Override
    public void onCorruptPayload() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(MainActivity.this.getString(R.string.not_sane_error))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, whichButton) -> {
                    AppUtil.getInstance(MainActivity.this).clearCredentialsAndRestart();
                    AppUtil.getInstance(MainActivity.this).restartApp();
                }).show();
    }

    @Override
    public void onRequestUpgrade() {
        startActivity(new Intent(MainActivity.this, UpgradeWalletActivity.class));
    }

    @Override
    public void onFetchTransactionsStart() {

        fetchTransactionsProgress = new ProgressDialog(this);
        fetchTransactionsProgress.setCancelable(false);
        fetchTransactionsProgress.setTitle(R.string.app_name);
        fetchTransactionsProgress.setMessage(getString(R.string.please_wait));
        fetchTransactionsProgress.show();
    }

    @Override
    public void onFetchTransactionCompleted() {

        if (fetchTransactionsProgress != null && fetchTransactionsProgress.isShowing()) {
            fetchTransactionsProgress.dismiss();
        }
    }

    @Override
    public void onScanInput(String strUri) {
        doScanInput(strUri);
    }

    @Override
    public void onStartBalanceFragment() {
        if(!isFinishing())    {
            Handler handler = new Handler();
            handler.post(() -> {
                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commitAllowingStateLoss();
            });
        }
    }

    @Override
    public void onExitConfirmToast() {
        ToastCustom.makeText(this, getString(R.string.exit_confirm), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
    }
}
