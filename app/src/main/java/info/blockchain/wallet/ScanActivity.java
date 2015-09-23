package info.blockchain.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.regex.Pattern;

import eu.livotov.labs.android.camview.ScannerLiveView;
import eu.livotov.labs.android.camview.camera.CameraController;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.ToastCustom;
import piuk.blockchain.android.R;

public class ScanActivity extends ActionBarActivity{

    public static final String SCAN_RESULT = "SCAN_RESULT";
    public static final String ERROR_INFO = "ERROR_INFO";

    public static final String SCAN_ACTION = "SCAN_ACTION";
    public static final int SCAN_PAIR = 0;
    public static final int SCAN_IMPORT = 1;
    public static final int SCAN_URI = 2;

    private boolean hasFlashLight = false;

    private ScannerLiveView camera;
    private CameraController controller;
    private boolean flashStatus;

    long start = 0;
    long end = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.scan_qr));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        final int action = getIntent().getIntExtra(SCAN_ACTION, -1);

        camera = (ScannerLiveView) findViewById(R.id.camview);
        camera.setHudVisible(false);
        camera.setPlaySound(false);
        camera.setScannerViewEventListener(new ScannerLiveView.ScannerViewEventListener() {
            @Override
            public void onScannerStarted(ScannerLiveView scanner) {
            }

            @Override
            public void onScannerStopped(ScannerLiveView scanner) {
            }

            @Override
            public void onScannerError(Throwable err) {
            }

            @Override
            public void onCodeScanned(String data) {
                start = System.currentTimeMillis();
                switch (action) {
                    case SCAN_PAIR:
                        boolean isValidPairingQR = (data.split("\\|", Pattern.LITERAL).length == 3);
                        if (isValidPairingQR)
                            scan(data);
                        else
                            ToastCustom.makeText(ScanActivity.this,"Invalid QR",ToastCustom.LENGTH_SHORT,ToastCustom.TYPE_ERROR);
                        break;

                    case SCAN_IMPORT:
                        String privKey;
                        try {
                            privKey = PrivateKeyFactory.getInstance().getFormat(data);
                        } catch (Exception e) {
                            privKey = null;
                        }
                        boolean isValidPrivKey = privKey != null ? true : false;
                        if (isValidPrivKey)
                            scan(data);
                        else
                            ToastCustom.makeText(ScanActivity.this, "Invalid QR", ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        break;

                    case SCAN_URI:
                        boolean isValidBitcoinUri = FormatsUtil.getInstance().isBitcoinUri(data);
                        boolean isValidBitcoinAddress = FormatsUtil.getInstance().isValidBitcoinAddress(data);
                        if (isValidBitcoinUri || isValidBitcoinAddress)
                            scan(data);
                        else
                            ToastCustom.makeText(ScanActivity.this, "Invalid Address.", ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        break;
                }
            }
        });

        hasFlashLight = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void scan(String data){
        camera.stopScanner();

        Intent dataIntent = new Intent();
        dataIntent.putExtra(SCAN_RESULT, data);
        setResult(Activity.RESULT_OK, dataIntent);
        end = System.currentTimeMillis();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.startScanner();
        AppUtil.getInstance(this).setIsBackgrounded(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        camera.stopScanner();
        super.onPause();
        AppUtil.getInstance(this).setIsBackgrounded(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        if(hasFlashLight) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.scan_actions, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_flash_light:
                try{doFlashLight();}catch (Exception e){}
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doFlashLight(){
        flashStatus = !flashStatus;
        camera.getCamera().getController().switchFlashlight(flashStatus);
    }
}