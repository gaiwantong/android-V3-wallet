package info.blockchain.wallet.view;

import com.google.common.collect.HashBiMap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.BitcoinLinkGenerator;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.CustomKeypad;
import info.blockchain.wallet.view.helpers.ToastCustom;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentReceiveBinding;

public class ReceiveFragment extends Fragment implements CustomKeypadCallback {

    private Locale locale = null;

    public CustomKeypad customKeypad;

    //Drop down
    private ArrayAdapter<String> receiveToAdapter = null;
    private List<String> receiveToList = null;
    private HashBiMap<Object, Integer> accountBiMap = null;
    private SparseIntArray spinnerIndexAccountIndexMap = null;

    //text
    private boolean textChangeAllowed = true;
    private String defaultSeperator;
    private String strBTC = "BTC";
    private String strFiat = null;
    private boolean isBTC = true;
    private double btc_fx = 319.13;
    private final String addressInfoLink = "https://support.blockchain.com/hc/en-us/articles/210353663-Why-is-my-bitcoin-address-changing-";
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    private PayloadManager payloadManager;

    private FragmentReceiveBinding binding;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                updateSpinnerList();
                displayQRCode(binding.accounts.spinner.getSelectedItemPosition());
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_receive, container, false);

        payloadManager = PayloadManager.getInstance();
        prefsUtil = new PrefsUtil(getActivity());
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        locale = Locale.getDefault();

        setupToolbar();

        defaultSeperator = getDefaultDecimalSeparator();

        setupViews();

        setCustomKeypad();

        return binding.getRoot();
    }

    private void setupToolbar(){

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                Fragment fragment = new BalanceFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
            }
        });

        if(((AppCompatActivity) getActivity()).getSupportActionBar() == null){
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((AppCompatActivity) getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.receive_bitcoin);
        setHasOptionsMenu(true);
    }

    private String getDefaultDecimalSeparator(){
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    private void setupViews(){

        binding.receiveMainContentShadow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)) {
                    onShareClicked();
                }
            }
        });

        binding.qr.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Send address", binding.receivingAddress.getText().toString());
                                ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                                clipboard.setPrimaryClip(clip);

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

            }
        });

        binding.qr.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {

                onShareClicked();

                return true;
            }
        });

        binding.amountContainer.amountBtc.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
        binding.amountContainer.amountBtc.setHint("0" + defaultSeperator + "00");
        binding.amountContainer.amountBtc.setSelectAllOnFocus(true);
        binding.amountContainer.amountBtc.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                String input = s.toString();

                binding.amountContainer.amountBtc.removeTextChangedListener(this);

                int unit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
                int max_len = 8;
                NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
                switch (unit) {
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

                try {
                    if (input.indexOf(defaultSeperator) != -1) {
                        String dec = input.substring(input.indexOf(defaultSeperator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                binding.amountContainer.amountBtc.setText(input.substring(0, input.length() - 1));
                                binding.amountContainer.amountBtc.setSelection(binding.amountContainer.amountBtc.getText().length());
                                s = binding.amountContainer.amountBtc.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                binding.amountContainer.amountBtc.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateFiatTextField(s.toString());

                    displayQRCode(binding.accounts.spinner.getSelectedItemPosition());
                    textChangeAllowed = true;
                }

                if (s.toString().contains(defaultSeperator))
                    binding.amountContainer.amountBtc.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    binding.amountContainer.amountBtc.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        binding.amountContainer.amountFiat.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
        binding.amountContainer.amountFiat.setHint("0" + defaultSeperator + "00");
        binding.amountContainer.amountFiat.setSelectAllOnFocus(true);
        binding.amountContainer.amountFiat.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                String input = s.toString();

                binding.amountContainer.amountFiat.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                try {
                    if (input.indexOf(defaultSeperator) != -1) {
                        String dec = input.substring(input.indexOf(defaultSeperator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                binding.amountContainer.amountFiat.setText(input.substring(0, input.length() - 1));
                                binding.amountContainer.amountFiat.setSelection(binding.amountContainer.amountFiat.getText().length());
                                s = binding.amountContainer.amountFiat.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                binding.amountContainer.amountFiat.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateBtcTextField(s.toString());

                    displayQRCode(binding.accounts.spinner.getSelectedItemPosition());
                    textChangeAllowed = true;
                }

                if (s.toString().contains(defaultSeperator))
                    binding.amountContainer.amountFiat.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    binding.amountContainer.amountFiat.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeperator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        receiveToList = new ArrayList<>();
        accountBiMap = HashBiMap.create();
        spinnerIndexAccountIndexMap = new SparseIntArray();
        updateSpinnerList();

        if (receiveToList.size() == 1)
            binding.fromRow.setVisibility(View.GONE);

        receiveToAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_item, receiveToList);
        receiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        binding.accounts.spinner.setAdapter(receiveToAdapter);
        binding.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    binding.accounts.spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.setDropDownWidth(binding.accounts.spinner.getWidth());
                }
            }
        });
        binding.accounts.spinner.post(new Runnable() {
            public void run() {
                binding.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        binding.accounts.spinner.setSelection(binding.accounts.spinner.getSelectedItemPosition());
                        Object object = accountBiMap.inverse().get(binding.accounts.spinner.getSelectedItemPosition());

                        if (prefsUtil.getValue("WARN_WATCH_ONLY_SPEND", true)) {
                            promptWatchOnlySpendWarning(object);
                        }

                        displayQRCode(binding.accounts.spinner.getSelectedItemPosition());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                });
            }
        });

        strBTC = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance().getLastPrice(getActivity(), strFiat);

        binding.amountContainer.currencyBtc.setText(strBTC);
        binding.amountContainer.currencyFiat.setText(strFiat);

        binding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        binding.slidingLayout.setTouchEnabled(false);
        binding.slidingLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
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

        binding.ivAddressInfo.setOnClickListener(v -> new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(getString(R.string.why_has_my_address_changed))
                .setMessage(getString(R.string.new_address_info))
                .setPositiveButton(R.string.learn_more, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse(addressInfoLink));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.ok, null)
                .show());
    }

    private void setCustomKeypad(){

        customKeypad = new CustomKeypad(getActivity(), (binding.keypadContainer.numericPad));
        customKeypad.setDecimalSeparator(defaultSeperator);

        //Enable custom keypad and disables default keyboard from popping up
        customKeypad.enableOnView(binding.amountContainer.amountBtc);
        customKeypad.enableOnView(binding.amountContainer.amountFiat);

        binding.amountContainer.amountBtc.setText("");
        binding.amountContainer.amountBtc.requestFocus();
    }

    @Nullable
    public CustomKeypad getCustomKeypad() {
        return customKeypad;
    }

    private void promptWatchOnlySpendWarning(Object object){

        if (object instanceof LegacyAddress && ((LegacyAddress) object).isWatchOnly()) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
            AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()),
                    R.layout.alert_watch_only_spend, null, false);
            dialogBuilder.setView(dialogBinding.getRoot());
            dialogBuilder.setCancelable(false);

            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);

            dialogBinding.confirmCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    binding.accounts.spinner.setSelection(0, true);
                    if(dialogBinding.confirmDontAskAgain.isChecked()) prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", false);
                    alertDialog.dismiss();
                }
            });

            dialogBinding.confirmContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(dialogBinding.confirmDontAskAgain.isChecked()) prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", false);
                    alertDialog.dismiss();
                }
            });

            alertDialog.show();
        }
    }

    private void updateSpinnerList() {
        //receiveToList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        receiveToList.clear();
        accountBiMap.clear();
        spinnerIndexAccountIndexMap.clear();

        int spinnerIndex = 0;

        if (payloadManager.getPayload().isUpgraded()) {

            //V3
            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
            int accountIndex = 0;
            for (Account item : accounts) {

                spinnerIndexAccountIndexMap.put(spinnerIndex, accountIndex);
                accountIndex++;

                if (item.isArchived())
                    continue;//skip archived account

                //no xpub watch only yet

                receiveToList.add(item.getLabel());
                accountBiMap.put(item, spinnerIndex);
                spinnerIndex++;
            }
        }

        List<LegacyAddress> legacyAddresses = payloadManager.getPayload().getLegacyAddresses();

        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)
                continue;//skip archived address

            //If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel() == null || legacyAddress.getLabel().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();

            //Prefix "watch-only"
            if (legacyAddress.isWatchOnly()) {
                labelOrAddress = getActivity().getString(R.string.watch_only_label) + " " + labelOrAddress;
            }

            receiveToList.add(labelOrAddress);
            accountBiMap.put(legacyAddress, spinnerIndex);
            spinnerIndex++;
        }

        //Notify adapter of list update
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (receiveToAdapter != null) receiveToAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            strBTC = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            btc_fx = ExchangeRateFactory.getInstance().getLastPrice(getActivity(), strFiat);
            binding.amountContainer.currencyBtc.setText(isBTC ? strBTC : strFiat);
            binding.amountContainer.currencyFiat.setText(isBTC ? strFiat : strBTC);
        } else {
            ;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        strBTC = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance().getLastPrice(getActivity(), strFiat);
        binding.amountContainer.currencyBtc.setText(isBTC ? strBTC : strFiat);
        binding.amountContainer.currencyFiat.setText(isBTC ? strFiat : strBTC);

        selectDefaultAccount();

        updateSpinnerList();

        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }

    private void selectDefaultAccount() {

        if (binding.accounts.spinner != null) {

            if (payloadManager.getPayload().isUpgraded()) {
                int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
                Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);
                int defaultSpinnerIndex = accountBiMap.get(defaultAccount);
                displayQRCode(defaultSpinnerIndex);
            } else {
                //V2
                displayQRCode(0);//default to 0
            }
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onPause();
    }

    private void displayQRCode(int spinnerIndex) {

        binding.accounts.spinner.setSelection(spinnerIndex);
        String receiveAddress = null;

        Object object = accountBiMap.inverse().get(spinnerIndex);
        boolean shouldShowInfoButton = showAddressInfoButtonIfNecessary(object);

        if (object instanceof LegacyAddress) {

            //V2
            receiveAddress = ((LegacyAddress) object).getAddress();

        } else {
            //V3
            receiveAddress = getV3ReceiveAddress((Account) object);
        }

        binding.receivingAddress.setText(receiveAddress);

        BigInteger bamount = null;
        try {
            long lamount = 0L;
            if (isBTC) {
                lamount = (long) (Math.round(NumberFormat.getInstance(locale).parse(binding.amountContainer.amountBtc.getText().toString()).doubleValue() * 1e8));
            } else {
                lamount = (long) (Math.round(NumberFormat.getInstance(locale).parse(binding.amountContainer.amountFiat.getText().toString()).doubleValue() * 1e8));
            }
            bamount = monetaryUtil.getUndenominatedAmount(lamount);
            if (bamount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
                ToastCustom.makeText(getActivity(), getActivity().getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                return;
            }

            if (!bamount.equals(BigInteger.ZERO)) {
                generateQRCode(BitcoinURI.convertToBitcoinURI(receiveAddress, Coin.valueOf(bamount.longValue()), "", ""), shouldShowInfoButton);
            } else {
                generateQRCode("bitcoin:" + receiveAddress, shouldShowInfoButton);
            }
        } catch (NumberFormatException | ParseException e) {
            generateQRCode("bitcoin:" + receiveAddress, shouldShowInfoButton);
        }
    }

    private boolean showAddressInfoButtonIfNecessary(Object object) {
        return !(object instanceof ImportedAccount || object instanceof LegacyAddress);
    }

    private void generateQRCode(final String uri, boolean displayInfoButton) {

        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                binding.ivAddressInfo.setVisibility(View.GONE);
                binding.qr.setVisibility(View.GONE);
                binding.receivingAddress.setVisibility(View.GONE);
                binding.progressBar2.setVisibility(View.VISIBLE);
            }

            @Override
            protected Bitmap doInBackground(Void... params) {

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

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                binding.progressBar2.setVisibility(View.GONE);
                binding.qr.setVisibility(View.VISIBLE);
                binding.receivingAddress.setVisibility(View.VISIBLE);
                binding.qr.setImageBitmap(bitmap);
                if (displayInfoButton) binding.ivAddressInfo.setVisibility(View.VISIBLE);

                setupBottomSheet(uri);
            }
        }.execute();
    }

    private String getV3ReceiveAddress(Account account) {

        try {
            int spinnerIndex = accountBiMap.get(account);
            int accountIndex = spinnerIndexAccountIndexMap.get(spinnerIndex);
            return payloadManager.getReceiveAddress(accountIndex);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateFiatTextField(String cBtc) {
        if(cBtc.isEmpty())cBtc = "0";
        double btc_amount = 0.0;
        try {
            btc_amount = monetaryUtil.getUndenominatedAmount(NumberFormat.getInstance(locale).parse(cBtc).doubleValue());
        } catch (NumberFormatException | ParseException e) {
            btc_amount = 0.0;
        }
        double fiat_amount = btc_fx * btc_amount;
        binding.amountContainer.amountFiat.setText(monetaryUtil.getFiatFormat(strFiat).format(fiat_amount));
    }

    private void updateBtcTextField(String cfiat) {
        if(cfiat.isEmpty())cfiat = "0";
        double fiat_amount = 0.0;
        try {
            fiat_amount = NumberFormat.getInstance(locale).parse(cfiat).doubleValue();
        } catch (NumberFormatException | ParseException e) {
            fiat_amount = 0.0;
        }
        double btc_amount = fiat_amount / btc_fx;
        binding.amountContainer.amountBtc.setText(monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(btc_amount)));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_qr).setVisible(false);
        menu.findItem(R.id.action_send).setVisible(false);
        MenuItem i = menu.findItem(R.id.action_share_receive).setVisible(true);

        i.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                onShareClicked();

                return false;
            }
        });
    }

    private void onShareClicked() {

        customKeypad.setNumpadVisibility(View.GONE);

        if (binding.slidingLayout != null) {
            if (binding.slidingLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {
                binding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                binding.receiveMainContentShadow.setVisibility(View.GONE);
            } else {

                new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_share)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                binding.slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                                binding.receiveMainContentShadow.setVisibility(View.VISIBLE);
                                binding.receiveMainContentShadow.bringToFront();

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

            }
        }

    }

    private void setupBottomSheet(String uri) {

        //Re-Populate list
        String strFileName = new AppUtil(getActivity()).getReceiveQRFilename();
        File file = new File(strFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                ToastCustom.makeText(getActivity(), e.getMessage(), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            }
        }
        file.setReadable(true, false);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }

        if (file != null && fos != null) {
            Bitmap bitmap = ((BitmapDrawable) binding.qr.getDrawable()).getBitmap();
            bitmap.compress(CompressFormat.PNG, 0, fos);

            try {
                fos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            ArrayList<SendPaymentCodeData> dataList = new ArrayList<>();

            PackageManager pm = getActivity().getPackageManager();

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setType("application/image");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

            Intent imageIntent = new Intent();
            imageIntent.setAction(Intent.ACTION_SEND);
            imageIntent.setType("image/png");
            imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

            try {
                BitcoinURI addressUri = new BitcoinURI(uri);
                String amount = addressUri.getAmount() != null ? " " + addressUri.getAmount().toPlainString() : "";
                String address = addressUri.getAddress() != null ? addressUri.getAddress().toString() : getString(R.string.email_request_body_fallback);
                String body = String.format(getString(R.string.email_request_body), amount, address);

                String builder = "mailto:" +
                        "?subject=" +
                        getString(R.string.email_request_subject) +
                        "&body=" +
                        body +
                        '\n' +
                        '\n' +
                        BitcoinLinkGenerator.getLink(addressUri);

                emailIntent.setData(Uri.parse(builder));

            } catch (BitcoinURIParseException e) {
                Log.e(ReceiveFragment.class.getSimpleName(), "setupBottomSheet() threw BitcoinURIParseException");
            }

            HashMap<String, Pair<ResolveInfo, Intent>> intentHashMap = new HashMap<>();

            List<ResolveInfo> emailInfos = pm.queryIntentActivities(emailIntent, 0);
            addResolveInfoToMap(emailIntent, intentHashMap, emailInfos);

            List<ResolveInfo> imageInfos = pm.queryIntentActivities(imageIntent, 0);
            addResolveInfoToMap(imageIntent, intentHashMap, imageInfos);

            SendPaymentCodeData d;

            Iterator it = intentHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry mapItem = (Map.Entry)it.next();
                Pair<ResolveInfo, Intent> pair = (Pair<ResolveInfo, Intent>) mapItem.getValue();
                ResolveInfo resolveInfo = pair.first;
                String context = resolveInfo.activityInfo.packageName;
                String packageClassName = resolveInfo.activityInfo.name;
                CharSequence label = resolveInfo.loadLabel(pm);
                Drawable icon = resolveInfo.loadIcon(pm);

                Intent intent = pair.second;
                intent.setClassName(context, packageClassName);

                d = new SendPaymentCodeData();
                d.setTitle(label.toString());
                d.setLogo(icon);
                d.setIntent(intent);
                dataList.add(d);

                it.remove();
            }

            ArrayAdapter adapter = new SendPaymentCodeAdapter(getActivity(), dataList);
            binding.shareAppList.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Prevents apps being added to the list twice, as it's confusing for users. Full email intent
     * takes priority.
     */
    private void addResolveInfoToMap(Intent intent, HashMap<String, Pair<ResolveInfo, Intent>> intentHashMap, List<ResolveInfo> resolveInfo) {
        for (ResolveInfo info : resolveInfo) {
            if (!intentHashMap.containsKey(info.activityInfo.name)) {
                intentHashMap.put(info.activityInfo.name, new Pair<>(info, new Intent(intent)));
            }
        }
    }

    @Override
    public void onKeypadClose() {
        customKeypad.setNumpadVisibility(View.GONE);
    }

    class SendPaymentCodeAdapter extends ArrayAdapter<SendPaymentCodeData> {
        private final Context context;
        private final ArrayList<SendPaymentCodeData> repoDataArrayList;

        public SendPaymentCodeAdapter(Context context, ArrayList<SendPaymentCodeData> repoDataArrayList) {

            super(context, R.layout.fragment_receive_share_row, repoDataArrayList);

            this.context = context;
            this.repoDataArrayList = repoDataArrayList;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = null;
            rowView = inflater.inflate(R.layout.fragment_receive_share_row, parent, false);

            ImageView image = (ImageView) rowView.findViewById(R.id.share_app_image);
            TextView title = (TextView) rowView.findViewById(R.id.share_app_title);

            image.setImageDrawable(repoDataArrayList.get(position).getLogo());
            title.setText(repoDataArrayList.get(position).getTitle());

            rowView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(repoDataArrayList.get(position).getIntent());
                }
            });

            return rowView;
        }
    }

    class SendPaymentCodeData {
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
