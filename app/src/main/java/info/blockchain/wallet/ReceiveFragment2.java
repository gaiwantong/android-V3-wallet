package info.blockchain.wallet;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import org.apache.commons.codec.DecoderException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

public class ReceiveFragment2 extends Fragment {
	
	private Locale locale = null;

    private ImageView ivReceivingQR = null;
	private TextView edReceivingAddress = null;
    private String currentSelectedAddress = null;
    private ReceiveAddress currentSelectedReceiveAddress = null;

	private EditText edAmount1 = null;
	private TextView tvCurrency1 = null;
	private TextView tvAmount2 = null;
	private TextView tvFiat2 = null;
	private Spinner spAccounts = null;
	private List<LegacyAddress> legacy = null;
	
	private int currentSelectedAccount = 0;

	private String strBTC = "BTC";
	private String strFiat = null;
	private boolean isBTC = true;
	private double btc_fx = 319.13;

	Bitmap ivReceivingQRImage;
	String[] nameOfAppsToShareWith = new String[] { "facebook", "twitter", "gmail", "instagram"};
	String[] blacklist = new String[]{"com.any.package.that.you.want.to.exclude"};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_receive2, container, false);

		Log.i("ReceiveFragment", "onCreateView");

		locale = Locale.getDefault();

		((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.receive);

//		listView = (ListView)rootView.findViewById(R.id.receive_share_app_list);
//		anim = AnimationUtils.loadAnimation(getActivity(),R.anim.slide_up3);
//		bottomSheet = (ScrollView)rootView.findViewById(R.id.receive_bottom_view);

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

				String strFileName = getActivity().getExternalCacheDir() + File.separator + "qr.png";
				File file = new File(strFileName);
				if(!file.exists()) {
					try {
						file.createNewFile();
					}
					catch(Exception e) {
						Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
				file.setReadable(true, false);

				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(file);
				}
				catch(FileNotFoundException fnfe) {
					;
				}

				android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = null;
				clip = android.content.ClipData.newPlainText("Send address", currentSelectedAddress);
				clipboard.setPrimaryClip(clip);

				if(file != null && fos != null) {
					Bitmap bitmap = ((BitmapDrawable)ivReceivingQR.getDrawable()).getBitmap();
					bitmap.compress(CompressFormat.PNG, 0, fos);

					try {
						fos.close();
					}
					catch(IOException ioe) {
						;
					}

					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_SEND);
					intent.setType("image/png");
					intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
					startActivity(Intent.createChooser(intent, getActivity().getText(R.string.send_payment_code)));
				}

				return true;
			}
		});

		edAmount1 = (EditText)rootView.findViewById(R.id.amount1);
		edAmount1.addTextChangedListener(new TextWatcher()	{
			public void afterTextChanged(Editable s) {

				updateTextFields();

				if(currentSelectedAddress != null) {
					displayQRCode();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }

			public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
		});

		tvCurrency1 = (TextView)rootView.findViewById(R.id.currency1);
		tvAmount2 = (TextView)rootView.findViewById(R.id.amount2);
		//tvAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.ITALIC);
		tvAmount2.setTextColor(0xff9f9f9f);
		tvFiat2 = (TextView)rootView.findViewById(R.id.fiat2);
		//tvFiat2.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.ITALIC);
		tvFiat2.setTextColor(0xff9f9f9f);
		tvAmount2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				toggleAmounts();
				displayQRCode();

				return false;
			}
		});
		tvFiat2.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				toggleAmounts();
				displayQRCode();

				return false;
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
							Log.i("ReceiveFragment", "assignHDReceiveAddress() OnItemSelectedListener, !(position >= hdAccountsIdx)");
							Log.i("ReceiveFragment", "currentSelectedAccount:" + currentSelectedAccount);
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
//        spAccounts.setSelection(0);

		strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC));
		strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
		btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

		tvAmount2.setText("0.00" + "\u00A0");
		tvCurrency1.setText(strBTC);
		tvFiat2.setText(strFiat);

		Log.i("ReceiveFragment", "assignHDReceiveAddress() onCreateView");
		assignHDReceiveAddress();

		edReceivingAddress = (TextView)rootView.findViewById(R.id.receiving_address);

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
			updateTextFields();
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
		updateTextFields();
	}

	@Override
	public void onPause() {
		super.onPause();
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
				lamount = (long)(NumberFormat.getInstance(locale).parse(tvAmount2.getText().toString()).doubleValue() * 1e8);
			}
			bamount = getUndenominatedAmount(lamount);
			if(!bamount.equals(BigInteger.ZERO)) {
				ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, bamount, "", "")));
			}
			else {
				ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
			}
		}
		catch(NumberFormatException nfe) {
			ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
		}
		catch(ParseException pe) {
			ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
		}
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

		ivReceivingQRImage = bitmap;
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

	private void toggleAmounts() {
		String tmp = edAmount1.getText().toString();
		if(tmp == null || tmp.length() == 0) {
			tmp = MonetaryUtil.getInstance().getFiatFormat(strFiat).format(0.00);
		}
		edAmount1.setText(tvAmount2.getText().toString());
		tvAmount2.setText(tmp);
		tvCurrency1.setText(isBTC ? strFiat : strBTC);
		tvFiat2.setText(isBTC ? strBTC : strFiat);
		isBTC = (isBTC) ? false : true;
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

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.action_merchant_directory).setVisible(false);
		menu.findItem(R.id.action_qr).setVisible(false);
		MenuItem i = menu.findItem(R.id.action_share_receive).setVisible(true);

		i.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {

				// your share intent
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, currentSelectedAddress);
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Payment address");

				//add image to email? - need to save to file then?

				startActivity(generateCustomChooserIntent(intent, blacklist));

				return false;
			}
		});
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		setHasOptionsMenu(true);
	}

	private Intent generateCustomChooserIntent(Intent prototype, String[] forbiddenChoices) {
		List<Intent> targetedShareIntents = new ArrayList<Intent>();
		List<HashMap<String, String>> intentMetaInfo = new ArrayList<HashMap<String, String>>();
		Intent chooserIntent;

		Intent dummy = new Intent(prototype.getAction());
		dummy.setType(prototype.getType());
		List<ResolveInfo> resInfo = getActivity().getPackageManager().queryIntentActivities(dummy, 0);

		if (!resInfo.isEmpty()) {

			for (ResolveInfo resolveInfo : resInfo) {

				if (resolveInfo.activityInfo == null || Arrays.asList(forbiddenChoices).contains(resolveInfo.activityInfo.packageName))
					continue;

				HashMap<String, String> info = new HashMap<String, String>();
				info.put("packageName", resolveInfo.activityInfo.packageName);
				info.put("className", resolveInfo.activityInfo.name);
				String appName = String.valueOf(resolveInfo.activityInfo.loadLabel(getActivity().getPackageManager()));
				info.put("simpleName", appName);

				if (Arrays.asList(nameOfAppsToShareWith).contains(appName.toLowerCase())) {
					intentMetaInfo.add(info);
				}
			}

			if (!intentMetaInfo.isEmpty()) {

				// create the custom intent list
				for (HashMap<String, String> metaInfo : intentMetaInfo) {
					Intent targetedShareIntent = (Intent) prototype.clone();
					targetedShareIntent.setPackage(metaInfo.get("packageName"));
					targetedShareIntent.setClassName(metaInfo.get("packageName"), metaInfo.get("className"));
					targetedShareIntents.add(targetedShareIntent);
				}
				String shareVia = getString(R.string.send_payment_code);
				String shareTitle = shareVia.substring(0, 1).toUpperCase() + shareVia.substring(1);

				chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), shareTitle);
				chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
				return chooserIntent;
			}
		}

		return Intent.createChooser(prototype, getString(R.string.send_payment_code));
	}

}
