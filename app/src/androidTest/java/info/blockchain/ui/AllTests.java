package info.blockchain.ui;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends ActivityInstrumentationTestCase2<Activity> {

    public AllTests(Class<Activity> activityClass) {
        super(Activity.class);
    }

    public static TestSuite suite() {

        TestSuite t = new TestSuite();
        t.addTestSuite(ClearWalletData.class);
        t.addTestSuite(CreateAWalletTest.class);

        //Pair valid funded wallet for testing - Tester must manually confirm email when received
        t.addTestSuite(ClearWalletData.class);
        t.addTestSuite(PairValidWallet.class);
        t.addTestSuite(BalanceScreenTest.class);

        return t;
    }

    @Override
    public void setUp() throws Exception {
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testEmpty() throws AssertionError{
        //to avoid warning we need an empty test
        TestCase.assertTrue(true);
    }
}