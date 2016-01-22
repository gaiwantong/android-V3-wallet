package info.blockchain.wallet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ReselectSpinner;
import info.blockchain.wallet.util.ToastCustom;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.R;

public class SendFragment extends Fragment implements View.OnClickListener, CustomKeypadCallback {

    public static boolean isKeypadVisible = false;
    private static Context context = null;
    private View rootView;

    private EditText edReceiveTo = null;
    private EditText edAmount1 = null;
    private TextView tvCurrency1 = null;
    private EditText edAmount2 = null;
    private TextView tvFiat2 = null;
    private MenuItem btSend;
    private TextView tvMax = null;
    private TableLayout numpad;

    private Spinner spinnerSendFrom = null;
    private ReselectSpinner spinnerReceiveTo = null;
    private List<String> accountList = null;

    private TextWatcher btcTextWatcher = null;
    private TextWatcher fiatTextWatcher = null;

    private ProgressDialog progress = null;

    private String strBTC = "BTC";
    private String strFiat = null;
    private double btc_fx = 319.13;
    private boolean textChangeAllowed = true;
    private String defaultSeparator;//Decimal separator based on locale
    private boolean spendInProgress = false;//Used to avoid double clicking on spend and opening confirm dialog twice
    private boolean spDestinationSelected = false;//When a destination is selected from dropdown, mark spend as 'Moved'

    private class PendingSpend {

        boolean isHD;
        int fromXpubIndex;
        LegacyAddress fromLegacyAddress;
        String note;
        String destination;
        BigInteger bigIntFee;
        BigInteger bigIntAmount;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        setupToolbar();

        setupViews();

        defaultSeparator = getDefaultDecimalSeparator();

        setBtcTextWatcher();

        setFiatTextWatcher();

        //accountList is linked to Adapters - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        accountList = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountList();

        setSendFromDropDown();

        setReceiveToDropDown();

        initVars();

        handleIncomingQRScan();

        decimalCompatCheck(rootView);

        return rootView;
    }

    private void setupToolbar(){

        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((ActionBarActivity) getActivity()).findViewById(R.id.account_spinner).setVisibility(View.GONE);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(R.string.send_bitcoin);
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

    }

    private void setupViews(){

        spinnerSendFrom = (Spinner) rootView.findViewById(R.id.accounts);
        spinnerReceiveTo = (ReselectSpinner) rootView.findViewById(R.id.sp_destination);
        edReceiveTo = ((EditText) rootView.findViewById(R.id.destination));

        edAmount1 = ((EditText) rootView.findViewById(R.id.amount1));
        edAmount2 = (EditText) rootView.findViewById(R.id.amount2);

        tvCurrency1 = (TextView) rootView.findViewById(R.id.currency1);
        tvFiat2 = (TextView) rootView.findViewById(R.id.fiat2);
        tvMax = (TextView) rootView.findViewById(R.id.max);

    }

    private void setSendFromDropDown(){

        if (accountList.size() == 1) rootView.findViewById(R.id.from_row).setVisibility(View.GONE);

        SendFromAdapter dataAdapter = new SendFromAdapter(getActivity(), R.layout.spinner_item, accountList);
        spinnerSendFrom.setAdapter(dataAdapter);
        spinnerSendFrom.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spinnerSendFrom.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spinnerSendFrom.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spinnerSendFrom.setDropDownWidth(spinnerSendFrom.getWidth());
            }
        });

        spinnerSendFrom.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        //Retain selected account/address for Balance and Receive fragment
                        AccountsUtil.getInstance(getActivity()).setCurrentSpinnerIndex(spinnerSendFrom.getSelectedItemPosition() + 1);//all account included

                        displayMaxAvailable(spinnerSendFrom.getSelectedItemPosition());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                }
        );

    }

    private void setReceiveToDropDown(){

        ReceiveToAdapter receiveToAdapter = new ReceiveToAdapter(getActivity(), R.layout.spinner_item, accountList);
        receiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);

        //If there is only 1 account/address - hide drop down
        if (accountList.size() <= 1)spinnerReceiveTo.setVisibility(View.GONE);

        spinnerReceiveTo.setAdapter(receiveToAdapter);
        spinnerReceiveTo.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    spinnerReceiveTo.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    spinnerReceiveTo.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                spinnerReceiveTo.setDropDownWidth(spinnerSendFrom.getWidth());
            }
        });

        spinnerReceiveTo.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                        int position = spinnerReceiveTo.getSelectedItemPosition();
                        spinnerReceiveTo.getSelectedItem().toString();
                        String receiveAddress = null;

                        if (position >= AccountsUtil.getInstance(getActivity()).getLastHDIndex()) {
                            //Legacy addresses
                            final LegacyAddress account = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - AccountsUtil.getInstance(getActivity()).getLastHDIndex());

                            if (account.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                                edReceiveTo.setText("");
                                ToastCustom.makeText(getActivity(), getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                return;
                            } else if (account.isWatchOnly()) {

                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.warning)
                                        .setCancelable(false)
                                        .setMessage(R.string.watchonly_address_receive_warning)
                                        .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                edReceiveTo.setText(account.getAddress());
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        edReceiveTo.setText("");
                                    }
                                }).show();

                                return;
                            } else {
                                receiveAddress = account.getAddress();
                            }

                        } else {
                            //hd accounts
                            Integer currentSelectedAccount = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position);
                            try {
                                ReceiveAddress currentSelectedReceiveAddress = HDPayloadBridge.getInstance(getActivity()).getReceiveAddress(currentSelectedAccount);
                                receiveAddress = currentSelectedReceiveAddress.getAddress();

                            } catch (IOException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                                    | MnemonicException.MnemonicWordException | AddressFormatException
                                    | DecoderException e) {
                                e.printStackTrace();
                            }
                        }

                        edReceiveTo.setText(receiveAddress);
                        spDestinationSelected = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

    }

    private String getDefaultDecimalSeparator(){
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    private void initVars(){

        edAmount1.addTextChangedListener(btcTextWatcher);

        edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
        edAmount2.setHint("0" + defaultSeparator + "00");
        edAmount2.addTextChangedListener(fiatTextWatcher);

        edReceiveTo.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    validateSpend(true, spinnerSendFrom.getSelectedItemPosition());
                }

                return false;
            }
        });
        edReceiveTo.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                if (edAmount1 != null && edReceiveTo != null && edAmount2 != null && spinnerSendFrom != null) {
//                    validateSpend(false, spinnerSendFrom.getSelectedItemPosition());
                }

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        });

        edAmount1.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
        edAmount1.setHint("0" + defaultSeparator + "00");
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    validateSpend(true, spinnerSendFrom.getSelectedItemPosition());
                }

                return false;
            }
        });

        strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);
    }

    private void handleIncomingQRScan(){

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String address_arg = bundle.getString("btc_address", "");
            String amount_arg = bundle.getString("btc_amount", "");
            if (!address_arg.equals("")) {
                edReceiveTo.setText(address_arg);
            }
            if (!amount_arg.equals("")) {
                edAmount1.removeTextChangedListener(btcTextWatcher);
                edAmount2.removeTextChangedListener(fiatTextWatcher);

                edAmount1.setText(amount_arg);
                edAmount1.setSelection(edAmount1.getText().toString().length());

                double btc_amount = 0.0;
                try {
                    btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()));
                } catch (NumberFormatException e) {
                    btc_amount = 0.0;
                }

                // sanity check on strFiat, necessary if the result of a URI scan
                if (strFiat == null) {
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
            }
        }
    }

    private void setBtcTextWatcher(){

        btcTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {

                String input = s.toString();

                long lamount = 0L;
                try {
                    //Long is safe to use, but double can lead to ugly rounding issues..
                    lamount = (BigDecimal.valueOf(MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(edAmount1.getText().toString()))).multiply(BigDecimal.valueOf(100000000)).longValue());

                    if (BigInteger.valueOf(lamount).compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
                        ToastCustom.makeText(getActivity(), getActivity().getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        edAmount1.setText("0.0");
                        return;
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }
                //TODO: I feel this is a little bit hacky, but it will solve the problem of keeping the amount after a scan
                ((MainActivity) getActivity()).sendFragmentBitcoinAmountStorage = lamount;

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
                    if (input.indexOf(defaultSeparator) != -1) {
                        String dec = input.substring(input.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount1.setText(input.substring(0, input.length() - 1));
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

//                    if (edAmount1 != null && edReceiveTo != null && edAmount2 != null && spinnerSendFrom != null) {
//                        validateSpend(false, spinnerSendFrom.getSelectedItemPosition());
//                    }
                    textChangeAllowed = true;
                }

                if (s.toString().contains(defaultSeparator))
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

    }

    private void setFiatTextWatcher(){

        fiatTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {

                String input = s.toString();

                edAmount2.removeTextChangedListener(this);

                int max_len = 2;
                NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
                fiatFormat.setMaximumFractionDigits(max_len + 1);
                fiatFormat.setMinimumFractionDigits(0);

                try {
                    if (input.indexOf(defaultSeparator) != -1) {
                        String dec = input.substring(input.indexOf(defaultSeparator));
                        if (dec.length() > 0) {
                            dec = dec.substring(1);
                            if (dec.length() > max_len) {
                                edAmount2.setText(input.substring(0, input.length() - 1));
                                edAmount2.setSelection(edAmount2.getText().length());
                                s = edAmount2.getEditableText();
                            }
                        }
                    }
                } catch (NumberFormatException nfe) {
                    ;
                }

                edAmount2.addTextChangedListener(this);

                if (textChangeAllowed) {
                    textChangeAllowed = false;
                    updateBtcTextField(s.toString());

//                    if (edAmount1 != null && edReceiveTo != null && edAmount2 != null && spinnerSendFrom != null) {
//                        validateSpend(false, spinnerSendFrom.getSelectedItemPosition());
//                    }
                    textChangeAllowed = true;
                }

                if (s.toString().contains(defaultSeparator))
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                else
                    edAmount2.setKeyListener(DigitsKeyListener.getInstance("0123456789" + defaultSeparator));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

    }

    /*
    Custom keypad implementation
    Numerous Samsung devices have keypad issues where decimal separators are absent.
     */
    private void decimalCompatCheck(View rootView) {

        edAmount1.removeTextChangedListener(btcTextWatcher);
        edAmount2.removeTextChangedListener(fiatTextWatcher);

        numpad = ((TableLayout) rootView.findViewById(R.id.numericPad));

        if (Build.MANUFACTURER.toLowerCase().contains("samsung")) {

            numpad.setVisibility(View.GONE);
            isKeypadVisible = false;
            edAmount1.setTextIsSelectable(true);
            edAmount1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        numpad.setVisibility(View.VISIBLE);
                        isKeypadVisible = true;
                    } else {
                        numpad.setVisibility(View.GONE);
                        isKeypadVisible = false;
                    }
                }
            });
            edAmount1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    numpad.setVisibility(View.VISIBLE);
                    isKeypadVisible = true;
                }
            });

            edAmount2.setTextIsSelectable(true);
            edAmount2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {

                    if (hasFocus) {
                        View view = getActivity().getCurrentFocus();
                        if (view != null) {
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        numpad.setVisibility(View.VISIBLE);
                        isKeypadVisible = true;
                    } else {
                        numpad.setVisibility(View.GONE);
                        isKeypadVisible = false;
                    }
                }
            });
            edAmount2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    numpad.setVisibility(View.VISIBLE);
                    isKeypadVisible = true;
                }
            });

            rootView.findViewById(R.id.button1).setOnClickListener(this);
            rootView.findViewById(R.id.button2).setOnClickListener(this);
            rootView.findViewById(R.id.button3).setOnClickListener(this);
            rootView.findViewById(R.id.button4).setOnClickListener(this);
            rootView.findViewById(R.id.button5).setOnClickListener(this);
            rootView.findViewById(R.id.button6).setOnClickListener(this);
            rootView.findViewById(R.id.button7).setOnClickListener(this);
            rootView.findViewById(R.id.button8).setOnClickListener(this);
            rootView.findViewById(R.id.button9).setOnClickListener(this);
            rootView.findViewById(R.id.button10).setOnClickListener(this);
            rootView.findViewById(R.id.button0).setOnClickListener(this);
            rootView.findViewById(R.id.buttonDeleteBack).setOnClickListener(this);
            rootView.findViewById(R.id.buttonDone).setOnClickListener(this);

        }

        edAmount1.addTextChangedListener(btcTextWatcher);
        edAmount2.addTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            strBTC = MonetaryUtil.getInstance().getBTCUnit(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
            strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
            btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);
            tvCurrency1.setText(strBTC);
            tvFiat2.setText(strFiat);
            displayMaxAvailable(spinnerSendFrom.getSelectedItemPosition());
        } else {
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
        tvCurrency1.setText(strBTC);
        tvFiat2.setText(strFiat);

        if (getArguments() != null)
            if (getArguments().getBoolean("incoming_from_scan", false)) {
                ;
            }

        if (spinnerSendFrom != null) {

            int currentSelected = AccountsUtil.getInstance(getActivity()).getCurrentSpinnerIndex();
            if (currentSelected != 0) currentSelected--;//exclude 'all account'
            spinnerSendFrom.setSelection(currentSelected);
        }

        displayMaxAvailable(spinnerSendFrom.getSelectedItemPosition());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateFiatTextField(String cBtc) {
        double btc_amount = 0.0;
        try {
            btc_amount = MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(NumberFormat.getInstance(Locale.getDefault()).parse(cBtc).doubleValue());
        } catch (NumberFormatException nfe) {
            btc_amount = 0.0;
        } catch (ParseException pe) {
            btc_amount = 0.0;
        }
        double fiat_amount = btc_fx * btc_amount;
        edAmount2.setText(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_amount));
    }

    private void updateBtcTextField(String cfiat) {

        double fiat_amount = 0.0;
        try {
            fiat_amount = NumberFormat.getInstance(Locale.getDefault()).parse(cfiat).doubleValue();
        } catch (NumberFormatException | ParseException e) {
            fiat_amount = 0.0;
        }
        double btc_amount = fiat_amount / btc_fx;
        edAmount1.setText(MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_amount)));
    }

    private void displayMaxAvailable(int position) {

        long amountAvailable = getMaxAvailable(position);

        if (amountAvailable > 0L) {
            double btc_balance = (((double) amountAvailable) / 1e8);
            tvMax.setText(getActivity().getResources().getText(R.string.max_available) + " " + MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_balance)) + " " + strBTC);
        } else {
            if (AppUtil.getInstance(getActivity()).isNotUpgraded()) {
                tvMax.setText(R.string.no_funds_available2);
            } else {
                tvMax.setText(R.string.no_funds_available);
            }
        }

    }

    private long getMaxAvailable(int position){

        long amount = 0L;
        int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();
        if (position >= hdAccountsIdx) {
            amount = MultiAddrFactory.getInstance().getLegacyBalance(AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx).getAddress());
        } else {
            String xpub = account2Xpub(position);
            if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
            } else {
                amount = 0L;
            }
        }

        return (amount - SendCoins.bFee.longValue());
    }

    public String account2Xpub(int sel) {

        Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        String xpub = null;
        if (hda instanceof ImportedAccount) {
            xpub = null;
        } else {
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

    private void sendClicked() {

        //Hide keyboard
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        //Check connectivity before we spend
        if(ConnectivityStatus.hasConnectivity(getActivity())){

            //Get spend details from UI
            final PendingSpend pendingSpend = getPendingSpendFromUIFields();
            if(pendingSpend != null){
                if(isValidSpend(pendingSpend)){
                    checkDoubleEncrypt(pendingSpend);
                }
            }

        }else{
            PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
            ToastCustom.makeText(getActivity(), getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
        }
    }

    private void checkDoubleEncrypt(final PendingSpend pendingSpend){

        if (!PayloadFactory.getInstance().get().isDoubleEncrypted() || DoubleEncryptionFactory.getInstance().isActivated()) {
            confirmPayment(pendingSpend);
        } else {
            alertDoubleEncryptedV3(pendingSpend);
        }
    }

    private void alertDoubleEncryptedV3(final PendingSpend pendingSpend){
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

                        if (DoubleEncryptionFactory.getInstance().validateSecondPassword(PayloadFactory.getInstance().get().getDoublePasswordHash(), PayloadFactory.getInstance().get().getSharedKey(), new CharSequenceX(pw), PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations())) {

                            if (pendingSpend.isHD) {

                                String encrypted_hex = PayloadFactory.getInstance().get().getHdWallet().getSeedHex();
                                String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                                        encrypted_hex,
                                        PayloadFactory.getInstance().get().getSharedKey(),
                                        pw,
                                        PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations());

                                try {
                                    Wallet hdw = WalletFactory.getInstance().restoreWallet(decrypted_hex, "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
                                    WalletFactory.getInstance().setWatchOnlyWallet(hdw);
                                } catch (IOException | DecoderException | AddressFormatException |
                                        MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException |
                                        MnemonicException.MnemonicWordException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(pw));
                            }

                            confirmPayment(pendingSpend);

                        } else {
                            ToastCustom.makeText(getActivity(), getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void confirmPayment(final PendingSpend pendingSpend) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.fragment_send_confirm, null);
                dialogBuilder.setView(dialogView);

                final AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.setCanceledOnTouchOutside(false);

                TextView confirmDestination = (TextView) dialogView.findViewById(R.id.confirm_to);
                confirmDestination.setText(pendingSpend.destination);

                TextView confirmFee = (TextView) dialogView.findViewById(R.id.confirm_fee);
                confirmFee.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(pendingSpend.bigIntFee.longValue()) + " " + strBTC);

                TextView confirmTotal = (TextView) dialogView.findViewById(R.id.confirm_total_to_send);
                BigInteger cTotal = (pendingSpend.bigIntAmount.add(pendingSpend.bigIntFee));
                confirmTotal.setText(MonetaryUtil.getInstance(getActivity()).getDisplayAmount(cTotal.longValue()) + " " + strBTC);

                TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
                confirmCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.cancel();
                        }
                    }
                });

                TextView confirmSend = (TextView) dialogView.findViewById(R.id.confirm_send);
                confirmSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (!spendInProgress) {
                            spendInProgress = true;

                            if (progress != null && progress.isShowing()) {
                                progress.dismiss();
                                progress = null;
                            }
                            progress = new ProgressDialog(getActivity());
                            progress.setCancelable(false);
                            progress.setTitle(R.string.app_name);
                            progress.setMessage(getString(R.string.sending));
                            progress.show();

                            context = getActivity();

                            final UnspentOutputsBundle unspents = SendFactory.getInstance(context).prepareSend(pendingSpend.fromXpubIndex, pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, BigInteger.ZERO, null);

                            if (unspents != null) {
                                executeSend(pendingSpend, unspents, alertDialog);
                            } else {

                                PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                                ToastCustom.makeText(context.getApplicationContext(), getResources().getString(R.string.transaction_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                closeDialog(alertDialog, false);
                            }

                            spendInProgress = false;
                        }
                    }
                });

                alertDialog.show();

                Looper.loop();

            }
        }).start();
    }

    private void executeSend(final PendingSpend pendingSpend, final UnspentOutputsBundle unspents, final AlertDialog alertDialog){

        SendFactory.getInstance(context).execSend(pendingSpend.fromXpubIndex, unspents.getOutputs(), pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, pendingSpend.note, false, new OpCallback() {

            public void onSuccess() {
            }

            @Override
            public void onSuccess(final String hash) {

                ToastCustom.makeText(context, getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                playAudio();

                PayloadBridge.getInstance(context).remoteSaveThread();

                if (pendingSpend.isHD) {

                    //Update v3 balance immediately after spend - until refresh from server
                    MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                    MultiAddrFactory.getInstance().setXpubAmount(HDPayloadBridge.getInstance(context).account2Xpub(pendingSpend.fromXpubIndex), MultiAddrFactory.getInstance().getXpubAmounts().get(HDPayloadBridge.getInstance(context).account2Xpub(pendingSpend.fromXpubIndex)) - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));

                } else {

                    //Update v2 balance immediately after spend - until refresh from server
                    MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                    //TODO - why are we not setting individual address balance as well, was this over looked?

                    //Reset double encrypt for V2
                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                }

                closeDialog(alertDialog, true);
            }

            public void onFail() {

                ToastCustom.makeText(context, getResources().getString(R.string.transaction_queued), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

                //Initial send failed - Put send in queue for reattempt
                String direction = MultiAddrFactory.SENT;
                if (spDestinationSelected) direction = MultiAddrFactory.MOVED;

                SendFactory.getInstance(context).execSend(pendingSpend.fromXpubIndex, unspents.getOutputs(), pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, pendingSpend.note, true, this);

                //Refresh BalanceFragment with the following - "placeholder" tx until websocket refreshes list
                final Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
                Bundle bundle = new Bundle();
                bundle.putLong("queued_bamount", (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                bundle.putString("queued_strNote", pendingSpend.note);
                bundle.putString("queued_direction", direction);
                bundle.putLong("queued_time", System.currentTimeMillis() / 1000);
                intent.putExtras(bundle);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }//wait for broadcast receiver to register
                        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
                        Looper.loop();
                    }
                }).start();

                //Reset double encrypt for V2
                if (!pendingSpend.isHD) {
                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                }

                closeDialog(alertDialog, true);
            }

            @Override
            public void onFailPermanently() {

                if (pendingSpend.isHD) {

                    if (getActivity() != null)
                        ToastCustom.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                } else {
                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                    if (getActivity() != null)
                        ToastCustom.makeText(getActivity().getApplicationContext(), getActivity().getApplicationContext().getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                }

                closeDialog(alertDialog, false);
            }

        });
    }

    private void closeDialog(AlertDialog alertDialog, boolean sendSuccess) {

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
        }

        if (sendSuccess) {
            Fragment fragment = new BalanceFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    private void playAudio(){

        AudioManager audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(context.getApplicationContext(), R.raw.alert);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mp.release();
                }

            });
            mp.start();
        }
    }

    private PendingSpend getPendingSpendFromUIFields(){

        PendingSpend pendingSpend = new PendingSpend();

        //Check if fields are parsable
        if ((edAmount1.getText() == null || edAmount1.getText().toString().isEmpty()) ||
                (edAmount2.getText() == null || edAmount2.getText().toString().isEmpty())){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return null;
        }
        if(edReceiveTo.getText() == null || edReceiveTo.getText().toString().isEmpty()){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return null;
        }

        //is V3?
        int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();//will be 0 if V2
        if (spinnerSendFrom.getSelectedItemPosition() >= hdAccountsIdx) {
            pendingSpend.isHD = false;
        }else{
            pendingSpend.isHD = true;
        }

        if(pendingSpend.isHD){
            //V3
            //Account index if V3, legacy address must be null
            pendingSpend.fromXpubIndex = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spinnerSendFrom.getSelectedItemPosition());
            pendingSpend.fromLegacyAddress = null;
        }else{
            //V2
            //Legacy address if V2, xpub index must be -1
            int legacyIndex = spinnerSendFrom.getSelectedItemPosition() - hdAccountsIdx;
            LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(legacyIndex);
            pendingSpend.fromLegacyAddress = legacyAddress;
            pendingSpend.fromXpubIndex = -1;
        }

        //Amount to send
        pendingSpend.bigIntAmount = BigInteger.valueOf(getLongValue(edAmount1.getText().toString()));

        //Fee
        pendingSpend.bigIntFee = SendCoins.bFee;
//        pendingSpend.bigIntFee = BigInteger.valueOf(PayloadFactory.getInstance().get().getOptions().getFeePerKB());

        //Destination
        pendingSpend.destination = edReceiveTo.getText().toString().trim();

        //Note
        pendingSpend.note = null;//future use

        return pendingSpend;
    }

    private boolean isValidSpend(PendingSpend pendingSpend) {

        //Validate amount
        if(!isValidAmount(pendingSpend.bigIntAmount)){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        //Validate sufficient funds
        long amountToSendIncludingFee = pendingSpend.bigIntAmount.longValue() + SendCoins.bFee.longValue();
        if(pendingSpend.isHD){
            String xpub = HDPayloadBridge.getInstance().account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(spinnerSendFrom.getSelectedItemPosition()));
            if(!hasSufficientFunds(xpub, null, amountToSendIncludingFee)){
                ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }else{
            if(!hasSufficientFunds(null, pendingSpend.fromLegacyAddress.getAddress(), amountToSendIncludingFee)){
                ToastCustom.makeText(getActivity(), getString(R.string.insufficient_funds), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                return false;
            }
        }

        //Validate addresses
        if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)){
            ToastCustom.makeText(getActivity(), getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return false;
        }

        return true;
    }

    private long getLongValue(String amountToSendString){

        String amountToSend = amountToSendString.replace(defaultSeparator, ".");
        //Long is safe to use, but double can lead to ugly rounding issues..
        return (BigDecimal.valueOf(MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(Double.parseDouble(amountToSend))).multiply(BigDecimal.valueOf(100000000)).longValue());
    }

    private boolean isValidAmount(BigInteger bAmount){

        if (bAmount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
            edAmount1.setText("0");
            return false;
        }
        if (!(bAmount.compareTo(BigInteger.ZERO) >= 0)) {
            return false;
        }

        return true;
    }

    private boolean hasSufficientFunds(String xpub, String legacyAddress, long amountToSendIncludingFee){

        if (xpub != null) {
            //HD
            if (xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long xpubBalance = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if ((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(amountToSendIncludingFee).longValue()) > xpubBalance) {
                    return false;
                }
            }
        } else {
            //Legacy
            long legacyAddressBalance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress);
            if ((MonetaryUtil.getInstance(getActivity()).getUndenominatedAmount(amountToSendIncludingFee).longValue()) > legacyAddressBalance) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onClick(View v) {

        String pad = "";
        switch (v.getId()) {
            case R.id.button1:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button2:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button3:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button4:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button5:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button6:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button7:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button8:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button9:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.button10:
                pad = defaultSeparator;
                break;
            case R.id.button0:
                pad = v.getTag().toString().substring(0, 1);
                break;
            case R.id.buttonDeleteBack:
                pad = null;
                break;
            case R.id.buttonDone:
                numpad.setVisibility(View.GONE);
                break;
        }

        if (pad != null) {
            // Append tapped #
            if (edAmount1.hasFocus()) {
                edAmount1.append(pad);
            } else if (edAmount2.hasFocus()) {
                edAmount2.append(pad);
            }
        } else {
            // Back clicked
            if (edAmount1.hasFocus()) {
                String e1 = edAmount1.getText().toString();
                if (e1.length() > 0) edAmount1.setText(e1.substring(0, e1.length() - 1));
            } else if (edAmount2.hasFocus()) {
                String e2 = edAmount2.getText().toString();
                if (e2.length() > 0) edAmount2.setText(e2.substring(0, e2.length() - 1));
            }
        }

        if (edAmount1.hasFocus() && edAmount1.getText().length() > 0) {
            edAmount1.post(new Runnable() {
                @Override
                public void run() {
                    edAmount1.setSelection(edAmount1.getText().toString().length());
                }
            });
        } else if (edAmount2.hasFocus() && edAmount2.getText().length() > 0) {
            edAmount2.post(new Runnable() {
                @Override
                public void run() {
                    edAmount2.setSelection(edAmount2.getText().toString().length());
                }
            });
        }
    }

    @Override
    public void onKeypadClose() {
        numpad.setVisibility(View.GONE);
        isKeypadVisible = false;
    }

    class SendFromAdapter extends ArrayAdapter<String> {

        public SendFromAdapter(Context context, int textViewResourceId, List<String> accounts) {
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
            if (isDropdown) layoutRes = R.layout.fragment_send_account_row_dropdown;

            View row = inflater.inflate(layoutRes, parent, false);

            TextView label = null;
            TextView balance = null;
            if (isDropdown) {
                label = (TextView) row.findViewById(R.id.receive_account_label);
                balance = (TextView) row.findViewById(R.id.receive_account_balance);
            } else
                label = (TextView) row.findViewById(R.id.text);

            String labelText = "";
            long amount = 0L;
            int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();
            if (position >= hdAccountsIdx) {
                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
                if(legacyAddress.isWatchOnly())labelText = getActivity().getString(R.string.watch_only_label);
                if (legacyAddress.getLabel() != null && legacyAddress.getLabel().length() > 0) {
                    labelText += legacyAddress.getLabel();
                } else {
                    labelText += legacyAddress.getAddress();
                }
            } else {
                Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
                labelText = hda.getLabel();
            }

            if (position >= hdAccountsIdx) {
                LegacyAddress legacyAddress = AccountsUtil.getInstance(getActivity()).getLegacyAddress(position - hdAccountsIdx);
                amount = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
            } else {
                Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(position));
                String xpub = account2Xpub(position);
                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                } else {
                    amount = 0L;
                }
            }

            if (isDropdown) {
                balance.setText("(" + MonetaryUtil.getInstance(getActivity()).getDisplayAmount(amount) + " " + strBTC + ")");
                label.setText(labelText);
            } else
                label.setText(labelText);

            return row;
        }
    }

    class ReceiveToAdapter extends ArrayAdapter<String> {

        private ArrayList<String> items;

        public ReceiveToAdapter(Context context, int resource, List<String> items) {
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
            if (isDropdown) {
                int layoutRes = R.layout.fragment_send_account_row_dropdown;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.receive_account_label);
                label.setText(items.get(position));
            } else {
                int layoutRes = R.layout.spinner_item;
                row = inflater.inflate(layoutRes, parent, false);
                TextView label = (TextView) row.findViewById(R.id.text);
                label.setText("");
            }

            return row;
        }
    }
}