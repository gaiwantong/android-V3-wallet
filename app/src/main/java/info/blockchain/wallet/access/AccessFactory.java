package info.blockchain.wallet.access;

import android.content.Context;

import info.blockchain.api.Access;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

public class AccessFactory {

    private static Access accessApi;
    private static String _pin = null;

    private static boolean isLoggedIn = false;

    private static Context context = null;
    private static AccessFactory instance = null;
    private static PrefsUtil prefs;

    private AccessFactory() {
        ;
    }

    public static AccessFactory getInstance(Context ctx) {

        context = ctx;
        prefs = new PrefsUtil(context);

        if (instance == null) {
            instance = new AccessFactory();
            accessApi = new Access();
        }

        return instance;
    }

    public boolean createPIN(CharSequenceX password, String pin) {

        if (pin == null || pin.equals("0000") || pin.length() != 4) {
            return false;
        }

        _pin = pin;

        new AppUtil(context).applyPRNGFixes();

        try {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            String key = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
            random.nextBytes(bytes);
            String value = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");

            JSONObject json = accessApi.setAccess(key, value, _pin);
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

    public CharSequenceX validatePIN(String pin) throws Exception {

        _pin = pin;

        String key = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encrypted_password = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");

        try {
            final JSONObject json = accessApi.validateAccess(key, _pin);
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

    public String getPIN() {
        return _pin;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean logged) {
        isLoggedIn = logged;
    }
}
