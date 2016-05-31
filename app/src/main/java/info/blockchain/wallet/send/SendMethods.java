package info.blockchain.wallet.send;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

public class SendMethods {

    public static long minutesPerBlock = 10;

    public static String getEstimatedConfirmationMessage(long fee, BigInteger[] absoluteFeeSuggestedEstimates, String likelyToConfirmMessage, String unlikelyToConfirmMessage){

        Arrays.sort(absoluteFeeSuggestedEstimates, Collections.reverseOrder());

        String estimateText = unlikelyToConfirmMessage;

        for(int i = 0; i < absoluteFeeSuggestedEstimates.length; i++){

            if(fee >= absoluteFeeSuggestedEstimates[i].longValue()){
                estimateText = likelyToConfirmMessage;
                estimateText = String.format(estimateText, ((i+1) * minutesPerBlock), (i+1));
                break;
            }
        }

        return estimateText;
    }
}
