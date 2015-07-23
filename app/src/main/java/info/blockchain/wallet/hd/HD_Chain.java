package info.blockchain.wallet.hd;

import java.util.ArrayList;
import java.util.List;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 *
 * HD_Chain.java : a chain in a Blockchain Android HD account
 *
 */
public class HD_Chain {

    private DeterministicKey cKey = null;
    private boolean isReceive;

    private ArrayList<HD_Address> mAddresses = null;

    private int addrIdx = 0;

    private String strPath = null;

    static private final int DESIRED_MARGIN = 32;
    static private final int ADDRESS_GAP_MAX = 20;

    private NetworkParameters mParams = null;
    
    private HD_Chain() { ; }

    /**
     * Constructor for an HD chain.
     *
     * @param NetworkParameters params
     * @param DeterministicKey mKey deterministic key for this chain
     * @param boolean isReceive this is the receive chain
     * @param int nbAddrs number of HD addresses to generate
     *
     */
    public HD_Chain(NetworkParameters params, DeterministicKey aKey, boolean isReceive, int nbAddrs) {

        mParams = params;
        this.isReceive = isReceive;
        int chain = isReceive ? 0 : 1;
        cKey = HDKeyDerivation.deriveChildKey(aKey, chain);

        mAddresses = new ArrayList<HD_Address>();
        for(int i = 0; i < nbAddrs; i++) {
            mAddresses.add(new HD_Address(mParams, cKey, i));
        }

        strPath = cKey.getPath();

    }

    /**
     * Test if this is the receive chain.
     *
     * @return boolean
     */
    public boolean isReceive() {
        return isReceive;
    }

    /**
     * Return HD_Address at provided index into chain.
     *
     * @return HD_Address
     */
    public HD_Address getAddressAt(int addrIdx) {
    	return new HD_Address(mParams, cKey, addrIdx);
    }

    /**
     * Get address index for this chain.
     *
     * @return int
     */
    public int getAddrIdx() {
        return addrIdx;
    }

    /**
     * Set address index for this chain.
     *
     * @param int idx index to be set.
     */
    public void setAddrIdx(int idx) {
        addrIdx = idx;
    }

    /**
     * Increment address index for this chain.
     *
     */
    public void incAddrIdx() {
        addrIdx++;
    }

    /**
     * Write chain to JSONObject.
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

            obj.put("type", isReceive ? "R" : "C");

            JSONArray addresses = new JSONArray();
            for(HD_Address addr : mAddresses) {
                addresses.put(addr.toJSON());
            }
            obj.put("addresses", addresses);
            obj.put("size", mAddresses.size());

            obj.put("path", strPath);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
