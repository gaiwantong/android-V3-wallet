package info.blockchain.wallet.view;

import com.google.zxing.client.android.CaptureActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.view.helpers.CustomKeypad;
import info.blockchain.wallet.viewModel.ReceiveViewModel;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityReceiveBinding;

public class ReceiveActivity extends BaseAuthActivity implements ReceiveViewModel.DataListener, CustomKeypadCallback {

    private static final String  SUPPORT_URI = "http://support.blockchain.com/";
    private static final int REQUEST_BACKUP = 2225;
    private static final int MERCHANT_ACTIVITY = 1;
    public static final int SCAN_URI = 2007;

    private ReceiveViewModel mViewModel;
    private ActivityReceiveBinding mBinding;

    private CustomKeypad mCustomKeypad;
    private BottomSheetBehavior mBottomSheetBehavior;

    protected BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
//                updateSpinnerList();
//                displayQRCode(binding.accounts.spinner.getSelectedItemPosition());
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_receive);
        mViewModel = new ReceiveViewModel(this);

        Toolbar toolbar = mBinding.toolbar.toolbarGeneral;
        toolbar.setTitle(getResources().getString(R.string.receive_bitcoin));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupLayout();

        mViewModel.onViewReady();
    }

    private void setupLayout() {
         mBottomSheetBehavior = BottomSheetBehavior.from(mBinding.bottomSheet.bottomSheet);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mBinding.content.receiveMainContentShadow.setAlpha(slideOffset / 2f);
                if (slideOffset > 0) {
                    mBinding.content.receiveMainContentShadow.bringToFront();
                }
            }
        });

        // change the state of the bottom sheet
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

// set the peek height
//        bottomSheetBehavior.setPeekHeight(340);

// set hideable or not
//        bottomSheetBehavior.setHideable(false);

        setCustomKeypad();
    }

    private void setCustomKeypad(){

        mCustomKeypad = new CustomKeypad(this, (mBinding.content.keypadContainer.numericPad));
        mCustomKeypad.setDecimalSeparator(getDefaultDecimalSeparator());

        //Enable custom keypad and disables default keyboard from popping up
//        mCustomKeypad.enableOnView(mBinding.amountContainer.amountBtc);
//        mCustomKeypad.enableOnView(mBinding.amountContainer.amountFiat);
//
//        mCustomKeypad.amountContainer.amountBtc.setText("");
//        mCustomKeypad.amountContainer.amountBtc.requestFocus();
    }

    private void onShareClicked() {
        onKeypadClose();

        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private String getDefaultDecimalSeparator(){
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SCAN_URI
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);

//            doScanInput(strResult);

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_BACKUP) {
//            resetNavigationDrawer();
        }
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
//                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
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
