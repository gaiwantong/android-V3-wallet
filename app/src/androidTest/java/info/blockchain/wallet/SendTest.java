package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.wallet.send.SendFactory;

public class SendTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public SendTest(String name, Context ctx) {
        super(name);
        context = ctx;
    }

    /**
     * @param String name
     */
    public SendTest(String name) {
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
        SendFactory factory = SendFactory.getInstance(context);
        assertTrue(factory != null);
    }
}
