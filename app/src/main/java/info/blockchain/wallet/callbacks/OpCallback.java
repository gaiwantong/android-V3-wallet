package info.blockchain.wallet.callbacks;

public interface OpCallback {
    public void onSuccess();
    public void onSuccess(String hash);

    public void onFail();
    public void onFailPermanently();
}