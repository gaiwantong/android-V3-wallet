package piuk.blockchain.android;

import android.support.annotation.CallSuper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import info.blockchain.wallet.access.AccessState;

/**
 * Created by adambennett on 05/08/2016.
 *
 * A base Activity for all activities which need auth timeouts
 */

public class BaseAuthActivity extends AppCompatActivity {

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        stopLogoutTimer();
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        startLogoutTimer();
    }

    /**
     * Starts the logout timer. Override in an activity if timeout is not needed.
     */
    protected void startLogoutTimer() {
        AccessState.getInstance().startLogoutTimer(this);
    }

    private void stopLogoutTimer() {
        AccessState.getInstance().stopLogoutTimer(this);
    }
}
