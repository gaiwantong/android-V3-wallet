package info.blockchain.wallet.viewModel;

import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.subscriptions.CompositeSubscription;

import static info.blockchain.wallet.view.CreateWalletFragment.KEY_INTENT_EMAIL;
import static info.blockchain.wallet.view.CreateWalletFragment.KEY_INTENT_PASSWORD;

public class RecoverFundsViewModel implements ViewModel {

    private DataListener mDataListener;
    @Inject protected AuthDataManager mAuthDataManager;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected AppUtil mAppUtil;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    public interface DataListener {

        String getRecoveryPhrase();

        Intent getPageIntent();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog(@StringRes int messageId);

        void dismissProgressDialog();

        void goToPinEntryPage();

    }

    public RecoverFundsViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
    }

    public void onViewReady() {
        // No-op
    }

    public void onContinueClicked() {
        String recoveryPhrase = mDataListener.getRecoveryPhrase();
        if (recoveryPhrase == null || recoveryPhrase.isEmpty()) {
            mDataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        String trimmed = recoveryPhrase.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        if (words != 12) {
            mDataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        String password = mDataListener.getPageIntent().getStringExtra(KEY_INTENT_PASSWORD);

        if (password == null || password.isEmpty()) {
            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
            mAppUtil.clearCredentialsAndRestart();
            return;
        }

        String email = mDataListener.getPageIntent().getStringExtra(KEY_INTENT_EMAIL);

        if (email == null || email.isEmpty()) {
            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
            mAppUtil.clearCredentialsAndRestart();
            return;
        }

        mDataListener.showProgressDialog(R.string.creating_wallet);

        mAuthDataManager.restoreHdWallet(email, password, recoveryPhrase)
                .doOnTerminate(() -> mDataListener.dismissProgressDialog())
                .subscribe(payload -> {
                    mDataListener.goToPinEntryPage();
                }, throwable -> {
                    mDataListener.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR);
                });
    }

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
