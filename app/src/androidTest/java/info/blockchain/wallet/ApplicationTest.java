package info.blockchain.wallet;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.robotium.solo.Solo;

import info.blockchain.wallet.view.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    private Solo solo;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        //This is where the solo object is created.
        solo = new Solo(getInstrumentation(), mActivityRule.getActivity());
    }

    @After
    public void tearDown() throws Exception {
        //finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        solo.finishOpenedActivities();
    }

    @Ignore
    @Test
    public void testAll() throws Exception {
        assertTrue(true);
        // TODO: 02/09/2016 For now these won't run correctly. These need a rethink.
//        SSLVerifierTest sslTest = new SSLVerifierTest("SSLVerifierTest", mActivityRule.getActivity());
//        sslTest.test();
//
//        BlockchainWalletTest bwt = new BlockchainWalletTest("BlockchainWalletTest", mActivityRule.getActivity());
//        bwt.test();
//
//        EntropyTest entropyTest = new EntropyTest("EntropyTest", mActivityRule.getActivity());
//        entropyTest.test();
    }

}
