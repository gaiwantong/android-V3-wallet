package info.blockchain.wallet.payload;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
//import android.util.Log;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.json.JSONException;
import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.crypto.MnemonicException;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_Account;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.WebUtil;

public class PayloadFactory	{
	
	private static Context context = null;

    private static PayloadFactory instance = null;
    private static Payload payload = null;
    private static String o_payload = null;

    private static CharSequenceX strTempPassword =  null;
    private static CharSequenceX strTempDoubleEncryptPassword =  null;
    private static String strCheckSum = null;
    private static boolean isNew = false;
    private static boolean syncPubKeys = true;

    private PayloadFactory()	{ ; }

    public static PayloadFactory getInstance() {

        if (instance == null) {
            instance = new PayloadFactory();
            payload = new Payload();
//            o_payload = new Payload();
            o_payload = "";
        }

        return instance;
    }

    public static PayloadFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if (instance == null) {
            instance = new PayloadFactory();
            payload = new Payload();
//            o_payload = new Payload();
            o_payload = "";
        }

        return instance;
    }

    public static PayloadFactory getInstance(String json) {

        if (instance == null) {
            instance = new PayloadFactory();
            payload = new Payload(json);
//            o_payload = new Payload(json);
            try {
                o_payload = payload.dumpJSON().toString();
            }
            catch(JSONException je) {
                o_payload = "";
            }
        }

        return instance;
    }

    public void wipe() {
        instance = null;
    }

    public CharSequenceX getTempPassword() {
        return strTempPassword;
    }

    public void setTempPassword(CharSequenceX temp_password) {
        this.strTempPassword = temp_password;
    }

    public CharSequenceX getTempDoubleEncryptPassword() {
        return strTempDoubleEncryptPassword;
    }

    public void setTempDoubleEncryptPassword(CharSequenceX temp_password2) {
        this.strTempDoubleEncryptPassword = temp_password2;
    }

    public String getCheckSum() {
        return strCheckSum;
    }

    public void setCheckSum(String checksum) {
        this.strCheckSum = checksum;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isSyncPubKeys() {
        return syncPubKeys;
    }

    public void setSyncPubKeys(boolean sync) {
        this.syncPubKeys = sync;
    }

    public Payload get(String guid, String sharedKey, CharSequenceX password) {

        try {
            String response = WebUtil.getInstance().postURL(WebUtil.PAYLOAD_DOMAIN + "wallet","method=wallet.aes.json&guid=" + guid + "&sharedKey=" + sharedKey + "&format=json");
            JSONObject jsonObject = new JSONObject(response);
            int iterations = AESUtil.PasswordPBKDF2Iterations;
            double version = 2.0;
            if(jsonObject.has("payload")) {
                String encrypted_payload = null;
                JSONObject _jsonObject = null;
                try {
                    _jsonObject = new JSONObject((String)jsonObject.get("payload"));
                }
                catch(Exception e) {
                    _jsonObject = null;
                }
                if(_jsonObject != null && _jsonObject.has("payload")) {
                    if(_jsonObject.has("pbkdf2_iterations")) {
                        iterations = Integer.valueOf(_jsonObject.get("pbkdf2_iterations").toString());
                    }
                    if(_jsonObject.has("version")) {
                        version = Double.valueOf(_jsonObject.get("version").toString());
                    }
                    encrypted_payload = (String)_jsonObject.get("payload");
                }
                else {
                    if(jsonObject.has("pbkdf2_iterations")) {
                        iterations = Integer.valueOf(jsonObject.get("pbkdf2_iterations").toString());
                    }
                    if(jsonObject.has("version")) {
                        version = Double.valueOf(jsonObject.get("version").toString());
                    }
                    encrypted_payload = (String)jsonObject.get("payload");
                }

                String decrypted = null;
                try {
                    decrypted = AESUtil.decrypt(encrypted_payload, password, iterations);
                }
                catch(Exception e) {
                	payload = null;
                	e.printStackTrace();
                	return null;
                }
                if(decrypted == null) {
                	payload = null;
                	return null;
                }
                payload = new Payload(decrypted);
                if(payload.getJSON() == null) {
                	payload = null;
                	return null;
                }

                try {
                    payload.parseJSON();
                }
                catch(JSONException je) {
                	payload = null;
                	je.printStackTrace();
                    return null;
                }

                payload.setIterations(iterations);
                payload.setVersion(version);
            }
            else {
                return null;
            }
        }
        catch(JSONException je) {
        	payload = null;
        	je.printStackTrace();
        	return null;
        }
        catch(Exception e) {
        	payload = null;
            e.printStackTrace();
        	return null;
        }

        return payload;
    }

    public Payload get() {
        return payload;
    }

    public void set(Payload p) {
        payload = p;
    }

    public boolean put(CharSequenceX password) {

		String strOldCheckSum = strCheckSum;
		String payloadCleartext = null;

		StringBuilder args = new StringBuilder();
		try	{

	    	if(o_payload != null && o_payload.equals(payload.dumpJSON().toString())) {
	    		return true;
	    	}

	    	payloadCleartext = payload.dumpJSON().toString();
	    	String payloadEncrypted = AESUtil.encrypt(payloadCleartext, new CharSequenceX(strTempPassword), AESUtil.PasswordPBKDF2Iterations);
	    	JSONObject rootObj = new JSONObject();
			rootObj.put("version", 2.0);
			rootObj.put("pbkdf2_iterations", AESUtil.PasswordPBKDF2Iterations);
			rootObj.put("payload", payloadEncrypted);
//			rootObj.put("guid", payload.getGuid());
//			rootObj.put("sharedKey", payload.getSharedKey());
//			rootObj.put("test", "OK");

			strCheckSum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(rootObj.toString().getBytes("UTF-8"))));

			String method = isNew ? "insert" : "update";

			String urlEncodedPayload = URLEncoder.encode(rootObj.toString());

			args.append("guid=");
			args.append(URLEncoder.encode(payload.getGuid(), "utf-8"));
			args.append("&sharedKey=");
			args.append(URLEncoder.encode(payload.getSharedKey(), "utf-8"));
			args.append("&payload=");
			args.append(urlEncodedPayload);
			args.append("&method=");
			args.append(method);
			args.append("&length=");
			args.append(rootObj.toString().length());
			args.append("&checksum=");
			args.append(URLEncoder.encode(strCheckSum, "utf-8"));

		}
		catch(NoSuchAlgorithmException nsae)	{
			nsae.printStackTrace();
		}
		catch(UnsupportedEncodingException uee)	{
			uee.printStackTrace();
		}
		catch(JSONException je)	{
			je.printStackTrace();
		}

		if (syncPubKeys) {
			args.append("&active=");
			
			String[] legacyAddrs = null;
			List<LegacyAddress> legacyAddresses = payload.getLegacyAddresses();
			List<String> addrs = new ArrayList<String>();
			for(LegacyAddress addr : legacyAddresses) {
				if(addr.getTag() == 0L) {
					addrs.add(addr.getAddress());
				}
			}

			args.append(StringUtils.join(addrs.toArray(new String[addrs.size()]), "|"));
		}

		/*
		if (email != null && email.length() > 0) {
			args.append("&email=");
			args.append(URLEncoder.encode(email, "utf-8"));
		}
		*/

		args.append("&device=");
		args.append("android");

		if(strOldCheckSum != null && strOldCheckSum.length() > 0)	{
			args.append("&old_checksum=");
			args.append(strOldCheckSum);
		}
		
		try	{
			WebUtil.getInstance().postURL(WebUtil.PAYLOAD_DOMAIN + "wallet", args.toString());
			isNew = false;
			store();
		}
		catch(Exception e)	{
            e.printStackTrace();
		}

		return true;
    }

    public String getAsString(String guid, String sharedKey, CharSequenceX password) throws JSONException {
        Payload payload = get(guid, sharedKey, password);
        return (payload != null) ? payload.dumpJSON().toString() : null;
    }

    public void store() {
        try {
        	o_payload = payload.dumpJSON().toString();
        }
        catch(JSONException je) {
        	je.printStackTrace();
        }
    }

    public boolean createBlockchainWallet(HD_Wallet hdw)	{
    	
    	String guid = UUID.randomUUID().toString();
    	String sharedKey = UUID.randomUUID().toString();
    	
    	payload = new Payload();
    	payload.setGuid(guid);
    	payload.setSharedKey(sharedKey);
    	
    	PrefsUtil.getInstance(context).setValue("guid", guid);
    	PrefsUtil.getInstance(context).setValue("sharedKey", sharedKey);
    	
    	HDWallet payloadHDWallet = new HDWallet();
    	payloadHDWallet.setSeedHex(hdw.getSeedHex());

    	List<HD_Account> hdAccounts = hdw.getAccounts();
    	List<Account> payloadAccounts = new ArrayList<Account>();
    	for(int i = 0; i < hdAccounts.size(); i++)	{
    		Account account = new Account();
        	try  {
            	String xpub = HD_WalletFactory.getInstance(context).get().getAccounts().get(i).xpubstr();
            	account.setXpub(xpub);
            	String xpriv = HD_WalletFactory.getInstance(context).get().getAccounts().get(i).xprvstr();
            	account.setXpriv(xpriv);
        	}
        	catch(IOException ioe)  {
        		ioe.printStackTrace();
        	}
        	catch(MnemonicException.MnemonicLengthException mle)  {
        		mle.printStackTrace();
        	}

    		payloadAccounts.add(account);
    	}
    	payloadHDWallet.setAccounts(payloadAccounts);
    	
    	payload.setHdWallets(payloadHDWallet);
    	
    	isNew = true;

    	return true;
    }

    public void remoteSaveThread() {

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				
				if(PayloadFactory.getInstance(context).get() != null)	{
					if(PayloadFactory.getInstance(context).put(strTempPassword))	{
			        	Toast.makeText(context, "Remote save OK", Toast.LENGTH_SHORT).show();
					}
					else	{
			        	Toast.makeText(context, "Remote save KO", Toast.LENGTH_SHORT).show();
					}
					
				}
				else	{
		        	Toast.makeText(context, "Remote payload corrupted, not saved to server.", Toast.LENGTH_SHORT).show();
				}

				handler.post(new Runnable() {
					@Override
					public void run() {
						;
					}
				});
				
				Looper.loop();

			}
		}).start();
	}

}