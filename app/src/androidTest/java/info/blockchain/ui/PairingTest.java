package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.LandingActivity;
import info.blockchain.wallet.PinEntryActivity;

import junit.framework.TestCase;

import piuk.blockchain.android.R;

public class PairingTest extends ActivityInstrumentationTestCase2<LandingActivity> {

    private Solo solo = null;

    private EditText walletIDView;
    private EditText walletPasswordView;

    public PairingTest() {
        super(LandingActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());

        //Navigate to manual pairing
        solo.clickOnView(solo.getView(R.id.login));
        solo.clickOnView(solo.getView(R.id.command_manual));

        //Set up views
        walletIDView = (EditText) solo.getView(R.id.wallet_id);
        walletPasswordView = (EditText) solo.getView(R.id.wallet_pass);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testPairingPasswordEmpty() throws AssertionError {

        //Clear text fields
        solo.clearEditText(walletIDView);
        solo.clearEditText(walletPasswordView);

        //Enter details
        String validUID = solo.getString(R.string.qa_test_guid_v3);
        solo.enterText(walletIDView, validUID);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForText(solo.getCurrentActivity().getString(R.string.invalid_password)));
    }

    public void testPairingUIDEmpty() throws AssertionError {

        //Clear text fields
        solo.clearEditText(walletIDView);
        solo.clearEditText(walletPasswordView);

        //Enter details
        String invalidpw = "asdf";
        solo.enterText(walletPasswordView, invalidpw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForText(solo.getCurrentActivity().getString(R.string.invalid_guid)));
    }

    public void testPairingPasswordInvalid() throws AssertionError {

        //Clear text fields
        solo.clearEditText(walletIDView);
        solo.clearEditText(walletPasswordView);

        //Enter details
        String validUID = solo.getString(R.string.qa_test_guid_v3);
        String invalidpw = "asdf";
        solo.enterText(walletIDView, validUID);
        solo.enterText(walletPasswordView, invalidpw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForText(solo.getCurrentActivity().getString(R.string.pairing_failed)));
    }

    public void testPairingV3Success() throws AssertionError {

        //Clear text fields
        solo.clearEditText(walletIDView);
        solo.clearEditText(walletPasswordView);

        //Enter details
        String validUID = solo.getString(R.string.qa_test_guid_v3);
        String validpw = solo.getString(R.string.qa_test_pw_v3);
        solo.enterText(walletIDView, validUID);
        solo.enterText(walletPasswordView, validpw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
    }

    public void testPairingV2Success() throws AssertionError {

        //Clear text fields
        solo.clearEditText(walletIDView);
        solo.clearEditText(walletPasswordView);

        //Enter details
        String validUID = solo.getString(R.string.qa_test_guid_v2);
        String validpw = solo.getString(R.string.qa_test_pw_v2);
        solo.enterText(walletIDView, validUID);
        solo.enterText(walletPasswordView, validpw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));
    }

}
