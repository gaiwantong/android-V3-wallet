package info.blockchain.wallet;

import android.content.Context;

import junit.framework.TestCase;

public class BlockchainTest extends TestCase {

    protected Context context = null;

    /**
     * @param String name
     * @param Context ctx
     */
    protected BlockchainTest(String name, Context ctx) {
        super(name);
        context = ctx;
    }

    /**
     * @param String name
     */
    protected BlockchainTest(String name) {
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
    protected void test() { ; }
}
