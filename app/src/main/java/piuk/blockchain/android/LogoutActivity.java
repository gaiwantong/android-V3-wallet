package piuk.blockchain.android;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import info.blockchain.wallet.access.AccessState;

/**
 * Created by adambennett on 04/08/2016.
 */

public class LogoutActivity extends AppCompatActivity {

    public static final String TAG = LogoutActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (getIntent().getAction().equals(AccessState.LOGOUT_ACTION)) {
                finish();
                System.exit(0);
            }
        }
    }
}
