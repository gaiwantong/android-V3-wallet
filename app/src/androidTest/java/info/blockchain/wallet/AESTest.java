package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.CharSequenceX;

public class AESTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public AESTest(String name, Context ctx) {
        super(name);
        context = ctx;
    }

    /**
     * @param String name
     */
    public AESTest(String name) {
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

        String cleartext = "test data";
        CharSequenceX pw = new CharSequenceX("password");
        int iterations = AESUtil.QRCodePBKDF2Iterations;

        String encrypted = AESUtil.encrypt(cleartext, pw, iterations);
        assertTrue(encrypted != null);

        String decrypted = AESUtil.decrypt(encrypted, pw, iterations);
        assertTrue(cleartext.equals(decrypted));

        String decrypted2 = AESUtil.decrypt(encrypted, new CharSequenceX("bogus"), iterations);
        assertTrue(!cleartext.equals(decrypted2));

        String decrypted3 = AESUtil.decrypt(encrypted, pw, iterations + 1);
        assertTrue(!cleartext.equals(decrypted3));

    }

}
