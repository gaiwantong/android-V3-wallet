package info.blockchain.wallet;

import android.content.Context;

import com.google.bitcoin.crypto.MnemonicException;

import java.io.IOException;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;

public class CreateHDWalletTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public CreateHDWalletTest(String name, Context ctx) {
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
        HD_WalletFactory hdwf = HD_WalletFactory.getInstance(context);
        AssertUtil.getInstance().assert_true(this, "HD_WalletFactory instance returned", hdwf != null);

        HD_Wallet hdw = null;

        //
        // test default wallet create
        //
        try {
            hdw = hdwf.newWallet(12, "", 1);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Good params create new wallet", hdw != null);
        }

        //
        // test default wallet create with bad params, should return a good default wallet
        //
        try {
            hdw = hdwf.newWallet(13, null, 0);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Bad params create new wallet", hdw != null);
        }

        //
        // test that Factory is holding a wallet
        //
        try {
            AssertUtil.getInstance().assert_true(this, "HD_WalletFactory is holding a wallet", hdwf.get() != null);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

    }

}
