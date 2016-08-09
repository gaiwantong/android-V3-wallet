package info.blockchain.wallet.view;

import com.google.zxing.client.android.CaptureActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;

import info.blockchain.wallet.app_rate.AppRate;
import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.model.ItemAccount;
import info.blockchain.wallet.model.PaymentConfirmationDetails;
import info.blockchain.wallet.model.SendModel;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.CustomKeypad;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.SendViewModel;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivitySendBinding;
import piuk.blockchain.android.databinding.AlertGenericWarningBinding;
import piuk.blockchain.android.databinding.AlertWatchOnlySpendBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;

public class SendActivity extends BaseAuthActivity implements SendViewModel.DataListener, CustomKeypadCallback {

    private final String TAG = getClass().getSimpleName();
    private final int SCAN_URI = 2007;

    private ActivitySendBinding binding;
    private SendViewModel viewModel;
    private PrefsUtil prefsUtil;

    private static CustomKeypad customKeypad;

    private TextWatcher btcTextWatcher = null;
    private TextWatcher fiatTextWatcher = null;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                ((SendAddressAdapter) binding.accounts.spinner.getAdapter()).updateData(viewModel.getAddressList(false));
                ((SendAddressAdapter) binding.spDestination.getAdapter()).updateData(viewModel.getAddressList(true));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_send);
        this.prefsUtil = new PrefsUtil(this);

        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        viewModel = new SendViewModel(new SendModel(),
                this,
                ExchangeRateFactory.getInstance().getLastPrice(this, fiatUnit),
                prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC),
                fiatUnit,
                this);
        binding.setViewModel(viewModel);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setupToolbar();

        setCustomKeypad();

        setupViews();

        String scanData = getIntent().getStringExtra("scan_data");
        if(scanData!=null)viewModel.handleIncomingQRScan(scanData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    private void setupToolbar() {
        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.send_bitcoin));
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
                } else {
                    startScanActivity();
                }
                return true;
            case R.id.action_send:
                customKeypad.setNumpadVisibility(View.GONE);

                ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                viewModel.setSendingAddress(selectedItem);
                viewModel.calculateTransactionAmounts(selectedItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        binding.customFee.getText().toString(),
                        () -> viewModel.sendClicked(false, binding.destination.getText().toString()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity() {
        if (!new AppUtil(this).isCameraOpen()) {
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_URI);
        } else {
            ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            viewModel.handleIncomingQRScan(data.getStringExtra(CaptureActivity.SCAN_RESULT));
        }
    }

    @Override
    public void onBackPressed() {
        if(customKeypad.isVisible()){
            onKeypadClose();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onKeypadClose() {
        customKeypad.setNumpadVisibility(View.GONE);
    }

    private void setCustomKeypad() {

        customKeypad = new CustomKeypad(this, (binding.keypad.numericPad));
        customKeypad.setDecimalSeparator(viewModel.getDefaultSeparator());

        //Enable custom keypad and disables default keyboard from popping up
        customKeypad.enableOnView(binding.amountRow.amountBtc);
        customKeypad.enableOnView(binding.amountRow.amountFiat);
        customKeypad.enableOnView(binding.customFee);

        binding.amountRow.amountBtc.setText("");
        binding.amountRow.amountBtc.requestFocus();
    }

    private void setupViews() {

        setupDestinationView();
        setupSendFromView();
        setupReceiveToView();

        setBtcTextWatcher();
        setFiatTextWatcher();

        binding.amountRow.amountBtc.addTextChangedListener(btcTextWatcher);
        binding.amountRow.amountBtc.setSelectAllOnFocus(true);

        binding.amountRow.amountFiat.setHint("0" + viewModel.getDefaultSeparator() + "00");
        binding.amountRow.amountFiat.addTextChangedListener(fiatTextWatcher);
        binding.amountRow.amountFiat.setSelectAllOnFocus(true);

        binding.amountRow.amountBtc.setHint("0" + viewModel.getDefaultSeparator() + "00");

        binding.customFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable customizedFee) {
                ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                viewModel.calculateTransactionAmounts(selectedItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        customizedFee.toString(),
                        null);
            }
        });

        binding.max.setOnClickListener(view -> {
            ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
            viewModel.spendAllClicked(selectedItem, binding.customFee.getText().toString());
        });
    }

    private void setupDestinationView(){
        binding.destination.setHorizontallyScrolling(false);
        binding.destination.setLines(3);
        binding.destination.setOnTouchListener((view, motionEvent) -> {
            binding.destination.setText("");
            return false;
        });
        binding.destination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && customKeypad != null)
                customKeypad.setNumpadVisibility(View.GONE);
        });
    }

    private void setupSendFromView() {

        binding.accounts.spinner.setAdapter(new SendAddressAdapter(this, R.layout.spinner_item, viewModel.getAddressList(false)));

        //Set drop down width equal to clickable view
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

        binding.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
                viewModel.setSendingAddress(selectedItem);
                viewModel.calculateTransactionAmounts(selectedItem,
                        binding.amountRow.amountBtc.getText().toString(),
                        binding.customFee.getText().toString(), null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void setupReceiveToView() {
        binding.spDestination.setAdapter(new SendAddressAdapter(this, R.layout.spinner_item, viewModel.getAddressList(true)));

        //Set drop down width equal to clickable view
        binding.spDestination.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.spDestination.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    binding.spDestination.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if(binding.accounts.spinner.getWidth() > 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        binding.spDestination.setDropDownWidth(binding.accounts.spinner.getWidth());
                    }
            }
        });

        binding.spDestination.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        ItemAccount selectedItem = (ItemAccount) binding.spDestination.getSelectedItem();
                        binding.destination.setText(selectedItem.label);
                        viewModel.setReceivingAddress(selectedItem);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    @Override
    public void onHideSendingAddressField() {
        binding.fromRow.setVisibility(View.GONE);
    }

    @Override
    public void onHideReceivingAddressField() {
        binding.spDestination.setVisibility(View.GONE);
    }

    @Override
    public void onShowInvalidAmount() {
        ToastCustom.makeText(this, getString(R.string.invalid_amount), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
        binding.amountRow.amountBtc.setText("0.0");
    }

    @Override
    public void onUpdateBtcAmount(String amount) {
        binding.amountRow.amountBtc.setText(amount);
        binding.amountRow.amountBtc.setSelection(binding.amountRow.amountBtc.getText().length());
    }

    @Override
    public void onRemoveBtcTextChangeListener() {
        binding.amountRow.amountBtc.removeTextChangedListener(btcTextWatcher);
    }

    @Override
    public void onAddBtcTextChangeListener() {
        binding.amountRow.amountBtc.addTextChangedListener(btcTextWatcher);
    }

    @Override
    public void onUpdateFiatAmount(String amount) {
        binding.amountRow.amountFiat.setText(amount);
        binding.amountRow.amountFiat.setSelection(binding.amountRow.amountFiat.getText().length());
    }

    @Override
    public void onRemoveFiatTextChangeListener() {
        binding.amountRow.amountFiat.removeTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void onAddFiatTextChangeListener() {
        binding.amountRow.amountFiat.addTextChangedListener(fiatTextWatcher);
    }

    @Override
    public void onShowErrorMessage(String message) {
        ToastCustom.makeText(this, message, ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
    }

    @Override
    public void onShowTransactionSuccess() {

        runOnUiThread(() -> {

            playAudio();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SendActivity.this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.modal_transaction_success, null);
            final AlertDialog alertDialog = dialogBuilder.setView(dialogView).create();
            alertDialog.setOnDismissListener(dialogInterface -> finish());
            alertDialog.show();

            new AppRate(SendActivity.this)
                    .setMinTransactionsUntilPrompt(3)
                    .incrementTransactionCount()
                    .init();
        });
    }

    @Override
    public void onUpdateBtcUnit(String unit) {
        binding.amountRow.currencyBtc.setText(unit);
        binding.tvFeeUnit.setText(unit);
    }

    @Override
    public void onUpdateFiatUnit(String unit) {
        binding.amountRow.currencyFiat.setText(unit);
    }

    @Override
    public void onSetBtcUnit(int unitBtc) {
        prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
    }

    @Override
    public void onSetSpendAllAmount(String textFromSatoshis) {
        runOnUiThread(() -> binding.amountRow.amountBtc.setText(textFromSatoshis));
    }

    @Override
    public void onShowWatchOnlySpend(String receivingAddress) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_watch_only_spend, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        final CheckBox confirmDismissForever = (CheckBox) dialogView.findViewById(R.id.confirm_dont_ask_again);

        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(v -> {
            binding.destination.setText("");
            if(confirmDismissForever.isChecked()) prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", false);
            alertDialog.dismiss();
        });

        TextView confirmContinue = (TextView) dialogView.findViewById(R.id.confirm_continue);
        confirmContinue.setOnClickListener(v -> {
            binding.destination.setText(receivingAddress);
            if(confirmDismissForever.isChecked()) prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", false);
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void setBtcTextWatcher(){

        btcTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                viewModel.afterBtcTextChanged(editable.toString());
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
            public void afterTextChanged(Editable editable) {
                viewModel.afterFiatTextChanged(editable.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };

    }

    @Override
    public void onShowPaymentDetails(PaymentConfirmationDetails details, final String validatedSecondPassword) {

        new Thread(() -> {

            Looper.prepare();

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SendActivity.this);
            FragmentSendConfirmBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(SendActivity.this),
                    R.layout.fragment_send_confirm, null, false);
            dialogBuilder.setView(dialogBinding.getRoot());

            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);

            dialogBinding.confirmFromLabel.setText(details.fromLabel);
            dialogBinding.confirmToLabel.setText(details.toLabel);
            dialogBinding.confirmAmountBtcUnit.setText(details.btcUnit);
            dialogBinding.confirmAmountFiatUnit.setText(details.fiatUnit);
            dialogBinding.confirmAmountBtc.setText(details.btcAmount);
            dialogBinding.confirmAmountFiat.setText(details.fiatAmount);
            dialogBinding.confirmFeeBtc.setText(details.btcFee);
            dialogBinding.confirmFeeFiat.setText(details.fiatFee);
            dialogBinding.confirmTotalBtc.setText(details.btcTotal);
            dialogBinding.confirmTotalFiat.setText(details.fiatTotal);

            dialogBinding.ivFeeInfo.setOnClickListener(view -> new AlertDialog.Builder(SendActivity.this)
                    .setTitle(R.string.transaction_fee)
                    .setMessage(getText(R.string.recommended_fee).toString()+"\n\n"+getText(R.string.transaction_surge).toString())
                    .setPositiveButton(android.R.string.ok, null).show());

            if(details.isSurge){
                dialogBinding.confirmFeeBtc.setTextColor(ContextCompat.getColor(SendActivity.this, R.color.blockchain_send_red));
                dialogBinding.confirmFeeFiat.setTextColor(ContextCompat.getColor(SendActivity.this, R.color.blockchain_send_red));
                dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
            }

            dialogBinding.tvCustomizeFee.setOnClickListener(v -> {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.cancel();
                }

                runOnUiThread(() -> {
                    binding.customFeeContainer.setVisibility(View.VISIBLE);

                    binding.customFee.setText(details.btcFee);
                    binding.customFee.setHint(details.btcSuggestedFee);
                    binding.customFee.requestFocus();
                    binding.customFee.setSelection(binding.customFee.getText().length());
                    customKeypad.setNumpadVisibility(View.GONE);
                });

                alertCustomSpend(details.btcSuggestedFee, details.btcUnit);

            });

            dialogBinding.confirmCancel.setOnClickListener(v -> {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.cancel();
                }
            });

            dialogBinding.confirmSend.setOnClickListener(v -> {
                viewModel.submitPayment(validatedSecondPassword, alertDialog);
            });

            alertDialog.show();

            Looper.loop();

        }).start();
    }

    @Override
    public void onShowWatchOnlySpendWarning(String address) {

        if(prefsUtil.getValue("WARN_WATCH_ONLY_SPEND", true)){

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            AlertWatchOnlySpendBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(SendActivity.this),
                    R.layout.alert_watch_only_spend, null, false);
            dialogBuilder.setView(dialogBinding.getRoot());
            dialogBuilder.setCancelable(false);

            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.setCanceledOnTouchOutside(false);

            dialogBinding.confirmCancel.setOnClickListener(v -> {
                binding.destination.setText("");
                if(dialogBinding.confirmDontAskAgain.isChecked()) prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", false);
                alertDialog.dismiss();
            });

            dialogBinding.confirmContinue.setOnClickListener(v -> {
                binding.destination.setText(address);
                if(dialogBinding.confirmDontAskAgain.isChecked()) prefsUtil.setValue("WARN_WATCH_ONLY_SPEND", false);
                alertDialog.dismiss();
            });

            alertDialog.show();
        }
    }

    @Override
    public void onShowAlterFee(String absoluteFeeSuggested,
                               String body,
                               int positiveAction,
                               int negativeAction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertGenericWarningBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(SendActivity.this),
                R.layout.alert_generic_warning, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialogFee = dialogBuilder.create();
        alertDialogFee.setCanceledOnTouchOutside(false);

        dialogBinding.tvBody.setText(body);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();
        });

        dialogBinding.confirmKeep.setText(getResources().getString(negativeAction));
        dialogBinding.confirmKeep.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();

            ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
            viewModel.setSendingAddress(selectedItem);
            viewModel.calculateTransactionAmounts(selectedItem,
                    binding.amountRow.amountBtc.getText().toString(),
                    binding.customFee.getText().toString(),
                    () -> viewModel.sendClicked(true, binding.destination.getText().toString()));
        });

        dialogBinding.confirmChange.setText(getResources().getString(positiveAction));
        dialogBinding.confirmChange.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();

            ItemAccount selectedItem = (ItemAccount) binding.accounts.spinner.getSelectedItem();
            viewModel.setSendingAddress(selectedItem);
            viewModel.calculateTransactionAmounts(selectedItem,
                    binding.amountRow.amountBtc.getText().toString(),
                    absoluteFeeSuggested,
                    () -> viewModel.sendClicked(true, binding.destination.getText().toString()));

        });
        alertDialogFee.show();
    }

    private void alertCustomSpend(String btcFee, String btcFeeUnit){

        String message = getResources().getString(R.string.recommended_fee)
                +"\n\n"
                +btcFee
                +" "+btcFeeUnit;

        new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_fee)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void playAudio(){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getApplicationContext(), R.raw.beep);
            mp.setOnCompletionListener(mp1 -> {
                mp1.reset();
                mp1.release();
            });
            mp.start();
        }
    }
}