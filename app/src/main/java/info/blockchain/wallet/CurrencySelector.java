package info.blockchain.wallet;

import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.PrefsUtil;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ArrayAdapter;
//import android.util.Log;

public class CurrencySelector extends Activity	{

	private SelectedSpinner spCurrencies = null;
	private Button bOK = null;
	private Button bCancel = null;
    private ArrayAdapter<CharSequence> spAdapter = null;
    private String[] currencies = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_currency);

//        String[] blockchain_currencies = CurrencyExchange.getInstance(this).getBlockchainCurrencies();
        String strFiatCode = PrefsUtil.getInstance(CurrencySelector.this).getValue(PrefsUtil.SELECTED_FIAT, "USD");
//        OtherCurrencyExchange.getInstance(this, blockchain_currencies, strFiatCode);

        currencies = ExchangeRateFactory.getInstance(this).getCurrencyLabels();
        spCurrencies = (SelectedSpinner)findViewById(R.id.receive_coins_default_currency);
        spAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies);
    	spCurrencies.setAdapter(spAdapter);

        bOK = (Button)findViewById(R.id.confirm);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	int currency = spCurrencies.getSelectedItemPosition();
	            PrefsUtil.getInstance(CurrencySelector.this).setValue(PrefsUtil.SELECTED_FIAT, currencies[currency].substring(currencies[currency].length() - 3));
            	finish();
            }
        });

        bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });

        initValues();

    }

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        else	{
        	;
        }

        return false;
    }

    private void initValues() {
        String strCurrency = PrefsUtil.getInstance(CurrencySelector.this).getValue(PrefsUtil.SELECTED_FIAT, "USD");
    	int sel = -1;
    	for(int i = 0; i < currencies.length; i++) {
    		if(currencies[i].endsWith(strCurrency)) {
    	        spCurrencies.setSelection(i);
    	        sel = i;
    	        break;
    		}
    	}
    	if(sel == -1) {
	        spCurrencies.setSelection(0);
    	}

    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
	    Rect dialogBounds = new Rect();
	    getWindow().getDecorView().getHitRect(dialogBounds);

	    if(!dialogBounds.contains((int) event.getX(), (int) event.getY()) && event.getAction() == MotionEvent.ACTION_DOWN) {
	    	return false;
	    }
	    else {
		    return super.dispatchTouchEvent(event);
	    }
	}

}
