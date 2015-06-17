package info.blockchain.wallet;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import info.blockchain.wallet.util.AppUtil;

public class UpgradeWalletActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upgrade_wallet);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(UpgradeWalletActivity.this).updatePinEntryTime();
    }

    public void upgradeClicked(View view) {
    }

    public void askLaterClicked(View view) {
    }
}