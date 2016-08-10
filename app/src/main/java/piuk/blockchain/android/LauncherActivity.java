package piuk.blockchain.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import info.blockchain.wallet.view.LandingActivity;
import info.blockchain.wallet.view.MainActivity;
import info.blockchain.wallet.view.PasswordRequiredActivity;
import info.blockchain.wallet.view.PinEntryActivity;
import info.blockchain.wallet.view.UpgradeWalletActivity;

/**
 * Created by adambennett on 09/08/2016.
 */

public class LauncherActivity extends AppCompatActivity implements LauncherViewModel.DataListener {

    private LauncherViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        mViewModel = new LauncherViewModel(this);

        Handler handler = new Handler();
        handler.postDelayed(new DelayStartRunnable(this), 500);
    }

    private void onViewReady() {
        mViewModel.onViewReady();
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public void onNoGuid() {
        startSingleActivity(LandingActivity.class);
    }

    @Override
    public void onRequestPin() {
        startSingleActivity(PinEntryActivity.class);
    }

    @Override
    public void onCorruptPayload() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.not_sane_error))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    mViewModel.getAppUtil().clearCredentialsAndRestart();
                    mViewModel.getAppUtil().restartApp();
                })
                .show();
    }

    @Override
    public void onRequestUpgrade() {
        startActivity(new Intent(this, UpgradeWalletActivity.class));
    }

    @Override
    public void onStartMainActivity() {
        startSingleActivity(MainActivity.class);
    }

    @Override
    public void onReEnterPassword() {
        startSingleActivity(PasswordRequiredActivity.class);
    }

    private void startSingleActivity(Class clazz) {
        Intent intent = new Intent(this, clazz);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private static class DelayStartRunnable implements Runnable {

        private LauncherActivity activity;

        DelayStartRunnable(LauncherActivity activity) {
            super();
            this.activity = activity;
        }

        @Override
        public void run() {
            activity.onViewReady();
        }
    }

}
