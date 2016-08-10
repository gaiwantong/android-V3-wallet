package info.blockchain.wallet.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import info.blockchain.wallet.payment.data.SuggestedFee;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;

import piuk.blockchain.android.BR;

/**
 * Created by riaanvos on 02/08/16.
 */
public class SendModel extends BaseObservable {

    //Views
    private String destinationAddress;
    private String maxAviable;
    private int maxAvailableVisibility;
    private int maxAvailableProgressVisibility;
    private int maxAvailableColor;
    private String estimate;
    private int estimateColor;
    private int customFeeColor;

    //TODO bind amountRow - 'include'

    public String defaultSeparator;//Decimal separator based on locale
    public boolean textChangeAllowed = true;
    public String btcUnit;
    public int btcUniti;
    public String fiatUnit;
    public double exchangeRate;
    public SuggestedFee suggestedFee;
    public HashMap<String, JSONObject> unspentApiResponse;//current selected <from address, unspent api response> - so we don't need to call api repeatedly

    public PendingTransaction pendingTransaction;
    public BigInteger[] absoluteFeeSuggestedEstimates;
    public BigInteger absoluteFeeSuggested;//Fee for 2nd block inclusion

    public String verifiedSecondPassword;

    //Vars used for warning user of large tx
    public static final int LARGE_TX_SIZE = 516;//kb
    public static final long LARGE_TX_FEE = 80000;//USD
    public static final double LARGE_TX_PERCENTAGE = 1.0;//%

    @Bindable
    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
        notifyPropertyChanged(BR.destinationAddress);
    }

    @Bindable
    public String getMaxAviable() {
        return maxAviable;
    }

    public void setMaxAviable(String maxAviable) {
        this.maxAviable = maxAviable;
        notifyPropertyChanged(BR.maxAviable);
    }

    @Bindable
    public int getMaxAvailableVisibility() {
        return maxAvailableVisibility;
    }

    public void setMaxAvailableVisibility(int visibility) {
        this.maxAvailableVisibility = visibility;
        notifyPropertyChanged(BR.maxAvailableVisibility);
    }

    @Bindable
    public int getMaxAvailableProgressVisibility() {
        return maxAvailableProgressVisibility;
    }

    public void setMaxAvailableProgressVisibility(int visibility) {
        this.maxAvailableProgressVisibility = visibility;
        notifyPropertyChanged(BR.maxAvailableProgressVisibility);
    }

    @Bindable
    public int getMaxAvailableColor() {
        return maxAvailableColor;
    }

    public void setMaxAvailableColor(int maxAvailableColor) {
        this.maxAvailableColor = maxAvailableColor;
        notifyPropertyChanged(BR.maxAvailableColor);
    }

    @Bindable
    public String getEstimate() {
        return estimate;
    }

    public void setEstimate(String estimate) {
        this.estimate = estimate;
        notifyPropertyChanged(BR.estimate);
    }

    @Bindable
    public int getEstimateColor() {
        return estimateColor;
    }

    public void setEstimateColor(int estimateColor) {
        this.estimateColor = estimateColor;
        notifyPropertyChanged(BR.estimateColor);
    }

    @Bindable
    public int getCustomFeeColor() {
        return customFeeColor;
    }

    public void setCustomFeeColor(int customFeeColor) {
        this.customFeeColor = customFeeColor;
        notifyPropertyChanged(BR.customFeeColor);
    }
}
