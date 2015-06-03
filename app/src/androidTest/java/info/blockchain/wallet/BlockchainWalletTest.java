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

        /*
         * In order to run these tests legit guid, shared key, pin identifiers, and encrypted passwords are needed.
         * The shared key can be obtained by observing the JSON console on your web wallet,
         * or by placing logcat statements in PinEntryActivity.java for this app and observing during an actual login.
         *
         * The pin identifier and the encrypted password can be obtained by observing AccessFactory.java for this app
         * during an actual login.
         */

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, "524b5e9f-72ea-4690-b28c-8c1cfce65ca0");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, "6088a51a-26f2-47e0-9d16-934dd2a6131a");

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_PIN_IDENTIFIER, "c1628eecbfe812478f1b613fd96701d2");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "rRxpxSHhrnAgvc1Fy2LBqTI0qC2pG8vW6H2+4kz/MbUt60qwUpFLB5QOoo0TDSE7");

        CharSequenceX pw = new CharSequenceX("blockchain_test_wallet_2");

        loginGoodParams(pw);

        loginBadPW(new CharSequenceX("bogus"));

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, "12345678-9abc-def0-ffff-ffffffffffff");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, "12345678-9abc-def0-ffff-ffffffffffff");

        loginBadParams(pw);

        //
        // login w/ PIN tests
        //
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_GUID, "524b5e9f-72ea-4690-b28c-8c1cfce65ca0");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY, "6088a51a-26f2-47e0-9d16-934dd2a6131a");

        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_PIN_IDENTIFIER, "c1628eecbfe812478f1b613fd96701d2");
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "rRxpxSHhrnAgvc1Fy2LBqTI0qC2pG8vW6H2+4kz/MbUt60qwUpFLB5QOoo0TDSE7");

//        loginGoodPIN();

        loginBadPIN();

    }

    public void loginGoodParams(CharSequenceX pw) {
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
            e.printStackTrace();
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Logged in with proper credentials", loggedIn);
        }
    }

    public void loginBadPW(CharSequenceX pw) {

        boolean loggedIn = false;

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(new CharSequenceX(pw));
        }
        catch(IOException | DecoderException | AddressFormatException
                | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicChecksumException
                | MnemonicException.MnemonicWordException | JSONException e) {
            ;
        }
        finally {
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad password", !loggedIn);
        }
    }

    public void loginBadParams(CharSequenceX pw) {

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
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad credentials", !loggedIn);
        }
    }

    public void loginGoodPIN() {
        CharSequenceX password = AccessFactory.getInstance(context).validatePIN("1234");
        AssertUtil.getInstance().assert_true(this, "Logged in with good PIN", password != null);
    }

    public void loginBadPIN() {
        CharSequenceX password = AccessFactory.getInstance(context).validatePIN("9999");
        AssertUtil.getInstance().assert_true(this, "Not logged in with bad PIN", password == null);
    }

}
