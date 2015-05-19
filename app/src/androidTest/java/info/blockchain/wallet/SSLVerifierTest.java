package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.wallet.util.SSLVerifierUtil;

public class SSLVerifierTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public SSLVerifierTest(String name, Context ctx) {
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

        SSLVerifierUtil ssl = SSLVerifierUtil.getInstance();
        AssertUtil.getInstance().assert_true(this, "Finds valid hostname", ssl.isValidHostname());
        AssertUtil.getInstance().assert_true(this, "Pins certificate", ssl.certificateIsPinned());

    }

}
