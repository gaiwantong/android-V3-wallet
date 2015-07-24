package info.blockchain.wallet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.TxQueue;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import info.blockchain.wallet.util.TypefaceUtil;
import piuk.blockchain.android.R;

//import android.util.Log;

public class SendFragment extends Fragment {

    private Locale locale = null;

    private EditText edDestination = null;
    private EditText edAmount1 = null;
    private TextView tvCurrency1 = null;
    private EditText edAmount2 = null;
    private TextView tvFiat2 = null;
    private Spinner spAccounts = null;
    private ReselectSpinner spDestination = null;
    private MenuItem btSend;
    private TextView tvMax = null;

    private TextWatcher btcTextWatcher = null;
    private TextWatcher fiatTextWatcher = null;

    private String currentSelectedFromAddress = null;
    private String currentSelectedToAddress = null;

    private String strBTC = "BTC";
    private String strFiat = null;
    private boolean isBTC = true;
    private double btc_fx = 319.13;

    private boolean textChangeAllowed = true;
    private String defaultSeparator;//Decimal separator based on locale

    private boolean spendInProgress = false;
    private List<String> _accounts = null;
    private boolean spDestinationSelected = false;

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

        rootView.setFilterTouchesWhenObscured(true);

        locale = Locale.getDefault();

        ((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((ActionBarActivity)getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.send_bitcoin);
        setHasOptionsMenu(true);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
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

        edDestination = ((EditText)rootView.findViewById(R.id.destination));
        edDestination.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.NORMAL);
        edDestination.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    validateSpend(true);
                }

                return false;
            }
        });
        edDestination.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                if (edAmount1 != null && edDestination != null && edAmount2 != null && spAccounts != null) {
                    currentSelectedToAddress = edDestination.getText().toString();
                    validateSpend(false);
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
        defaultSeparator =Character.toString(symbols.getDecimalSeparator());

        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
        edAmount1.setHint("0" + defaultSeparator + "00");
        edAmount1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface(), Typeface.NORMAL);
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    validateSpend(true);
                }

                return false;
            }
        });

        btcTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {

                edAmount1.removeTextChangedListener(this);

                int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
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
                    double d = Double.parseDouble(s.toString());
                    String s1 = btcFormat.format(d);
                    if (s1.indexOf(defaultSeparator) != -1) {
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount1.setText(s1.substring(0, s1.length() - 1));
                                edAmount1.setSelection(edAmount1.getText().length());
                                s = edAmount1.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                edAmount1.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateFiatTextField(s.toString());

                    if (edAmount1 != null && edDestination != null && edAmount2 != null && spAccounts != null) {
                        currentSelectedToAddress = edDestination.getText().toString();
                        validateSpend(false);
                    }
                    textChangeAllowed = true;
                }

                if(s.toString().contains(defaultSeparator))
                    edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

        edAmount1.addTextChangedListener(btcTextWatcher);

        tvCurrency1 = (TextView)rootView.findViewById(R.id.currency1);
        edAmount2 = (EditText)rootView.findViewById(R.id.amount2);
        edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"+ defaultSeparator));
        edAmount2.setHint("0" + defaultSeparator + "00");
        tvFiat2 = (TextView)rootView.findViewById(R.id.fiat2);
        fiatTextWatcher = new TextWatcher()	{
            public void afterTextChanged(Editable s) {

                edAmount2.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                try	{
                    double d = Double.parseDouble(s.toString());
                    String s1 = fiatFormat.format(d);
                    if(s1.indexOf(defaultSeparator) != -1)	{
                        String dec = s1.substring(s1.indexOf(defaultSeparator));
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

                    if(edAmount1 != null && edDestination != null && edAmount2 != null && spAccounts != null) {
                        currentSelectedToAddress = edDestination.getText().toString();
                        validateSpend(false);
                    }
                    textChangeAllowed = true;
                }

                if(s.toString().contains(defaultSeparator))
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }

            public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        };
        edAmount2.addTextChangedListener(fiatTextWatcher);

        spAccounts = (Spinner)rootView.findViewById(R.id.accounts);

        _accounts = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountList();

        if(_accounts.size()==1)rootView.findViewById(R.id.from_row).setVisibility(View.GONE);

        AccountAdapter dataAdapter = new AccountAdapter(getActivity(), R.layout.spinner_item, _accounts);
        spAccounts.setAdapter(dataAdapter);
        spAccounts.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spAccounts.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spAccounts.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spAccounts.setDropDownWidth(spAccounts.getWidth());
            }
        });

        spAccounts.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        int position = spAccounts.getSelectedItemPosition();
                        AccountsUtil.getInstance(getActivity()).setCurrentSpinnerIndex(position + 1);//all account included

                        if (position >= AccountsUtil.getInstance(getActivity()).getLastHDIndex()) {

                            LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - AccountsUtil.getInstance(getActivity()).getLastHDIndex());

                            if(legacyAddress.getTag() == PayloadFactory.WATCHONLY_ADDRESS)    {
                                spAccounts.setSelection(0);
                                ToastCustom.makeText(getActivity(), getString(R.string.watchonly_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            }
                            else if(legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)   {
                                spAccounts.setSelection(0);
                                ToastCustom.makeText(getActivity(), getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            }
                            else    {
                                currentSelectedFromAddress = legacyAddress.getAddress();
                            }

                        }

                        displayMaxAvailable();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                }
        );

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);

        tvMax = (TextView)rootView.findViewById(R.id.max);
        tvMax.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());

        DestinationAdapter destinationAdapter = new DestinationAdapter(getActivity(), R.layout.spinner_item, _accounts);
        destinationAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spDestination = (ReselectSpinner)rootView.findViewById(R.id.sp_destination);
        if(_accounts.size() <= 1)
            spDestination.setVisibility(View.GONE);
        spDestination.setAdapter(destinationAdapter);
        spDestination.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spDestination.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spDestination.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spDestination.setDropDownWidth(spAccounts.getWidth());
            }
        });

        spDestination.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        int position = spDestination.getSelectedItemPosition();
                        spDestination.getSelectedItem().toString();

                        if (position >= AccountsUtil.getInstance(getActivity()).getLastHDIndex()) {
                            //Legacy addresses
                            LegacyAddress account = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - AccountsUtil.getInstance(getActivity()).getLastHDIndex());

                            if(account.getTag() == PayloadFactory.WATCHONLY_ADDRESS)    {
                                edDestination.setText("");
                                ToastCustom.makeText(getActivity(), getString(R.string.watchonly_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            }
                            else if(account.getTag() == PayloadFactory.ARCHIVED_ADDRESS)   {
                                edDestination.setText("");
                                ToastCustom.makeText(getActivity(), getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            }
                            else    {
                                currentSelectedToAddress = account.getAddress();
                            }

                        } else {
                            //hd accounts
                            Integer currentSelectedAccount = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position);
                            try {
                                ReceiveAddress currentSelectedReceiveAddress = HDPayloadBridge.getInstance(getActivity()).getReceiveAddress(currentSelectedAccount);
                                currentSelectedToAddress = currentSelectedReceiveAddress.getAddress();

                            } catch (IOException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                                    | MnemonicException.MnemonicWordException | AddressFormatException
                                    | DecoderException e) {
                                e.printStackTrace();
                            }
                        }

//                        edDestination.setText(label);//future use
                        edDestination.setText(currentSelectedToAddress);
                        spDestinationSelected = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

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
                edAmount1.removeTextChangedListener(btcTextWatcher);
                edAmount2.removeTextChangedListener(fiatTextWatcher);

                edAmount1.setText(amount_arg);
                edAmount1.setSelection(edAmount1.getText().toString().length());

                double btc_amount = 0.0;
                try {
                    btc_amount = NumberFormat.getInstance(locale).parse(edAmount1.getText().toString()).doubleValue();
                }
                catch(NumberFormatException | ParseException e) {
                    btc_amount = 0.0;
                }

                // sanity check on strFiat, necessary if the result of a URI scan
                if(strFiat == null) {
                    strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
                }
                btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

                double fiat_amount = btc_fx * btc_amount;
                edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
//                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
                strBTC = MonetaryUtil.getInstance().getBTCUnit(MonetaryUtil.UNIT_BTC);
                tvCurrency1.setText(strBTC);
                tvFiat2.setText(strFiat);

                edAmount1.addTextChangedListener(btcTextWatcher);
                edAmount2.addTextChangedListener(fiatTextWatcher);

                validate = true;
            }
            if(validate) {
                validateSpend(true);
            }
        }

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
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

        MainActivity.currentFragment = this;

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
        tvCurrency1.setText(isBTC ? strBTC : strFiat);
        tvFiat2.setText(isBTC ? strFiat : strBTC);

        if(getArguments()!=null)
            if(getArguments().getBoolean("incoming_from_scan", false)) {
                ;
            }

        if(spAccounts != null) {

            int currentSelected = AccountsUtil.getInstance(getActivity()).getCurrentSpinnerIndex();
            if(currentSelected!=0)currentSelected--;//exclude 'all account'
            spAccounts.setSelection(currentSelected);
        }

        displayMaxAvailable();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void validateSpend(boolean showMessages) {

        pendingSpend.amount = null;
        pendingSpend.destination = null;
        pendingSpend.bamount = BigInteger.ZERO;
        pendingSpend.bfee = SendFactory.bFee;
        pendingSpend.isHD = true;
        pendingSpend.btc_units = strBTC;

        int position = spAccounts.getSelectedItemPosition();
        int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();

        //Legacy addresses
        if(position >= hdAccountsIdx) {
            LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
            currentSelectedFromAddress = legacyAddress.getAddress();
            if(legacyAddress.getLabel() != null && legacyAddress.getLabel().length() > 0) {
                pendingSpend.sending_from = legacyAddress.getLabel();
            }
            else {
                pendingSpend.sending_from = legacyAddress.getAddress();
            }
            pendingSpend.isHD = false;
        }
        else {

            //HD accounts
            Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
            pendingSpend.sending_from = hda.getLabel();
            pendingSpend.isHD = true;
        }

        pendingSpend.destination = currentSelectedToAddress;
        if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)) {
            if(showMessages) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            return;
        }

        if(isBTC) {
            pendingSpend.amount = edAmount1.getText().toString();
        }
        else {
            pendingSpend.amount = edAmount2.getText().toString();
        }
        long lamount = 0L;
        try {
            lamount = (long)(NumberFormat.getInstance(locale).parse(pendingSpend.amount).doubleValue() * 1e8);
            pendingSpend.bamount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount);
            if(!(pendingSpend.bamount.compareTo(BigInteger.ZERO) >= 0)) {
                if(showMessages) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return;
            }
        }
        catch(NumberFormatException nfe) {
            if(showMessages) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            return;
        }
        catch(ParseException pe) {
            if(showMessages) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            return;
        }

        if(pendingSpend.isHD) {
            String xpub = HDPayloadBridge.getInstance().account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));

            if(xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long _lamount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendFactory.bFee.longValue()) > _lamount) {
                    if(showMessages) {
                        ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                    return;
                }
            }
        }
        else {
            long _lamount = MultiAddrFactory.getInstance().getLegacyBalance(currentSelectedFromAddress);
            if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendFactory.bFee.longValue()) > _lamount) {
                if(showMessages) {
                    ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                return;
            }
        }

    }

    private void updateFiatTextField(String cBtc) {
        double btc_amount = 0.0;
        try {
            btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(NumberFormat.getInstance(locale).parse(cBtc).doubleValue());
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
        catch(NumberFormatException | ParseException e) {
            fiat_amount = 0.0;
        }
        double btc_amount = fiat_amount / btc_fx;
        edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_amount)));
    }

    private void displayMaxAvailable() {

        int position = spAccounts.getSelectedItemPosition();
        long amount = 0L;
        int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();
        if(position >= hdAccountsIdx) {
            amount = MultiAddrFactory.getInstance().getLegacyBalance(AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx).getAddress());
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
        
        long amount_available = amount - SendFactory.bFee.longValue();
        if(amount_available > 0L) {
            double btc_balance = (((double)amount_available) / 1e8);
            tvMax.setText(getActivity().getResources().getText(R.string.max_available) + " " + MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_balance)) + " " + strBTC);
        }
        else {
            tvMax.setText(R.string.no_funds_available);
        }

    }

    public String account2Xpub(int sel) {

        Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        String xpub = null;
        if(hda instanceof ImportedAccount) {
            xpub = null;
        }
        else {
            xpub = HDPayloadBridge.getInstance(getActivity()).account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        }

        return xpub;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_merchant_directory).setVisible(false);
        menu.findItem(R.id.action_qr).setVisible(true);
        menu.findItem(R.id.action_share_receive).setVisible(false);

        btSend = menu.findItem(R.id.action_send);
        btSend.setVisible(true);

        btSend.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                sendClicked();

                return false;
            }
        });
    }

    private void sendClicked(){

        if(isBTC) {
            pendingSpend.btc_amount = edAmount1.getText().toString().replace(defaultSeparator,".");
            pendingSpend.fiat_amount = edAmount2.getText().toString().replace(defaultSeparator, ".");
        }
        else {
            pendingSpend.fiat_amount = edAmount1.getText().toString().replace(defaultSeparator, ".");
            pendingSpend.btc_amount = edAmount2.getText().toString().replace(defaultSeparator,".");
        }

        pendingSpend.btc_units = strBTC;

        double amt = 0.0;
        try {
            amt = Double.parseDouble(pendingSpend.btc_amount);
        }
        catch(NumberFormatException nfe) {
            ;
        }
        if(amt == 0.0) {
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        if(pendingSpend.isHD) {
            if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {

                confirmPayment(true, AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()), null);
            }
            else if(DoubleEncryptionFactory.getInstance().isActivated()) {

                confirmPayment(true, AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()), null);
            }
            else {

                final EditText password = new EditText(getActivity());
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.enter_double_encryption_pw)
                        .setView(password)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                final String pw = password.getText().toString();

                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));

                                if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword(), PayloadFactory.getInstance().get().getIterations())) {

                                    String encrypted_hex = PayloadFactory.getInstance().get().getHdWallet().getSeedHex();
                                    String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                                            encrypted_hex,
                                            PayloadFactory.getInstance().get().getSharedKey(),
                                            pw,
                                            PayloadFactory.getInstance().get().getIterations());

                                    try {
                                        HD_Wallet hdw = HD_WalletFactory.getInstance(getActivity()).restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
                                        HD_WalletFactory.getInstance(getActivity()).setWatchOnlyWallet(hdw);
                                    } catch (IOException | DecoderException | AddressFormatException |
                                            MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException |
                                            MnemonicException.MnemonicWordException e) {
                                        e.printStackTrace();
                                    } finally {
                                        ;
                                    }

                                    confirmPayment(true, AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()), null);
                                } else {
                                    ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
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
            List<LegacyAddress> legacy = AccountsUtil.getInstance(getActivity()).getLegacyAddresses();
            for(int i = 0; i < legacy.size(); i++) {
                if(legacy.get(i).getAddress().equals(currentSelectedFromAddress)) {
                    addr = legacy.get(i);
                    break;
                }
            }

            if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {

                confirmPayment(false, -1, addr);
            }
            else if(DoubleEncryptionFactory.getInstance().isActivated()) {

                confirmPayment(false, -1, addr);
            }
            else {

                final LegacyAddress legacyAddress = addr;
                final EditText password = new EditText(getActivity());
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.enter_double_encryption_pw)
                        .setView(password)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                final String pw = password.getText().toString();

                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));

                                if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword(), PayloadFactory.getInstance().get().getIterations())) {

                                    confirmPayment(false, -1, legacyAddress);
                                } else {
                                    ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
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

    class AccountAdapter extends ArrayAdapter<String>{

        public AccountAdapter(Context context, int textViewResourceId, List<String> accounts) {
            super(context, textViewResourceId, accounts);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, false);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent, boolean isDropdown) {

            LayoutInflater inflater = getActivity().getLayoutInflater();

            int layoutRes = R.layout.spinner_item;
            if(isDropdown)layoutRes = R.layout.fragment_send_account_row_dropdown;

            View row =  inflater.inflate(layoutRes, parent, false);

            TextView label = null;
            TextView balance = null;
            if(isDropdown) {
                label = (TextView) row.findViewById(R.id.receive_account_label);
                balance = (TextView) row.findViewById(R.id.receive_account_balance);
            }else
                label = (TextView) row.findViewById(R.id.text);

            String labelText = "";
            long amount = 0L;
            int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();
            if(position >= hdAccountsIdx) {
                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
                if(legacyAddress.getLabel() != null && legacyAddress.getLabel().length() > 0) {
                    labelText = legacyAddress.getLabel();
                }
                else {
                    labelText = legacyAddress.getAddress();
                }
            }
            else {
                Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
                labelText = hda.getLabel();
            }

            if(position >= hdAccountsIdx) {
                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
                amount = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
            }
            else {
                Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
                String xpub = account2Xpub(position);
                if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                }
                else {
                    amount = 0L;
                }
            }

            if(isDropdown) {
                balance.setText("(" + MonetaryUtil.getInstance(getActivity()).getDisplayAmount(amount) + " " + strBTC + ")");
                label.setText(labelText);
            }else
                label.setText(labelText);

            return row;
        }
    }

    private void confirmPayment(final boolean isHd, final int currentAcc, final LegacyAddress legacyAddress){

        if(ConnectivityStatus.hasConnectivity(getActivity()))    {

            if(isValidSpend()) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.fragment_send_confirm, null);
                dialogBuilder.setView(dialogView);

                final AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(false);

                TextView confirmDestination = (TextView) dialogView.findViewById(R.id.confirm_to);
                confirmDestination.setText(pendingSpend.destination);

                TextView confirmFee = (TextView) dialogView.findViewById(R.id.confirm_fee);
                confirmFee.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(pendingSpend.bfee.longValue()) + " " + strBTC);

                TextView confirmTotal = (TextView) dialogView.findViewById(R.id.confirm_total_to_send);
                BigInteger cTotal = (pendingSpend.bamount.add(pendingSpend.bfee));
                confirmTotal.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(cTotal.longValue()) + " " + strBTC);

                TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
                confirmCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) alertDialog.cancel();
                    }
                });

                TextView confirmSend = (TextView) dialogView.findViewById(R.id.confirm_send);
                confirmSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(!spendInProgress) {
                            spendInProgress = true;

                            final int account = currentAcc;
                            final String destination = pendingSpend.destination;
                            final BigInteger bamount = pendingSpend.bamount;
                            final BigInteger bfee = pendingSpend.bfee;
                            final String strNote = null;

                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    Looper.prepare();

                                    UnspentOutputsBundle unspents = SendFactory.getInstance(getActivity()).send1(isHd ? account : -1, destination, bamount, legacyAddress == null ? null : legacyAddress, bfee, strNote);

                                    if(unspents != null) {

                                        if(isHd) {

                                            SendFactory.getInstance(getActivity()).send2(account, unspents.getOutputs(), destination, bamount, null, bfee, strNote, false, new OpCallback() {

                                                public void onSuccess() {
                                                }

                                                @Override
                                                public void onSuccess(final String hash) {
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            ToastCustom.makeText(getActivity(), getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                                            PayloadFactory.getInstance(getActivity()).remoteSaveThread();

                                                            MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (bamount.longValue() + bfee.longValue()));
                                                            MultiAddrFactory.getInstance().setXpubAmount(HDPayloadBridge.getInstance(getActivity()).account2Xpub(account), MultiAddrFactory.getInstance().getXpubAmounts().get(HDPayloadBridge.getInstance(getActivity()).account2Xpub(account)) - (bamount.longValue() + bfee.longValue()));

                                                            updateTx(isHd, strNote, hash, currentAcc, null);
                                                        }
                                                    });
                                                }

                                                public void onFail() {

                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            ToastCustom.makeText(getActivity(), getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                                        }
                                                    });
                                                }

                                            });
                                        }
                                        else if (legacyAddress != null) {

                                            SendFactory.getInstance(getActivity()).send2(-1, unspents.getOutputs(), destination, bamount, legacyAddress, bfee, strNote, false, new OpCallback() {

                                                public void onSuccess() {
                                                }

                                                @Override
                                                public void onSuccess(final String hash) {
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            ToastCustom.makeText(getActivity(), getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                                                            if (strNote != null) {
                                                                PayloadFactory.getInstance(getActivity()).remoteSaveThread();
                                                            }

                                                            MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (bamount.longValue() + bfee.longValue()));
                                                            MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (bamount.longValue() + bfee.longValue()));
                                                            MultiAddrFactory.getInstance().setLegacyBalance(destination, MultiAddrFactory.getInstance().getLegacyBalance(destination) - (bamount.longValue() + bfee.longValue()));

                                                            updateTx(isHd, strNote, hash, 0, legacyAddress);
                                                        }
                                                    });
                                                }

                                                public void onFail() {
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            ToastCustom.makeText(getActivity(), getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                                        }
                                                    });
                                                }

                                            });
                                        }

                                        if (alertDialog != null && alertDialog.isShowing())
                                            alertDialog.cancel();

                                        updateTx(isHd, strNote, TxQueue.TX_QUEUED, 0, legacyAddress);
//                                        ToastCustom.makeText(getActivity(), getResources().getString(R.string.transaction_queued), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

                                        Fragment fragment = new BalanceFragment();
                                        FragmentManager fragmentManager = getFragmentManager();
                                        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
                                    }
                                    else{
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                ToastCustom.makeText(getActivity(), getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                                if (alertDialog != null && alertDialog.isShowing())
                                                    alertDialog.cancel();
                                                Fragment fragment = new BalanceFragment();
                                                FragmentManager fragmentManager = getFragmentManager();
                                                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

                                            }
                                        });
                                    }

                                    Looper.loop();

                                }
                            }).start();

                            spendInProgress = false;
                        }
                    }
                });

                alertDialog.show();
            }
        }
        else    {
            ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

    }

    private boolean isValidSpend() {

        try{
            if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }catch(Exception e){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        long lamount = 0L;
        try {
            lamount = (long)(NumberFormat.getInstance(locale).parse(pendingSpend.amount).doubleValue() * 1e8);
            if(!(pendingSpend.bamount.compareTo(BigInteger.ZERO) >= 0)) {
                ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }
        catch(NumberFormatException | ParseException e) {
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        if(pendingSpend.isHD) {

            String xpub = HDPayloadBridge.getInstance().account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spAccounts.getSelectedItemPosition()));

            if(xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long _lamount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendFactory.bFee.longValue()) > _lamount) {
                    ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    return false;
                }
            }
        }
        else {
            long _lamount = MultiAddrFactory.getInstance().getLegacyBalance(currentSelectedFromAddress);
            if((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(lamount).longValue() + SendFactory.bFee.longValue()) > _lamount) {
                ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }

        return true;
    }

    class DestinationAdapter extends ArrayAdapter<String> {

        private ArrayList<String> items;

        public DestinationAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
            this.items = new ArrayList<String>(items);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, true);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, false);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent, boolean isDropdown) {

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View row = null;
            if(isDropdown){
                int layoutRes = R.layout.fragment_send_account_row_dropdown;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.receive_account_label);
                label.setText(items.get(position));
            }else{
                int layoutRes = R.layout.spinner_item;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.text);
                label.setText("");
            }

            return row;
        }
    }

    private void updateTx(boolean isHd, String strNote, String hash, int currentAcc, LegacyAddress legacyAddress) {

        String direction = MultiAddrFactory.SENT;
        if(spDestinationSelected)direction = MultiAddrFactory.MOVED;

        if(isHd){
            Tx tx = new Tx(hash, strNote, direction, -(pendingSpend.bamount.doubleValue()+pendingSpend.bfee.doubleValue()), System.currentTimeMillis()/1000, new HashMap<Integer,String>());
            if(spDestinationSelected)tx.setIsMove(true);
            MultiAddrFactory.getInstance().getXpubTxs().get(account2Xpub(currentAcc)).add(tx);
        }else{
            Tx tx = new Tx(hash, strNote, direction, -(pendingSpend.bamount.doubleValue()+pendingSpend.bfee.doubleValue()), System.currentTimeMillis()/1000, new HashMap<Integer,String>());
            if(spDestinationSelected)tx.setIsMove(true);
            MultiAddrFactory.getInstance().getAddressLegacyTxs(legacyAddress.getAddress()).add(tx);
            MultiAddrFactory.getInstance().getLegacyTxs().add(tx);
        }

    }
}