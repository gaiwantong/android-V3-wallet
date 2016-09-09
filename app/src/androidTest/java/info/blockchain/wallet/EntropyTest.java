package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.api.ExternalEntropy;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.Util;
import info.blockchain.wallet.util.WebUtil;

import org.bitcoinj.core.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;

import static piuk.blockchain.android.R.id.result;

public class EntropyTest extends BlockchainTest {

    Context ctx;

    /**
     * @param String  name
     * @param Context ctx
     */
    public EntropyTest(String name, Context ctx) {
        super(name, ctx);
        this.ctx = ctx;
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
    public void _test() {

        AssertUtil.getInstance().exitOnFail(true);

        //Test xor
        predefinedXOR();

        //Ensure creation fails if any errors occur
        serverResultIncomplete();
        serverConnectionError();
        hexDecodeError();
        xorError();
    }

    /*
    Test XOR method with predefined values
     */
    public void predefinedXOR() {

        //http://xor.pw/
        byte[] data1 = Hex.decode("033cdcaecb07a62695963be9dbeec3362f1b26b048e8e15fb239c8f6967e8410");
        byte[] data2 = Hex.decode("3980d19c880f6cfecf97ba22e8d1e03c9566448e37de0b4757a898a76c71fa64");
        String expectedResult = "3abc0d324308cad85a0181cb333f230aba7d623e7f36ea18e5915051fa0f7e74";

        byte[] xor = Util.getInstance().xor(data1, data2);
        AssertUtil.getInstance().assert_true(this, "XOR", expectedResult.equals(Hex.toHexString(xor)));
    }

    /*
    If the server returns incomplete result, confirm that no new address is generated for the wallet
     */
    public void serverResultIncomplete() {

        AssertUtil.getInstance().assert_true(this, "serverResultIncomplete", newLegacyAddress(0) == null);
    }

    /*
    If there is a server issue and server-side entropy cannot be obtained, confirm that no new address is generated for the wallet
     */
    public void serverConnectionError() {

        AssertUtil.getInstance().assert_true(this, "serverConnectionError", newLegacyAddress(1) == null);
    }

    /*
    If server result cannot be decoded, confirm that no new address is generated for the wallet
     */
    public void hexDecodeError() {

        AssertUtil.getInstance().assert_true(this, "hexDecodeError", newLegacyAddress(2) == null);
    }

    /*
    If xor fails, confirm that no new address is generated for the wallet
     */
    public void xorError() {

        AssertUtil.getInstance().assert_true(this, "xorError", newLegacyAddress(3) == null);
    }

    /*
    Duplicate of PayloadBridge.newLegacyAddress() - testCase added to break at various points
     */
    public ECKey newLegacyAddress(int testCase) {

        new AppUtil(context).applyPRNGFixes();

        byte[] data = null;
        try {
            data = new ExternalEntropy().getRandomBytes();

            //TestCase insert start
            if (testCase == 2) data = null;//emulate Hex decode error
            //TestCase insert end
        } catch (Exception e) {
            return null;//testCase 2 will trigger this
        }

        ECKey ecKey = null;
        if (data != null) {
            byte[] rdata = new byte[32];
            SecureRandom random = new SecureRandom();
            random.nextBytes(rdata);
            byte[] privbytes = Util.getInstance().xor(data, rdata);

            //TestCase insert start
            if (testCase == 3) privbytes = null;//emulate xor error
            //TestCase insert end

            if (privbytes == null) {
                return null;
            }
            ecKey = ECKey.fromPrivate(privbytes, true);
            // erase all byte arrays:
            random.nextBytes(privbytes);
            random.nextBytes(rdata);
            random.nextBytes(data);
        } else {
            return null;//testCase 3 will trigger this
        }
        return ecKey;
    }
}
