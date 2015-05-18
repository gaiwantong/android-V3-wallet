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

/**
 *
 * HD_WalletFactory.java : singleton class for creating/restoring/reading Blockchain Android HD wallet
 *
 * BIP39,44 wraparound for BitcoinJ
 *
 */
public class HD_WalletFactory	{

    private static HD_WalletFactory instance = null;
    private static List<HD_Wallet> wallets = null;
    private static HD_Wallet watch_only_wallet = null;

    private static Logger mLogger = LoggerFactory.getLogger(HD_WalletFactory.class);

    private static Context context = null;

    private HD_WalletFactory()	{ ; }

    /**
     * Return instance for a full wallet including seed and private keys.
     *
     * @param  Context ctx app context
     *
     * @return HD_WalletFactory
     *
     */
    public static HD_WalletFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if(instance == null) {
            wallets = new ArrayList<HD_Wallet>();
            instance = new HD_WalletFactory();
        }

        return instance;
    }

    /**
     * Return instance for a watch only wallet. No seed, no private keys.
     *
     * @param  Context ctx app context
     * @param  String[] xpub restore these accounts only
     *
     * @return HD_WalletFactory
     *
     */
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

    /**
     * Return watch only wallet for this instance.
     *
     * @return HD_Wallet
     *
     */
    public HD_Wallet getWatchOnlyWallet()   {
    	return watch_only_wallet;
    }

    /**
     * Set watch only wallet for this instance.
     *
     * @param  HD_Wallet hdw
     *
     */
    public void setWatchOnlyWallet(HD_Wallet hdw)   {
    	watch_only_wallet = hdw;
    }

    /**
     * Create new HD wallet.
     *
     * @param  int nbWords number of words in menmonic
     * @param  String passphrase optional BIP39 passphrase
     * @param  int nbAccounts create this number of accounts
     *
     * @return HD_Wallet
     *
     */
    public HD_Wallet newWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException   {

        HD_Wallet hdw = null;

        if((nbWords % 3 != 0) || (nbWords < 12 || nbWords > 24)) {
            nbWords = 12;
        }

        if(nbAccounts < 1) {
            nbAccounts = 1;
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

    /**
     * Restore HD wallet.
     *
     * @param  String data Either BIP39 mnemonic or hex seed
     * @param  String passphrase optional BIP39 passphrase
     * @param  int nbAccounts create this number of accounts
     *
     * @return HD_Wallet
     *
     */
    public HD_Wallet restoreWallet(String data, String passphrase, int nbAccounts) throws AddressFormatException, IOException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException  {

        HD_Wallet hdw = null;

        if(nbAccounts < 1) {
            nbAccounts = 1;
        }

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

    /**
     * Get HD wallet for this instance.
     *
     * @return HD_Wallet
     *
     */
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

    /**
     * Set HD wallet for this instance.
     *
     * @param  HD_Wallet wallet
     *
     */
    public void set(HD_Wallet wallet)	{
    	
    	if(wallet != null)	{
            wallets.clear();
        	wallets.add(wallet);
    	}

    }

}
