package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.google.bitcoin.crypto.MnemonicException;
import com.robotium.solo.Solo;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
import info.blockchain.wallet.PolicyActivity;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class CreateAWalletTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private EditText emailAddressView;
    private EditText walletPasswordView;
    private EditText walletPasswordConfirmView;

    public CreateAWalletTest() {
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

    private void navigateToCreate(){

        //Navigate to create wallet
        solo.clickOnView(solo.getView(R.id.create));
        try{solo.sleep(1000);}catch (Exception e){}

        //Set up views
        emailAddressView = (EditText)solo.getCurrentActivity().findViewById(R.id.email_address);
        walletPasswordView = (EditText)solo.getCurrentActivity().findViewById(R.id.wallet_pass);
        walletPasswordConfirmView = (EditText)solo.getCurrentActivity().findViewById(R.id.wallet_pass_confrirm);
    }

    public void testA_InvalidEmail()  throws AssertionError{
        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = "abcdefg";
        String pw = "Abaiyus75v!*^%!";
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForText(solo.getCurrentActivity().getString(R.string.invalid_email)));
    }

    public void testB_MismatchedPassword()  throws AssertionError{
        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = "qatest@qatest.com";
        String pw1 = "Abaiyus75v!*^%!";
        String pw2 = "Aba5v!*^%!12345";
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw1);
        solo.enterText(walletPasswordConfirmView, pw2);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForText(solo.getCurrentActivity().getString(R.string.password_mismatch_error)));
    }

    public void testC_PasswordStrengthIndicator()  throws AssertionError{
        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        ArrayList<String> strengthsSet = new ArrayList<>();
        strengthsSet.add(0, solo.getString(R.string.strength_weak));
        strengthsSet.add(1, solo.getString(R.string.strength_medium));
        strengthsSet.add(2, solo.getString(R.string.strength_strong));
        strengthsSet.add(3, solo.getString(R.string.strength_very_strong));

        for (int i = 0; i < strengthsSet.size(); i++) {

            solo.clearEditText(walletPasswordView);

            //Enter details
            String pw = null;
            switch (i){
                case 0: pw = "weakweak";break;
                case 1: pw = "RegularR";break;
                case 2: pw = "Str0ngPass80";break;
                case 3: pw = "!vErY##stR0ng?PassW32*%";break;
            }
            solo.enterText(walletPasswordView, pw);

            //Test result
            TestCase.assertEquals("Password: '"+pw+"' strength not equal to: '"+strengthsSet.get(i)+"'", true, solo.waitForText(strengthsSet.get(i)));
        }
    }

    public void testD_TermsOfServiceLink()  throws AssertionError{
        navigateToCreate();
        solo.clickOnView(solo.getView(R.id.tos));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PolicyActivity.class));

        solo.goBack();
        solo.goBack();
        solo.goBack();
    }

    public void testE_DefineNewPasswordModal()  throws AssertionError{
        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = "qatest@qatest.com";
        String pw = "aaaaaaaaa";
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        TestCase.assertEquals(true, solo.waitForDialogToOpen());
        solo.clickOnView(solo.getView(android.R.id.button1));//Yes
        TestCase.assertEquals(true, solo.waitForDialogToClose());

        //Test result
        TestCase.assertEquals(true, walletPasswordView.getText().toString().isEmpty());


        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        solo.waitForDialogToOpen();
        solo.clickOnView(solo.getView(android.R.id.button2));//No
        solo.waitForDialogToClose();

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));

        try{solo.sleep(2000);}catch (Exception e){}
        UiUtil.getInstance(getActivity()).wipeWallet();
    }

    public void testF_CreateValidWallet()  throws AssertionError{

        navigateToCreate();
        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = "qatest@qatest.com";
        String pw = "!vErY##stR0ng?PassW32*%";
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
//        TestCase.assertEquals(true, solo.waitForText(getActivity().getString(R.string.create_pin)));

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
    }

    public void testG_ConfirmationCodeForgetWallet() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {

        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));

        solo.clickOnText(getActivity().getString(R.string.wipe_wallet));
        try{solo.sleep(1000);}catch (Exception e){}

        //Test wiped
        TestCase.assertTrue(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_GUID, "").isEmpty());
        TestCase.assertTrue(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty());
    }
}
