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

    private static Context context = null;

    private static WalletUtil instance = null;

    private WalletUtil() { ; }

    public static WalletUtil getInstance(Context ctx) {

        if(instance == null) {

            context = ctx;

            instance = new WalletUtil();
        }

        return instance;
    }

    public void setValidCredentials()   {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, guid);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, sharedKey);

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_PIN_IDENTIFIER, pin_identifier);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, encrypted_password);
    }

    public String getValidPassword()   {
        return password;
    }

    public String getValidPin()   {
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
