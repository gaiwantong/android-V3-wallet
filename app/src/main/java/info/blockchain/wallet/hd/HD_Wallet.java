package info.blockchain.wallet.hd;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.crypto.MnemonicException;

import com.google.common.base.Joiner;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 *
 * HD_Wallet.java : Blockchain Android HD wallet
 *
 */
public class HD_Wallet {

    private byte[] mSeed = null;
    private String strPassphrase = null;
    private List<String> mWordList = null;

    private DeterministicKey mKey = null;
    private DeterministicKey mRoot = null;

    private ArrayList<HD_Account> mAccounts = null;

    private NetworkParameters mParams = null;

    private HD_Wallet() { ; }

    /**
     * Constructor for HD wallet.
     *
     * @param MnemonicCode mc mnemonic code object
     * @param NetworkParameters params
     * @param byte[] seed seed for this wallet
     * @param String passphrase optional BIP39 passphrase
     * @param int nbAccounts nubber of accounts to create
     *
     */
    public HD_Wallet(MnemonicCode mc, NetworkParameters params, byte[] seed, String passphrase, int nbAccounts) throws MnemonicException.MnemonicLengthException {

        mParams = params;
        mSeed = seed;
        strPassphrase = passphrase;

        mWordList = mc.toMnemonic(mSeed);
        byte[] hd_seed = MnemonicCode.toSeed(mWordList, strPassphrase, MnemonicCode.Version.V0_6);
        mKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        DeterministicKey t1 = HDKeyDerivation.deriveChildKey(mKey, 44|ChildNumber.PRIV_BIT);
        mRoot = HDKeyDerivation.deriveChildKey(t1, ChildNumber.PRIV_BIT);

        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < nbAccounts; i++) {
            String acctName = String.format("account %02d", i);
            mAccounts.add(new HD_Account(mParams, mRoot, acctName, i));
        }

    }

    /**
     * Constructor for watch-only HD wallet initialized from submitted XPUBs.
     *
     * @param NetworkParameters params
     * @param String[] xpub array of XPUB strings
     *
     */
    public HD_Wallet(NetworkParameters params, String[] xpub) throws AddressFormatException {

        mParams = params;
        DeterministicKey aKey = null;
        mAccounts = new ArrayList<HD_Account>();
        for(int i = 0; i < xpub.length; i++) {
            aKey = createMasterPubKeyFromXPub(xpub[i]);
            String acctName = String.format("account %02d", 0);
            mAccounts.add(new HD_Account(mParams, aKey, acctName, i));
        }

    }

    /**
     * Return wallet seed as hex string.
     *
     * @return String
     *
     */
    public String getSeedHex() {
        return Utils.bytesToHexString(mSeed);
    }

    /**
     * Return wallet mnemonic as string containing space separated words.
     *
     * @return String
     *
     */
    public String getMnemonic() {
        return Joiner.on(" ").join(mWordList);
    }

    /**
     * Return wallet BIP39 passphrase.
     *
     * @return String
     *
     */
    public String getPassphrase() {
        return strPassphrase;
    }

    /**
     * Return accounts for this wallet.
     *
     * @return List<HD_Account>
     *
     */
    public List<HD_Account> getAccounts() {
        return mAccounts;
    }

    /**
     * Return accounts for submitted account id.
     *
     * @param int accountId
     *
     * @return HD_Account
     *
     */
    public HD_Account getAccount(int accountId) {
        return mAccounts.get(accountId);
    }

    /**
     * Add new account.
     *
     */
    public void addAccount() {
        String strName = String.format("Account %d", mAccounts.size());
        mAccounts.add(new HD_Account(mParams, mRoot, strName, mAccounts.size()));
    }

    /**
     * Add new account and name with label.
     *
     * @param String label
     *
     */
    public void addAccount(String label) {

    	if(label == null) {
    		addAccount();
    	}
    	else {
        	mAccounts.add(new HD_Account(mParams, mRoot, label, mAccounts.size()));
    	}

    }

    /**
     * Return wallet seed as byte array.
     *
     * @return byte[]
     *
     */
    private byte[] getSeed() {
        return mSeed;
    }

    /**
     * Restore watch-only account deterministic public key from XPUB.
     *
     * @return DeterministicKey
     *
     */
    private DeterministicKey createMasterPubKeyFromXPub(String xpubstr) throws AddressFormatException {

        byte[] xpubBytes = Base58.decodeChecked(xpubstr);

        ByteBuffer bb = ByteBuffer.wrap(xpubBytes);
        if(bb.getInt() != 0x0488B21E)   {
            throw new AddressFormatException("invalid xpub version");
        }

        byte[] chain = new byte[32];
        byte[] pub = new byte[33];
        // depth:
        bb.get();
        // parent fingerprint:
        bb.getInt();
        // child no.
        bb.getInt();
        bb.get(chain);
        bb.get(pub);

        return HDKeyDerivation.createMasterPubKeyFromBytes(pub, chain);
    }

    /**
     * Write entire wallet to JSONObject.
     * Not used in Blockchain HD wallet.
     * Use payload classes instead.
     * For debugging only.
     *
     * @return JSONObject
     *
     */
    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            if(mSeed != null) {
                obj.put("seed", Utils.bytesToHexString(mSeed));
                obj.put("seed base58", Base58.encode(mSeed));
                obj.put("passphrase", strPassphrase);

                JSONArray words = new JSONArray();
                for(String w : mWordList) {
                    words.put(w);
                }
                obj.put("mnemonic", words);
            }

            JSONArray accts = new JSONArray();
            for(HD_Account acct : mAccounts) {
                accts.put(acct.toJSON());
            }
            obj.put("accounts", accts);
            obj.put("size", mAccounts.size());

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
