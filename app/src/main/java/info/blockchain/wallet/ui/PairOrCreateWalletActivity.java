package info.blockchain.wallet.ui;

import com.google.zxing.client.android.CaptureActivity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.viewModel.PairingViewModel;

import piuk.blockchain.android.R;

public class PairOrCreateWalletActivity extends ActionBarActivity {

    public static final int PAIRING_QR = 2005;
    public static Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blockchain_blue)));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.account_spinner).setVisibility(View.GONE);

        fragment = new CreateWalletFragment();
        if (getIntent().getIntExtra("starting_fragment", 1) == 1)
            fragment = new PairWalletFragment();

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager.getBackStackEntryCount() > 0)
                    fragmentManager.popBackStack();
                else {
                    Intent intent = new Intent(PairOrCreateWalletActivity.this, LandingActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {

        if (fragment != null && fragment instanceof ManualPairingFragment) {
            fragment = null;
            getFragmentManager().popBackStack();
        } else {
            AppUtil.getInstance(PairOrCreateWalletActivity.this).restartApp();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == PAIRING_QR) {
            if (data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
                PairingViewModel viewModel = new PairingViewModel(PairOrCreateWalletActivity.this);
                viewModel.pairWithQR(data.getStringExtra(CaptureActivity.SCAN_RESULT));
            }
        }
    }
}
