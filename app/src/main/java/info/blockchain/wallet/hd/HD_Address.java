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

/**
 *
 * HD_Address.java : an address in a Blockchain Android HD chain
 *
 */
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

    /**
     * Constructor an HD address.
     *
     * @param NetworkParameters params
     * @param DeterministicKey mKey deterministic key for this address
     * @param int child index of this address in its chain
     *
     */
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

    /**
     * Return BIP44 path for this address (m / purpose' / coin_type' / account' / change / address_index).
     *
     * @return String
     *
     */
    public String getPath() {
        return strPath;
    }

    /**
     * Return public address for this instance.
     *
     * @return String
     *
     */
    public String getAddressString() {
        return ecKey.toAddress(mParams).toString();
    }

    /**
     * Return private key for this address (compressed WIF format).
     *
     * @return String
     *
     */
    public String getPrivateKeyString() {

        if(ecKey.hasPrivKey()) {
            return ecKey.getPrivateKeyEncoded(mParams).toString();
        }
        else    {
            return null;
        }

    }

    /**
     * Return index of this address within its chain.
     *
     * @return int
     *
     */
    public int getIndex() {
        return mChildNum;
    }

    /**
     * Write address to JSONObject.
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
