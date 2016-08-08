package info.blockchain.wallet.access;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import info.blockchain.api.Access;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import javax.inject.Inject;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.LogoutActivity;
import piuk.blockchain.android.di.Injector;

public class AccessState {

    private static final long LOGOUT_TIMEOUT_MILLIS = 1000L * 30L;
    public static final String LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT";

    @Inject protected PrefsUtil prefs;
    @Inject protected Access accessApi;
    @Inject protected AppUtil mAppUtil;
    private String mPin;
    private boolean isLoggedIn = false;
    private PendingIntent logoutPendingIntent;
    private static AccessState instance;

    public void initAccessState(Context context) {
        Injector.getInstance().getAppComponent().inject(this);

        Intent intent = new Intent(context, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(AccessState.LOGOUT_ACTION);
        logoutPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static AccessState getInstance() {
        if (instance == null)
            instance = new AccessState();
        return instance;
    }

    public boolean createPIN(CharSequenceX password, String passedPin) {

        if (passedPin == null || passedPin.equals("0000") || passedPin.length() != 4) {
            return false;
        }

        mPin = passedPin;

        mAppUtil.applyPRNGFixes();

        try {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            String key = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
            random.nextBytes(bytes);
            String value = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");

            JSONObject json = accessApi.setAccess(key, value, mPin);
            if (json.get("success") != null) {
                String encrypted_password = AESUtil.encrypt(password.toString(), new CharSequenceX(value), AESUtil.PinPbkdf2Iterations);
                prefs.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encrypted_password);
                prefs.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, key);
                return true;
            } else {
                return false;
            }

        } catch (UnsupportedEncodingException uee) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public CharSequenceX validatePIN(String passedPin) throws Exception {

        mPin = passedPin;

        String key = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encrypted_password = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");

        try {
            final JSONObject json = accessApi.validateAccess(key, mPin);
            String decryptionKey = (String) json.get("success");
            return new CharSequenceX(AESUtil.decrypt(encrypted_password, new CharSequenceX(decryptionKey), AESUtil.PinPbkdf2Iterations));
        } catch (UnsupportedEncodingException uee) {
            throw uee;
        } catch (Exception e) {
            e.printStackTrace();

            if (e.getMessage().contains("Incorrect PIN"))
                return null;
            else
                throw e;
        }
    }

    public void setPIN(String pin) {
        mPin = pin;
    }

    public String getPIN() {
        return mPin;
    }

    /**
     * Called from {@link BaseAuthActivity#onPause()} ()}
     */
    public void startLogoutTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS, logoutPendingIntent);
    }

    /**
     * Called from {@link BaseAuthActivity#onResume()}
     */
    public void stopLogoutTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(logoutPendingIntent);
    }

    public void logout(Context context) {
        Intent intent = new Intent(context, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LOGOUT_ACTION);
        context.startActivity(intent);
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }
}
