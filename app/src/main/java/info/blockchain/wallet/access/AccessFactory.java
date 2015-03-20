package info.blockchain.wallet.access;

import java.io.*;
import java.security.SecureRandom;

import android.content.Context;
//import android.util.Log;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.json.JSONObject;

import info.blockchain.wallet.util.Web;

public class AccessFactory	{

    private static String _key = null;
    private static String _value = null;
    private static String _pin = null;

    private static boolean isLoggedIn = false;

    private static Context context = null;
    private static AccessFactory instance = null;

    private AccessFactory()	{ ; }

    public static AccessFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if (instance == null) {
            instance = new AccessFactory();
        }

        return instance;
    }

    public boolean createPÏN(CharSequenceX password, String pin) {

        if(pin == null || pin.equals("0000") || pin.length() != 4) {
            return false;
        }

        _pin = pin;

        try {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            _key = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
//            Log.i("AccessFactory", "_key:" + _key);
            random.nextBytes(bytes);
            _value = new String(org.spongycastle.util.encoders.Hex.encode(bytes), "UTF-8");
//            Log.i("AccessFactory", "_value:" + _value);
            
            JSONObject json = apiStoreKey();
//            Log.i("AccessFactory", "JSON response:" + json.toString());
			if(json.get("success") != null) {
				String encrypted_password = AESUtil.encrypt(password.toString(), new CharSequenceX(_value), AESUtil.PasswordPBKDF2Iterations);
				PrefsUtil.getInstance(context).setValue(PrefsUtil.ENCRYPTED_PASSWORD, encrypted_password);
				PrefsUtil.getInstance(context).setValue(PrefsUtil.PIN_LOOKUP, _key);
				return true;
			}
			else {
				return false;
			}

        }
        catch(UnsupportedEncodingException uee) {
            return false;
        }
        catch(Exception e) {
        	e.printStackTrace();
            return false;
        }

    }
    
    public CharSequenceX validatePÏN(String pin) {
    	
    	CharSequenceX password = null;

        _pin = pin;
        _key = PrefsUtil.getInstance(context).getValue(PrefsUtil.PIN_LOOKUP, "");
        String encrypted_password = PrefsUtil.getInstance(context).getValue(PrefsUtil.ENCRYPTED_PASSWORD, "");
        
        try {
			final JSONObject json = apiGetValue();
//            Log.i("AccessFactory", "JSON response:" + json.toString());
			String decryptionKey = (String)json.get("success");
			password = new CharSequenceX(AESUtil.decrypt(encrypted_password, new CharSequenceX(decryptionKey), AESUtil.PasswordPBKDF2Iterations));
			return password;
        }
        catch(UnsupportedEncodingException uee) {
            return null;
        }
        catch(Exception e) {
        	e.printStackTrace();
            return null;
        }
    }

    public boolean writeAccess() {
        return true;
    }

    public String getKey() {
        return _key;
    }

    public String getValue() {
        return _value;
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

    public String encryptPW(CharSequenceX pw) {
        return AESUtil.encrypt(pw.toString(), new CharSequenceX(getValue()), AESUtil.PasswordPBKDF2Iterations);
    }

    public String decryptPW(CharSequenceX pw, CharSequenceX dkey) {
        return AESUtil.decrypt(pw.toString(), new CharSequenceX(dkey), AESUtil.PasswordPBKDF2Iterations);
    }

    private JSONObject apiGetValue() throws Exception {

        StringBuilder args = new StringBuilder();

        args.append("key=" + _key);
        args.append("&pin=" + _pin);
        args.append("&method=get");

        String response = Web.postURL(Web.ACCESS_URL, args.toString());

        if (response == null || response.length() == 0)
            throw new Exception("Invalid Server Response");

        return new JSONObject(response);
    }

    private JSONObject apiStoreKey() throws Exception {

        StringBuilder args = new StringBuilder();

        args.append("key=" + _key);
        args.append("&value=" + _value);
        args.append("&pin=" + _pin);
        args.append("&method=put");

        String response = Web.postURL(Web.ACCESS_URL, args.toString());

        if (response == null || response.length() == 0)
            throw new Exception("Invalid Server Response");

        return new JSONObject(response);
    }
}
