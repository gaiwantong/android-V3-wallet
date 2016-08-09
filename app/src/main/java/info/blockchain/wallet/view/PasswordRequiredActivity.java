package info.blockchain.wallet.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;

import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.PasswordRequiredViewModel;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.LauncherActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityPasswordRequiredBinding;

/**
 * Created by adambennett on 09/08/2016.
 */

public class PasswordRequiredActivity extends BaseAuthActivity implements PasswordRequiredViewModel.DataListener {

    private PasswordRequiredViewModel mViewModel;
    private ActivityPasswordRequiredBinding mBinding;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_password_required);
        mViewModel = new PasswordRequiredViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.confirm_password));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mBinding.buttonContinue.setOnClickListener(view -> mViewModel.onContinueClicked());
        mBinding.buttonForget.setOnClickListener(view -> mViewModel.onForgetWalletClicked());

        mViewModel.onViewReady();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void restartApp() {
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public String getPassword() {
        return mBinding.fieldPassword.getText().toString();
    }

    @Override
    public void resetPasswordField() {
        if (!isFinishing()) mBinding.fieldPassword.setText("");
    }

    @Override
    public void goToPinPage() {
        startActivity(new Intent(this, PinEntryActivity.class));
    }

    @Override
    public void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(R.string.app_name);
        mProgressDialog.setMessage(getString(message));

        if (!isFinishing()) mProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.destroy();
        dismissProgressDialog();
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }
}
