package info.blockchain.wallet.model;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;

import java.math.BigInteger;

public class PendingTransaction {

    public SpendableUnspentOutputs unspentOutputBundle;
    public ItemAccount sendingObject;
    public ItemAccount receivingObject;
    public String note;
    public String receivingAddress;
    public BigInteger bigIntFee;
    public BigInteger bigIntAmount;

    public boolean isHD(){
        return (sendingObject.accountObject instanceof Account);
    }

    @Override
    public String toString() {
        return "PendingTransaction{" +
                "unspentOutputBundle=" + unspentOutputBundle +
                ", sendingObject=" + sendingObject +
                ", receivingObject=" + receivingObject +
                ", note='" + note + '\'' +
                ", receivingAddress='" + receivingAddress + '\'' +
                ", bigIntFee=" + bigIntFee +
                ", bigIntAmount=" + bigIntAmount +
                '}';
    }
}
