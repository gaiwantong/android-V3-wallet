package info.blockchain.wallet.hd;

import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.Charset;

import android.content.Context;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.params.MainNetParams;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;
//import org.apache.commons.lang.ArrayUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONException;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.LinuxSecureRandom;

public class HD_WalletFactory	{

    private static HD_WalletFactory instance = null;
    private static List<HD_Wallet> wallets = null;
    private static HD_Wallet watch_only_wallet = null;

    private static Logger mLogger = LoggerFactory.getLogger(HD_WalletFactory.class);

    public static String strJSONFilePath = null;
    
    private static Context context = null;

    private HD_WalletFactory()	{ ; }

    public static HD_WalletFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if(instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    public static HD_WalletFactory getInstance(Context ctx, String path) {

    	context = ctx;
        strJSONFilePath = path;

        if (instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    public static HD_WalletFactory getInstance(Context ctx, String[] xpub) throws AddressFormatException {
    	
    	context = ctx;

        if(instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        if(watch_only_wallet == null) {
        	watch_only_wallet = new HD_Wallet(MainNetParams.get(), xpub);
        }

        return instance;
    }

    public HD_Wallet getWatchOnlyWallet()   {
    	return watch_only_wallet;
    }

    public void setWatchOnlyWallet(HD_Wallet hdw)   {
    	watch_only_wallet = hdw;
    }

    public HD_Wallet newWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException   {

        HD_Wallet hdw = null;

        if((nbWords % 3 != 0) || (nbWords < 12 || nbWords > 24)) {
            nbWords = 12;
        }

        // len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
        int len = (nbWords / 3) * 4;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MainNetParams.get();

        LinuxSecureRandom random = new LinuxSecureRandom();
        byte seed[] = new byte[len];
        random.engineNextBytes(seed);

        InputStream wis = context.getResources().getAssets().open("wordlist/english.txt");
        if(wis != null) {
            MnemonicCode mc = new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);
            hdw = new HD_Wallet(mc, params, seed, passphrase, nbAccounts);
            wis.close();
        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet restoreWallet(String data, String passphrase, int nbAccounts) throws AddressFormatException, IOException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException  {

        HD_Wallet hdw = null;

        if(passphrase == null) {
            passphrase = "";
        }

        NetworkParameters params = MainNetParams.get();

        InputStream wis = context.getResources().getAssets().open("wordlist/english.txt");
        if(wis != null) {
            List<String> words = null;

            if(data.contains(" ")) {
                words = Arrays.asList(data.trim().split("\\s+"));
            }

            MnemonicCode mc = null;
            mc = new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);

            byte[] seed = null;
            if(data.startsWith("xpub")) {
                String[] xpub = data.split(":");
                hdw = new HD_Wallet(params, xpub);
            }
            else if(data.length() % 4 == 0 && !data.contains(" ")) {
                seed = Hex.decodeHex(data.toCharArray());
                hdw = new HD_Wallet(mc, params, seed, passphrase, nbAccounts);
            }
            else {
                seed = mc.toEntropy(words);
                hdw = new HD_Wallet(mc, params, seed, passphrase, nbAccounts);
            }

            wis.close();

        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    public HD_Wallet get() throws IOException, MnemonicException.MnemonicLengthException {

        if(wallets.size() < 1) {
            /*
            // if wallets list is empty, create 12-word wallet without passphrase and 2 accounts
//            wallets.add(0, newWallet(12, "", 2));
            wallets.clear();
            wallets.add(newWallet(12, "", 2));
            */
            return null;
        }

        return wallets.get(0);
    }

    public void set(HD_Wallet wallet)	{
    	
    	if(wallet != null)	{
            wallets.clear();
        	wallets.add(wallet);
    	}

    }

    public void saveWalletToJSON(String password) throws MnemonicException.MnemonicLengthException, IOException, JSONException {
        serialize(get().toJSON(), password);
    }

    public HD_Wallet restoreWalletfromJSON(String password) throws DecoderException, MnemonicException.MnemonicLengthException {

        HD_Wallet hdw = null;

        NetworkParameters params = MainNetParams.get();

        JSONObject obj = null;
        try {
            obj = deserialize(password);
            if(obj != null) {
                hdw = new HD_Wallet(context, obj, params);
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        catch(JSONException je) {
            je.printStackTrace();
        }

        wallets.clear();
        wallets.add(hdw);

        return hdw;
    }

    private void serialize(JSONObject jsonobj, String password) throws IOException, JSONException {

        File newfile = new File(strJSONFilePath + "bc_wallet.dat");
        File tmpfile = new File(strJSONFilePath + "bc_wallet.tmp");

        // serialize to byte array.
        String jsonstr = jsonobj.toString(4);
        byte[] cleartextBytes = jsonstr.getBytes(Charset.forName("UTF-8"));

        // prepare tmp file.
        if(tmpfile.exists()) {
            tmpfile.delete();
        }

        String data = null;
        if(password != null) {
            data = AESUtil.encrypt(jsonstr, new CharSequenceX(password), AESUtil.DefaultPBKDF2Iterations);
        }
        else {
            data = jsonstr;
        }

        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile), "UTF-8"));
        try {
            out.write(data);
        } finally {
            out.close();
        }

        // rename tmp file
        if(tmpfile.renameTo(newfile)) {
            mLogger.info("file saved to  " + newfile.getPath());
        }
        else {
            mLogger.warn("rename to " + newfile.getPath() + " failed");
        }
    }

    private JSONObject deserialize(String password) throws IOException, JSONException {

        File file = new File(strJSONFilePath + "bc_wallet.dat");
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;

        while((str = in.readLine()) != null) {
            sb.append(str);
        }

        JSONObject node = null;
        if(password == null) {
            node = new JSONObject(sb.toString());
        }
        else {
            node = new JSONObject(AESUtil.decrypt(sb.toString(), new CharSequenceX(password), AESUtil.DefaultPBKDF2Iterations));
        }

        return node;
    }

}
