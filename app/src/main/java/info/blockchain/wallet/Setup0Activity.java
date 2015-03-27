package info.blockchain.wallet;

import info.blockchain.wallet.util.ConnectivityStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
//import android.util.Log;

public class Setup0Activity extends Activity	{
	
	private Button btCreate = null;
	private TextView btLogin = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    		setContentView(R.layout.setup0);
    	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setTitle(R.string.app_name);
            
            btCreate = (Button)findViewById(R.id.create);
            btCreate.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

            		Intent intent = new Intent(Setup0Activity.this, Setup00Activity.class);
            		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra("starting_fragment",0);
            		startActivity(intent);

                	return false;
                }
            });
            
            btLogin = (TextView)findViewById(R.id.login);
            btLogin.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

            		Intent intent = new Intent(Setup0Activity.this, Setup00Activity.class);
            		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra("starting_fragment",1);
            		startActivity(intent);

                	return false;
                }
            });
            
    		if(!ConnectivityStatus.hasConnectivity(this)) {
    	    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	        
    	        final String message = getString(R.string.check_connectivity_exit);
    	 
    	        builder.setMessage(message)
    	        	.setCancelable(false)
    	            .setPositiveButton(R.string.dialog_continue,
    	                new DialogInterface.OnClickListener() {
    	                    public void onClick(DialogInterface d, int id) {
    	                        d.dismiss();
    							Intent intent = new Intent(Setup0Activity.this, Setup0Activity.class);
    							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    							startActivity(intent);
    	                    }
    	            });

    	        builder.create().show();
    		}

    }
}
