package info.blockchain.wallet.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;

import info.blockchain.api.PersistentUrls;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityLandingBinding;

public class LandingActivity extends BaseAuthActivity {

    public static final String KEY_STARTING_FRAGMENT = "starting_fragment";
    public static final String KEY_INTENT_RECOVERING_FUNDS = "recovering_funds";

    private ActivityLandingBinding binding;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CREATE_FRAGMENT, LOGIN_FRAGMENT})
    public @interface StartingFragment {
    }

    public static final int CREATE_FRAGMENT = 0;
    public static final int LOGIN_FRAGMENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_landing);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTitle(R.string.app_name);

        //TODO - Temporarily hide this option - WIP
//        if(BuildConfig.DOGFOOD || BuildConfig.DEBUG) {
//            binding.settingsBtn.setVisibility(View.VISIBLE);
//        }
        binding.settingsBtn.setVisibility(View.GONE);

        binding.create.setOnClickListener(view -> startLandingActivity(CREATE_FRAGMENT));
        binding.login.setOnClickListener(view -> startLandingActivity(LOGIN_FRAGMENT));
        binding.recoverFunds.setOnClickListener(view -> showFundRecoveryWarning());

        if (!ConnectivityStatus.hasConnectivity(this)) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setMessage(getString(R.string.check_connectivity_exit))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue, (d, id) -> {
                        Intent intent = new Intent(LandingActivity.this, LandingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    })
                    .create()
                    .show();
        }
    }

    private void startLandingActivity(@StartingFragment int createFragment) {
        Intent intent = new Intent(this, PairOrCreateWalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_STARTING_FRAGMENT, createFragment);
        startActivity(intent);
    }

    private void startRecoveryActivityFlow() {
        Intent intent = new Intent(this, PairOrCreateWalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_STARTING_FRAGMENT, CREATE_FRAGMENT);
        intent.putExtra(KEY_INTENT_RECOVERING_FUNDS, true);
        startActivity(intent);
    }

    private void showFundRecoveryWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.recover_funds_warning_message)
                .setPositiveButton(R.string.dialog_continue, (dialogInterface, i) -> startRecoveryActivityFlow())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void startLogoutTimer() {
        // No-op
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user creates a new wallet or enters confidential information
        // consume event
        return new AppUtil(this).detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }

    public void onSettingsClicked(View view) {

        PrefsUtil prefsUtil = new PrefsUtil(this);
        int currentEnvironment = prefsUtil.getValue(PrefsUtil.KEY_BACKEND_ENVIRONMENT,0);

        PersistentUrls urls = PersistentUrls.getInstance();

        String[] backends = {"Production", "Dev", "Staging"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select backend")
                .setSingleChoiceItems(backends, currentEnvironment,
                        (dialogInterface, i) -> {
                            switch (i) {
                                case 0:
                                    urls.setProductionEnvironment();
                                    prefsUtil.setValue(PrefsUtil.KEY_BACKEND_ENVIRONMENT, 0);
                                    break;
                                case 1:
//                                    urls.setDevelopmentEnvironment();
                                    prefsUtil.setValue(PrefsUtil.KEY_BACKEND_ENVIRONMENT, 1);
                                    break;
                                case 2:
//                                    urls.setStagingEnvironment();
                                    prefsUtil.setValue(PrefsUtil.KEY_BACKEND_ENVIRONMENT, 2);
                                    break;
                            }
                        });

        builder.create().show();
    }
}
