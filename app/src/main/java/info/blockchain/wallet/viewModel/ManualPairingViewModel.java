package info.blockchain.wallet.viewModel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;

import info.blockchain.api.Access;
import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.rxjava.RxUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;

public class ManualPairingViewModel implements ViewModel {

    @Inject protected AppUtil mAppUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected Access mAccess;
    @Inject protected AuthDataManager mAuthDataManager;
    private DataListener mDataListener;
    private int timer = 0;
    private String payload;
    @VisibleForTesting boolean mWaitingForAuth = false;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    public interface DataListener {

        String getGuid();

        String getPassword();

        void goToPinPage();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void restartApp();

        void updateWaitingForAuthDialog(int secondsRemaining);

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

        void dismissProgressDialog();

        void resetPasswordField();

    }

    public ManualPairingViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
    }

    public void onViewReady() {
        // No-op
    }

    public void onContinueClicked() {

        String guid = mDataListener.getGuid();
        String password = mDataListener.getPassword();

        if (guid == null || guid.isEmpty()) {
            mDataListener.showToast(R.string.invalid_guid, ToastCustom.TYPE_ERROR);
        } else if (password == null || password.isEmpty()) {
            mDataListener.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
        } else {
            verifyPassword(new CharSequenceX(password), guid);
        }
    }

    private void verifyPassword(CharSequenceX password, String guid) {
        mDataListener.showProgressDialog(R.string.validating_password, null, false);

        mWaitingForAuth = true;

        mCompositeSubscription.add(
                mAuthDataManager.getSessionId(guid)
                        .flatMap(sessionId -> mAuthDataManager.getEncryptedPayload(guid, sessionId))
                        .subscribe(response -> {
                            payload = response;
                            if (response.equals(Access.KEY_AUTH_REQUIRED)) {
                                mDataListener.showProgressDialog(R.string.validating_password, null, true);

                                mCompositeSubscription.add(
                                        waitForAuth(guid).subscribe(s -> {
                                            mWaitingForAuth = false;
                                            payload = s;

                                            if (payload == null || payload.equals(Access.KEY_AUTH_REQUIRED)) {
                                                showErrorToastAndRestart(R.string.auth_failed);
                                                return;

                                            }
                                            attemptDecryptPayload(password, guid, payload);

                                        }, throwable -> {
                                            mWaitingForAuth = false;
                                            showErrorToastAndRestart(R.string.auth_failed);
                                        }));
                            } else {
                                attemptDecryptPayload(password, guid, payload);
                            }
                        }, throwable -> {
                            throwable.printStackTrace();
                            showErrorToastAndRestart(R.string.auth_failed);
                        }));
    }

    private void attemptDecryptPayload(CharSequenceX password, String guid, String payload) {
        mAuthDataManager.attemptDecryptPayload(password, guid, payload, new AuthDataManager.DecryptPayloadListener() {
            @Override
            public void onSuccess() {
                mDataListener.goToPinPage();
            }

            @Override
            public void onPairFail() {
                showErrorToast(R.string.pairing_failed);
            }

            @Override
            public void onCreateFail() {
                showErrorToast(R.string.double_encryption_password_error);
            }

            @Override
            public void onAuthFail() {
                showErrorToast(R.string.auth_failed);
            }

            @Override
            public void onFatalError() {
                showErrorToastAndRestart(R.string.auth_failed);
            }
        });
    }

    private Observable<String> waitForAuth(String guid) {
        mCompositeSubscription.add(
                showCheckEmailDialog()
                        .compose(RxUtil.applySchedulers())
                        .subscribe(new Subscriber<Integer>() {
                            @Override
                            public void onCompleted() {
                                // Only called if timer has run out
                                mDataListener.dismissProgressDialog();
                            }

                            @Override
                            public void onError(Throwable e) {
                                showErrorToast(R.string.auth_failed);
                            }

                            @Override
                            public void onNext(Integer integer) {
                                // Called every time the timer counts down
                                mDataListener.updateWaitingForAuthDialog(timer);
                            }
                        }));

        return mAuthDataManager.startPollingAuthStatus(guid);
    }

    private Observable<Integer> showCheckEmailDialog() {
        // 120 seconds
        timer = 2 * 60;
        mDataListener.showProgressDialog(R.string.check_email_to_auth_login, String.valueOf(timer), true);
        return Observable.defer(() -> Observable.create(subscriber -> {
            while (mWaitingForAuth) {
                subscriber.onNext(timer);
                timer--;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }

                if (timer <= 0) {
                    mWaitingForAuth = false;
                    mAppUtil.clearCredentialsAndRestart();
                    subscriber.onError(new Throwable("Time expired"));
                }
            }

            subscriber.onCompleted();
        }));
    }

    public void onProgressCancelled() {
        mWaitingForAuth = false;
        destroy();
    }

    @UiThread
    private void showErrorToast(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.resetPasswordField();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
    }

    @UiThread
    private void showErrorToastAndRestart(@StringRes int message) {
        mDataListener.resetPasswordField();
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
        mAppUtil.clearCredentialsAndRestart();
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }

    @Override
    public void destroy() {
        // Clear all subscriptions so that:
        // 1) all processes stop and no memory is leaked
        // 2) processes don't try to update a null View
        // 3) background processes don't leak memory
        mCompositeSubscription.clear();
    }
}
