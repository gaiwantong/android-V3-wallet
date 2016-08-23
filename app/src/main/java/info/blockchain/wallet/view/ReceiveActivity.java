package info.blockchain.wallet.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.view.helpers.CustomKeypad;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.ReceiveViewModel;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityReceiveBinding;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;

public class ReceiveActivity extends BaseAuthActivity implements ReceiveViewModel.DataListener, CustomKeypadCallback {

    private static final String TAG = ReceiveActivity.class.getSimpleName();
    private static final String LINK_ADDRESS_INFO = "https://support.blockchain.com/hc/en-us/articles/210353663-Why-is-my-bitcoin-address-changing-";

    private ReceiveViewModel mViewModel;
    private ActivityReceiveBinding mBinding;

    private CustomKeypad mCustomKeypad;
    private BottomSheetBehavior mBottomSheetBehavior;

    // Drop down
    private ArrayAdapter<String> receiveToAdapter;

    private boolean textChangeAllowed = true;
    private boolean isBTC = true;
    private boolean mShowInfoButton = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_receive);
        mViewModel = new ReceiveViewModel(this, Locale.getDefault());

        Toolbar toolbar = mBinding.toolbar.toolbarGeneral;
        toolbar.setTitle(getResources().getString(R.string.receive_bitcoin));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mViewModel.onViewReady();

        setupLayout();
        selectDefaultAccount();
    }

    private void setupLayout() {
        mBottomSheetBehavior = BottomSheetBehavior.from(mBinding.bottomSheet.bottomSheet);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // No-op
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mBinding.content.receiveMainContentShadow.setAlpha(slideOffset / 2f);
                if (slideOffset > 0) {
                    mBinding.content.receiveMainContentShadow.setVisibility(View.VISIBLE);
                    mBinding.content.receiveMainContentShadow.bringToFront();
                } else {
                    mBinding.content.receiveMainContentShadow.setVisibility(View.GONE);
                }
            }
        });

        mBinding.content.receiveMainContentShadow.setOnClickListener(view ->
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        if (mViewModel.getReceiveToList().size() == 1) {
            mBinding.content.fromRow.setVisibility(View.GONE);
        }

        // BTC Field
        mBinding.content.amountContainer.amountBtc.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        mBinding.content.amountContainer.amountBtc.setHint("0" + getDefaultDecimalSeparator() + "00");
        mBinding.content.amountContainer.amountBtc.addTextChangedListener(mBtcTextWatcher);

        // Fiat Field
        mBinding.content.amountContainer.amountFiat.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        mBinding.content.amountContainer.amountFiat.setHint("0" + getDefaultDecimalSeparator() + "00");
        mBinding.content.amountContainer.amountFiat.addTextChangedListener(mFiatTextWatcher);

        mBinding.content.amountContainer.currencyBtc.setText(mViewModel.getBtcUnit());
        mBinding.content.amountContainer.currencyFiat.setText(mViewModel.getFiatUnit());

        receiveToAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, mViewModel.getReceiveToList());
        receiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        mBinding.content.accounts.spinner.setAdapter(receiveToAdapter);
        mBinding.content.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mBinding.content.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        mBinding.content.accounts.spinner.setDropDownWidth(mBinding.content.accounts.spinner.getWidth());
                    }
                });

        mBinding.content.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                mBinding.content.accounts.spinner.setSelection(mBinding.content.accounts.spinner.getSelectedItemPosition());
                Object object = mViewModel.getAccountItemForPosition(mBinding.content.accounts.spinner.getSelectedItemPosition());

                if (mViewModel.warnWatchOnlySpend()) {
                    promptWatchOnlySpendWarning(object);
                }

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        });

        mBinding.content.ivAddressInfo.setOnClickListener(v -> new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.why_has_my_address_changed))
                .setMessage(getString(R.string.new_address_info))
                .setPositiveButton(R.string.learn_more, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse(LINK_ADDRESS_INFO));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.ok, null)
                .show());

        setCustomKeypad();
    }

    private TextWatcher mBtcTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {

            String input = s.toString();

            mBinding.content.amountContainer.amountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(mViewModel.getMaxBtcLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            try {
                if (input.contains(getDefaultDecimalSeparator())) {
                    String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                    if (dec.length() > 0) {
                        dec = dec.substring(1);
                        if (dec.length() > mViewModel.getMaxBtcLength()) {
                            mBinding.content.amountContainer.amountBtc.setText(input.substring(0, input.length() - 1));
                            mBinding.content.amountContainer.amountBtc.setSelection(mBinding.content.amountContainer.amountBtc.getText().length());
                            s = mBinding.content.amountContainer.amountBtc.getEditableText();
                        }
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "afterTextChanged: ", e);
            }

            mBinding.content.amountContainer.amountBtc.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                mViewModel.updateFiatTextField(input);

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                textChangeAllowed = true;
            }

            if (s.toString().contains(getDefaultDecimalSeparator())) {
                mBinding.content.amountContainer.amountBtc.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            } else {
                mBinding.content.amountContainer.amountBtc.setKeyListener(DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }
    };

    private TextWatcher mFiatTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {

            String input = s.toString();

            mBinding.content.amountContainer.amountFiat.removeTextChangedListener(this);

            int max_len = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(max_len + 1);
            fiatFormat.setMinimumFractionDigits(0);

            try {
                if (input.contains(getDefaultDecimalSeparator())) {
                    String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                    if (dec.length() > 0) {
                        dec = dec.substring(1);
                        if (dec.length() > max_len) {
                            mBinding.content.amountContainer.amountFiat.setText(input.substring(0, input.length() - 1));
                            mBinding.content.amountContainer.amountFiat.setSelection(mBinding.content.amountContainer.amountFiat.getText().length());
                            s = mBinding.content.amountContainer.amountFiat.getEditableText();
                        }
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "afterTextChanged: ", e);
            }

            mBinding.content.amountContainer.amountFiat.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                mViewModel.updateBtcTextField(input);

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                textChangeAllowed = true;
            }

            if (s.toString().contains(getDefaultDecimalSeparator())) {
                mBinding.content.amountContainer.amountFiat.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            } else {
                mBinding.content.amountContainer.amountFiat.setKeyListener(DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }
    };

    private void setCustomKeypad() {

        mCustomKeypad = new CustomKeypad(this, (mBinding.content.keypadContainer.numericPad));
        mCustomKeypad.setDecimalSeparator(getDefaultDecimalSeparator());

        // Enable custom keypad and disables default keyboard from popping up
        mCustomKeypad.enableOnView(mBinding.content.amountContainer.amountBtc);
        mCustomKeypad.enableOnView(mBinding.content.amountContainer.amountFiat);

        mBinding.content.amountContainer.amountBtc.setText("");
        mBinding.content.amountContainer.amountBtc.requestFocus();
    }

    private void selectDefaultAccount() {
        if (mBinding.content.accounts.spinner != null) {
            displayQRCode(mViewModel.getDefaultSpinnerPosition());
        }
    }

    private void displayQRCode(int spinnerIndex) {

        mBinding.content.accounts.spinner.setSelection(spinnerIndex);
        String receiveAddress;

        Object object = mViewModel.getAccountItemForPosition(spinnerIndex);
        mShowInfoButton = showAddressInfoButtonIfNecessary(object);

        if (object instanceof LegacyAddress) {
            receiveAddress = ((LegacyAddress) object).getAddress();
        } else {
            receiveAddress = mViewModel.getV3ReceiveAddress((Account) object);
        }

        mBinding.content.receivingAddress.setText(receiveAddress);

        long amountLong;
        if (isBTC) {
            amountLong = mViewModel.getLongAmount(mBinding.content.amountContainer.amountBtc.getText().toString());
        } else {
            amountLong = mViewModel.getLongAmount(mBinding.content.amountContainer.amountFiat.getText().toString());
        }

        BigInteger amountBigInt = mViewModel.getUndenominatedAmount(amountLong);
        if (mViewModel.getIfAmountInvalid(amountBigInt)) {
            ToastCustom.makeText(this, this.getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            return;
        }

        if (!amountBigInt.equals(BigInteger.ZERO)) {
            mViewModel.generateQrCode(BitcoinURI.convertToBitcoinURI(receiveAddress, Coin.valueOf(amountBigInt.longValue()), "", ""));
        } else {
            mViewModel.generateQrCode("bitcoin:" + receiveAddress);
        }
    }

    @Override
    public void updateFiatTextField(String text) {
        mBinding.content.amountContainer.amountFiat.setText(text);
    }

    @Override
    public void updateBtcTextField(String text) {
        mBinding.content.amountContainer.amountBtc.setText(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.content.amountContainer.currencyBtc.setText(isBTC ? mViewModel.getBtcUnit() : mViewModel.getFiatUnit());
        mBinding.content.amountContainer.currencyFiat.setText(isBTC ? mViewModel.getFiatUnit() : mViewModel.getBtcUnit());

        mViewModel.updateSpinnerList();

        selectDefaultAccount();
    }

    @Override
    public void showQrLoading() {
        mBinding.content.ivAddressInfo.setVisibility(View.GONE);
        mBinding.content.qr.setVisibility(View.GONE);
        mBinding.content.receivingAddress.setVisibility(View.GONE);
        mBinding.content.progressBar2.setVisibility(View.VISIBLE);
    }

    @Override
    public void showQrCode(@Nullable Bitmap bitmap) {
        mBinding.content.progressBar2.setVisibility(View.GONE);
        mBinding.content.qr.setVisibility(View.VISIBLE);
        mBinding.content.receivingAddress.setVisibility(View.VISIBLE);
        mBinding.content.qr.setImageBitmap(bitmap);
        if (mShowInfoButton) {
            mBinding.content.ivAddressInfo.setVisibility(View.VISIBLE);
        }

//        setupBottomSheet(uri);
    }

    private void promptWatchOnlySpendWarning(Object object) {
        if (object instanceof LegacyAddress && ((LegacyAddress) object).isWatchOnly()) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
            AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.alert_watch_only_spend, null, false);
            dialogBuilder.setView(dialogBinding.getRoot());
            dialogBuilder.setCancelable(false);

            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);

            dialogBinding.confirmCancel.setOnClickListener(v -> {
                mBinding.content.accounts.spinner.setSelection(0, true);
                if (dialogBinding.confirmDontAskAgain.isChecked()) {
                    mViewModel.setWarnWatchOnlySpend(false);
                }
                alertDialog.dismiss();
            });

            dialogBinding.confirmContinue.setOnClickListener(v -> {
                if (dialogBinding.confirmDontAskAgain.isChecked()) {
                    mViewModel.setWarnWatchOnlySpend(false);
                }
                alertDialog.dismiss();
            });

            alertDialog.show();
        }
    }

    private boolean showAddressInfoButtonIfNecessary(Object object) {
        return !(object instanceof ImportedAccount || object instanceof LegacyAddress);
    }

    private void onShareClicked() {
        onKeypadClose();

        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onSpinnerDataChanged() {
        if (receiveToAdapter != null) receiveToAdapter.notifyDataSetChanged();
    }

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_receive, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                onShareClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCustomKeypad.isVisible()) {
            onKeypadClose();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.destroy();
    }

    @Override
    public void onKeypadClose() {
        mCustomKeypad.setNumpadVisibility(View.GONE);
    }
}
