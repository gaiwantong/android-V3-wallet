package info.blockchain.wallet.model;

/**
 * Created by riaanvos on 05/08/16.
 */
public class PaymentConfirmationDetails {

    public String fromLabel;
    public String toLabel;
    public String btcUnit;
    public String fiatUnit;
    public String btcAmount;
    public String fiatAmount;
    public String btcFee;
    public String fiatFee;
    public String btcTotal;
    public String fiatTotal;
    public String btcSuggestedFee;
    public boolean isSurge;

    @Override
    public String toString() {
        return "ConfirmationDetails{" +
                "fromLabel='" + fromLabel + '\'' +
                ", toLabel='" + toLabel + '\'' +
                ", btcUnit='" + btcUnit + '\'' +
                ", fiatUnit='" + fiatUnit + '\'' +
                ", btcAmount='" + btcAmount + '\'' +
                ", fiatAmount='" + fiatAmount + '\'' +
                ", btcFee='" + btcFee + '\'' +
                ", fiatFee='" + fiatFee + '\'' +
                ", btcTotal='" + btcTotal + '\'' +
                ", fiatTotal='" + fiatTotal + '\'' +
                ", btcSuggestedFee='" + btcSuggestedFee + '\'' +
                ", isSurge=" + isSurge +
                '}';
    }

}
