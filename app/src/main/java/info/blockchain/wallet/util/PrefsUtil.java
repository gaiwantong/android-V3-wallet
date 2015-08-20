package info.blockchain.wallet.util;
 
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrefsUtil {

    public static final String DEFAULT_CURRENCY = "USD";

    public static final String KEY_PIN_IDENTIFIER       = "pin_kookup_key";
    public static final String KEY_ENCRYPTED_PASSWORD   = "encrypted_password";
    public static final String KEY_GUID                 = "guid";
    public static final String KEY_SHARED_KEY           = "sharedKey";
    public static final String KEY_PIN_FAILS            = "pin_fails";
    // public static final String KEY_LOGGED_IN              = "logged_in";
    public static final String KEY_BTC_UNITS            = "btcUnits";
    public static final String KEY_SELECTED_FIAT        = "ccurrency";
    public static final String KEY_INITIAL_ACCOUNT_NAME = "_1ST_ACCOUNT_NAME";
	public static final String KEY_EMAIL           		= "email";
	public static final String KEY_EMAIL_VERIFIED 		= "code_verified";
    public static final String KEY_SESSION_ID 			= "session_id";
    public static final String KEY_HD_UPGRADED_LAST_REMINDER = "hd_upgraded_last_reminder";
    public static final String KEY_ASK_LATER = "ask_later";
    public static final String KEY_EMAIL_VERIFY_ASK_LATER = "email_verify_ask_later";

    private static Context   context  = null;
    private static PrefsUtil instance = null;

    private PrefsUtil() {
        ;
    }

    public static PrefsUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new PrefsUtil();
        }

        return instance;
    }

    public String getValue(String name, String value) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
	}

	public boolean setValue(String name, String value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(name, (value == null || value.length() < 1) ? "" : value);
		return editor.commit();
	}

	public int getValue(String name, int value) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    return prefs.getInt(name, 0);
	}

	public boolean setValue(String name, int value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putInt(name, (value < 0) ? 0 : value);
		return editor.commit();
	}

    public boolean setValue(String name, long value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    public long getValue(String name, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(name, 0L);
    }

	public boolean getValue(String name, boolean value) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    return prefs.getBoolean(name, value);
	}

	public boolean setValue(String name, boolean value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(name, value);
		return editor.commit();
	}

    public boolean removeValue(String name) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.remove(name);
		return editor.commit();
	}

	public boolean clear() {
		String cookie = getValue(KEY_SESSION_ID,null);
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		/*
		editor.remove(KEY_PIN_IDENTIFIER);
		editor.remove(KEY_ENCRYPTED_PASSWORD);
		editor.remove(KEY_GUID);
		editor.remove(KEY_SHARED_KEY);
		editor.remove(KEY_PIN_FAILS);
		*/
		editor.clear();
		if(cookie!=null)setValue(KEY_SESSION_ID, cookie);
		return editor.commit();
	}

}
