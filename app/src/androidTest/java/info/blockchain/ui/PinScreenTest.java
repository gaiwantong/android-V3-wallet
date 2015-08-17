package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import junit.framework.TestCase;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
import info.blockchain.wallet.LandingActivity;
import piuk.blockchain.android.R;

import info.blockchain.ui.util.UiUtil;

public class PinScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private EditText walletIDView;
    private EditText walletPasswordView;

    public PinScreenTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        //Press back button twice to exit app
        solo.goBack();
        solo.goBack();
    }

    public void testA_PairingV3Success()  throws AssertionError{

        UiUtil.getInstance(solo.getCurrentActivity()).wipeWallet();

        //Navigate to manual pairing
        solo.clickOnView(solo.getView(R.id.login));
        solo.clickOnView(solo.getView(R.id.command_manual));

        //Set up views
        walletIDView = (EditText) solo.getView(R.id.wallet_id);
        walletPasswordView = (EditText) solo.getView(R.id.wallet_pass);

        //Clear text fields
        solo.clearEditText(walletIDView);
        solo.clearEditText(walletPasswordView);

        //Enter details
        String validUID = solo.getString(R.string.qa_test_guid_v3);
        String validpw = solo.getString(R.string.qa_test_pw_v3);
        solo.enterText(walletIDView, validUID);
        solo.enterText(walletPasswordView, validpw);

        //PIN entry
        solo.clickOnView(solo.getView(R.id.command_next));

        solo.waitForActivity(PinEntryActivity.class);

        //Create PIN
        for (int i = 0; i < 2; i++) {
            solo.sleep(1000);
            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        }

        //Test result
        TestCase.assertEquals(true, solo.waitForText("Received")); //TO-DO: Replace with translated string
    }

    public void testB_PinPasswordSuccess() throws AssertionError{

        //Enter incorrect PIN three times, validate toasts
        for (int i = 0; i < 3; i++) {
            solo.sleep(1000);
            UiUtil.getInstance(getActivity()).enterPin(solo,solo.getString(R.string.qa_test_pin2));
            solo.waitForText(solo.getCurrentActivity().getString(R.string.invalid_pin));
        }
        solo.waitForText(solo.getCurrentActivity().getString(R.string.pin_3_strikes));

        //Select 'Use Password'
        TestCase.assertEquals(true, solo.waitForDialogToOpen());
        solo.clickOnView(solo.getView(android.R.id.button1)); //Use Password

        //Enter correct password and wait for PIN screen
        TestCase.assertEquals(true, solo.waitForDialogToOpen());
        String validpw = solo.getString(R.string.qa_test_pw_v3);
        solo.clickOnEditText(0);
        solo.enterText(0, validpw);
        solo.clickOnView(solo.getView(android.R.id.button1)); //Submit
        solo.waitForDialogToClose();
        solo.sleep(1000);

        solo.waitForActivity(PinEntryActivity.class);

        //Create new PIN - mismatching
        solo.sleep(1000);
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        solo.sleep(1000);
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin2));
        solo.waitForText(solo.getCurrentActivity().getString(R.string.pin_mismatch_error));

        //Create new PIN - matching
        for (int i = 0; i < 2; i++) {
            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        }

        //Validate tx feed
        TestCase.assertEquals(true, solo.waitForText("Received")); //TO-DO: Replace with translated string
    }

    public void testC_PinForgetWallet() throws AssertionError{

        //Enter incorrect PIN three times, validate toasts
        for (int i = 0; i < 3; i++) {
            solo.sleep(1000);
            UiUtil.getInstance(getActivity()).enterPin(solo,solo.getString(R.string.qa_test_pin2));
            solo.waitForText(solo.getCurrentActivity().getString(R.string.invalid_pin));
        }
        solo.waitForText(solo.getCurrentActivity().getString(R.string.pin_3_strikes));

        //Select 'Forget Wallet'
        TestCase.assertEquals(true, solo.waitForDialogToOpen());
        solo.clickOnView(solo.getView(android.R.id.button2)); //Use Password

        //Validate landing page
        TestCase.assertEquals(true, solo.waitForActivity(LandingActivity.class));
    }

}