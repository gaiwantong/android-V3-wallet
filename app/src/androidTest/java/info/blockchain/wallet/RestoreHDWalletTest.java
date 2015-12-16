package info.blockchain.wallet;

import android.content.Context;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;

public class RestoreHDWalletTest extends BlockchainTest {

    /**
     * @param String  name
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
        WalletFactory hdwf = getFactoryInstance(context);

        Wallet hdw = restoreGoodHexSeed(hdwf);

        hdw = restoreBadMnemonic(hdwf);

        hdw = restoreGoodMnemonic(hdwf);

        Wallet hdwp = restoreGoodMnemonicWithPassphrase(hdwf);

        differentReceiveChain(hdw, hdwp);

        differentChangeChain(hdw, hdwp);

        hdw = isHoldingWallet(hdwf);

    }

    public WalletFactory getFactoryInstance(Context ctx) {
        WalletFactory hdwf = WalletFactory.getInstance();
        AssertUtil.getInstance().assert_true(this, "WalletFactory instance returned", hdwf != null);
        return hdwf;
    }

    public Wallet restoreGoodMnemonic(WalletFactory hdwf) {
        Wallet hdw = null;

        //
        // test wallet restore with good mnemonic
        //
        try {
            hdw = hdwf.restoreWallet("all all all all all all all all all all all all", "", 1);
        } catch (IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        } finally {
            AssertUtil.getInstance().assert_true(this, "Good mnemonic restore non null wallet", hdw != null);
        }

        return hdw;
    }

    public Wallet restoreBadMnemonic(WalletFactory hdwf) {
        Wallet hdw = null;

        //
        // test wallet restore with bad mnemonic
        //
        try {
            hdw = hdwf.restoreWallet("all all all all all all all all all all all bogus", "", 1);
        } catch (IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        } finally {
            AssertUtil.getInstance().assert_true(this, "Bad mnemonic restore null wallet", hdw == null);
        }

        return hdw;
    }

    public Wallet restoreGoodMnemonicWithPassphrase(WalletFactory hdwf) {
        Wallet hdw_pass = null;

        //
        // test wallet restore with good mnemonic and passphrase
        //
        try {
            hdw_pass = hdwf.restoreWallet("all all all all all all all all all all all all", "passphrase", 1);
        } catch (IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        } finally {
            AssertUtil.getInstance().assert_true(this, "Good mnemonic + passphrase restore non null wallet", hdw_pass != null);
        }

        return hdw_pass;
    }

    public Wallet restoreGoodHexSeed(WalletFactory hdwf) {
        Wallet hdw = null;

        //
        // test wallet restore with good mnemonic
        //
        try {
            hdw = hdwf.restoreWallet("0660cc198330660cc198330660cc1983", "", 1);
        } catch (IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException e) {
            ;
        } finally {
            AssertUtil.getInstance().assert_true(this, "Good hex seed restore non null wallet", hdw != null);
        }

        return hdw;
    }

    public void differentReceiveChain(Wallet hdw, Wallet hdwp) {
        //
        // make sure address spaces are different w/ and wo/ passphrase
        //
        AssertUtil.getInstance().assert_true(this, "Receive address space different for w/ and wo/ passphrase", !hdw.getAccount(0).getReceive().getAddressAt(0).getAddressString().equals(hdwp.getAccount(0).getReceive().getAddressAt(0).getAddressString()));
    }

    public void differentChangeChain(Wallet hdw, Wallet hdwp) {
        //
        // make sure address spaces are different w/ and wo/ passphrase
        //
        AssertUtil.getInstance().assert_true(this, "Change address space different for w/ and wo/ passphrase", !hdw.getAccount(0).getChange().getAddressAt(0).getAddressString().equals(hdwp.getAccount(0).getChange().getAddressAt(0).getAddressString()));
    }

    public Wallet isHoldingWallet(WalletFactory hdwf) {
        Wallet hdw = null;

        //
        // test that Factory is holding a wallet
        //
        try {
            hdw = hdwf.get();
            AssertUtil.getInstance().assert_true(this, "WalletFactory is holding a wallet", hdw != null);
        } catch (IOException | MnemonicException.MnemonicLengthException e) {
            ;
        }

        return hdw;
    }

}
