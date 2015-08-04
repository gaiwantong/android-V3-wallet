package info.blockchain.wallet.send;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.bitcoin.core.Transaction;

import org.spongycastle.util.encoders.Hex;

import info.blockchain.wallet.util.WebUtil;

public class FeeUtil {

    private static final int ESTIMATED_INPUT_LEN = 148; // compressed key
    private static final int ESTIMATED_OUTPUT_LEN = 34;

    private static BigInteger bAvgFee = null;
    private static BigInteger bHighestFee = null;
    private static BigInteger bPriorityFee = null;  // recommended priority fee
    private static double dPriorityMultiplier = 1.5;
    private static BigInteger bMinFee750KB = null;
    private static BigInteger bMinFee1MB = null;
    private static int totalBytes = -1;

    private static FeeUtil instance = null;

    private FeeUtil()    { ; }

    public static FeeUtil getInstance()  {

        if(instance == null)    {

            bAvgFee = BigInteger.valueOf(10000L);
            bHighestFee = BigInteger.valueOf(50000L);
            bPriorityFee = calcPriority();

            instance = new FeeUtil();
        }

        return instance;
    }

    //
    // use unsigned tx here
    //
    public long getPriority(Transaction tx, List<MyTransactionOutPoint> outputs)   {

        long priority = 0L;

        for(MyTransactionOutPoint output : outputs)   {
            priority += output.getValue().longValue() * output.getConfirmations();
        }
        //
        // calculate priority
        //
        long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());
        priority /= estimatedSize;

        return priority;
    }

    //
    // use signed tx here
    //
    public BigInteger calculatedFee(Transaction tx)   {

        String hexString = new String(Hex.encode(tx.bitcoinSerialize()));
        int size = hexString.length();

        return feeCalculation(size);
    }

    //
    // use unsigned tx here
    //
    public BigInteger estimatedFee(Transaction tx)   {

        int size = estimatedSize(tx.getOutputs().size(), tx.getInputs().size());

        return feeCalculation(size);
    }

    public BigInteger estimatedFee(int inputs, int outputs)   {

        int size = estimatedSize(inputs, outputs);

        return feeCalculation(size);
    }

    public BigInteger getAvgFee() {
        return bAvgFee;
    }

    public BigInteger getHighestFee() {
        return bHighestFee;
    }

    public BigInteger getPriorityFee() {
        return bPriorityFee;
    }

    public BigInteger getRecommendedFee(int inputs, int outputs)    {

        if(isStressed())    {
            return stressFee();
        }
        else    {
            return estimatedFee(inputs, outputs);
        }

    }

    public void update()    {

        try {
            String response = WebUtil.getInstance().getURL(WebUtil.BTCX_FEE);
            averageFee(response);
            highestFee(response);
            totalBytes(response);
            minFee750KB(response);
            minFee1MB(response);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isStressed()   {
        return (totalBytes > 15000000 && bAvgFee.compareTo(BigInteger.valueOf(30000L)) >= 0);
    }

    private void averageFee(String s) {

        double avg_fee = -1.0;

        try {
            Pattern pattern = Pattern.compile("Average fee for payers: ([.\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);

                NumberFormat nf = NumberFormat.getInstance(Locale.US);
                nf.setMaximumFractionDigits(8);

                avg_fee = nf.parse(value.trim()).doubleValue() / 1000;
                bAvgFee = BigInteger.valueOf((long)(Math.round(avg_fee * 1e8)));
                bPriorityFee = calcPriority();
            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    private void highestFee(String s) {

        double high_fee = -1.0;

        try {
            Pattern pattern = Pattern.compile("Maximum fee: ([.\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);

                NumberFormat nf = NumberFormat.getInstance(Locale.US);
                nf.setMaximumFractionDigits(8);

                high_fee = nf.parse(value.trim()).doubleValue() / 1000;
                bHighestFee = BigInteger.valueOf((long)(Math.round(high_fee * 1e8)));
            }

        }
        catch(Exception e) {
            ;
        }

    }

    private void minFee750KB(String s) {

        double min_fee = -1.0;

        try {
            Pattern pattern = Pattern.compile("Minimum fee/KB for 750KB block: ([.\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);

                NumberFormat nf = NumberFormat.getInstance(Locale.US);
                nf.setMaximumFractionDigits(8);

                min_fee = nf.parse(value.trim()).doubleValue() / 1000;
                bMinFee750KB = BigInteger.valueOf((long)(Math.round(min_fee * 1e8)));
            }

        }
        catch(Exception e) {
            ;
        }

    }

    private void minFee1MB(String s) {

        double min_fee = -1.0;

        try {
            Pattern pattern = Pattern.compile("Minimum fee/KB for 1MB block: ([.\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);

                NumberFormat nf = NumberFormat.getInstance(Locale.US);
                nf.setMaximumFractionDigits(8);

                min_fee = nf.parse(value.trim()).doubleValue() / 1000;
                bMinFee1MB = BigInteger.valueOf((long)(Math.round(min_fee * 1e8)));
            }

        }
        catch(Exception e) {
            ;
        }

    }

    private void totalBytes(String s) {

        try {
            Pattern pattern = Pattern.compile("Total bytes: ([\\d]+)");
            Matcher matcher = pattern.matcher(s);
            if(matcher.find())  {
                String value = matcher.group(1);
                totalBytes = Integer.parseInt(value);
            }

        }
        catch(Exception e) {
            ;
        }

    }

    private BigInteger feeCalculation(int size)   {

        int thousands = size / 1000;
        int remainder = size % 1000;

        long fee = SendFactory.bFee.longValue() * thousands;
        if(remainder > 0L)   {
            fee += SendFactory.bFee.longValue();
        }

        return BigInteger.valueOf(fee);
    }

    private int estimatedSize(int inputs, int outputs)   {
        return (outputs * ESTIMATED_OUTPUT_LEN) + (inputs * ESTIMATED_INPUT_LEN) + inputs;
    }

    private static BigInteger calcPriority()   {
        return BigInteger.valueOf((long)Math.round(bAvgFee.doubleValue() * dPriorityMultiplier));
    }

    private BigInteger stressFee()   {
        return bAvgFee;
    }

}
