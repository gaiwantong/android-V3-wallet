package info.blockchain.wallet.model;

public class RecipientModel {

    private String mAddress;
    private String mValue;

    public RecipientModel(String address, String value) {
        mAddress = address;
        mValue = value;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getValue() {
        return mValue;
    }
}
