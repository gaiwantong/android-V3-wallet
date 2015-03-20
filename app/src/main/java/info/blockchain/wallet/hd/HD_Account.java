package info.blockchain.wallet.hd;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

import org.json.JSONException;
import org.json.JSONObject;

public class HD_Account {

    private DeterministicKey aKey = null;
    private String strLabel = null;
    private int	mAID;
    private boolean isArchived = false;

    private HD_Chain mReceive = null;
    private HD_Chain mChange = null;

    private NetworkParameters mParams = null;

    private HD_Account() { ; }

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

    public boolean hasPubKey(byte[] pubkey, byte[] pubkeyhash) {
        if(mReceive.hasPubKey(pubkey, pubkeyhash)) {
            return true;
        }

        return mChange.hasPubKey(pubkey, pubkeyhash);
    }

    public String xpubstr() {
        return aKey.serializePubB58();
    }

    public String xprvstr() {

        if(aKey.hasPrivate()) {
            return aKey.serializePrivB58();
        }
        else {
            return null;
        }

    }

    public String getLabel() {
        return strLabel;
    }

    public void setLabel(String label) {
        strLabel = label;
    }

    public int getId() {
        return mAID;
    }

    public HD_Chain getReceive() {
        return mReceive;
    }

    public HD_Chain getChange() {
        return mChange;
    }

    public HD_Chain getChain(int idx) {
        return (idx == 0) ? mReceive : mChange;
    }

    public int size() {
        return mReceive.length() + mChange.length();
    }

    public int receiveSize() {
        return mReceive.length();
    }

    public int changeSize() {
        return mChange.length();
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        this.isArchived = archived;
    }

    public HD_Address seenAddress(Address addr) {
        HD_Address retval;

        retval = mReceive.seenAddress(addr);
        if(retval == null) {
            retval = mChange.seenAddress(addr);
        }

        if(retval != null) {
            retval.setAccount(this);
        }

        return retval;
    }

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
