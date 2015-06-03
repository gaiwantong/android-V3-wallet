package info.blockchain.wallet;

import com.robotium.solo.Solo;
import android.test.ActivityInstrumentationTestCase2;

public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    public ApplicationTest() {
        super(MainActivity.class);

        try {
            testAll();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setUp() throws Exception {
        //setUp() is run before a test case is started.
        //This is where the solo object is created.
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        //tearDown() is run after a test case has finished.
        //finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        solo.finishOpenedActivities();
    }

    public void testAll() throws Exception {

        AESTest aesTest = new AESTest("AESTest", getActivity());
        aesTest.test();

        CreateHDWalletTest createHDTest = new CreateHDWalletTest("CreateHDWalletTest", getActivity());
        createHDTest.test();

        RestoreHDWalletTest restoreHDTest = new RestoreHDWalletTest("RestoreHDWalletTest", getActivity());
        restoreHDTest.test();

        PairingTest pairingTest = new PairingTest("PairingTest", getActivity());
        pairingTest.test();

        DoubleEncryptionTest deTest = new DoubleEncryptionTest("DoubleEncryptionTest", getActivity());
        deTest.test();

        SSLVerifierTest sslTest = new SSLVerifierTest("SSLVerifierTest", getActivity());
        sslTest.test();

        BlockchainWalletTest bwt = new BlockchainWalletTest("BlockchainWalletTest", getActivity());
        bwt.test();

        SendTest sendTest = new SendTest("SendTest", getActivity());
        sendTest.test();

    }

}
