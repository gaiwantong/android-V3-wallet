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
        HD_WalletFactory hdwf = getFactoryInstance(context);

        HD_Wallet hdw = restoreGoodHexSeed(hdwf);

        hdw = restoreBadMnemonic(hdwf);

        hdw = restoreGoodMnemonic(hdwf);

        HD_Wallet hdwp = restoreGoodMnemonicWithPassphrase(hdwf);

        differentReceiveChain(hdw, hdwp);

        differentChangeChain(hdw, hdwp);

        hdw = isHoldingWallet(hdwf);

    }

    public HD_WalletFactory getFactoryInstance(Context ctx) {
        HD_WalletFactory hdwf = HD_WalletFactory.getInstance(ctx);
        AssertUtil.getInstance().assert_true(this, "HD_WalletFactory instance returned", hdwf != null);
        return hdwf;
    }

    public HD_Wallet restoreGoodMnemonic(HD_WalletFactory hdwf) {
        HD_Wallet hdw = null;

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
            AssertUtil.getInstance().assert_true(this, "Good mnemonic restore non null wallet", hdw != null);
        }

        return hdw;
    }

    public HD_Wallet restoreBadMnemonic(HD_WalletFactory hdwf) {
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
            AssertUtil.getInstance().assert_true(this, "Bad mnemonic restore null wallet", hdw == null);
        }

        return hdw;
    }

    public HD_Wallet restoreGoodMnemonicWithPassphrase(HD_WalletFactory hdwf) {
        HD_Wallet hdw_pass = null;

        //
        // test wallet restore with good mnemonic and passphrase
        //
        try {
            hdw_pass = hdwf.restoreWallet("all all all all all all all all all all all all", "passphrase", 1);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Good mnemonic + passphrase restore non null wallet", hdw_pass != null);
        }

        return hdw_pass;
    }

    public HD_Wallet restoreGoodHexSeed(HD_WalletFactory hdwf) {
        HD_Wallet hdw = null;

        //
        // test wallet restore with good mnemonic
        //
        try {
            hdw = hdwf.restoreWallet("0660cc198330660cc198330660cc1983", "", 1);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Good hex seed restore non null wallet", hdw != null);
        }

        return hdw;
    }

    public void differentReceiveChain(HD_Wallet hdw, HD_Wallet hdwp) {
        //
        // make sure address spaces are different w/ and wo/ passphrase
        //
        AssertUtil.getInstance().assert_true(this, "Receive address space different for w/ and wo/ passphrase", !hdw.getAccount(0).getReceive().getAddressAt(0).getAddressString().equals(hdwp.getAccount(0).getReceive().getAddressAt(0).getAddressString()));
    }

    public void differentChangeChain(HD_Wallet hdw, HD_Wallet hdwp) {
        //
        // make sure address spaces are different w/ and wo/ passphrase
        //
        AssertUtil.getInstance().assert_true(this, "Change address space different for w/ and wo/ passphrase", !hdw.getAccount(0).getChange().getAddressAt(0).getAddressString().equals(hdwp.getAccount(0).getChange().getAddressAt(0).getAddressString()));
    }

    public HD_Wallet isHoldingWallet(HD_WalletFactory hdwf) {
        HD_Wallet hdw = null;

        //
        // test that Factory is holding a wallet
        //
        try {
            hdw = hdwf.get();
            AssertUtil.getInstance().assert_true(this, "HD_WalletFactory is holding a wallet", hdw != null);
        }
        catch(IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

        return hdw;
    }

}
