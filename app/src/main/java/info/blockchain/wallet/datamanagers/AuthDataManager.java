package info.blockchain.wallet.datamanagers;

import android.support.annotation.VisibleForTesting;

import info.blockchain.api.Access;
import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.payload.BlockchainWallet;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.rxjava.RxUtil;
import info.blockchain.wallet.util.AESUtilWrapper;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

public class AuthDataManager {

    @Inject protected PayloadManager mPayloadManager;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected Access mAccess;
    @Inject protected AppUtil mAppUtil;
    @Inject protected AESUtilWrapper mAESUtil;
    @Inject protected AccessState mAccessState;
    @Inject protected StringUtils mStringUtils;
    @VisibleForTesting protected int timer;

    public AuthDataManager() {
        Injector.getInstance().getAppComponent().inject(this);
    }

    public Observable<String> getEncryptedPayload(String guid, String sessionId) {
        return Observable.fromCallable(() -> mAccess.getEncryptedPayload(guid, sessionId))
                .compose(RxUtil.applySchedulers());
    }

    public Observable<String> getSessionId(String guid) {
        return Observable.fromCallable(() -> mAccess.getSessionId(guid))
                .compose(RxUtil.applySchedulers());
    }

    public Observable<Void> updatePayload(String sharedKey, String guid, CharSequenceX password) {
        return getUpdatePayloadObservable(sharedKey, guid, password)
                .compose(RxUtil.applySchedulers());
    }

    public Observable<CharSequenceX> validatePin(String pin) {
        return Observable.fromCallable(() -> mAccessState.validatePIN(pin))
                .compose(RxUtil.applySchedulers());
    }

    public Observable<Boolean> createPin(CharSequenceX password, String pin) {
        return Observable.fromCallable(() -> mAccessState.createPIN(password, pin))
                .compose(RxUtil.applySchedulers());
    }

    public Observable<Payload> createHdWallet(String password, String walletName) {
        return Observable.fromCallable(() -> mPayloadManager.createHDWallet(password, walletName))
                .compose(RxUtil.applySchedulers())
                .doOnNext(payload -> {
                    if (payload != null) {
                        // Successfully created and saved
                        mAppUtil.setNewlyCreated(true);
                        mPrefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                        mAppUtil.setSharedKey(payload.getSharedKey());
                    }
                });
    }

    public Observable<Payload> restoreHdWallet(String email, String password, String passphrase) {
        mPayloadManager.setEmail(email);
        return Observable.fromCallable(() -> mPayloadManager.restoreHDWallet(
                password, passphrase, mStringUtils.getString(R.string.default_wallet_name)))
                .doOnNext(payload -> {
                    if (payload == null) {
                        throw Exceptions.propagate(new Throwable("Save failed"));
                    } else {
                        mPrefsUtil.setValue(PrefsUtil.KEY_GUID, payload.getGuid());
                        mAppUtil.setSharedKey(payload.getSharedKey());
                    }
                })
                .compose(RxUtil.applySchedulers());
    }

    public Observable<String> startPollingAuthStatus(String guid) {
        // Get session id
        return getSessionId(guid)
                // return Observable that emits ticks every two seconds, pass in Session ID
                .flatMap(sessionId -> Observable.interval(2, TimeUnit.SECONDS)
                        // For each emission from the timer, try to get the payload
                        .map(tick -> getEncryptedPayload(guid, sessionId).toBlocking().first())
                        // If auth not required, emit payload
                        .filter(s -> !s.equals(Access.KEY_AUTH_REQUIRED))
                        // If error called, emit Auth Required
                        .onErrorReturn(throwable -> Observable.just(Access.KEY_AUTH_REQUIRED).toBlocking().first())
                        // Make sure threading is correct
                        .compose(RxUtil.applySchedulers())
                        // Only emit the first object
                        .first());
    }

    private Observable<Void> getUpdatePayloadObservable(String sharedKey, String guid, CharSequenceX password) {
        return Observable.defer(() -> Observable.create(subscriber -> {
            try {
                mPayloadManager.initiatePayload(
                        sharedKey,
                        guid,
                        password,
                        () -> {

                            mPayloadManager.setTempPassword(password);
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        });
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }));
    }

    public Observable<Integer> createCheckEmailTimer() {
        timer = 2 * 60;

        return Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(aLong -> timer--)
                .takeUntil(aLong -> timer < 0);
    }

    /*
    TODO - move to jar
     */
    public void attemptDecryptPayload(CharSequenceX password, String guid, String payload, DecryptPayloadListener listener) {
        try {
            JSONObject jsonObject = new JSONObject(payload);

            //TODO - Payload v1, 2 or 3?
            if (jsonObject.has("payload")) {
                String encrypted_payload = jsonObject.getString("payload");

                int iterations = BlockchainWallet.DEFAULT_PBKDF2_ITERATIONS;
                if (jsonObject.has("pbkdf2_iterations")) {
                    iterations = jsonObject.getInt("pbkdf2_iterations");
                }

                String decrypted_payload = null;
                try {
                    decrypted_payload = mAESUtil.decrypt(encrypted_payload, password, iterations);
                } catch (Exception e) {
                    listener.onFatalError();
                }

                if (decrypted_payload != null) {
                    JSONObject decryptedJsonObject = new JSONObject(decrypted_payload);

                    if (decryptedJsonObject.has("sharedKey")) {
                        mPrefsUtil.setValue(PrefsUtil.KEY_GUID, guid);
                        mPayloadManager.setTempPassword(password);

                        String sharedKey = decryptedJsonObject.getString("sharedKey");
                        mAppUtil.setSharedKey(sharedKey);

                        updatePayload(sharedKey, guid, password)
                                .compose(RxUtil.applySchedulers())
                                .subscribe(new Subscriber<Void>() {
                                    @Override
                                    public void onCompleted() {
                                        mPrefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                                        listener.onSuccess();
                                    }

                                    @Override
                                    public void onError(Throwable throwable) {

                                        if (throwable instanceof InvalidCredentialsException) {
                                            listener.onAuthFail();

                                        } else if (throwable instanceof PayloadException) {
                                            //This shouldn't happen - Payload retrieved from server couldn't be parsed
                                            listener.onFatalError();

                                        } else if (throwable instanceof HDWalletException) {
                                            //This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                                            listener.onFatalError();

                                        }else{
                                            listener.onPairFail();
                                        }
                                    }

                                    @Override
                                    public void onNext(Void aVoid) {
                                        // No-op
                                    }
                                });
                    }
                } else {
                    // Decryption failed
                    listener.onAuthFail();
                }
            }
        } catch (JSONException e) {
            listener.onFatalError();
        }
    }

    public interface DecryptPayloadListener {

        void onSuccess();

        void onPairFail();

        void onAuthFail();

        void onFatalError();
    }
}
