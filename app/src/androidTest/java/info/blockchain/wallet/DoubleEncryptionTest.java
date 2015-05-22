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

        DoubleEncryptionFactory def = getDoubleEncryptionInstance();

        String hash = getHash(def, sharedKey, pw, iterations);

        String encrypted = encrypt(def, cleartext, sharedKey, pw, iterations);

        String decrypted = decryptOK(def, encrypted, cleartext, sharedKey, pw, iterations);

        String decrypted2 = decryptFailPW(def, encrypted, cleartext, sharedKey, new CharSequenceX("bogus"), iterations);

        String decrypted3 = decryptFailIterations(def, encrypted, cleartext, sharedKey, pw, iterations + 1);
    }

    public DoubleEncryptionFactory getDoubleEncryptionInstance() {
        DoubleEncryptionFactory def = DoubleEncryptionFactory.getInstance();
        AssertUtil.getInstance().assert_true(this, "DoubleEncryptionFactory instance returned", def != null);

        return def;
    }

    public String getHash(DoubleEncryptionFactory def, String sharedKey, CharSequenceX pw, int iterations) {
        String hash = def.getHash(sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Hash returned", def.validateSecondPassword(hash, sharedKey, pw, iterations));
        return hash;
    }

    public String encrypt(DoubleEncryptionFactory def, String cleartext, String sharedKey, CharSequenceX pw, int iterations) {
        String encrypted = def.encrypt(cleartext, sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Encrypted string returned", encrypted != null);
        return encrypted;
    }

    public String decryptOK(DoubleEncryptionFactory def, String encrypted, String cleartext, String sharedKey, CharSequenceX pw, int iterations) {
        String decrypted = def.decrypt(encrypted, sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypted == cleartext", cleartext.equals(decrypted));
        return decrypted;
    }

    public String decryptFailPW(DoubleEncryptionFactory def, String encrypted, String cleartext, String sharedKey, CharSequenceX pw, int iterations) {
        String decrypted = def.decrypt(encrypted, sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypt fails w/ bad password", !cleartext.equals(decrypted));
        return decrypted;
    }

    public String decryptFailIterations(DoubleEncryptionFactory def, String encrypted, String cleartext, String sharedKey, CharSequenceX pw, int iterations) {
        String decrypted = def.decrypt(encrypted, sharedKey, pw.toString(), iterations);
        AssertUtil.getInstance().assert_true(this, "Decrypt fails w/ bad no. of iterations", !cleartext.equals(decrypted));
        return decrypted;
    }

}
