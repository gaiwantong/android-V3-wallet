package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;

public class DoubleEncryptionTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public DoubleEncryptionTest(String name, Context ctx) {
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

        String cleartext = "test data";
        CharSequenceX pw = new CharSequenceX("password");
        int iterations = AESUtil.DoubleEncryptionPBKDF2Iterations;
        String sharedKey = "524b5e9f-72ea-4690-b28c-8c1cfce65ca0";

        DoubleEncryptionFactory def = DoubleEncryptionFactory.getInstance();
        AssertUtil.getInstance().assert_true(this, "DoubleEncryptionFactory instance returned", def != null);

        String hash = def.getHash(sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Hash returned", def.validateSecondPassword(hash, sharedKey, pw, iterations));

        String encrypted = def.encrypt(cleartext, sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Encrypted string returned", encrypted != null);

        String decrypted = def.decrypt(encrypted, sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypted == cleartext", cleartext.equals(decrypted));

        String decrypted2 = def.decrypt(encrypted, sharedKey, "bogus", iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypt fails w/ bad password", !cleartext.equals(decrypted2));

        String decrypted3 = def.decrypt(encrypted, sharedKey, pw.toString(), iterations + 1);
        AssertUtil.getInstance().assert_true(this, "Decrypt fails w/ bad no. of iterations", !cleartext.equals(decrypted3));

    }

}
