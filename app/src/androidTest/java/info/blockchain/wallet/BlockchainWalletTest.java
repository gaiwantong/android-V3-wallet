package info.blockchain.wallet;

import android.content.Context;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;

import java.io.IOException;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrefsUtil;

public class BlockchainWalletTest extends BlockchainTest {

    /**
     * @param String name
     * @param Context ctx
     */
    public BlockchainWalletTest(String name, Context ctx) {
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

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, "--LEGIT_GUID_HERE--");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, "--LEGIT_SHARED_KEY_HERE--");

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_PIN_IDENTIFIER, "LEGIT_PIN_IDENTIFIER_HERE");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "LEGIT_ENCRYPTED_PASSWORD_HERE");

        /*
         * In order to run these tests legit guid, shared key, pin identifiers, and encrypted passwords are needed.
         * The shared key can be obtained by observing the JSON console on your web wallet,
         * or by placing logcat statements in PinEntryActivity.java for this app and observing during an actual login.
         *
         * The pin identifier and the encrypted password can be obtained by observing AccessFactory.java for this app
         * during an actual login.
         */

        CharSequenceX pw = new CharSequenceX("--LEGIT_PASSWORD_HERE--");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, "--LEGIT_GUID_HERE--");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, "--LEGIT_SHARED_KEY_HERE--");

        //
        // login w/ password tests
        //
        boolean loggedIn = false;

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(pw);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException | JSONException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Logged in with proper credentials", loggedIn);
        }

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(new CharSequenceX("bogus"));
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException | JSONException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad password", loggedIn);
        }

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, "12345678-9abc-def0-ffff-ffffffffffff");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, "12345678-9abc-def0-ffff-ffffffffffff");

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(pw);
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException | JSONException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad credentials", loggedIn);
        }

        //
        // login w/ PIN tests
        //
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_PIN_IDENTIFIER, "LEGIT_PIN_IDENTIFIER_HERE");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "LEGIT_ENCRYPTED_PASSWORD_HERE");

        CharSequenceX password = AccessFactory.getInstance(context).validatePIN("LEGIT_PIN_HERE");
        AssertUtil.getInstance().assert_true(this, "Logged in with good PIN", password != null);

        password = AccessFactory.getInstance(context).validatePIN("9999");
        AssertUtil.getInstance().assert_true(this, "Not logged in with bad PIN", password == null);

    }

}