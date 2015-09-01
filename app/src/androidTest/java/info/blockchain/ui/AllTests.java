package info.blockchain.ui;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends ActivityInstrumentationTestCase2<Activity> {

    public static boolean enableUserInteraction = false;//false will skip any tests that require user interaction (confirmation code input, manual pair confirm)

    public AllTests(Class<Activity> activityClass) {
        super(Activity.class);
    }

    public static TestSuite suite() {

        TestSuite t = new TestSuite();

        //Test creating a wallet
        t.addTestSuite(ClearWalletData.class);
        t.addTestSuite(CreateAWalletTest.class);

        //Test email confirmation
        t.addTestSuite(ClearWalletData.class);
        t.addTestSuite(ConfirmationCodeTest.class);

        //Test pairing
        t.addTestSuite(ClearWalletData.class);
        t.addTestSuite(PairingTest.class);

        //Test balance screen
        t.addTestSuite(BalanceScreenTest.class);

        //Test send screen
        t.addTestSuite(SendScreenTest.class);

        //Test send screen
        t.addTestSuite(ReceiveScreenTest.class);

        //Test my accounts screen
        t.addTestSuite(MyAccountsScreenTest.class);

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