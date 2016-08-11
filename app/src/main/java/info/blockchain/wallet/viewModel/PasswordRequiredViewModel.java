package info.blockchain.wallet.viewModel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;

import info.blockchain.api.Access;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by adambennett on 09/08/2016.
 */

public class PasswordRequiredViewModel implements ViewModel {

    @Inject protected AppUtil mAppUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected Access mAccess;
    private DataListener mDataListener;
    private boolean mWaitingForAuth = false;
    private int timer = 0;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    public interface DataListener {

        String getPassword();

        void resetPasswordField();

        void goToPinPage();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void restartApp();

        void updateWaitingForAuthDialog(int secondsRemaining);

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable);

        void dismissProgressDialog();

    }

    public PasswordRequiredViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
    }

    public void onViewReady() {
        // No-op
    }

    public void onContinueClicked() {
        if (mDataListener.getPassword().length() > 1) {
            verifyPassword(new CharSequenceX(mDataListener.getPassword()));
        } else {
            mDataListener.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            mDataListener.restartApp();
        }

        mDataListener.resetPasswordField();
    }

    public void onForgetWalletClicked() {
        mAppUtil.clearCredentialsAndRestart();
    }

    private void verifyPassword(CharSequenceX password) {
        mDataListener.showProgressDialog(R.string.validating_password, null, false);

        String guid = mPrefsUtil.getValue(PrefsUtil.KEY_GUID, "");
        final String[] payload = new String[1];
        mWaitingForAuth = true;

        getSessionId(guid)
                .flatMap(sessionId -> getEncryptedPayload(guid, sessionId))
                .compose(applySchedulers())
                .subscribe(response -> {
                    payload[0] = response;
                    if (response.equals(Access.KEY_AUTH_REQUIRED)) {
                        waitForAuth(guid).subscribe(s -> {
                            payload[0] = s;

                            if (payload[0] == null || payload[0].equals("Authorization Required")) {
                                showErrorToastAndRestart(R.string.auth_failed);
                                return;

                            }
                            attemptDecryptPayload(password, guid, payload[0]);

                        }, throwable -> {
                            showErrorToastAndRestart(R.string.auth_failed);
                        });
                    } else {
                        attemptDecryptPayload(password, guid, payload[0]);
                    }
                    // No op
                }, throwable -> {
                    throwable.printStackTrace();
                    showErrorToastAndRestart(R.string.auth_failed);
                });
    }

    private void attemptDecryptPayload(CharSequenceX password, String guid, String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);

            if (jsonObject.has("payload")) {
                String encrypted_payload = jsonObject.getString("payload");

                int iterations = PayloadManager.WalletPbkdf2Iterations;
                if (jsonObject.has("pbkdf2_iterations")) {
                    iterations = Integer.valueOf(jsonObject.get("pbkdf2_iterations").toString());
                }

                String decrypted_payload = null;
                try {
                    decrypted_payload = AESUtil.decrypt(encrypted_payload, password, iterations);
                } catch (Exception e) {
                    showErrorToastAndRestart(R.string.pairing_failed_decrypt_error);
                }

                if (decrypted_payload != null) {
                    JSONObject decryptedJsonObject = new JSONObject(decrypted_payload);

                    if (decryptedJsonObject.has("sharedKey")) {
                        mPrefsUtil.setValue(PrefsUtil.KEY_GUID, guid);
                        mPayloadManager.setTempPassword(password);

                        String sharedKey = decryptedJsonObject.getString("sharedKey");
                        mAppUtil.setSharedKey(sharedKey);

                        initiatePayload(sharedKey, guid, password)
                                .compose(applySchedulers())
                                .subscribe(new Subscriber<Void>() {
                                    @Override
                                    public void onCompleted() {
                                        mDataListener.goToPinPage();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        mDataListener.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR);
                                    }

                                    @Override
                                    public void onNext(Void aVoid) {
                                        mDataListener.showToast(R.string.double_encryption_password_error, ToastCustom.TYPE_ERROR);
                                    }
                                });
                    }
                } else {
                    // Decryption failed
                    mDataListener.showToast(R.string.auth_failed, ToastCustom.TYPE_ERROR);
                }
            }
        } catch (JSONException e) {
            showErrorToastAndRestart(R.string.auth_failed);
        }
    }

    @UiThread
    private void showErrorToastAndRestart(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
        mAppUtil.clearCredentialsAndRestart();
    }

    private Observable<String> waitForAuth(String guid) {
        mDataListener.showProgressDialog(R.string.validating_password, null, true);

        showCheckEmailDialog()
                .compose(applySchedulers())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        // Only called if timer has run out
                        mDataListener.dismissProgressDialog();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mDataListener.dismissProgressDialog();
                        mDataListener.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR);
                    }

                    @Override
                    public void onNext(Integer timer) {
                        mDataListener.updateWaitingForAuthDialog(timer);
                    }
                });

        return pollForAuthStatus(guid)
                .compose(applySchedulers())
                .first();
    }

    private Observable<String> pollForAuthStatus(String guid) {
        return Observable.defer(() -> Observable.create(subscriber -> {
            int sleep = 2 * 1000;
            String sessionId;
            try {
                sessionId = mAccess.getSessionId(guid);
                while (mWaitingForAuth) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        String response = mAccess.getEncryptedPayload(guid, sessionId);
                        if (!response.equals(Access.KEY_AUTH_REQUIRED)) {
                            mWaitingForAuth = false;
                            subscriber.onNext(response);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                mWaitingForAuth = false;
                subscriber.onNext(Access.KEY_AUTH_REQUIRED);
            } catch (Exception e) {
                mWaitingForAuth = false;
                subscriber.onError(e);
            }
        }));
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

    private Observable<String> getEncryptedPayload(String guid, String sessionId) {
        return Observable.fromCallable(() -> mAccess.getEncryptedPayload(guid, sessionId))
                .compose(applySchedulers());
    }

    private Observable<String> getSessionId(String guid) {
        return Observable.fromCallable(() -> mAccess.getSessionId(guid))
                .compose(applySchedulers());
    }

    private Observable<Void> initiatePayload(String sharedkey, String guid, CharSequenceX password) {
        return Observable.defer(() -> Observable.create(subscriber -> {
            try {
                mPayloadManager.initiatePayload(
                        sharedkey,
                        guid,
                        password,
                        new PayloadManager.InitiatePayloadListener() {
                            @Override
                            public void onInitSuccess() {
                                mPrefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                                mPayloadManager.setTempPassword(password);
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onInitPairFail() {
                                subscriber.onError(new Throwable("onInitPairFail"));
                            }

                            @Override
                            public void onInitCreateFail(String s) {
                                subscriber.onError(new Throwable("onInitCreateFail: " + s));
                            }
                        });
            } catch (Exception e) {
                // R.string.pairing_failed
                subscriber.onError(new Throwable("Create password failed: " + e));
            }
        }));
    }

    /**
     * Applies standard Schedulers to an Observable, ie IO for subscription, Main Thread for
     * Observing
     */
    private <T> Observable.Transformer<T, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void onProgressCancelled() {
        mWaitingForAuth = false;
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }

    @Override
    public void destroy() {
        mCompositeSubscription.clear();
    }
}
