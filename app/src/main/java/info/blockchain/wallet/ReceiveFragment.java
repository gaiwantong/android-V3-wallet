package info.blockchain.wallet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.commons.codec.DecoderException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

//import info.blockchain.wallet.util.ReceiveAddressPool;

public class ReceiveFragment extends Fragment {
	
	private Locale locale = null;
	
    private ImageView ivReceivingQR = null;
	private TextView edReceivingAddress = null;
    private String currentSelectedAddress = null;
    private ReceiveAddress currentSelectedReceiveAddress = null;

	private EditText edAmount1 = null;
	private EditText edAmount2 = null;
	private Spinner spAccounts = null;
	private TextView tvCurrency1 = null;
	private TextView tvFiat2 = null;
	private List<LegacyAddress> legacy = null;
	
	private int currentSelectedAccount = 0;
	private static int currentSelectedItem = 0;

	private String strBTC = "BTC";
	private String strFiat = null;
	private boolean isBTC = true;
	private double btc_fx = 319.13;

	private SlidingUpPanelLayout mLayout;
	private ListView sendPaymentCodeAppListlist;
	private View rootView;
	private LinearLayout mainContent;
	private LinearLayout mainContentShadow;

	private boolean textChangeAllowed = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
		this.rootView = rootView;

		locale = Locale.getDefault();

		((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
		((ActionBarActivity)getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
		((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.receive_bitcoin);
		setHasOptionsMenu(true);

		Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = new BalanceFragment();
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
			}
		});

		mainContent = (LinearLayout)rootView.findViewById(R.id.receive_main_content);
		mainContentShadow = (LinearLayout)rootView.findViewById(R.id.receive_main_content_shadow);
		mainContentShadow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)){
					onShareClicked();
				}
			}
		});

        ivReceivingQR = (ImageView)rootView.findViewById(R.id.qr);
        ivReceivingQR.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
  			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
  		    android.content.ClipData clip = null;
		    clip = android.content.ClipData.newPlainText("Send address", currentSelectedAddress);
			Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
  		    clipboard.setPrimaryClip(clip);
                  }
    	});
      
        ivReceivingQR.setOnLongClickListener(new View.OnLongClickListener() {
    	  public boolean onLongClick(View view) {

			  onShareClicked();

			  return true;
		  }
    	});

		tvCurrency1 = (TextView)rootView.findViewById(R.id.currency1);
		tvFiat2 = (TextView)rootView.findViewById(R.id.fiat2);

        edAmount1 = (EditText)rootView.findViewById(R.id.amount1);
        edAmount1.addTextChangedListener(new TextWatcher()	{
        	public void afterTextChanged(Editable s) {

                edAmount1.removeTextChangedListener(this);

                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
                switch(unit) {
                    case MonetaryUtil.MICRO_BTC:
                        max_len = 2;
                        break;
                    case MonetaryUtil.MILLI_BTC:
                        max_len = 4;
                        break;
                    default:
                        max_len = 8;
                        break;
                }
                btcFormat.setMaximumFractionDigits(max_len + 1);
                btcFormat.setMinimumFractionDigits(0);

                DecimalFormatSymbols decFormatSymbols = new DecimalFormatSymbols(Locale.getDefault());
                char sep = decFormatSymbols.getDecimalSeparator();

                try	{
                    double d = Double.parseDouble(s.toString());
                    String s1 = btcFormat.format(d);
                    if(s1.indexOf(sep) != -1)	{
                        String dec = s1.substring(s1.indexOf(sep));
                        if(dec.length() > 0)	{
                            dec = dec.substring(1);
                            if(dec.length() > max_len)	{
                                edAmount1.setText(s1.substring(0, s1.length() - 1));
                                edAmount1.setSelection(edAmount1.getText().length());
                                s = edAmount1.getEditableText();
                            }
                        }
                    }
                }
                catch(NumberFormatException nfe)	{
                    ;
                }

                edAmount1.addTextChangedListener(this);

                if(textChangeAllowed) {
					textChangeAllowed = false;
					updateFiatTextField(s.toString());

					if (currentSelectedAddress != null) {
						displayQRCode();
					}
					textChangeAllowed = true;
				}
        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });

        edAmount2 = (EditText)rootView.findViewById(R.id.amount2);
        edAmount2.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {

                edAmount2.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                DecimalFormatSymbols decFormatSymbols = new DecimalFormatSymbols(Locale.getDefault());
                char sep = decFormatSymbols.getDecimalSeparator();

                try	{
                    double d = Double.parseDouble(s.toString());
                    String s1 = fiatFormat.format(d);
                    if(s1.indexOf(sep) != -1)	{
                        String dec = s1.substring(s1.indexOf(sep));
                        if(dec.length() > 0)	{
                            dec = dec.substring(1);
                            if(dec.length() > max_len)	{
                                edAmount2.setText(s1.substring(0, s1.length() - 1));
                                edAmount2.setSelection(edAmount2.getText().length());
                                s = edAmount2.getEditableText();
                            }
                        }
                    }
                }
                catch(NumberFormatException nfe)	{
                    ;
                }

                edAmount2.addTextChangedListener(this);

                if(textChangeAllowed) {
					textChangeAllowed = false;
					updateBtcTextField(s.toString());

					if (currentSelectedAddress != null) {
						displayQRCode();
					}
					textChangeAllowed = true;
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				;
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				;
			}
		});

        spAccounts = (Spinner)rootView.findViewById(R.id.accounts);
        final List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
    	ImportedAccount iAccount = null;
        if(PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
        	iAccount = new ImportedAccount("Imported addresses", PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        }
        if(accounts.get(accounts.size() - 1) instanceof ImportedAccount) {
        	accounts.remove(accounts.size() - 1);
        }
        final int hdAccountsIdx = accounts.size();
        final List<String> _accounts = new ArrayList<String>();
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
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	dataAdapter.setDropDownViewResource(R.layout.spinner_item2);
    	spAccounts.setAdapter(dataAdapter);
        spAccounts.post(new Runnable() {
            public void run() {
                spAccounts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                      int position = spAccounts.getSelectedItemPosition();
                      if(position >= hdAccountsIdx) {
                          Toast.makeText(getActivity(), "Legacy:" + _accounts.get(position), Toast.LENGTH_SHORT).show();
                          currentSelectedAddress = legacy.get(position - hdAccountsIdx).getAddress();
                          displayQRCode();
                      }
                      else {
                          Toast.makeText(getActivity(), "Account " + position + ":" + accounts.get(position).getLabel(), Toast.LENGTH_SHORT).show();
                          currentSelectedAccount = position;
                          //Log.i("ReceiveFragment", "assignHDReceiveAddress() OnItemSelectedListener, !(position >= hdAccountsIdx)");
                          //Log.i("ReceiveFragment", "currentSelectedAccount:" + currentSelectedAccount);
                          assignHDReceiveAddress();
                          displayQRCode();
                      }
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    	;
                    }
                });
            }
        });
        spAccounts.setSelection(currentSelectedItem);

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

		tvCurrency1.setText(strBTC);
		tvFiat2.setText(strFiat);

        assignHDReceiveAddress();

      	edReceivingAddress = (TextView)rootView.findViewById(R.id.receiving_address);

		mLayout = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
		mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
		mLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
			@Override
			public void onPanelSlide(View panel, float slideOffset) {
			}

			@Override
			public void onPanelExpanded(View panel) {

			}

			@Override
			public void onPanelCollapsed(View panel) {

			}

			@Override
			public void onPanelAnchored(View panel) {
			}

			@Override
			public void onPanelHidden(View panel) {
			}
		});

        displayQRCode();

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
//            updateTextFields();
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
//        updateTextFields();

		currentSelectedItem = getArguments().getInt("selected_account");
		if(spAccounts != null) {
			spAccounts.setSelection(currentSelectedItem);
		}
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
	
    private void displayQRCode() {

		edReceivingAddress.setText(currentSelectedAddress);

		BigInteger bamount = null;
		try {
			long lamount = 0L;
			if(isBTC) {
                lamount = (long)(NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue() * 1e8);
			}
			else {
                lamount = (long)(NumberFormat.getInstance(locale).parse(edAmount2.getText().toString()).doubleValue() * 1e8);
			}
            bamount = getUndenominatedAmount(lamount);
			if(!bamount.equals(BigInteger.ZERO)) {
				ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, bamount, "", "")));
                write2NFC(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, bamount, "", ""));
			}
			else {
				ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
                write2NFC(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", ""));
			}
		}
		catch(NumberFormatException nfe) {
			ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
            write2NFC(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", ""));
		}
		catch(ParseException pe) {
			ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
            write2NFC(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", ""));
		}

		setupBottomSheet();
    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 260;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

    private void assignHDReceiveAddress() {

    	try {
          	currentSelectedReceiveAddress = HDPayloadBridge.getInstance(getActivity()).getReceiveAddress(currentSelectedAccount);
          	currentSelectedAddress = currentSelectedReceiveAddress.getAddress();
    	}
    	catch(IOException ioe) {
    		;
    	}
    	catch(MnemonicException.MnemonicLengthException mle) {
    		;
    	}
    	catch(MnemonicException.MnemonicWordException mwe) {
    		;
    	}
    	catch(MnemonicException.MnemonicChecksumException mce) {
    		;
    	}
    	catch(AddressFormatException afe) {
    		;
    	}
    	catch(DecoderException de) {
    		;
    	}

//    	ReceiveAddressPool.getInstance().update();

    }

//    private void toggleAmounts() {
//    	String tmp = edAmount1.getText().toString();
//    	if(tmp == null || tmp.length() == 0) {
//    		tmp = MonetaryUtil.getInstance().getFiatFormat(strFiat).format(0.00);
//    	}
//    	edAmount1.setText(edAmount2.getText().toString());
//    	edAmount2.setText(tmp);
//    	tvCurrency1.setText(isBTC ? strFiat : strBTC);
//    	tvFiat2.setText(isBTC ? strBTC : strFiat);
//    	isBTC = (isBTC) ? false : true;
//    }
//
//    private void updateTextFields() {
//        if(isBTC) {
//            double btc_amount = 0.0;
//            try {
//                btc_amount = getUndenominatedAmount(NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue());
//            }
//            catch(NumberFormatException nfe) {
//                btc_amount = 0.0;
//            }
//            catch(ParseException pe) {
//                btc_amount = 0.0;
//            }
//            double fiat_amount = btc_fx * btc_amount;
//            edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
//            tvCurrency1.setText(strBTC);
//            tvFiat2.setText(strFiat);
//        }
//        else {
//            double fiat_amount = 0.0;
//            try {
//                fiat_amount = NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue();
//            }
//            catch(NumberFormatException nfe) {
//                fiat_amount = 0.0;
//            }
//            catch(ParseException pe) {
//                fiat_amount = 0.0;
//            }
//            double btc_amount = fiat_amount / btc_fx;
//            edAmount2.setText(MonetaryUtil.getInstance().getBTCFormat().format(getDenominatedAmount(btc_amount)) + "\u00A0");
//            tvCurrency1.setText(strFiat);
//            tvFiat2.setText(strBTC);
//        }
//    }

	private void updateFiatTextField(String cBtc) {
		double btc_amount = 0.0;
		try {
			btc_amount = getUndenominatedAmount(NumberFormat.getInstance(locale).parse(cBtc).doubleValue());
		}
		catch(NumberFormatException nfe) {
			btc_amount = 0.0;
		}
		catch(ParseException pe) {
			btc_amount = 0.0;
		}
		double fiat_amount = btc_fx * btc_amount;
		edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
	}

	private void updateBtcTextField(String cfiat){

		double fiat_amount = 0.0;
		try {
			fiat_amount = NumberFormat.getInstance(locale).parse(cfiat).doubleValue();
		}
		catch(NumberFormatException nfe) {
			fiat_amount = 0.0;
		}
		catch(ParseException pe) {
			fiat_amount = 0.0;
		}
		double btc_amount = fiat_amount / btc_fx;
//		edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(getDenominatedAmount(btc_amount)) + "\u00A0");
		edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(getDenominatedAmount(btc_amount)));
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

    private void write2NFC(final String uri) {

        if (Build.VERSION.SDK_INT < 16){
            return;
        }

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfc != null && nfc.isNdefPushEnabled() ) {
            nfc.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    NdefRecord uriRecord = NdefRecord.createUri(uri);
                    return new NdefMessage(new NdefRecord[]{ uriRecord });
                }
            }, getActivity());
        }

    }

    @Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.action_merchant_directory).setVisible(false);
		menu.findItem(R.id.action_qr).setVisible(false);
		menu.findItem(R.id.action_send).setVisible(false);
		MenuItem i = menu.findItem(R.id.action_share_receive).setVisible(true);

		i.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {

				onShareClicked();

				return false;
			}
		});
	}

	private void onShareClicked(){

		if (mLayout != null) {
			if (mLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
				mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
				mainContentShadow.setVisibility(View.GONE);
			} else {
				mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
				mainContentShadow.setVisibility(View.VISIBLE);
				mainContentShadow.bringToFront();
			}
		}
	}

	private void setupBottomSheet(){

		//Re-Populate list
		String strFileName = getActivity().getExternalCacheDir() + File.separator + "qr.png";
		File file = new File(strFileName);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (Exception e) {
				Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		file.setReadable(true, false);

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException fnfe) {
			;
		}

		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
		android.content.ClipData clip = null;
		clip = android.content.ClipData.newPlainText("Send address", currentSelectedAddress);
		clipboard.setPrimaryClip(clip);

		if (file != null && fos != null) {
			Bitmap bitmap = ((BitmapDrawable) ivReceivingQR.getDrawable()).getBitmap();
			bitmap.compress(CompressFormat.PNG, 0, fos);

			try {
				fos.close();
			} catch (IOException ioe) {
				;
			}

			ArrayList<SendPaymentCodeData> dataList = new ArrayList<SendPaymentCodeData>();

			PackageManager pm = getActivity().getPackageManager();

			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.setType("image/png");
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
			List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

			SendPaymentCodeData d;
			for (ResolveInfo resInfo : resInfos) {

				String context = resInfo.activityInfo.packageName;
				String packageClassName = resInfo.activityInfo.name;
				CharSequence label = resInfo.loadLabel(pm);
				Drawable icon = resInfo.loadIcon(pm);

				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND);
				shareIntent.setType("image/png");
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
				shareIntent.setClassName(context, packageClassName);

				d = new SendPaymentCodeData();
				d.setTitle(label.toString());
				d.setLogo(icon);
				d.setIntent(shareIntent);
				dataList.add(d);
			}

			ArrayAdapter adapter = new SendPaymentCodeAdapter(getActivity(), dataList);
			sendPaymentCodeAppListlist = (ListView) rootView.findViewById(R.id.share_app_list);
			sendPaymentCodeAppListlist.setAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
	}

	class SendPaymentCodeAdapter extends ArrayAdapter<SendPaymentCodeData>
	{
		private final Context context;
		private final ArrayList<SendPaymentCodeData> repoDataArrayList;

		public SendPaymentCodeAdapter(Context context, ArrayList<SendPaymentCodeData> repoDataArrayList) {

			super(context, R.layout.fragment_receive_share_row, repoDataArrayList);

			this.context = context;
			this.repoDataArrayList = repoDataArrayList;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View rowView = null;
			rowView = inflater.inflate(R.layout.fragment_receive_share_row, parent, false);

			ImageView image = (ImageView) rowView.findViewById(R.id.share_app_image);
			TextView title = (TextView) rowView.findViewById(R.id.share_app_title);

			image.setImageDrawable(repoDataArrayList.get(position).getLogo());
			title.setText(repoDataArrayList.get(position).getTitle());

			rowView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(repoDataArrayList.get(position).getIntent());
				}
			});

			return rowView;
		}
	}

	class SendPaymentCodeData
	{
		private Drawable logo;
		private String title;
		private Intent intent;

		public SendPaymentCodeData() {

		}

		public Intent getIntent() {
			return intent;
		}

		public void setIntent(Intent intent) {
			this.intent = intent;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Drawable getLogo() {
			return logo;
		}

		public void setLogo(Drawable logo) {
			this.logo = logo;
		}
	}
}
