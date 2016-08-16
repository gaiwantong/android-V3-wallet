package info.blockchain.wallet.cache;

import info.blockchain.wallet.payment.data.SuggestedFee;

public class DynamicFeeCache {

    private static DynamicFeeCache instance;

    private SuggestedFee suggestedFee;

    private DynamicFeeCache() {
        // No-op
    }

    public static DynamicFeeCache getInstance() {
        if (instance == null) {
            instance = new DynamicFeeCache();
        }
        return instance;
    }

    public SuggestedFee getSuggestedFee() {
        return suggestedFee;
    }

    public void setSuggestedFee(SuggestedFee suggestedFee) {
        this.suggestedFee = suggestedFee;
    }

    public void destroy() {
        instance = null;
    }
}
