package info.blockchain.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.regex.Pattern;

import eu.livotov.zxscan.ScannerView;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import piuk.blockchain.android.R;

public class ScanActivity extends ActionBarActivity{

    public static final String SCAN_RESULT = "SCAN_RESULT";
    public static final String ERROR_INFO = "ERROR_INFO";

    private ScannerView scanner;
    private boolean hasFlashLight = false;
    private boolean flashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.scan_qr));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        scanner = (ScannerView) findViewById(R.id.scanner);
        scanner.setHudVisible(false);
        scanner.setScannerViewEventListener(new ScannerView.ScannerViewEventListener() {
            @Override
            public void onScannerReady() {

            }

            @Override
            public void onScannerFailure(int i) {
                scanner.stopScanner();
                Intent dataIntent = new Intent();
                dataIntent.putExtra(ERROR_INFO, "Camera unavailable - ERROR CODE: "+i);
                setResult(Activity.RESULT_CANCELED, dataIntent);
                finish();
            }

            public boolean onCodeScanned(final String data) {

                //ZXScanLib v2.0.1: Currently scanner tries to recognize all supported barcodes - So we'll check for valid QR
                String privKey;
                try {privKey = PrivateKeyFactory.getInstance().getFormat(data);}catch (Exception e){privKey = null;}

                boolean isValidPrivKey = privKey!=null ? true : false;
                boolean isValidBitcoinUri = FormatsUtil.getInstance().isBitcoinUri(data);
                boolean isValidPairingQR = (data.split("\\|", Pattern.LITERAL).length == 3);
                boolean isValidBitcoinAddress = FormatsUtil.getInstance().isValidBitcoinAddress(data);

                if(isValidBitcoinUri || isValidPrivKey || isValidPairingQR || isValidBitcoinAddress) {

                    scanner.stopScanner();

                    Intent dataIntent = new Intent();
                    dataIntent.putExtra(SCAN_RESULT, data);
                    setResult(Activity.RESULT_OK, dataIntent);
                    finish();
                }
                return true;
            }
        });
        scanner.startScanner();

        hasFlashLight = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(ScanActivity.this).updatePinEntryTime();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stopScanner();
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
        if(!flashOn) {
            Camera.Parameters para = scanner.getCamera().getCamera().getParameters();
            para.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            scanner.getCamera().getCamera().setParameters(para);

            flashOn = true;
        }else{
            Camera.Parameters para = scanner.getCamera().getCamera().getParameters();
            para.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            scanner.getCamera().getCamera().setParameters(para);
            flashOn = false;
        }
    }
}