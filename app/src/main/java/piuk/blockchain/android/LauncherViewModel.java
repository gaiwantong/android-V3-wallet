package piuk.blockchain.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.viewModel.ViewModel;

import javax.inject.Inject;

import piuk.blockchain.android.di.Injector;

/**
 * Created by adambennett on 09/08/2016.
 */

public class LauncherViewModel implements ViewModel {

    public static final String INTENT_EXTRA_VERIFIED = "verified";

    @Inject protected AppUtil mAppUtil;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected AccessState mAccessState;
    private DataListener mDataListener;

    public interface DataListener {

        Intent getPageIntent();

        void onNoGuid();

        void onRequestPin();

        void onCorruptPayload();

        void onRequestUpgrade();

        void onStartMainActivity();

        void onReEnterPassword();

    }

    public LauncherViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
    }

    public void onViewReady() {

        boolean isPinValidated = false;
        Bundle extras = mDataListener.getPageIntent().getExtras();
        if (extras != null && extras.containsKey(INTENT_EXTRA_VERIFIED)) {
            isPinValidated = extras.getBoolean(INTENT_EXTRA_VERIFIED);
        }

        boolean hasLoggedOut = mPrefsUtil.getValue(PrefsUtil.LOGGED_OUT, false);

        // No GUID? Treat as new installation
        if (mPrefsUtil.getValue(PrefsUtil.KEY_GUID, "").isEmpty()) {
            mPayloadManager.setTempPassword(new CharSequenceX(""));
            mDataListener.onNoGuid();

        } else if (hasLoggedOut && !mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, "").isEmpty()) {
            // User has logged out recently. Show password reentry page
            mDataListener.onReEnterPassword();

        } else if (mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            // No PIN ID? Treat as installed app without confirmed PIN
            mDataListener.onRequestPin();

        } else if (!mAppUtil.isSane()) {
            // Installed app, check sanity
            mDataListener.onCorruptPayload();

        } else if (isPinValidated
                && !mPayloadManager.getPayload().isUpgraded()
                && !mPrefsUtil.getValue(PrefsUtil.KEY_HD_UPGRADE_ASK_LATER, false)
                && mPrefsUtil.getValue(PrefsUtil.KEY_HD_UPGRADE_LAST_REMINDER, 0L) == 0L) {
            // Legacy app has not been prompted for upgrade

            mAccessState.setIsLoggedIn(true);
            mDataListener.onRequestUpgrade();

        } else if (isPinValidated || (mAccessState.isLoggedIn())) {
            // App has been PIN validated
            mAccessState.setIsLoggedIn(true);
            mDataListener.onStartMainActivity();
        } else {
            mDataListener.onRequestPin();
        }
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }

    @Override
    public void destroy() {
        // No-op
    }
}
