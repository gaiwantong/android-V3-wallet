package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.wallet.pairing.PairingFactory;

public class PairingTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public PairingTest(String name, Context ctx) {
        super(name, ctx);
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

        String strGood = "1|70c46c4c-6fb2-4790-a4d9-9160ed942263|V5H7lH+FixpBPTuU2Uv+LLVn0FPs+Kv9VKsHc/TY3PRYBmuVQAWOStJh/6/35+FlIMuybutRLJcfjdycGg26WhwKNsqlzM/im0nOzc7PmjJS8UXcTuQE7NgGRkIi3Xcb20M2/x5gZ7dnvAHYue1P/A==";

        String strBad1 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // missing 2 parts
        String strBad2 = "1524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // won't decrypt
        String strBad3 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0|BOGUS_PJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";

        PairingFactory pf = getPairingInstance();

        goodString(pf, strGood);

        badString(pf, strBad1);

        badString(pf, strBad2);

        badString(pf, strBad3);

    }

    public PairingFactory getPairingInstance() {
        PairingFactory pf = PairingFactory.getInstance(context);
        AssertUtil.getInstance().assert_true(this, "PairingFactory instance returned", pf != null);
        return pf;
    }

    public void goodString(PairingFactory pf, String str) {
        AssertUtil.getInstance().assert_true(this, "QRcode: good string", pf.handleQRCode(str));
    }

    public void badString(PairingFactory pf, String str) {
        AssertUtil.getInstance().assert_true(this, "QRcode: bad string", !pf.handleQRCode(str));
    }

}
