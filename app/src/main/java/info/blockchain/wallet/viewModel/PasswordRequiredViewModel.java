package info.blockchain.wallet.viewModel;

import android.support.annotation.StringRes;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.Observable;
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
    private DataListener mDataListener;
    private CompositeSubscription mCompositeSubscription;

    public interface DataListener {

        String getPassword();

        void resetPasswordField();

        void goToPinPage();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void restartApp();

        void showProgressDialog(@StringRes int message);

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
        mDataListener.showProgressDialog(R.string.validating_password);

        mCompositeSubscription.add(createUpdatePayloadObservable(password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(() -> mPayloadManager.setTempPassword(new CharSequenceX("")))
                .doAfterTerminate(() -> mDataListener.dismissProgressDialog())
                .subscribe(o -> {
                    mDataListener.showToast(R.string.pin_4_strikes_password_accepted, ToastCustom.TYPE_OK);

                    mPayloadManager.setTempPassword(password);
                    mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                    mDataListener.goToPinPage();
                }, throwable -> {
                    mDataListener.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
                    mDataListener.restartApp();
                })
        );
    }

    private Observable<Void> createUpdatePayloadObservable(CharSequenceX password) {
        return Observable.defer(() -> Observable.create(subscriber -> {
            try {
                mPayloadManager.initiatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password,
                        new PayloadManager.InitiatePayloadListener() {
                            @Override
                            public void onInitSuccess() {
                                subscriber.onNext(null);
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
                subscriber.onError(new Throwable("Create password failed: " + e));
            }
        }));
    }


    @Override
    public void destroy() {
        mCompositeSubscription.clear();
    }
}
