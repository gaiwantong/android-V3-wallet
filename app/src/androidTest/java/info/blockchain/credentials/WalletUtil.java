package info.blockchain.credentials;

import android.content.Context;

import info.blockchain.wallet.util.PrefsUtil;

public class WalletUtil {

    private static String password = "-- REDACTED --";
    private static String guid = "-- REDACTED --";
    private static String sharedKey = "-- REDACTED --";
    private static String pin_identifier = "-- REDACTED --";
    private static String encrypted_password = "-- REDACTED --";
    private static String pin = "-- REDACTED --";

    private static String payload = "-- REDACTED --";

    private static String hd_spend_address = "-- REDACTED --";
    private static String hd_receive_address = "-- REDACTED --";
    private static String legacy_spend_address = "-- REDACTED --";

    private static WalletUtil instance = null;

    private WalletUtil() {
        // No-op
    }

    public static WalletUtil getInstance() {
        if (instance == null)
            instance = new WalletUtil();
        return instance;
    }

    public void setValidCredentials(Context context) {
        PrefsUtil prefs = new PrefsUtil(context);
        prefs.setValue(PrefsUtil.KEY_GUID, guid);
        prefs.setValue(PrefsUtil.KEY_SHARED_KEY, sharedKey);

        prefs.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, pin_identifier);
        prefs.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encrypted_password);
    }

    public String getValidPassword() {
        return password;
    }

    public String getValidPin() {
        return pin;
    }

    public String getGuid() {
        return guid;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public String getPinIdentifier() {
        return pin_identifier;
    }

    public String getEncryptedPassword() {
        return encrypted_password;
    }

    public String getPayload() {
        return payload;
    }

    public String getHdSpendAddress() {
        return hd_spend_address;
    }

    public String getHdReceiveAddress() {
        return hd_receive_address;
    }

    public String getLegacySpendAddress() {
        return legacy_spend_address;
    }
}
