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
        super(name);
        context = ctx;
    }

    /**
     * @param String name
     */
    public CreateHDWalletTest(String name) {
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
        HD_WalletFactory hdwf = HD_WalletFactory.getInstance(context);
        assertTrue(hdwf != null);

        HD_Wallet hdw = null;

        //
        // test default wallet create
        //
        try {
            hdw = hdwf.newWallet(12, "", 1);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }
        finally {
            assertTrue(hdw != null);
        }

        //
        // test default wallet create with bad params, should return a good default wallet
        //
        try {
            hdw = hdwf.newWallet(13, null, 0);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }
        finally {
            assertTrue(hdw != null);
        }

        //
        // test that Factory is holding a wallet
        //
        try {
            assertTrue(hdwf.get() != null);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }

    }

}
