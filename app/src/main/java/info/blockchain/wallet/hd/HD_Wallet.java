package info.blockchain.wallet.hd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.google.bitcoin.core.Address;
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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class HD_Wallet {

    private byte[] mSeed = null;
    private String strPassphrase = null;
    private List<String> mWordList = null;

    private DeterministicKey mKey = null;
    private DeterministicKey mRoot = null;

    private ArrayList<HD_Account> mAccounts = null;

    private NetworkParameters mParams = null;

    private HD_Wallet() { ; }

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

    public HD_Wallet(Context ctx, JSONObject jsonobj, NetworkParameters params) throws DecoderException, JSONException, IOException, MnemonicException.MnemonicLengthException {

        mParams = params;
        int nbAccounts = 2;
        mSeed = Hex.decodeHex(((String)jsonobj.get("seed")).toCharArray());
        strPassphrase = (String)jsonobj.getString("passphrase");
        nbAccounts = (Integer)jsonobj.getInt("size");

        InputStream wis = ctx.getResources().getAssets().open("wordlist/english.txt");
        MnemonicCode mc = null;
        if(wis != null) {
            mc = new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);
            wis.close();
        }

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

    /*
    create from account xpub key(s)
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

    public byte[] getSeed() {
        return mSeed;
    }

    public String getSeedHex() {
        return Utils.bytesToHexString(mSeed);
    }

    public String getMnemonic() {
        return Joiner.on(" ").join(mWordList);
    }

    public String getPassphrase() {
        return strPassphrase;
    }

    public List<HD_Account> getAccounts() {
        return mAccounts;
    }

    public HD_Account getAccount(int accountId) {
        return mAccounts.get(accountId);
    }

    public void addAccount() {
        String strName = String.format("Account %d", mAccounts.size());
        mAccounts.add(new HD_Account(mParams, mRoot, strName, mAccounts.size()));
    }
    
    public void addAccount(String label) {

    	if(label == null) {
    		addAccount();
    	}
    	else {
        	mAccounts.add(new HD_Account(mParams, mRoot, label, mAccounts.size()));
    	}

    }

    public int size() {

        int sz = 0;
        for(HD_Account acct : mAccounts) {
            sz += acct.size();
        }

        return sz;
    }

    /*
    returns HD_Address if address has been seen for this wallet, null otherwise. account and chain info are included during the search
    */
    public HD_Address seenAddress(Address addr) {

        HD_Address ret = null;
        for(HD_Account acct : mAccounts) {
            ret = acct.seenAddress(addr);
            if(ret != null) {
                return ret;
            }
        }

        return null;
    }

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
