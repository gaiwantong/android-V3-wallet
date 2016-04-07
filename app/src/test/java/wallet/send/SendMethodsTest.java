package wallet.send;

import android.content.Context;

import info.blockchain.wallet.send.SendMethods;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;

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

}
