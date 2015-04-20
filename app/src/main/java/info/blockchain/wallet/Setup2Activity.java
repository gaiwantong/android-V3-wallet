package info.blockchain.wallet;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.crypto.MnemonicException;

import java.io.IOException;

import info.blockchain.wallet.pairing.PairingFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.PrefsUtil;

//import android.util.Log;

public class Setup2Activity extends Activity	{
	
	private ImageView ivImage = null;
	private TextView tvCaption = null;
	private TextView tvCommand = null;
	private EditText edAccountName = null;
	private ViewStub layoutTop = null;
	private Button btContinue = null;
	
	private SelectedSpinner spCurrencies = null;
    private ArrayAdapter<CharSequence> spCurrenciesAdapter = null;
    private String[] currencyLabels = null;
    
    private String strEmail = null;
    private String strPassword = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTitle(R.string.app_name);

        Bundle extras = getIntent().getExtras();

        boolean isPairing = false;
        if(extras != null && extras.containsKey(PairingFactory.KEY_EXTRA_IS_PAIRING))	{
            isPairing = extras.getBoolean(PairingFactory.KEY_EXTRA_IS_PAIRING);
        }

        if(extras != null && extras.containsKey("_email"))	{
            strEmail = extras.getString("_email");
        }
        if(extras != null && extras.containsKey("_pw"))	{
            strPassword = extras.getString("_pw");
        }

        ivImage = (ImageView)findViewById(R.id.image1);
        ivImage.setImageResource(R.drawable.graphic_accounts);
        tvCaption = (TextView)findViewById(R.id.caption);
        tvCaption.setText(R.string.setup2);
        tvCommand = (TextView)findViewById(R.id.command);
        tvCommand.setVisibility(View.GONE);

        layoutTop = (ViewStub)findViewById(R.id.content);
        layoutTop.setLayoutResource(R.layout.include_setup2);
        View inflated = layoutTop.inflate();

        edAccountName = (EditText)inflated.findViewById(R.id.account_name);
        /*
        if(isPairing && PayloadFactory.getInstance(Setup2Activity.this).get().getHdWallets().size() > 0) {
            edAccountName.setVisibility(View.GONE);
        }
        */

        final boolean _isPairing = isPairing;
        btContinue = (Button)inflated.findViewById(R.id.confirm);
        btContinue.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                String strAccountName = edAccountName.getText().toString();
                PrefsUtil.getInstance(Setup2Activity.this).setValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, strAccountName);

                if(_isPairing) {
                    AppUtil.getInstance(Setup2Activity.this).restartApp();
                }
                else {
                    int currency = spCurrencies.getSelectedItemPosition();
                    PrefsUtil.getInstance(Setup2Activity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, currencyLabels[currency].substring(currencyLabels[currency].length() - 3));

                    //
                    // save drawerEmail here
                    //

                    // create wallet
                    // restart
                    try {
                        HDPayloadBridge.getInstance(Setup2Activity.this).createHDWallet(12, "", 1);
                        PayloadFactory.getInstance().setTempPassword(new CharSequenceX(strPassword));

                        PayloadFactory.getInstance(Setup2Activity.this).remoteSaveThread();

                        AppUtil.getInstance(Setup2Activity.this).restartApp();
                    }
                    catch(IOException ioe) {
                        Toast.makeText(Setup2Activity.this, R.string.hd_error, Toast.LENGTH_SHORT).show();
                        AppUtil.getInstance(Setup2Activity.this).wipeApp();
                    }
                    catch(MnemonicException.MnemonicLengthException mle) {
                        Toast.makeText(Setup2Activity.this, R.string.hd_error, Toast.LENGTH_SHORT).show();
                        AppUtil.getInstance(Setup2Activity.this).wipeApp();
                    }

                }

                return false;
            }
        });

        spCurrencies = (SelectedSpinner)findViewById(R.id.currency);

        if(!isPairing) {
            currencyLabels = ExchangeRateFactory.getInstance(this).getCurrencyLabels();
            spCurrenciesAdapter = new ArrayAdapter(this, R.layout.spinner_item, currencyLabels);
            spCurrenciesAdapter.setDropDownViewResource(R.layout.spinner_item2);
            spCurrencies.setAdapter(spCurrenciesAdapter);
        }
        else {
            spCurrencies.setVisibility(View.GONE);
        }
    }
}
