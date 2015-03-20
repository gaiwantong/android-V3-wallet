package info.blockchain.wallet;

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

public class Setup3Activity extends Activity	{
	
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
            ivImage.setImageResource(R.drawable.graphic_pinentry);
            tvCaption = (TextView)findViewById(R.id.caption);
            tvCaption.setText(R.string.setup3);
            tvCommand = (TextView)findViewById(R.id.command);
            tvCommand.setVisibility(View.GONE);

            layoutTop = (ViewStub)findViewById(R.id.content);
            layoutTop.setLayoutResource(R.layout.include_setup0);
            View inflated = layoutTop.inflate();

    }

}
