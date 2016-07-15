package info.blockchain.wallet.view;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import info.blockchain.wallet.access.AccessState;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityAboutBinding;

public class AboutActivity extends Activity {

    private String strMerchantPackage = "info.blockchain.merchant";

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_about);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        binding.about.setText(getString(R.string.about, BuildConfig.VERSION_NAME, "2015"));

        binding.rateUs.setOnClickListener(v -> {
            String appPackageName = getPackageName();
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(marketIntent);
        });

        if (hasWallet()) {
            binding.freeWallet.setVisibility(View.GONE);
        } else {
            binding.freeWallet.setOnClickListener(v -> {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + strMerchantPackage));
                startActivity(marketIntent);
            });
        }

    }

    private boolean hasWallet() {
        PackageManager pm = this.getPackageManager();
        try {
            pm.getPackageInfo(strMerchantPackage, 0);
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessState.getInstance(this).stopLogoutTimer();
    }

    @Override
    protected void onPause() {
        AccessState.getInstance(this).startLogoutTimer();
        super.onPause();
    }
}
