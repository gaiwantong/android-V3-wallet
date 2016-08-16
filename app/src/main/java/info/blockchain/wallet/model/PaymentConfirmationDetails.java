package info.blockchain.wallet.model;

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
    public boolean isLargeTransaction;
    public boolean hasConsumedAmounts;

    @Override
    public String toString() {
        return "PaymentConfirmationDetails{" +
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
                ", isLargeTransaction=" + isLargeTransaction +
                ", hasConsumedAmounts=" + hasConsumedAmounts +
                '}';
    }
}
