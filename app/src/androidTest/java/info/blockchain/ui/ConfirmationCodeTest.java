package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.google.bitcoin.crypto.MnemonicException;
import com.robotium.solo.Solo;

import junit.framework.TestCase;

import java.io.IOException;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class ConfirmationCodeTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private EditText emailAddressView;
    private EditText walletPasswordView;
    private EditText walletPasswordConfirmView;

    public ConfirmationCodeTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        solo.goBack();
        solo.goBack();
    }

    private void navigateToCreate(){

        //Navigate to create wallet
        solo.clickOnView(solo.getView(R.id.create));
        try{solo.sleep(1000);}catch (Exception e){}

        //Set up views
        emailAddressView = (EditText)solo.getCurrentActivity().findViewById(R.id.email_address);
        walletPasswordView = (EditText)solo.getCurrentActivity().findViewById(R.id.wallet_pass);
        walletPasswordConfirmView = (EditText)solo.getCurrentActivity().findViewById(R.id.wallet_pass_confrirm);
    }

    public void testA_CreateValidWallet()  throws AssertionError{
        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = solo.getString(R.string.qa_test_email);
        String pw = solo.getString(R.string.qa_test_password1);
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
    }

    public void testB_EnterIncorrectCode() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));

        solo.enterText((EditText) solo.getView(R.id.confirmBox0), "A");
        solo.enterText((EditText) solo.getView(R.id.confirmBox1), "A");
        solo.enterText((EditText) solo.getView(R.id.confirmBox2), "A");
        solo.enterText((EditText) solo.getView(R.id.confirmBox3), "A");
        solo.enterText((EditText) solo.getView(R.id.confirmBox4), "A");

        TestCase.assertTrue(solo.waitForText("Invalid Response Email Verification Code Incorrect"));//message generated server side
    }

    public void testC_ResendEmail() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));

        solo.clickOnText(getActivity().getString(R.string.resend_email_confirmation));

        //Confirm email received needs to be done manually by tester - so we'll make a sound to alert tester
        UiUtil.getInstance(getActivity()).soundAlert();
    }

    public void testD_ForgetWallet() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));

        solo.clickOnText(getActivity().getString(R.string.wipe_wallet));
        try{solo.sleep(1000);}catch (Exception e){}

        //Test wiped
        TestCase.assertTrue(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_GUID, "").isEmpty());
        TestCase.assertTrue(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty());
    }

    public void testE_CreateValidWallet()  throws AssertionError{
        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = solo.getString(R.string.qa_test_email);
        String pw = solo.getString(R.string.qa_test_password1);
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
    }

    public void testF_EnterCorrectCode() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {

        if(AllTests.enableUserInteraction) {

            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));

            //Confirm email received needs to be done manually by tester - so we'll make a sound to alert tester
            UiUtil.getInstance(getActivity()).soundAlert();

            TestCase.assertTrue(solo.waitForText(getActivity().getString(R.string.my_bitcoin_wallet), 1, 240000));
        }
    }
}
