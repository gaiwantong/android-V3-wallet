package info.blockchain.wallet.cache;

import org.json.JSONObject;

public class DefaultAccountUnspentCache {

    private static DefaultAccountUnspentCache instance;

    private String xpub;
    private JSONObject unspentApiResponse;

    private DefaultAccountUnspentCache() {
        // No-op
    }

    public static DefaultAccountUnspentCache getInstance() {
        if (instance == null) {
            instance = new DefaultAccountUnspentCache();
        }
        return instance;
    }

    public void destroy() {
        instance = null;
    }

    public JSONObject getUnspentApiResponse() {
        return unspentApiResponse;
    }

    public void setUnspentApiResponse(String xpub, JSONObject unspentApiResponse) {
        this.unspentApiResponse = unspentApiResponse;
    }

    public String getXpub() {
        return xpub;
    }

    public void setXpub(String xpub) {
        this.xpub = xpub;
    }
}

