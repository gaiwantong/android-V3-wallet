package info.blockchain.wallet.pairing;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import android.content.Context;
//import android.util.Log;

import org.spongycastle.util.encoders.Hex;

import org.json.JSONObject;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.Web;

public class PairingFactory	{
	
	private static Context context = null;

    private static PairingFactory instance = null;

    private PairingFactory()	{ ; }

    public static PairingFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if (instance == null) {
            instance = new PairingFactory();
        }

        return instance;
    }

    public boolean handleQRCode(String raw) {

        if(raw == null || raw.length() == 0 || raw.charAt(0) != '1') {
            return false;
        }

        String[] components = raw.split("\\|", Pattern.LITERAL);

        if(components.length != 3) {
//            Log.i("Pairing", "does not have 3 components");
            return false;
        }

        String guid = components[1];
        if(guid.length() != 36) {
//            Log.i("Pairing", "guid != 36 length");
            return false;
        }
//        Log.i("Pairing", "guid:" + guid);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.GUID, guid);

        String encrypted = components[2];

        String temp_password = null;
        try {
            temp_password = getPairingEncryptionPassword(guid);
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        String decrypted = AESUtil.decrypt(encrypted, new CharSequenceX(temp_password), AESUtil.QRCodePBKDF2Iterations);
        String[] sharedKeyAndPassword = decrypted.split("\\|", Pattern.LITERAL);

        if(sharedKeyAndPassword.length < 2) {
//            Log.i("Pairing", "sharedKeyAndPassword >= 2");
            return false;
        }

        String sharedKey = sharedKeyAndPassword[0];
        if(sharedKey.length() != 36) {
//            Log.i("Pairing", "sharedKey != 36 length");
            return false;
        }
//        Log.i("Pairing", "SharedKey:" + sharedKey);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.SHARED_KEY, sharedKey);

        CharSequenceX password = null;
        try {
			password = new CharSequenceX(new String(Hex.decode(sharedKeyAndPassword[1]), "UTF-8"));
			PayloadFactory.getInstance().setTempPassword(password);
        }
        catch(UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return false;
        }
        
        return true;
    }

    public String getPairingEncryptionPassword(final String guid) throws Exception {
        StringBuilder args = new StringBuilder();

        args.append("guid=" + guid);
        args.append("&method=pairing-encryption-password");

        return Web.postURL(Web.PAIRING_DOMAIN + "wallet", args.toString());
    }

    public String getWalletManualPairing(final String guid) throws Exception {
        StringBuilder args = new StringBuilder();

        args.append("guid=" + guid);
        args.append("&method=pairing-encryption-password");

        String response = Web.getURL(Web.PAIRING_DOMAIN + "wallet/" + guid + "?format=json&resend_code=false");

        JSONObject jsonObject = new JSONObject(response);
//        Log.i("Pairing", "Returned object:" + jsonObject.toString());

        String payload = (String)jsonObject.get("payload");
        if (payload == null || payload.length() == 0) {
            throw new Exception("Error Fetching Wallet Payload");
        }

        return payload;
    }

}
