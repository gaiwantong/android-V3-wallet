package info.blockchain.wallet.viewModel;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.TextView;

import info.blockchain.wallet.callbacks.DialogButtonCallback;
import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.StringUtils;
import info.blockchain.wallet.util.ViewUtils;
import info.blockchain.wallet.view.helpers.ToastCustom;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.exceptions.Exceptions;
import rx.subscriptions.CompositeSubscription;

import static info.blockchain.wallet.view.CreateWalletFragment.KEY_INTENT_EMAIL;
import static info.blockchain.wallet.view.CreateWalletFragment.KEY_INTENT_PASSWORD;
import static info.blockchain.wallet.view.LandingActivity.KEY_INTENT_RECOVERING_FUNDS;

public class PinEntryViewModel implements ViewModel {

    private static final int PIN_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 4;

    private DataListener mDataListener;
    @Inject protected AuthDataManager mAuthDataManager;
    @Inject protected AppUtil mAppUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected StringUtils mStringUtils;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    private String email;
    private CharSequenceX password;
    private boolean recoveringFunds = false;
    @VisibleForTesting String mUserEnteredPin = "";
    @VisibleForTesting String mUserEnteredConfirmationPin;
    @VisibleForTesting boolean bAllowExit = true;

    public interface DataListener {

        Intent getPageIntent();

        TextView[] getPinBoxArray();

        void showProgressDialog(@StringRes int messageId, @Nullable String suffix);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void dismissProgressDialog();

        void showMaxAttemptsDialog();

        void showValidationDialog();

        void showCommonPinWarning(DialogButtonCallback callback);

        void showWalletVersionNotSupportedDialog(String walletVersion);

        void goToUpgradeWalletActivity();

        void restartPage();

        void setTitleString(@StringRes int title);

        void setTitleVisibility(@ViewUtils.Visibility int visibility);

        void clearPinBoxes();

        void goToPasswordRequiredActivity();
    }

    public PinEntryViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
    }

    public void onViewReady() {
        if (mDataListener.getPageIntent() != null) {
            Bundle extras = mDataListener.getPageIntent().getExtras();
            if (extras != null) {
                if (extras.containsKey(KEY_INTENT_EMAIL)) {
                    email = extras.getString(KEY_INTENT_EMAIL);
                }

                if (extras.containsKey(KEY_INTENT_PASSWORD)) {
                    //noinspection ConstantConditions
                    password = new CharSequenceX(extras.getString(KEY_INTENT_PASSWORD));
                }

                if (extras.containsKey(KEY_INTENT_RECOVERING_FUNDS)) {
                    recoveringFunds = extras.getBoolean(KEY_INTENT_RECOVERING_FUNDS);
                }

                if (password != null && password.length() > 0 && email != null && !email.isEmpty()) {
                    // Previous page was CreateWalletFragment
                    bAllowExit = false;
                    saveLoginAndPassword();
                    if (!recoveringFunds) {
                        // If funds recovered, wallet already restored, no need to overwrite payload
                        // with another new wallet
                        mDataListener.showProgressDialog(R.string.create_wallet, "...");
                        createWallet();
                    }
                }
            }
        }

        checkPinFails();
    }

    public void onDeleteClicked() {
        if (mUserEnteredPin.length() > 0) {
            // Remove last char from pin string
            mUserEnteredPin = mUserEnteredPin.substring(0, mUserEnteredPin.length() - 1);

            // Clear last box
            mDataListener.getPinBoxArray()[mUserEnteredPin.length()].setBackgroundResource(R.drawable.rounded_view_blue_white_border);
        }
    }

    public void padClicked(View view) {
        if (mUserEnteredPin.length() == PIN_LENGTH) {
            return;
        }

        // Append tapped #
        mUserEnteredPin = mUserEnteredPin + view.getTag().toString().substring(0, 1);
        mDataListener.getPinBoxArray()[mUserEnteredPin.length() - 1].setBackgroundResource(R.drawable.rounded_view_dark_blue);

        // Perform appropriate action if PIN_LENGTH has been reached
        if (mUserEnteredPin.length() == PIN_LENGTH) {

            // Throw error on '0000' to avoid server-side type issue
            if (mUserEnteredPin.equals("0000")) {
                showErrorToast(R.string.zero_pin);
                clearPinViewAndReset();
                return;
            }

            // Only show warning on first entry and if user is creating a new PIN
            if (isCreatingNewPin() && isPinCommon(mUserEnteredPin) && mUserEnteredConfirmationPin == null) {
                mDataListener.showCommonPinWarning(new DialogButtonCallback() {
                    @Override
                    public void onPositiveClicked() {
                        clearPinViewAndReset();
                    }

                    @Override
                    public void onNegativeClicked() {
                        validateAndConfirmPin();
                    }
                });
            } else {
                validateAndConfirmPin();
            }
        }
    }

    private void validateAndConfirmPin() {
        // Validate
        if (!mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            mDataListener.setTitleVisibility(View.INVISIBLE);
            validatePIN(mUserEnteredPin);
        } else if (mUserEnteredConfirmationPin == null) {
            // End of Create -  Change to Confirm
            mUserEnteredConfirmationPin = mUserEnteredPin;
            mUserEnteredPin = "";
            mDataListener.setTitleString(R.string.confirm_pin);
            clearPinBoxes();
        } else if (mUserEnteredConfirmationPin.equals(mUserEnteredPin)) {
            // End of Confirm - Pin is confirmed
            createNewPin(mUserEnteredPin);
        } else {
            // End of Confirm - Pin Mismatch
            showErrorToast(R.string.pin_mismatch_error);
            mDataListener.setTitleString(R.string.create_pin);
            clearPinViewAndReset();
        }
    }

    private void clearPinViewAndReset() {
        clearPinBoxes();
        mUserEnteredConfirmationPin = null;
    }

    public void clearPinBoxes() {
        mUserEnteredPin = "";
        mDataListener.clearPinBoxes();
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    void updatePayload(CharSequenceX password) {
        mDataListener.showProgressDialog(R.string.decrypting_wallet, null);

        mCompositeSubscription.add(
                mAuthDataManager.updatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doOnTerminate(() -> mDataListener.dismissProgressDialog())
                        .subscribe(aVoid -> {
                            mAppUtil.setSharedKey(mPayloadManager.getPayload().getSharedKey());

                            setAccountLabelIfNecessary();

                            if (!mPayloadManager.getPayload().isUpgraded()) {
                                mDataListener.goToUpgradeWalletActivity();
                            } else {
                                mAppUtil.restartAppWithVerifiedPin();
                            }

                        }, throwable -> {

                            if (throwable instanceof InvalidCredentialsException) {
                                mDataListener.goToPasswordRequiredActivity();

                            } else if (throwable instanceof ServerConnectionException) {
                                mDataListener.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);

                            } else if (throwable instanceof UnsupportedVersionException) {
                                mDataListener.showWalletVersionNotSupportedDialog(throwable.getMessage());

                            } else if (throwable instanceof DecryptionException) {
                                mDataListener.goToPasswordRequiredActivity();

                            } else if (throwable instanceof PayloadException) {
                                //This shouldn't happen - Payload retrieved from server couldn't be parsed
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();

                            } else if (throwable instanceof HDWalletException) {
                                //This shouldn't happen. HD fatal error - not safe to continue - don't clear credentials
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                                mAppUtil.restartApp();
                            }

                        }));
    }

    public void validatePassword(CharSequenceX password) {
        mDataListener.showProgressDialog(R.string.validating_password, null);

        mCompositeSubscription.add(
                mAuthDataManager.updatePayload(
                        mPrefsUtil.getValue(PrefsUtil.KEY_SHARED_KEY, ""),
                        mPrefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                        password)
                        .doOnSubscribe(() -> mPayloadManager.setTempPassword(new CharSequenceX("")))
                        .subscribe(o -> {
                            mDataListener.showToast(R.string.pin_4_strikes_password_accepted, ToastCustom.TYPE_OK);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
                            mPrefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);
                            mDataListener.restartPage();
                            mDataListener.dismissProgressDialog();
                        }, throwable -> {

                            if (throwable instanceof ServerConnectionException) {
                                mDataListener.showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR);

                            } else {
                                showErrorToast(R.string.invalid_password);
                                mDataListener.showValidationDialog();
                            }

                        }));
    }

    private void createNewPin(String pin) {
        mDataListener.showProgressDialog(R.string.creating_pin, null);

        mCompositeSubscription.add(
                mAuthDataManager.createPin(mPayloadManager.getTempPassword(), pin)
                        .subscribe(createSuccessful -> {
                            mDataListener.dismissProgressDialog();
                            if (createSuccessful) {
                                mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                                updatePayload(mPayloadManager.getTempPassword());
                            } else {
                                throw Exceptions.propagate(new Throwable("Pin create failed"));
                            }
                        }, throwable -> {
                            showErrorToast(R.string.create_pin_failed);
                            mPrefsUtil.clear();
                            mAppUtil.restartApp();
                        }));
    }

    private void validatePIN(String pin) {
        mDataListener.showProgressDialog(R.string.validating_pin, null);

        mAuthDataManager.validatePin(pin)
                .subscribe(password -> {
                    mDataListener.dismissProgressDialog();
                    if (password != null) {
                        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, 0);
                        updatePayload(password);
                    } else {
                        incrementFailureCount();
                    }
                }, throwable -> {
                    showErrorToast(R.string.unexpected_error);
                    mDataListener.restartPage();
                });
    }

    public void incrementFailureCount() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        mPrefsUtil.setValue(PrefsUtil.KEY_PIN_FAILS, ++fails);
        showErrorToast(R.string.invalid_pin);
        mDataListener.restartPage();
    }

    // Check user's password if PIN fails >= 4
    private void checkPinFails() {
        int fails = mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0);
        if (fails >= MAX_ATTEMPTS) {
            showErrorToast(R.string.pin_4_strikes);
            mPayloadManager.getPayload().stepNumber = 0;
            mDataListener.showMaxAttemptsDialog();
        }
    }

    private void saveLoginAndPassword() {
        mPrefsUtil.setValue(PrefsUtil.KEY_EMAIL, email);
        mPayloadManager.setEmail(email);
        mPayloadManager.setTempPassword(password);
    }

    private void setAccountLabelIfNecessary() {
        if (mAppUtil.isNewlyCreated()
                && mPayloadManager.getPayload().getHdWallet() != null
                && (mPayloadManager.getPayload().getHdWallet().getAccounts().get(0).getLabel() == null
                || mPayloadManager.getPayload().getHdWallet().getAccounts().get(0).getLabel().isEmpty())) {

            mPayloadManager.getPayload().getHdWallet().getAccounts().get(0).setLabel(mStringUtils.getString(R.string.default_wallet_name));
        }
    }

    private void createWallet() {
        mCompositeSubscription.add(
                mAuthDataManager.createHdWallet(password.toString(), mStringUtils.getString(R.string.default_wallet_name))
                        .doAfterTerminate(() -> mDataListener.dismissProgressDialog())
                        .subscribe(payload -> {
                            if (payload == null) {
                                showErrorToast(R.string.remote_save_ko);
                            }
                        }, throwable -> showErrorToastAndRestartApp(R.string.hd_error)));
    }

    private boolean isPinCommon(String pin) {
        List<String> commonPins = new ArrayList<String>() {{
            add("1234");
            add("1111");
            add("1212");
            add("7777");
            add("1004");
        }};
        return commonPins.contains(pin);
    }

    public void resetApp() {
        mAppUtil.clearCredentialsAndRestart();
    }

    public boolean allowExit() {
        return bAllowExit;
    }

    public boolean isCreatingNewPin() {
        return mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty();
    }

    @UiThread
    private void showErrorToast(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
    }

    @UiThread
    private void showErrorToastAndRestartApp(@StringRes int message) {
        mDataListener.dismissProgressDialog();
        mDataListener.showToast(message, ToastCustom.TYPE_ERROR);
        resetApp();
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
