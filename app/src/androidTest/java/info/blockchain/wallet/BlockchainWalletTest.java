package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.credentials.WalletUtil;
import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.payload.HDPayloadBridge;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;

import java.io.IOException;

public class BlockchainWalletTest extends BlockchainTest {

    /**
     * @param String  name
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
        PrefsUtil prefs = new PrefsUtil(context);
        prefs.setValue(PrefsUtil.KEY_GUID, WalletUtil.getInstance(context).getGuid());
        prefs.setValue(PrefsUtil.KEY_SHARED_KEY, WalletUtil.getInstance(context).getSharedKey());

        prefs.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, WalletUtil.getInstance(context).getPinIdentifier());
        prefs.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, WalletUtil.getInstance(context).getEncryptedPassword());

        CharSequenceX pw = new CharSequenceX(WalletUtil.getInstance(context).getValidPassword());

        loginGoodParams(pw);

        loginBadPW(new CharSequenceX("bogus"));

        prefs.setValue(PrefsUtil.KEY_GUID, "12345678-9abc-def0-ffff-ffffffffffff");
        prefs.setValue(PrefsUtil.KEY_SHARED_KEY, "12345678-9abc-def0-ffff-ffffffffffff");

        loginBadParams(pw);

        //
        // login w/ PIN tests
        //
        prefs.setValue(PrefsUtil.KEY_GUID, WalletUtil.getInstance(context).getGuid());
        prefs.setValue(PrefsUtil.KEY_SHARED_KEY, WalletUtil.getInstance(context).getSharedKey());

        prefs.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, WalletUtil.getInstance(context).getPinIdentifier());
        prefs.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, WalletUtil.getInstance(context).getEncryptedPassword());

        loginGoodPIN();

        loginBadPIN();

    }

    public void loginGoodParams(CharSequenceX pw) {
        //
        // login w/ password tests
        //
        boolean loggedIn = false;

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(pw);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AssertUtil.getInstance().assert_true(this, "Logged in with proper credentials", loggedIn);
        }
    }

    public void loginBadPW(CharSequenceX pw) {

        boolean loggedIn = false;

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(new CharSequenceX(pw));
        } catch (Exception e) {
            ;
        } finally {
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad password", !loggedIn);
        }
    }

    public void loginBadParams(CharSequenceX pw) {

        boolean loggedIn = false;

        try {
            loggedIn = HDPayloadBridge.getInstance(context).init(pw);
        } catch (Exception e) {
            ;
        } finally {
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad credentials", !loggedIn);
        }
    }

    public void loginGoodPIN() {
        CharSequenceX password = null;
        try {
            password = AccessFactory.getInstance(context).validatePIN(WalletUtil.getInstance(context).getValidPin());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AssertUtil.getInstance().assert_true(this, "Logged in with good PIN", password != null);
        }
    }

    public void loginBadPIN() {
        CharSequenceX password = null;
        try {
            password = AccessFactory.getInstance(context).validatePIN("9999");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AssertUtil.getInstance().assert_true(this, "Not logged in with bad PIN", password == null);
        }
    }

}
