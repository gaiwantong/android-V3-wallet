package info.blockchain.wallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefsUtil implements PersistentPrefs {

    // TODO: 08/08/2016 Inject preference manager through constructor
    private SharedPreferences preferenceManager;

    public PrefsUtil(Context context) {
        this.preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getValue(String name, String value) {
        return preferenceManager.getString(name, (value == null || value.isEmpty()) ? "" : value);
    }

    @Override
    public boolean setValue(String name, String value) {
        Editor editor = preferenceManager.edit();
        editor.putString(name, (value == null || value.isEmpty()) ? "" : value);
        return editor.commit();
    }

    @Override
    public int getValue(String name, int value) {
        return preferenceManager.getInt(name, 0);
    }

    @Override
    public boolean setValue(String name, int value) {
        Editor editor = preferenceManager.edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        return editor.commit();
    }

    @Override
    public boolean setValue(String name, long value) {
        Editor editor = preferenceManager.edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    @Override
    public long getValue(String name, long value) {

        long result;
        try {
            result = preferenceManager.getLong(name, 0L);
        } catch (Exception e) {
            result = (long) preferenceManager.getInt(name, 0);
        }

        return result;
    }

    @Override
    public boolean getValue(String name, boolean value) {
        return preferenceManager.getBoolean(name, value);
    }

    @Override
    public boolean setValue(String name, boolean value) {
        Editor editor = preferenceManager.edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    @Override
    public boolean has(String name) {
        return preferenceManager.contains(name);
    }

    @Override
    public boolean removeValue(String name) {
        Editor editor = preferenceManager.edit();
        editor.remove(name);
        return editor.commit();
    }

    @Override
    public boolean clear() {
        Editor editor = preferenceManager.edit();
        editor.clear();
        return editor.commit();
    }

    /**
     * Clears everything but the GUID. Returns false if GUID is not present
     *
     * @return boolean  GUID present/not present or could not be written to prefs
     */
    @Override
    public boolean logOut() {
        String guid = preferenceManager.getString(PrefsUtil.KEY_GUID, "");
        clear();

        setValue(PrefsUtil.LOGGED_OUT, true);

        // TODO: 09/08/2016 This currently doesn't work - why? 
        return !guid.isEmpty() & setValue(PrefsUtil.KEY_GUID, guid);
    }

    /**
     * Reset value once user logged in
     *
     * @return boolean  Successful write or not
     */
    @Override
    public boolean logIn() {
        return setValue(PrefsUtil.LOGGED_OUT, false);
    }
}
