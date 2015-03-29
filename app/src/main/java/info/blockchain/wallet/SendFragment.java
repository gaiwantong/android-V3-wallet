package info.blockchain.wallet;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.ParseException;

import android.content.DialogInterface;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
import android.graphics.Typeface;
import android.util.Log;

import org.apache.commons.codec.DecoderException;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.MnemonicException;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class SendFragment extends Fragment {
	
	private Locale locale = null;

	private ImageView ivReceive = null;
	private ImageView ivHome = null;
	private ImageView ivSend = null;

    private LinearLayout layoutReceive = null;
    private LinearLayout layoutHome = null;
    private LinearLayout layoutSend = null;

    private LinearLayout layoutReceiveIcon = null;
    private LinearLayout layoutHomeIcon = null;
    private LinearLayout layoutSendIcon = null;
    
    private EditText edDestination = null;
    private EditText edAmount1 = null;
	private TextView tvCurrency1 = null;
	private TextView tvAmount2 = null;
	private TextView tvFiat2 = null;
	private Spinner spAccounts = null;
	private Button btSend = null;
	private TextView tvMax = null;
	
	private int currentSelectedAccount = 0;
	private String currentSelectedAddress = null;
	private static int currentSelectedItem = 0;

    private int hdAccountsIdx = 0;
    private List<String> _accounts = null;
	private ImportedAccount iAccount = null;
	private List<LegacyAddress> legacy = null;
    private List<Account> accounts = null;

	private String strBTC = "BTC";
	private String strFiat = null;
	private boolean isBTC = true;
	private double btc_fx = 319.13;
	
	private BigInteger bFee = Utils.toNanoCoins("0.0001");
	
	private class PendingSpend {
		boolean isHD;
		String amount;
		BigInteger bamount;
		BigInteger bfee;
		String destination;
		String sending_from;
		String btc_amount;
        String fiat_amount;
        String btc_units;
	};
	
	private PendingSpend pendingSpend = new PendingSpend();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_send, container, false);
		
		locale = Locale.getDefault();

		((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.send);

        layoutReceive = (LinearLayout)rootView.findViewById(R.id.iconsReceive2);
        layoutHome = (LinearLayout)rootView.findViewById(R.id.iconsHome2);
        layoutSend = (LinearLayout)rootView.findViewById(R.id.iconsSend2);

        ivReceive = (ImageView)rootView.findViewById(R.id.view_receive);
        ivHome = (ImageView)rootView.findViewById(R.id.view_home);
        ivSend = (ImageView)rootView.findViewById(R.id.view_send);

        layoutReceiveIcon = (LinearLayout)rootView.findViewById(R.id.view_receive1);
        layoutHomeIcon = (LinearLayout)rootView.findViewById(R.id.view_home1);
        layoutSendIcon = (LinearLayout)rootView.findViewById(R.id.view_send1);
        
        layoutReceiveIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	
        		Fragment fragment = new ReceiveFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        layoutHomeIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	
        		Fragment fragment = new BalanceFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        layoutSendIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));

            	return false;
            }
        });

        layoutReceive.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));

        		Fragment fragment = new ReceiveFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        layoutHome.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));

        		Fragment fragment = new BalanceFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        layoutSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));

            	return false;
            }
        });

        ivReceive.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));

            	return false;
            }
        });

        ivHome.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));

            	return false;
            }
        });

        ivSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

            	return false;
            }
        });

    	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
    	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
    	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));

        btSend = ((Button)rootView.findViewById(R.id.send));
        btSend.setVisibility(View.INVISIBLE);
        btSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
            	if(isBTC) {
            		pendingSpend.btc_amount = edAmount1.getText().toString();
            		pendingSpend.fiat_amount = tvAmount2.getText().toString();
            	}
            	else {
            		pendingSpend.fiat_amount = edAmount1.getText().toString();
            		pendingSpend.btc_amount = tvAmount2.getText().toString();
            	}

                pendingSpend.btc_units = strBTC;

                btSend.setVisibility(View.INVISIBLE);
	        	
        		final FragmentManager fragmentManager = getFragmentManager();
        		final Fragment fragment = new SendFragment2();
        		final Bundle args = new Bundle();

				if(pendingSpend.isHD) {
			    	if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
		        		args.putBoolean("hd", true);
		        		args.putInt("account", currentSelectedAccount);
		        		args.putString("destination", pendingSpend.destination);
		        		args.putString("bamount", pendingSpend.bamount.toString());
		        		args.putString("bfee", pendingSpend.bfee.toString());
		        		args.putString("sending_from", pendingSpend.sending_from);
		        		args.putString("btc_amount", pendingSpend.btc_amount);
		        		args.putString("fiat_amount", pendingSpend.fiat_amount);
                        args.putString("btc_units", pendingSpend.btc_units);
		        		fragment.setArguments(args);
		        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			    	}
			    	else if(DoubleEncryptionFactory.getInstance().isActivated()) {
		        		args.putBoolean("hd", true);
		        		args.putInt("account", currentSelectedAccount);
		        		args.putString("destination", pendingSpend.destination);
		        		args.putString("bamount", pendingSpend.bamount.toString());
		        		args.putString("bfee", pendingSpend.bfee.toString());
		        		args.putString("sending_from", pendingSpend.sending_from);
		        		args.putString("btc_amount", pendingSpend.btc_amount);
		        		args.putString("fiat_amount", pendingSpend.fiat_amount);
                        args.putString("btc_units", pendingSpend.btc_units);
		        		fragment.setArguments(args);
		        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			    	}
			    	else {

			    		final EditText password = new EditText(getActivity());
		        		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			    		
			    		new AlertDialog.Builder(getActivity())
			    	    .setTitle(R.string.app_name)
			    	    .setMessage(R.string.enter_double_encryption_pw)
			    	    .setView(password)
			    	    .setCancelable(false)
			    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			    	        public void onClick(DialogInterface dialog, int whichButton) {

			    	        	final String pw = password.getText().toString();
			    	        	
			    	        	PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));
			    	        	
			    	        	if(DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword(), PayloadFactory.getInstance().get().getIterations())) {
			    	        		
			    	        		String encrypted_hex = PayloadFactory.getInstance().get().getHdWallet().getSeedHex();
			    	        		String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
			    	        				encrypted_hex,
			    	        				PayloadFactory.getInstance().get().getSharedKey(),
			    	        				pw,
			    	        				PayloadFactory.getInstance().get().getIterations());
			    	        		
			    	    			try {
			    	    				HD_Wallet hdw = HD_WalletFactory.getInstance(getActivity()).restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
			    	    				HD_WalletFactory.getInstance(getActivity()).setWatchOnlyWallet(hdw);
			    	    			}
			    	            	catch(IOException ioe) {
			    	            		ioe.printStackTrace();
			    	            	}
			    	            	catch(DecoderException de) {
			    	            		de.printStackTrace();
			    	            	}
			    	            	catch(AddressFormatException afe) {
			    	            		afe.printStackTrace();
			    	            	}
			    	            	catch(MnemonicException.MnemonicLengthException mle) {
			    	            		mle.printStackTrace();
			    	            	}
			    	            	catch(MnemonicException.MnemonicChecksumException mce) {
			    	            		mce.printStackTrace();
			    	            	}
			    	            	catch(MnemonicException.MnemonicWordException mwe) {
			    	            		mwe.printStackTrace();
			    	            	}
			    	    			finally {
			    	    				;
			    	    			}

					        		args.putBoolean("hd", true);
					        		args.putInt("account", currentSelectedAccount);
					        		args.putString("destination", pendingSpend.destination);
					        		args.putString("bamount", pendingSpend.bamount.toString());
					        		args.putString("bfee", pendingSpend.bfee.toString());
					        		args.putString("sending_from", pendingSpend.sending_from);
					        		args.putString("btc_amount", pendingSpend.btc_amount);
					        		args.putString("fiat_amount", pendingSpend.fiat_amount);
                                    args.putString("btc_units", pendingSpend.btc_units);
	            	        		fragment.setArguments(args);
		        	        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			    	        	}
			    	        	else {
			                        Toast.makeText(getActivity(), R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
			    	        		PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
			    	        	}

			    	        }
			    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			    	        public void onClick(DialogInterface dialog, int whichButton) {
			    	        	;
			    	        }
			    	    }).show();

			    	}
				}
				else {
		    		LegacyAddress addr = null;
		            for(int i = 0; i < legacy.size(); i++) {
		            	if(legacy.get(i).getAddress().equals(currentSelectedAddress)) {
		            		addr = legacy.get(i);
		            		break;
		            	}
		            }

			    	if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
    	        		args.putBoolean("hd", false);
    	        		args.putInt("account", -1);
    	        		args.putString("destination", pendingSpend.destination);
    	        		args.putString("bamount", pendingSpend.bamount.toString());
    	        		args.putString("bfee", pendingSpend.bfee.toString());
    	        		args.putString("legacy_addr", addr.getAddress());
    	        		args.putString("legacy_priv", addr.getEncryptedKey());
    	        		args.putString("sending_from", pendingSpend.sending_from);
    	        		args.putString("btc_amount", pendingSpend.btc_amount);
    	        		args.putString("fiat_amount", pendingSpend.fiat_amount);
                        args.putString("btc_units", pendingSpend.btc_units);
    	        		fragment.setArguments(args);
    	        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			    	}
			    	else if(DoubleEncryptionFactory.getInstance().isActivated()) {
    	        		args.putBoolean("hd", false);
    	        		args.putInt("account", -1);
    	        		args.putString("destination", pendingSpend.destination);
    	        		args.putString("bamount", pendingSpend.bamount.toString());
    	        		args.putString("bfee", pendingSpend.bfee.toString());
    	        		args.putString("legacy_addr", addr.getAddress());
    	        		args.putString("legacy_priv", addr.getEncryptedKey());
    	        		args.putString("sending_from", pendingSpend.sending_from);
    	        		args.putString("btc_amount", pendingSpend.btc_amount);
    	        		args.putString("fiat_amount", pendingSpend.fiat_amount);
                        args.putString("btc_units", pendingSpend.btc_units);
    	        		fragment.setArguments(args);
    	        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			    	}
			    	else {

			    		final LegacyAddress legacyAddress = addr;
			    		final EditText password = new EditText(getActivity());
		        		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			    		
			    		new AlertDialog.Builder(getActivity())
			    	    .setTitle(R.string.app_name)
			    	    .setMessage(R.string.enter_double_encryption_pw)
			    	    .setView(password)
			    	    .setCancelable(false)
			    	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			    	        public void onClick(DialogInterface dialog, int whichButton) {

			    	        	final String pw = password.getText().toString();
			    	        	
			    	        	PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));
			    	        	
			    	        	if(DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword(), PayloadFactory.getInstance().get().getIterations())) {
	            	        		args.putBoolean("hd", false);
	            	        		args.putInt("account", -1);
	            	        		args.putString("destination", pendingSpend.destination);
	            	        		args.putString("bamount", pendingSpend.bamount.toString());
	            	        		args.putString("bfee", pendingSpend.bfee.toString());
	            	        		args.putString("legacy_addr", legacyAddress.getAddress());
	            	        		args.putString("legacy_priv", legacyAddress.getEncryptedKey());
	            	        		args.putString("sending_from", pendingSpend.sending_from);
	            	        		args.putString("btc_amount", pendingSpend.btc_amount);
	            	        		args.putString("fiat_amount", pendingSpend.fiat_amount);
                                    args.putString("btc_units", pendingSpend.btc_units);
	            	        		fragment.setArguments(args);
		        	        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			    	        	}
			    	        	else {
			                        Toast.makeText(getActivity(), R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
			    	        		PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
			    	        	}

			    	        }
			    	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			    	        public void onClick(DialogInterface dialog, int whichButton) {
			    	        	;
			    	        }
			    	    }).show();
			    	}
		    	}

            }
        });

        edDestination = ((EditText)rootView.findViewById(R.id.destination));
        edDestination.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {
		        	validateSpend(true);
		        }

		        return false;
		    }
		});
        edDestination.addTextChangedListener(new TextWatcher()	{
        	public void afterTextChanged(Editable s) {

        		if(edAmount1 != null && edDestination != null && tvAmount2 != null && spAccounts != null) {
            		validateSpend(false);
        		}

        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });
        
        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {
		        	validateSpend(true);
		        }

		        return false;
		    }
		});
        edAmount1.addTextChangedListener(new TextWatcher()	{
        	public void afterTextChanged(Editable s) {

                updateTextFields();

        		if(edAmount1 != null && edDestination != null && tvAmount2 != null && spAccounts != null) {
            		validateSpend(false);
        		}

        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });

        tvCurrency1 = (TextView)rootView.findViewById(R.id.currency1);
        tvAmount2 = (TextView)rootView.findViewById(R.id.amount2);
        tvAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.ITALIC);
        tvAmount2.setTextColor(0xff9f9f9f);
        tvFiat2 = (TextView)rootView.findViewById(R.id.fiat2);
        tvFiat2.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.ITALIC);
        tvFiat2.setTextColor(0xff9f9f9f);
        tvAmount2.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
            	toggleAmounts();

            	return false;
            }
        });
        tvFiat2.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
            	toggleAmounts();

            	return false;
            }
        });

        spAccounts = (Spinner)rootView.findViewById(R.id.accounts);
        accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
        if(PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
        	iAccount = new ImportedAccount("Imported addresses", PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        }
        if(accounts.get(accounts.size() - 1) instanceof ImportedAccount) {
        	accounts.remove(accounts.size() - 1);
        }
        hdAccountsIdx = accounts.size();
        _accounts = new ArrayList<String>();
        for(int i = 0; i < accounts.size(); i++) {
        	_accounts.add((accounts.get(i).getLabel() == null || accounts.get(i).getLabel().length() == 0) ? "Account: " + (i + 1) : accounts.get(i).getLabel());
        }
        if(iAccount != null) {
    		legacy = iAccount.getLegacyAddresses();
            for(int j = 0; j < legacy.size(); j++) {
            	_accounts.add((legacy.get(j).getLabel() == null || legacy.get(j).getLabel().length() == 0) ? legacy.get(j).getAddress() : legacy.get(j).getLabel());
            }
        }
    	ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_item, _accounts);
    	dataAdapter.setDropDownViewResource(R.layout.spinner_item2);
    	spAccounts.setAdapter(dataAdapter);
    	spAccounts.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                      int position = spAccounts.getSelectedItemPosition();
                      if(position >= hdAccountsIdx) {
                          Toast.makeText(getActivity(), "Legacy:" + _accounts.get(position), Toast.LENGTH_SHORT).show();
                          currentSelectedAddress = legacy.get(position - hdAccountsIdx).getAddress();
                      }
                      else {
                          Toast.makeText(getActivity(), "Account " + position + ":" + accounts.get(position).getLabel(), Toast.LENGTH_SHORT).show();
                          currentSelectedAccount = position;
                      }
                      
                      displayMaxAvailable();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    	;
                    }
                }
            );
        spAccounts.setSelection(0);

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        tvAmount2.setText("0.00" + "\u00A0");
        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);

        tvMax = (TextView)rootView.findViewById(R.id.max);
        tvMax.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        displayMaxAvailable();

        // get bundle
        Bundle bundle = this.getArguments();
        if(bundle != null) {
            boolean validate = false;
            String address_arg = bundle.getString("btc_address", "");
            String amount_arg = bundle.getString("btc_amount", "");
            if(!address_arg.equals("")) {
                edDestination.setText(address_arg);
                validate = false;
            }
            if(!amount_arg.equals("")) {
                edAmount1.setText(amount_arg);
                edAmount1.setSelection(edAmount1.getText().toString().length());

                double btc_amount = 0.0;
                try {
                    btc_amount = NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue();
                }
                catch(NumberFormatException nfe) {
                    btc_amount = 0.0;
                }
                catch(ParseException pe) {
                    btc_amount = 0.0;
                }

                // sanity check on strFiat, necessary if the result of a URI scan
                if(strFiat == null) {
                    strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
                }
                btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

                double fiat_amount = btc_fx * btc_amount;
                tvAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount) + "\u00A0");
                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
                strBTC = MonetaryUtil.getInstance().getBTCUnit(MonetaryUtil.UNIT_BTC);
                tvCurrency1.setText(strBTC);
                tvFiat2.setText(strFiat);

                validate = true;
            }
            if(validate) {
                validateSpend(true);
            }

            if(spAccounts != null) {
                spAccounts.setSelection(currentSelectedItem);
            }

        }

        return rootView;
	}

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
            btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
            tvCurrency1.setText(isBTC ? strBTC : strFiat);
            tvFiat2.setText(isBTC ? strFiat : strBTC);
            displayMaxAvailable();
        }
        else {
        	;
        }
    }

    @Override
    public void onResume() {
    	super.onResume();
        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
        tvCurrency1.setText(isBTC ? strBTC : strFiat);
        tvFiat2.setText(isBTC ? strFiat : strBTC);
        displayMaxAvailable();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
    	if(spAccounts != null) {
    		currentSelectedItem = spAccounts.getSelectedItemPosition();
    	}
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void validateSpend(boolean showMessages) {
		
		pendingSpend.amount = null;
		pendingSpend.destination = null;
		pendingSpend.bamount = BigInteger.ZERO;
		pendingSpend.bfee = bFee;
        pendingSpend.isHD = true;
        pendingSpend.btc_units = strBTC;

        int position = spAccounts.getSelectedItemPosition();
        if(position >= hdAccountsIdx) {
            currentSelectedAddress = legacy.get(position - hdAccountsIdx).getAddress();
            if(legacy.get(position - hdAccountsIdx).getLabel() != null && legacy.get(position - hdAccountsIdx).getLabel().length() > 0) {
                pendingSpend.sending_from = legacy.get(position - hdAccountsIdx).getLabel();
            }
            else {
                pendingSpend.sending_from = legacy.get(position - hdAccountsIdx).getAddress();
            }
            pendingSpend.isHD = false;
        }
        else {
            currentSelectedAccount = position;
            pendingSpend.sending_from = accounts.get(position).getLabel();
            pendingSpend.isHD = true;
        }

		pendingSpend.destination = edDestination.getText().toString();
		if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)) {
			if(showMessages) {
	            Toast.makeText(getActivity(), R.string.invalid_bitcoin_address, Toast.LENGTH_SHORT).show();
			}
    		btSend.setVisibility(View.INVISIBLE);
			return;
		}
		
		if(isBTC) {
			pendingSpend.amount = edAmount1.getText().toString();
		}
		else {
			pendingSpend.amount = tvAmount2.getText().toString();
		}
		long lamount = 0L;
		double damount = 0.0;
		try {
            lamount = (long)(NumberFormat.getInstance(locale).parse(pendingSpend.amount).doubleValue() * 1e8);
            pendingSpend.bamount = getUndenominatedAmount(lamount);
			if(!(pendingSpend.bamount.compareTo(BigInteger.ZERO) >= 0)) {
				if(showMessages) {
		            Toast.makeText(getActivity(), R.string.invalid_amount, Toast.LENGTH_SHORT).show();
				}
	    		btSend.setVisibility(View.INVISIBLE);
				return;
			}
		}
		catch(NumberFormatException nfe) {
			if(showMessages) {
	            Toast.makeText(getActivity(), R.string.invalid_amount, Toast.LENGTH_SHORT).show();
			}
    		btSend.setVisibility(View.INVISIBLE);
			return;
		}
		catch(ParseException pe) {
			if(showMessages) {
	            Toast.makeText(getActivity(), R.string.invalid_amount, Toast.LENGTH_SHORT).show();
			}
    		btSend.setVisibility(View.INVISIBLE);
			return;
		}

		if(pendingSpend.isHD) {
			String xpub = HDPayloadBridge.getInstance().account2Xpub(currentSelectedAccount);

			if(xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
				long _lamount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
				if((getUndenominatedAmount(lamount).longValue() + bFee.longValue()) > _lamount) {
					if(showMessages) {
			            Toast.makeText(getActivity(), R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
					}
		    		btSend.setVisibility(View.INVISIBLE);
					return;
				}
			}
		}
		else {
			long _lamount = MultiAddrFactory.getInstance().getLegacyBalance(currentSelectedAddress);
			if((getUndenominatedAmount(lamount).longValue() + bFee.longValue()) > _lamount) {
				if(showMessages) {
		            Toast.makeText(getActivity(), R.string.insufficient_funds, Toast.LENGTH_SHORT).show();
				}
	    		btSend.setVisibility(View.INVISIBLE);
				return;
			}
		}
		
		btSend.setVisibility(View.VISIBLE);

	}

    private void toggleAmounts() {
    	String tmp = edAmount1.getText().toString();
    	if(tmp == null || tmp.length() == 0) {
    		tmp = MonetaryUtil.getInstance().getFiatFormat(strFiat).format(0.00);
    	}

    	//
    	// hack to prevent clipping of right-justified italic text
    	//
    	if(tvAmount2.getText().toString().endsWith("\u00A0")) {
        	edAmount1.setText(tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 1));
    	}
    	else {
        	edAmount1.setText(tvAmount2.getText().toString());
    	}
    	tvAmount2.setText(tmp + "\u00A0");
    	tvCurrency1.setText(isBTC ? strFiat : strBTC);
    	tvFiat2.setText(isBTC ? strBTC : strFiat);
    	isBTC = (isBTC) ? false : true;

        validateSpend(true);
    }

    private void updateTextFields() {
        if(isBTC) {
            double btc_amount = 0.0;
            try {
                btc_amount = getUndenominatedAmount(NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue());
            }
            catch(NumberFormatException nfe) {
                btc_amount = 0.0;
            }
            catch(ParseException pe) {
                btc_amount = 0.0;
            }
            double fiat_amount = btc_fx * btc_amount;
            tvAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
            tvCurrency1.setText(strBTC);
            tvFiat2.setText(strFiat);
        }
        else {
            double fiat_amount = 0.0;
            try {
                fiat_amount = NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue();
            }
            catch(NumberFormatException nfe) {
                fiat_amount = 0.0;
            }
            catch(ParseException pe) {
                fiat_amount = 0.0;
            }
            double btc_amount = fiat_amount / btc_fx;
            tvAmount2.setText(MonetaryUtil.getInstance().getBTCFormat().format(getDenominatedAmount(btc_amount)) + "\u00A0");
            tvCurrency1.setText(strFiat);
            tvFiat2.setText(strBTC);
        }
    }

    private void displayMaxAvailable() {

    	int position = spAccounts.getSelectedItemPosition();
		long amount = 0L;
        
        if(position >= hdAccountsIdx) {
        	amount = MultiAddrFactory.getInstance().getLegacyBalance(legacy.get(position - hdAccountsIdx).getAddress());
        }
        else {
            String xpub = account2Xpub(position);
			if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
				amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
			}
			else {
				amount = 0L;
			}
        }
        
        long amount_available = amount - bFee.longValue();
        if(amount_available > 0L) {
        	double btc_balance = (((double)amount_available) / 1e8);
        	tvMax.setText(getActivity().getResources().getText(R.string.max_available) + " " + MonetaryUtil.getInstance().getBTCFormat().format(getDenominatedAmount(btc_balance)) + " " + strBTC);
        }
        else {
        	tvMax.setText(R.string.no_funds_available);
        }

    }

	public String account2Xpub(int sel) {

		Account hda = accounts.get(sel);
		String xpub = null;
	    if(hda instanceof ImportedAccount) {
	    	xpub = null;
	    }
	    else {
			xpub = HDPayloadBridge.getInstance(getActivity()).account2Xpub(sel);
	    }
	    
	    return xpub;
	}

    private String getDisplayAmount(long value) {

        String strAmount = null;

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = Double.toString((((double)(value * 1000000L)) / 1e8));
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = Double.toString((((double)(value * 1000L)) / 1e8));
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    private BigInteger getUndenominatedAmount(long value) {

        BigInteger amount = BigInteger.ZERO;

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = BigInteger.valueOf(value / 1000000L);
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = BigInteger.valueOf(value / 1000L);
                break;
            default:
                amount = BigInteger.valueOf(value);
                break;
        }

        return amount;
    }

    private double getUndenominatedAmount(double value) {

        double amount = 0.0;

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = value / 1000000.0;
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = value / 1000.0;
                break;
            default:
                amount = value;
                break;
        }

        return amount;
    }

    private double getDenominatedAmount(double value) {

        double amount = 0.0;

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = value * 1000000.0;
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = value * 1000.0;
                break;
            default:
                amount = value;
                break;
        }

        return amount;
    }

}
