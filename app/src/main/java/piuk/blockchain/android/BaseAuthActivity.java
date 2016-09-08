package piuk.blockchain.android;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.rxjava.RxUtil;
import info.blockchain.wallet.util.SSLVerifyUtil;

import rx.subscriptions.CompositeSubscription;

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */

public class BaseAuthActivity extends AppCompatActivity {

    private AlertDialog mAlertDialog;
    private SSLVerifyUtil mSSLVerifyUtil = new SSLVerifyUtil(this);
    private static CompositeSubscription mCompositeSubscription;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DOGFOOD) {
            disallowScreenshots();
        }

        mCompositeSubscription = new CompositeSubscription();

        // Subscribe to SSL pinning events
        mCompositeSubscription.add(
                mSSLVerifyUtil.getSslPinningSubject()
                        .compose(RxUtil.applySchedulers())
                        .subscribe(sslEvent -> {
                                    switch (sslEvent) {
                                        case ServerDown:
                                            showAlertDialog(getString(R.string.ssl_no_connection), false);
                                            break;
                                        case PinningFail:
                                            showAlertDialog(getString(R.string.ssl_pinning_invalid), true);
                                            break;
                                        case NoConnection:
                                            showAlertDialog(getString(R.string.ssl_no_connection), false);
                                            break;
                                        case Success:
                                            // No-op
                                        default:
                                            // No-op
                                    }
                                },
                                Throwable::printStackTrace));
    }

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

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.clear();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
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

    /**
     * Override if you want a particular activity to be able to be screenshot.
     */
    protected void disallowScreenshots() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void showAlertDialog(final String message, final boolean forceExit) {
        if (mAlertDialog != null) mAlertDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(message)
                .setCancelable(false);

        if (!forceExit) {
            builder.setPositiveButton(R.string.retry, (d, id) -> {
                // Retry
                mSSLVerifyUtil.validateSSL();
            });
        }

        builder.setNegativeButton(R.string.exit, (d, id) -> finish());

        mAlertDialog = builder.create();

        if (!isFinishing()) {
            mAlertDialog.show();
        }
    }
}
