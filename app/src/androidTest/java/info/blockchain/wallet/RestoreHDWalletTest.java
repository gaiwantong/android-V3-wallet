package info.blockchain.wallet;

import android.content.Context;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;

public class RestoreHDWalletTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public RestoreHDWalletTest(String name, Context ctx) {
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
        // test wallet restore with bad mnemonic
        //
        try {
            hdw = hdwf.restoreWallet("all all all all all all all all all all all bogus", "", 1);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Bad params restore null wallet", hdw == null);
        }

        //
        // test wallet restore with good mnemonic
        //
        try {
            hdw = hdwf.restoreWallet("all all all all all all all all all all all all", "", 1);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Good params restore non null wallet", hdw != null);
        }

        //
        // test wallet restore with good mnemonic and passphrase
        //
        HD_Wallet hdw_pass = null;
        try {
            hdw_pass = hdwf.restoreWallet("all all all all all all all all all all all all", "passphrase", 1);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Good params + passphrase restore non null wallet", hdw_pass != null);
        }

        //
        // make sure address spaces are different w/ and wo/ passphrase
        //
        AssertUtil.getInstance().assert_true(this, "Receive address space different for w/ and wo/ passphrase", !hdw.getAccount(0).getReceive().getAddressAt(0).getAddressString().equals(hdw_pass.getAccount(0).getReceive().getAddressAt(0).getAddressString()));
        AssertUtil.getInstance().assert_true(this, "Change address space different for w/ and wo/ passphrase", !hdw.getAccount(0).getChange().getAddressAt(0).getAddressString().equals(hdw_pass.getAccount(0).getChange().getAddressAt(0).getAddressString()));

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
