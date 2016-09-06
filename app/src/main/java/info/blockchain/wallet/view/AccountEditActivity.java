package info.blockchain.wallet.view;

import com.google.zxing.client.android.CaptureActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import info.blockchain.wallet.model.AccountEditModel;
import info.blockchain.wallet.model.PaymentConfirmationDetails;
import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.util.ViewUtils;
import info.blockchain.wallet.view.customviews.MaterialProgressDialog;
import info.blockchain.wallet.view.helpers.SecondPasswordHandler;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.AccountEditViewModel;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityAccountEditBinding;
import piuk.blockchain.android.databinding.AlertGenericWarningBinding;
import piuk.blockchain.android.databinding.AlertShowExtendedPublicKeyBinding;
import piuk.blockchain.android.databinding.FragmentSendConfirmBinding;

public class AccountEditActivity extends BaseAuthActivity implements AccountEditViewModel.DataListener{

    private final int ADDRESS_LABEL_MAX_LENGTH = 17;
    private final int SCAN_PRIVX = 302;

    private ActivityAccountEditBinding binding;
    private AccountEditViewModel viewModel;
    private MaterialProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_edit);
        viewModel = new AccountEditViewModel(new AccountEditModel(), this, this);
        binding.setViewModel(viewModel);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setupToolbar();
        viewModel.setDataFromIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {
        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.edit));
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);
    }

    @Override
    public void onPromptAccountLabel() {
        final AppCompatEditText etLabel = new AppCompatEditText(this);
        etLabel.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

        FrameLayout frameLayout = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, this);
        params.setMargins(marginInPixels, 0, marginInPixels, 0);

        frameLayout.addView(etLabel, params);
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.name)
                .setMessage(R.string.assign_display_name)
                .setView(frameLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, (dialog, whichButton) ->
                        viewModel.updateAccountLabel(etLabel.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onConnectivityLoss() {
        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
    }

    @Override
    public void onToast(String errorMessage, String type) {
        ToastCustom.makeText(AccountEditActivity.this, errorMessage, ToastCustom.LENGTH_LONG, type);
    }

    @Override
    public void onSetResult(int resultCode) {
        setResult(resultCode);
    }

    @Override
    public void onStartScanActivity() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, this);
        }else{
            if (!new AppUtil(this).isCameraOpen()) {
                Intent intent = new Intent(this, CaptureActivity.class);
                startActivityForResult(intent, SCAN_PRIVX);
            } else {
                ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        }
    }

    @Override
    public void onPromptPrivateKey(String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.privx_required)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        new SecondPasswordHandler(this).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        onStartScanActivity();
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        viewModel.setSecondPassword(validateSecondPassword);
                        onStartScanActivity();
                    }
                }))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void onShowPaymentDetails(PaymentConfirmationDetails details, PendingTransaction pendingTransaction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        FragmentSendConfirmBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
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

        String feeMessage = "";
        if(details.isSurge){
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
            feeMessage += getString(R.string.transaction_surge);

        }

        if(details.hasConsumedAmounts){
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);

            if(details.hasConsumedAmounts){
                if(details.isSurge) feeMessage += "\n\n";
                feeMessage += getString(R.string.large_tx_high_fee_warning);
            }

        }

        final String finalFeeMessage = feeMessage;
        dialogBinding.ivFeeInfo.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_fee)
                .setMessage(finalFeeMessage)
                .setPositiveButton(android.R.string.ok, null).show());

        if(details.isSurge){
            dialogBinding.confirmFeeBtc.setTextColor(ContextCompat.getColor(this, R.color.blockchain_send_red));
            dialogBinding.confirmFeeFiat.setTextColor(ContextCompat.getColor(this, R.color.blockchain_send_red));
            dialogBinding.ivFeeInfo.setVisibility(View.VISIBLE);
        }

        dialogBinding.tvCustomizeFee.setVisibility(View.GONE);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            viewModel.submitPayment(alertDialog, pendingTransaction);
        });

        alertDialog.show();

        if(details.isLargeTransaction){
            onShowLargeTransactionWarning(alertDialog);
        }

    }

    private void onShowLargeTransactionWarning(AlertDialog alertDialog) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertGenericWarningBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_generic_warning, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialogFee = dialogBuilder.create();
        alertDialogFee.setCanceledOnTouchOutside(false);

        dialogBinding.tvBody.setText(R.string.large_tx_warning);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialogFee.isShowing()) alertDialogFee.cancel();
        });

        dialogBinding.confirmKeep.setText(getResources().getString(R.string.go_back));
        dialogBinding.confirmKeep.setOnClickListener(v -> {
            alertDialogFee.dismiss();
            alertDialog.dismiss();
        });

        dialogBinding.confirmChange.setText(getResources().getString(R.string.accept_higher_fee));
        dialogBinding.confirmChange.setOnClickListener(v -> {
            alertDialogFee.dismiss();
        });

        alertDialogFee.show();
    }

    @Override
    public void onPromptArchive(String title, String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> viewModel.archiveAccount())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public void onPromptBIP38Password(final String data) {
        final AppCompatEditText password = new AppCompatEditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) ->
                        viewModel.importBIP38Address(data, password.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void onPrivateKeyImportMismatch() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.private_key_successfully_imported)+"\n\n"+getString(R.string.private_key_not_matching_address))
                .setPositiveButton(R.string.try_again, (dialog, whichButton) -> viewModel.onClickScanXpriv(null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onPrivateKeyImportSuccess() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.success)
                .setMessage(R.string.private_key_successfully_imported)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    @Override
    public void onShowXpubSharingWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.xpub_sharing_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, (dialog, whichButton) ->
                        viewModel.showAddressDetails()).setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    @Override
    public void onShowAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        AlertShowExtendedPublicKeyBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_show_extended_public_key, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.tvWarningHeading.setText(heading);
        dialogBinding.tvXpubNote.setText(note);
        dialogBinding.tvExtendedXpub.setText(copy);
        dialogBinding.tvExtendedXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
        dialogBinding.ivQr.setImageBitmap(bitmap);

        dialogBinding.tvExtendedXpub.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = null;
            clip = android.content.ClipData.newPlainText("Send address", qrString);
            ToastCustom.makeText(this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
            clipboard.setPrimaryClip(clip);
        });

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.cancel();
            }
        });

        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK){
            viewModel.handleIncomingScanIntent(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStartScanActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onShowTransactionSuccess() {

        runOnUiThread(() -> {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.modal_transaction_success, null);
            final AlertDialog alertDialog = dialogBuilder.setView(dialogView).create();
            alertDialog.setOnDismissListener(dialogInterface -> finish());
            alertDialog.show();
        });
    }

    @Override
    public void onShowProgressDialog(String title, String message) {
        onDismissProgressDialog();

        progress = new MaterialProgressDialog(this);
        progress.setMessage(message);
        progress.show();
    }

    @Override
    public void onDismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }
}
