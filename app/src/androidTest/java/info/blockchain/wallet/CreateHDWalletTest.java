package info.blockchain.wallet;

import android.content.Context;

import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;

class CreateWalletTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public CreateWalletTest(String name, Context ctx) {
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
        WalletFactory hdwf = getFactoryInstance(context);

        Wallet hdw = createDefaultWallet(hdwf);

        hdw = createDefaultWalletWithBadParams(hdwf);

        hdw = isHoldingWallet(hdwf);

    }

    public WalletFactory getFactoryInstance(Context ctx) {
        WalletFactory hdwf = WalletFactory.getInstance();
        AssertUtil.getInstance().assert_true(this, "WalletFactory instance returned", hdwf != null);
        return hdwf;
    }

    public Wallet createDefaultWallet(WalletFactory hdwf) {
        Wallet hdw = null;

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

        return hdw;
    }

    public Wallet createDefaultWalletWithBadParams(WalletFactory hdwf) {
        Wallet hdw = null;

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

        return hdw;
    }

    public Wallet isHoldingWallet(WalletFactory hdwf) {
        Wallet hdw = null;

        //
        // test that Factory is holding a wallet
        //
        try {
            hdw = hdwf.get();
            AssertUtil.getInstance().assert_true(this, "WalletFactory is holding a wallet", hdw != null);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

        return hdw;
    }

}
