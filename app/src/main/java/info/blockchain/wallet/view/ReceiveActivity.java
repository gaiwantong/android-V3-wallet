package info.blockchain.wallet.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.view.adapters.ShareReceiveIntentAdapter;
import info.blockchain.wallet.view.helpers.CustomKeypad;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.ReceiveViewModel;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
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
    private ArrayAdapter<String> mReceiveToAdapter;

    private boolean mTextChangeAllowed = true;
    private boolean mIsBTC = true;
    private boolean mShowInfoButton = false;
    private String mUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_receive);
        mViewModel = new ReceiveViewModel(this, Locale.getDefault());

        Toolbar toolbar = mBinding.toolbar.toolbarGeneral;
        toolbar.setTitle(getResources().getString(R.string.receive_bitcoin));
        setSupportActionBar(toolbar);

        mViewModel.onViewReady();

        setupLayout();
        selectDefaultAccount();
    }

    private void setupLayout() {
        setCustomKeypad();

        // Bottom Sheet
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
        mBinding.content.amountContainer.amountFiat.setText("0" + getDefaultDecimalSeparator() + "00");
        mBinding.content.amountContainer.amountFiat.addTextChangedListener(mFiatTextWatcher);

        // Units
        mBinding.content.amountContainer.currencyBtc.setText(mViewModel.getCurrencyHelper().getBtcUnit());
        mBinding.content.amountContainer.currencyFiat.setText(mViewModel.getCurrencyHelper().getFiatUnit());

        // Spinner
        mReceiveToAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, mViewModel.getReceiveToList());
        mReceiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        mBinding.content.accounts.spinner.setAdapter(mReceiveToAdapter);
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

        // Info Button
        mBinding.content.ivAddressInfo.setOnClickListener(v -> showAddressChangedInfo());

        // QR Code
        mBinding.content.qr.setOnClickListener(v -> showClipboardWarning());
        mBinding.content.qr.setOnLongClickListener(view -> {
            onShareClicked();
            return true;
        });
    }

    private TextWatcher mBtcTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();

            mBinding.content.amountContainer.amountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(mViewModel.getCurrencyHelper().getMaxBtcDecimalLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, mViewModel.getCurrencyHelper().getMaxBtcDecimalLength(), mBinding.content.amountContainer.amountBtc);

            mBinding.content.amountContainer.amountBtc.addTextChangedListener(this);

            if (mTextChangeAllowed) {
                mTextChangeAllowed = false;
                mViewModel.updateFiatTextField(s.toString());

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                mTextChangeAllowed = true;
            }
            setKeyListener(s, mBinding.content.amountContainer.amountBtc);
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
            int maxLength = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(maxLength + 1);
            fiatFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, maxLength, mBinding.content.amountContainer.amountFiat);

            mBinding.content.amountContainer.amountFiat.addTextChangedListener(this);

            if (mTextChangeAllowed) {
                mTextChangeAllowed = false;
                mViewModel.updateBtcTextField(s.toString());

                displayQRCode(mBinding.content.accounts.spinner.getSelectedItemPosition());
                mTextChangeAllowed = true;
            }
            setKeyListener(s, mBinding.content.amountContainer.amountFiat);
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

    private void setKeyListener(Editable s, EditText editText) {
        if (s.toString().contains(getDefaultDecimalSeparator())) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        } else {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        }
    }

    private Editable formatEditable(Editable s, String input, int maxLength, EditText editText) {
        try {
            if (input.contains(getDefaultDecimalSeparator())) {
                String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > maxLength) {
                        editText.setText(input.substring(0, input.length() - 1));
                        editText.setSelection(editText.getText().length());
                        s = editText.getEditableText();
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "afterTextChanged: ", e);
        }
        return s;
    }

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

        Object object = mViewModel.getAccountItemForPosition(spinnerIndex);
        mShowInfoButton = showAddressInfoButtonIfNecessary(object);

        String receiveAddress;
        if (object instanceof LegacyAddress) {
            receiveAddress = ((LegacyAddress) object).getAddress();
        } else {
            receiveAddress = mViewModel.getV3ReceiveAddress((Account) object);
        }

        mBinding.content.receivingAddress.setText(receiveAddress);

        long amountLong;
        if (mIsBTC) {
            amountLong = mViewModel.getCurrencyHelper().getLongAmount(
                    mBinding.content.amountContainer.amountBtc.getText().toString());
        } else {
            amountLong = mViewModel.getCurrencyHelper().getLongAmount(
                    mBinding.content.amountContainer.amountFiat.getText().toString());
        }

        BigInteger amountBigInt = mViewModel.getCurrencyHelper().getUndenominatedAmount(amountLong);

        if (mViewModel.getCurrencyHelper().getIfAmountInvalid(amountBigInt)) {
            ToastCustom.makeText(this, this.getString(R.string.invalid_amount), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        if (!amountBigInt.equals(BigInteger.ZERO)) {
            mUri = BitcoinURI.convertToBitcoinURI(receiveAddress, Coin.valueOf(amountBigInt.longValue()), "", "");
        } else {
            mUri = "bitcoin:" + receiveAddress;
        }

        mViewModel.generateQrCode(mUri);
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
        mBinding.content.amountContainer.currencyBtc.setText(
                mIsBTC ? mViewModel.getCurrencyHelper().getBtcUnit() : mViewModel.getCurrencyHelper().getFiatUnit());
        mBinding.content.amountContainer.currencyFiat.setText(
                mIsBTC ? mViewModel.getCurrencyHelper().getFiatUnit() : mViewModel.getCurrencyHelper().getBtcUnit());
        mViewModel.updateSpinnerList();
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
    }

    private void setupBottomSheet(String uri) {
        List<ReceiveViewModel.SendPaymentCodeData> list = mViewModel.getIntentDataList(uri);
        if (list != null) {
            ShareReceiveIntentAdapter adapter = new ShareReceiveIntentAdapter(list);
            mBinding.bottomSheet.recyclerView.setAdapter(adapter);
            mBinding.bottomSheet.recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter.notifyDataSetChanged();
        }
    }

    private boolean showAddressInfoButtonIfNecessary(Object object) {
        return !(object instanceof ImportedAccount || object instanceof LegacyAddress);
    }

    private void onShareClicked() {
        onKeypadClose();

        new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    setupBottomSheet(mUri);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showClipboardWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = android.content.ClipData.newPlainText("Send address", mBinding.content.receivingAddress.getText().toString());
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                    clipboard.setPrimaryClip(clip);

                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private AlertDialog showAddressChangedInfo() {
        return new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.why_has_my_address_changed))
                .setMessage(getString(R.string.new_address_info))
                .setPositiveButton(R.string.learn_more, (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse(LINK_ADDRESS_INFO));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    private void promptWatchOnlySpendWarning(Object object) {
        if (object instanceof LegacyAddress && ((LegacyAddress) object).isWatchOnly()) {

            AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(this), R.layout.alert_watch_only_spend, null, false);

            AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setView(dialogBinding.getRoot())
                    .setCancelable(false)
                    .create();

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

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    @Override
    public Bitmap getQrBitmap() {
        return ((BitmapDrawable) mBinding.content.qr.getDrawable()).getBitmap();
    }

    @Override
    public void showToast(String message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, message, ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onSpinnerDataChanged() {
        if (mReceiveToAdapter != null) mReceiveToAdapter.notifyDataSetChanged();
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
        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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

    private Context getActivity() {
        return this;
    }

    @Override
    public void onKeypadClose() {
        mCustomKeypad.setNumpadVisibility(View.GONE);
    }
}
