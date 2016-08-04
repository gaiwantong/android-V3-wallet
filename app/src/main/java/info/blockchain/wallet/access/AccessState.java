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

import piuk.blockchain.android.LogoutActivity;

public class AccessState {

    private static final long LOGOUT_TIMEOUT = 1000L * 30L; // 30 seconds in milliseconds
    public static final String LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT";

    private Access accessApi;

    private static PrefsUtil prefs;

    private static String pin;
    private static boolean isLoggedIn = false;
    private Context mContext;
    private PendingIntent logoutPendingIntent;
    private static AccessState instance;

    public void initAccessState(Context context) {
        mContext = context;
        prefs = new PrefsUtil(context);
        accessApi = new Access();
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

        pin = passedPin;

        new AppUtil(mContext).applyPRNGFixes();

        try {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            String key = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
            random.nextBytes(bytes);
            String value = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");

            JSONObject json = accessApi.setAccess(key, value, pin);
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

        pin = passedPin;

        String key = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encrypted_password = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");

        try {
            final JSONObject json = accessApi.validateAccess(key, pin);
            String decryptionKey = (String) json.get("success");
            CharSequenceX password = new CharSequenceX(AESUtil.decrypt(encrypted_password, new CharSequenceX(decryptionKey), AESUtil.PinPbkdf2Iterations));
            return password;
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
        this.pin = pin;
    }

    public String getPIN() {
        return pin;
    }

    /**
     * Called from all activities' onPause
     */
    public void startLogoutTimer() {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT, logoutPendingIntent);
    }

    /**
     * Called from all activities' onResume
     */
    public void stopLogoutTimer() {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(logoutPendingIntent);
    }

    public void logout() {
        Intent intent = new Intent(mContext, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LOGOUT_ACTION);
        mContext.startActivity(intent);
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
    }
}
