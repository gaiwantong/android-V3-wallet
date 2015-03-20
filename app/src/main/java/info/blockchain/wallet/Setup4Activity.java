package info.blockchain.wallet;

import info.blockchain.wallet.util.OSUtil;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

public class Setup4Activity extends Activity	{
	
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
            ivImage.setImageResource(R.drawable.graphic_emailconf);
            tvCaption = (TextView)findViewById(R.id.caption);
            tvCaption.setText(R.string.setup4);
            tvCommand = (TextView)findViewById(R.id.command);
            tvCommand.setText(R.string.command1);
            
            ivImage.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                	
                	String strPackageName = "com.google.android.gm";
                	
                	if(OSUtil.getInstance(Setup4Activity.this).hasPackage(strPackageName)) {
                		Intent intent = getPackageManager().getLaunchIntentForPackage(strPackageName);
                		startActivity(intent);
                	}

                	return false;
                }
            });

            tvCommand.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                	//
                	// resend e-mail ???
                	//

                	return false;
                }
            });

            layoutTop = (ViewStub)findViewById(R.id.content);
            layoutTop.setLayoutResource(R.layout.include_setup0);
            View inflated = layoutTop.inflate();

    }

}
