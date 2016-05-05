package wallet.send;

import android.content.Context;

import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.SendMethods;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.FeeUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;
import java.util.HashMap;

import wallet.test_data.UnspentTestData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class SendMethodsTest {

    @Mock
    Context mMockContext;

    @Test
    public void getEstimatedConfirmationMessageTest() {

        String likelyToConfirmMessage = "Estimated confirmation time: ~[--minutes--] minutes ([--block_count--] blocks)";
        String unlikelyToConfirmMessage = "Your fee is so small that your transaction may never be confirmed.";

        BigInteger[] estimates = new BigInteger[6];
        estimates[0] = BigInteger.valueOf(5000);//1
        estimates[1] = BigInteger.valueOf(4000);//2
        estimates[2] = BigInteger.valueOf(3000);//3
        estimates[3] = BigInteger.valueOf(2000);//4
        estimates[4] = BigInteger.valueOf(1000);//5
        estimates[5] = BigInteger.valueOf(500);//6

        String result = SendMethods.getEstimatedConfirmationMessage(0, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is(unlikelyToConfirmMessage));

        result = SendMethods.getEstimatedConfirmationMessage(200, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is(unlikelyToConfirmMessage));

        result = SendMethods.getEstimatedConfirmationMessage(500, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~60 minutes (6 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(800, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~60 minutes (6 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(1000, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~50 minutes (5 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(2000, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~40 minutes (4 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(3300, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~30 minutes (3 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(4000, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~20 minutes (2 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(5000, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~10 minutes (1 blocks)"));

        result = SendMethods.getEstimatedConfirmationMessage(10000, estimates, likelyToConfirmMessage, unlikelyToConfirmMessage);
        assertThat(result, is("Estimated confirmation time: ~10 minutes (1 blocks)"));
    }

    @Test
    public void getUnspentOutputPointsTest(){

        try {
            SendFactory instance = SendFactory.getInstance(mMockContext);
            instance.fromAddressPathMap = new HashMap<>();

            UnspentOutputsBundle unspentOutputPoints = null;
            BigInteger spendAmount = BigInteger.ZERO;
            BigInteger feePerKb = FeeUtil.AVERAGE_FEE_PER_KB;
            BigInteger dust = SendCoins.bDust;
            int inputs = 0;
            int outputs = 0;

            //First coin minus fee
            inputs = 1;
            outputs = 1;
            spendAmount = BigInteger.valueOf(80000l - 5760l);
            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

            /*
            Dust inclusion removed to match up with js
             */
//            //First coin minus fee, add dust to test if consumed
//            inputs = 1;
//            outputs = 1;
//            spendAmount = BigInteger.valueOf(80000l - 5760l - dust.longValue());
//            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
//            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
//            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));
//
//            //First coin minus fee, add a bit more than dust to push it to 2 expected outputs
//            inputs = 1;
//            outputs = 2;
//            spendAmount = BigInteger.valueOf(80000l - 5760l - (dust.longValue()*2));
//            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
//            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
//            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

            //First coin should use 2 inputs and expect change because fee must still be included
            inputs = 2;
            outputs = 2;
            spendAmount = BigInteger.valueOf(80000l);
            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

            //Two coins minus fee should be exactly 2 inputs and expect no change
            inputs = 2;
            outputs = 1;
            spendAmount = BigInteger.valueOf(150000l - 10200l);
            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

            inputs = 3;
            outputs = 2;
            spendAmount = BigInteger.valueOf(150000l);
            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

            inputs = 5;
            outputs = 2;
            spendAmount = BigInteger.valueOf(260000l);
            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

            //5 inputs, but fee of 24540l pushes to 6 inputs
            inputs = 6;
            outputs = 2;
            spendAmount = BigInteger.valueOf(280000l);
            unspentOutputPoints = instance.getUnspentOutputPoints(true, null, spendAmount, feePerKb, UnspentTestData.apiResponseString);
            assertThat(unspentOutputPoints.getOutputs().size(), is(inputs));
            assertThat(unspentOutputPoints.getRecommendedFee().longValue(), is(UnspentTestData.feeMap.get(outputs)[inputs]));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
