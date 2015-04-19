package info.blockchain.wallet.hd;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * HD_Account.java : an account in a Blockchain Android HD wallet
 *
 */
public class HD_Account {

    private DeterministicKey aKey = null;
    private String strLabel = null;
    private int	mAID;
    private boolean isArchived = false;

    private HD_Chain mReceive = null;
    private HD_Chain mChange = null;

    private NetworkParameters mParams = null;

    private HD_Account() { ; }

    /**
     * Constructor for HD account.
     *
     * @param NetworkParameters params
     * @param DeterministicKey mKey deterministic key for this account
     * @param String label label for this account optional BIP39 passphrase
     * @param int child id within the wallet for this account
     *
     */
    public HD_Account(NetworkParameters params, DeterministicKey mKey, String label, int child) {

        mParams = params;
        strLabel = label;
        mAID = child;

        if(mKey.hasPrivate()) {
            // L0PRV & STDVx: private derivation.
            int childnum = child;
            childnum |= ChildNumber.PRIV_BIT;
            aKey = HDKeyDerivation.deriveChildKey(mKey, childnum);
        }
        else {
        	// assign master key to account key
            aKey = mKey;
        }

        mReceive = new HD_Chain(mParams, aKey, true, 2);
        mChange = new HD_Chain(mParams, aKey, false, 2);

    }

    /**
     * Return XPUB string for this account.
     *
     * @return String
     *
     */
    public String xpubstr() {
        return aKey.serializePubB58();
    }

    /**
     * Return xprv string for this account.
     *
     * @return String
     *
     */
    public String xprvstr() {

        if(aKey.hasPrivate()) {
            return aKey.serializePrivB58();
        }
        else {
            return null;
        }

    }

    /**
     * Return label for this account.
     *
     * @return String
     *
     */
    public String getLabel() {
        return strLabel;
    }

    /**
     * Set label for this account.
     *
     * @param String label label to be set to this account.
     *
     */
    public void setLabel(String label) {
        strLabel = label;
    }

    /**
     * Return id for this account.
     *
     * @return int
     *
     */
    public int getId() {
        return mAID;
    }

    /**
     * Return receive chain this account.
     *
     * @return HD_Chain
     *
     */
    public HD_Chain getReceive() {
        return mReceive;
    }

    /**
     * Return change chain this account.
     *
     * @return HD_Chain
     *
     */
    public HD_Chain getChange() {
        return mChange;
    }

    /**
     * Return chain for this account as indicated by index: 0 = receive, 1 = change.
     *
     * @return HD_Chain
     *
     */
    public HD_Chain getChain(int idx) {
        return (idx == 0) ? mReceive : mChange;
    }

    /**
     * Write account to JSONObject.
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

            obj.put("label", strLabel);
            obj.put("id", mAID);
            if(aKey.hasPrivate()) {
                obj.put("xpub", xpubstr());
                obj.put("xprv", xprvstr());
            }
            obj.put("receive", mReceive.toJSON());
            obj.put("change", mChange.toJSON());
            obj.put("archived", isArchived);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
