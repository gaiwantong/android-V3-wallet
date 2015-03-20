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

public class HD_Chain {

    private DeterministicKey cKey = null;
    private boolean isReceive;

    private ArrayList<HD_Address> mAddresses = null;

    private int addrIdx = 0;

    static private final int DESIRED_MARGIN = 32;
    static private final int ADDRESS_GAP_MAX = 20;

    private NetworkParameters mParams = null;
    
    private HD_Chain() { ; }

    public HD_Chain(NetworkParameters params, DeterministicKey aKey, boolean isReceive, int nbAddrs) {

        mParams = params;
        this.isReceive = isReceive;
        int chain = isReceive ? 0 : 1;
        cKey = HDKeyDerivation.deriveChildKey(aKey, chain);

        mAddresses = new ArrayList<HD_Address>();
        for(int i = 0; i < nbAddrs; i++) {
            mAddresses.add(new HD_Address(mParams, cKey, i));
        }

    }

    public static int maxSafeExtend() {
        return DESIRED_MARGIN - ADDRESS_GAP_MAX;
    }

    public boolean isReceive() {
        return isReceive;
    }

    public void addAddress() {
        mAddresses.add(new HD_Address(mParams, cKey, mAddresses.size()));
    }

    public void addAddresses(int nbAddresses) {
        for(int i = 0; i < nbAddresses; i++) {
            addAddress();
        }
    }

    public void addAddressAt(int addrIdx) {
        if(addrIdx > mAddresses.size()) {
            mAddresses.ensureCapacity(addrIdx + 1);
        }
        mAddresses.add(addrIdx, new HD_Address(mParams, cKey, addrIdx));
    }

    public void addAddressesAt(int nbAddresses, int addrIdx) {
        for(int i = addrIdx; i < nbAddresses; i++) {
            addAddressAt(i);
        }
    }

    public HD_Address getAddressAt(int addrIdx) {
    	return new HD_Address(mParams, cKey, addrIdx);
    }

    public List<HD_Address> getAddresses() {
        return mAddresses;
    }

    public int length() {
        return mAddresses.size();
    }

    public boolean hasPubKey(byte[] pubkey, byte[] pubkeyhash) {
        for(HD_Address hda : mAddresses) {
            if(hda.isSameAs(pubkey, pubkeyhash)) {
                return true;
            }
        }

        return false;
    }

    public HD_Address seenAddress(Address addr) {
        for(HD_Address hda : mAddresses) {
            if(hda.isSameAs(addr)) {
                hda.setChain(this);
                return hda;
            }
        }

        return null;
    }

    public int getAddrIdx() {
        return addrIdx;
    }

    public void setAddrIdx(int idx) {
        addrIdx = idx;
    }

    public void incAddrIdx() {
        addrIdx++;
    }

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

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
