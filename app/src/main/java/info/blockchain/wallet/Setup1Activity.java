package info.blockchain.wallet;

import java.io.IOException;

import org.apache.commons.codec.DecoderException;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
//import android.util.Log;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PasswordUtil;
import info.blockchain.wallet.util.PrefsUtil;

public class Setup1Activity extends Activity	{
	
	private ImageView ivImage = null;
	private TextView tvCaption = null;
	private TextView tvCommand = null;
	private ViewStub layoutTop = null;

	private EditText edEmail = null;
	private EditText edPassword1 = null;
	private EditText edPassword2 = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    		setContentView(R.layout.setup);
    	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setTitle(R.string.app_name);
            
            ivImage = (ImageView)findViewById(R.id.image1);
            ivImage.setImageResource(R.drawable.graphic_email);
            tvCaption = (TextView)findViewById(R.id.caption);
            tvCaption.setText(R.string.setup1);
            tvCommand = (TextView)findViewById(R.id.command);
            tvCommand.setVisibility(View.GONE);

            layoutTop = (ViewStub)findViewById(R.id.content);
            layoutTop.setLayoutResource(R.layout.include_setup1);
            View inflated = layoutTop.inflate();
            
            edEmail = (EditText)inflated.findViewById(R.id.email);
            edPassword1 = (EditText)inflated.findViewById(R.id.pw1);
            edPassword2 = (EditText)inflated.findViewById(R.id.pw2);
            edPassword2.setOnEditorActionListener(new OnEditorActionListener() {
    		    @Override
    		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    		        if(actionId == EditorInfo.IME_ACTION_DONE) {

    		        	final String em = edEmail.getText().toString();
    		        	final String pw1 = edPassword1.getText().toString();
    		        	final String pw2 = edPassword2.getText().toString();
    		        	
    		        	if(em == null || !FormatsUtil.getInstance().isValidEmailAddress(em)) {
				        	Toast.makeText(Setup1Activity.this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
    		        	}
    		        	else if(pw1 == null || pw1.length() < 11 || pw1.length() > 255 || pw2 == null || pw2.length() < 11 || pw2.length() > 255) {
				        	Toast.makeText(Setup1Activity.this, getString(R.string.invalid_password), Toast.LENGTH_SHORT).show();
    		        	}
    		        	else if(!pw1.equals(pw2)) {
				        	Toast.makeText(Setup1Activity.this, getString(R.string.password_mismatch_error), Toast.LENGTH_SHORT).show();
    		        	}
    		        	else {

//        		        	if(PasswordUtil.getInstance().isWeak(pw1)) {
//
//    				    		new AlertDialog.Builder(Setup1Activity.this)
//    				    	    .setTitle(R.string.app_name)
//    							.setMessage(R.string.weak_password)
//    				    	    .setCancelable(false)
//    				    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//    				    	        public void onClick(DialogInterface dialog, int whichButton) {
//    				    	        	edPassword1.setText("");
//    				    	        	edPassword2.setText("");
//    				    	        }
//    				    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//    				    	        public void onClick(DialogInterface dialog, int whichButton) {
//    		        		        	Intent intent = new Intent(Setup1Activity.this, Setup2Activity.class);
//    		        		        	intent.putExtra("_email", em);
//    		        		        	intent.putExtra("_pw", pw1);
//    		        		    		startActivity(intent);
//    				    	        }
//    				    	    }).show();
//
//        		        	}
//        		        	else {
//	        		        	Intent intent = new Intent(Setup1Activity.this, Setup2Activity.class);
//	        		        	intent.putExtra("_email", em);
//	        		        	intent.putExtra("_pw", pw1);
//	        		    		startActivity(intent);
//        		        	}

    		        	}
    		        	
    		        }

    		        return false;
    		    }
    		});

    }

}
