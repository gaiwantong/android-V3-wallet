package info.blockchain.wallet.payload;

import org.json.JSONObject;
import org.json.JSONException;

public class Cache {

    protected String externalAccountPubKey = null;
    protected String externalAccountChainCode = null;
    protected String internalAccountPubKey = null;
    protected String internalAccountChainCode = null;

    public Cache() {
        ;
    }

    public String getExternalAccountPubKey() {
        return externalAccountPubKey;
    }

    public void setExternalAccountPubKey(String externalAccountPubKey) {
        this.externalAccountPubKey = externalAccountPubKey;
    }

    public String getExternalAccountChainCode() {
        return externalAccountChainCode;
    }

    public void setExternalAccountChainCode(String externalAccountChainCode) {
        this.externalAccountChainCode = externalAccountChainCode;
    }

    public String getInternalAccountPubKey() {
        return internalAccountPubKey;
    }

    public void setInternalAccountPubKey(String internalAccountPubKey) {
        this.internalAccountPubKey = internalAccountPubKey;
    }

    public String getInternalAccountChainCode() {
        return internalAccountChainCode;
    }

    public void setInternalAccountChainCode(String internalAccountChainCode) {
        this.internalAccountChainCode = internalAccountChainCode;
    }

    public JSONObject dumpJSON() throws JSONException {

        JSONObject obj = new JSONObject();

        obj.put("externalAccountPubKey", externalAccountPubKey == null ? "" : externalAccountPubKey);
        obj.put("externalAccountChainCode", externalAccountChainCode == null ? "" : externalAccountChainCode);
        obj.put("internalAccountPubKey", internalAccountPubKey == null ? "" : internalAccountPubKey);
        obj.put("internalAccountChainCode", internalAccountChainCode == null ? "" : internalAccountChainCode);

        return obj;
    }

}
