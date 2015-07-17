package info.blockchain.wallet;

public interface OpCallback {
    public void onSuccess();
    public void onSuccess(String hash);

    public void onFail();
}