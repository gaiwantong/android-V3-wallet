package info.blockchain.wallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefsUtil implements PersistantPrefs {

    private SharedPreferences preferenceManager;

    // TODO: 05/08/2016 Inject context into this class
    public PrefsUtil(Context context) {
        this.preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getValue(String name, String value) {
        return preferenceManager.getString(name, (value == null || value.length() < 1) ? "" : value);
    }

    public boolean setValue(String name, String value) {
        Editor editor = preferenceManager.edit();
        editor.putString(name, (value == null || value.length() < 1) ? "" : value);
        return editor.commit();
    }

    public int getValue(String name, int value) {
        return preferenceManager.getInt(name, 0);
    }

    public boolean setValue(String name, int value) {
        Editor editor = preferenceManager.edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        return editor.commit();
    }

    public boolean setValue(String name, long value) {
        Editor editor = preferenceManager.edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    public long getValue(String name, long value) {

        long result = 0l;
        try {
            result = preferenceManager.getLong(name, 0L);
        } catch (Exception e) {
            result = (long) preferenceManager.getInt(name, 0);
        }

        return result;
    }

    public boolean getValue(String name, boolean value) {
        return preferenceManager.getBoolean(name, value);
    }

    public boolean setValue(String name, boolean value) {
        Editor editor = preferenceManager.edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    public boolean has(String name) {
        return preferenceManager.contains(name);
    }

    public boolean removeValue(String name) {
        Editor editor = preferenceManager.edit();
        editor.remove(name);
        return editor.commit();
    }

    public boolean clear() {
        Editor editor = preferenceManager.edit();
        editor.clear();
        return editor.commit();
    }

}
