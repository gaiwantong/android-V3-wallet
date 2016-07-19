package info.blockchain.wallet.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.util.AppUtil;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityLandingBinding;

public class LandingActivity extends Activity {

    private ActivityLandingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_landing);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTitle(R.string.app_name);

        binding.create.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Intent intent = new Intent(LandingActivity.this, PairOrCreateWalletActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("starting_fragment", 0);
                startActivity(intent);

                return false;
            }
        });

        binding.login.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Intent intent = new Intent(LandingActivity.this, PairOrCreateWalletActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("starting_fragment", 1);
                startActivity(intent);

                return false;
            }
        });

        if (!ConnectivityStatus.hasConnectivity(this)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            final String message = getString(R.string.check_connectivity_exit);

            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.dismiss();
                                    Intent intent = new Intent(LandingActivity.this, LandingActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });

            builder.create().show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessState.getInstance(this).stopLogoutTimer();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        //Test for screen overlays before user creates a new wallet or enters confidential information
        if(new AppUtil(this).detectObscuredWindow(event)){
            return true;//consume event
        }else{
            return super.dispatchTouchEvent(event);
        }
    }
}
