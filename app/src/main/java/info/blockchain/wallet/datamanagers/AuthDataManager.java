package info.blockchain.wallet.datamanagers;

import android.support.annotation.VisibleForTesting;

import info.blockchain.api.Access;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.rxjava.RxUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import piuk.blockchain.android.di.Injector;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by adambennett on 12/08/2016.
 */

public class AuthDataManager {

    @Inject protected PayloadManager mPayloadManager;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected Access mAccess;
    @Inject protected AppUtil mAppUtil;

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

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    protected Observable<Void> initiatePayload(String sharedKey, String guid, CharSequenceX password) {
        return Observable.defer(() -> Observable.create(subscriber -> {
            try {
                mPayloadManager.initiatePayload(
                        sharedKey,
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
                                // This syntactically incorrect, but also convenient in this case
                                subscriber.onNext(null);
                            }
                        });
            } catch (Exception e) {
                subscriber.onError(new Throwable("Create password failed: " + e));
            }
        }));
    }

    public void attemptDecryptPayload(CharSequenceX password, String guid, String payload, DecryptPayloadListener listener) {
        try {
            JSONObject jsonObject = new JSONObject(payload);

            if (jsonObject.has("payload")) {
                String encrypted_payload = jsonObject.getString("payload");

                int iterations = PayloadManager.WalletPbkdf2Iterations;
                if (jsonObject.has("pbkdf2_iterations")) {
                    iterations = jsonObject.getInt("pbkdf2_iterations");
                }

                String decrypted_payload = null;
                try {
                    decrypted_payload = AESUtil.decrypt(encrypted_payload, password, iterations);
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

                        initiatePayload(sharedKey, guid, password)
                                .compose(RxUtil.applySchedulers())
                                .subscribe(new Subscriber<Void>() {
                                    @Override
                                    public void onCompleted() {
                                        listener.onSuccess();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        listener.onPairFail();
                                    }

                                    @Override
                                    public void onNext(Void aVoid) {
                                        // onNext in this case is an error, treat as such
                                        listener.onCreateFail();
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

        void onCreateFail();

        void onAuthFail();

        void onFatalError();
    }
}
