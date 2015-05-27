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
        int iterations = AESUtil.QRCodePBKDF2Iterations;

        String encrypted = encryptOK(cleartext, pw, iterations);

        String decrypted = decryptOK(encrypted, cleartext, pw, iterations);

        String decrypted2 = decryptFailPW(encrypted, cleartext, new CharSequenceX("bogus"), iterations);

        String decrypted3 = decryptFailIterations(encrypted, cleartext, pw, iterations + 1);
    }

    public String encryptOK(String cleartext, CharSequenceX pw, int iterations) {
        String encrypted = AESUtil.encrypt(cleartext, pw, iterations);
        AssertUtil.getInstance().assert_true(this, "Encrypted string returned", encrypted != null);
        return encrypted;
    }

    public String decryptOK(String encrypted, String cleartext, CharSequenceX pw, int iterations) {
        String decrypted = AESUtil.decrypt(encrypted, pw, iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypted == cleartext", cleartext.equals(decrypted));
        return decrypted;
    }

    public String decryptFailPW(String encrypted, String cleartext, CharSequenceX pw, int iterations) {
        String decrypted = AESUtil.decrypt(encrypted, new CharSequenceX("bogus"), iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypt fails w/ bad password", !cleartext.equals(decrypted));
        return decrypted;
    }

    public String decryptFailIterations(String encrypted, String cleartext, CharSequenceX pw, int iterations) {
        String decrypted = AESUtil.decrypt(encrypted, pw, iterations + 1);
        AssertUtil.getInstance().assert_true(this, "Decrypt fails w/ bad no. of iterations", !cleartext.equals(decrypted));
        return decrypted;
    }

}
