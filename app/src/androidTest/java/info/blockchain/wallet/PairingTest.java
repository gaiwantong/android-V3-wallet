package info.blockchain.wallet;

import android.content.Context;

import junit.framework.Assert;

import info.blockchain.wallet.pairing.PairingFactory;

public class PairingTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public PairingTest(String name, Context ctx) {
        super(name);
        context = ctx;
    }

    /**
     * @param String name
     */
    public PairingTest(String name) {
        super(name);
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test something
     */
    public void test() {

        String strGood = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0|MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // missing 1 part
        String strBad1 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // missing 2 parts
        String strBad2 = "1524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // won't decrypt
        String strBad3 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0|BOGUS_PJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";

        PairingFactory pf = PairingFactory.getInstance(context);
        AssertUtil.getInstance().assert_true(this, "PairingFactory instance returned", pf != null);
        AssertUtil.getInstance().assert_true(this, "QRcode: good string", pf.handleQRCode(strGood));
        AssertUtil.getInstance().assert_true(this, "QRcode: bad string", !pf.handleQRCode(strBad1));
        AssertUtil.getInstance().assert_true(this, "QRcode: bad string", !pf.handleQRCode(strBad2));
        AssertUtil.getInstance().assert_true(this, "QRcode: bad string", !pf.handleQRCode(strBad3));

    }

}
