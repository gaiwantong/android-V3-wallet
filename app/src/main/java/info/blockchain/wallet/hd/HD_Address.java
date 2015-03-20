package info.blockchain.wallet.hd;

import java.util.Arrays;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.ChildNumber;

import org.json.JSONException;
import org.json.JSONObject;

public class HD_Address {

    private int mChildNum;
    private String strPath = null;
    private ECKey ecKey = null;
    private byte[] mPubKey = null;
    private byte[] mPubKeyHash = null;

    private HD_Account hdAccount = null;
    private HD_Chain hdChain = null;

    private NetworkParameters mParams = null;

    private HD_Address() { ; }

    public HD_Address(NetworkParameters params, DeterministicKey cKey, int child) {

        mParams = params;
        mChildNum = child;

        DeterministicKey dk = HDKeyDerivation.deriveChildKey(cKey, new ChildNumber(mChildNum, false));
        // compressed WIF private key format
        ecKey = new ECKey(dk.getPrivKeyBytes(), dk.getPubKeyBytes());

        long now = Utils.now().getTime() / 1000;
        ecKey.setCreationTimeSeconds(now);

        mPubKey = ecKey.getPubKey();
        mPubKeyHash = ecKey.getPubKeyHash();

        strPath = dk.getPath();
    }

    public boolean isSameAs(byte[] pubkey, byte[] pubkeyhash) {
        if(pubkey != null) {
            return Arrays.equals(pubkey, mPubKey);
        }
        else if(pubkeyhash != null) {
            return Arrays.equals(pubkeyhash, mPubKeyHash);
        }
        else {
            return false;
        }
    }

    public String getPath() {
        return strPath;
    }

    public String getAddressString() {
        return ecKey.toAddress(mParams).toString();
    }

    public String getPrivateKeyString() {

        if(ecKey.hasPrivKey()) {
            return ecKey.getPrivateKeyEncoded(mParams).toString();
        }
        else    {
            return null;
        }

    }

    public Address getAddress() {
        return ecKey.toAddress(mParams);
    }

    public int getIndex() {
        return mChildNum;
    }

    public boolean isSameAs(Address addr) {
        return ecKey.toAddress(mParams).toString().equals(addr.toString());
    }

    // used by HD_Wallet.seenAddress(Address addr)
    public void setAccount(HD_Account account) {
        hdAccount = account;
    }

    public HD_Account getAccount() {
        return hdAccount;
    }

    // used by HD_Wallet.seenAddress(Address addr)
    public void setChain(HD_Chain chain) {
        hdChain = chain;
    }

    private HD_Chain getChain() {
        return hdChain;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("path", strPath);
            obj.put("address", getAddressString());
            obj.put("id", mChildNum);
            if(ecKey.hasPrivKey()) {
                obj.put("key", getPrivateKeyString());
            }

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
