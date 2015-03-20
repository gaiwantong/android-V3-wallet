package info.blockchain.wallet;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import net.sourceforge.zbar.Symbol;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;

public class Setup5Activity extends Activity	{

	private static final int PAIRING_QR = 2005;

	private ImageView ivImage = null;
	private TextView tvCaption = null;
	private TextView tvCommand = null;
	private ViewStub layoutTop = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    		setContentView(R.layout.setup);
    	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setTitle(R.string.app_name);
            
            ivImage = (ImageView)findViewById(R.id.image1);
            ivImage.setImageResource(R.drawable.graphic_pairdevice);
            tvCaption = (TextView)findViewById(R.id.caption);
            tvCaption.setText(R.string.setup5);
            tvCommand = (TextView)findViewById(R.id.command);
            tvCommand.setText(R.string.command2);

            tvCommand.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                	
                	launchQRScan();

                	return false;
                }
            });

            layoutTop = (ViewStub)findViewById(R.id.content);
            layoutTop.setLayoutResource(R.layout.include_setup0);
            View inflated = layoutTop.inflate();

    }

	private void launchQRScan() {
		Intent intent = new Intent(Setup5Activity.this, ZBarScannerActivity.class);
		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
		startActivityForResult(intent, PAIRING_QR);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(resultCode == Activity.RESULT_OK && requestCode == PAIRING_QR)	{
			if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{
	        	AppUtil.getInstance(Setup5Activity.this).wipeApp();
				String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
	        	Log.i("Pairing result", strResult);
	        	pairingThread(strResult);
			}
        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == PAIRING_QR)	{
        	AppUtil.getInstance(Setup5Activity.this).wipeApp();
		}
        else {
        	;
        }

	}

    private void pairingThread(final String data) {

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();

				if(PairingFactory.getInstance(Setup5Activity.this).handleQRCode(data))	{
		        	Toast.makeText(Setup5Activity.this, "Pairing OK", Toast.LENGTH_SHORT).show();
            		Intent intent = new Intent(Setup5Activity.this, Setup2Activity.class);
            		intent.putExtra("pairing", true);
            		startActivity(intent);
				}
				else	{
		        	Toast.makeText(Setup5Activity.this, "Pairing KO", Toast.LENGTH_SHORT).show();
		        	AppUtil.getInstance(Setup5Activity.this).wipeApp();
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

}
