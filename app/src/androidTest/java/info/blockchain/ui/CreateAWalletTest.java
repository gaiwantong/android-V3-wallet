package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import junit.framework.TestCase;

import java.util.ArrayList;

import info.blockchain.wallet.LandingActivity;
import info.blockchain.wallet.PinEntryActivity;
import info.blockchain.wallet.PolicyActivity;
import piuk.blockchain.android.R;

public class CreateAWalletTest extends ActivityInstrumentationTestCase2<LandingActivity> {

    private Solo solo = null;

    private EditText emailAddressView;
    private EditText walletPasswordView;
    private EditText walletPasswordConfirmView;

    public CreateAWalletTest() {
        super(LandingActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());

        //Navigate to create wallet
        solo.clickOnView(solo.getView(R.id.create));

        //Set up views
        emailAddressView = (EditText)solo.getView(R.id.email_address);
        walletPasswordView = (EditText)solo.getView(R.id.wallet_pass);
        walletPasswordConfirmView = (EditText)solo.getView(R.id.wallet_pass_confrirm);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testInvalidEmail()  throws AssertionError{

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

    public void testMismatchedPassword()  throws AssertionError{

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

    public void testPasswordStrengthIndicator()  throws AssertionError{

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
                case 3: pw = "!Very>Str0ng?Pass80*%";break;
            }
            solo.enterText(walletPasswordView, pw);

            //Test result
            TestCase.assertEquals("Password: '"+pw+"' strength not equal to: '"+strengthsSet.get(i)+"'", true, solo.waitForText(strengthsSet.get(i)));
        }
    }

    public void testBlockchainTermsOfServiceLink()  throws AssertionError{

        //First hide keyboard
        solo.pressSoftKeyboardNextButton();//skip email
        solo.pressSoftKeyboardNextButton();//skip pw1
        solo.pressSoftKeyboardNextButton();//skip pw2

        solo.clickOnView(solo.getView(R.id.tos));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PolicyActivity.class));

    }

    public void testDefineNewPasswordModal()  throws AssertionError{

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

        solo.waitForDialogToOpen();
        solo.clickOnView(solo.getView(android.R.id.button1));//Yes
        solo.waitForDialogToClose();

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
    }

    public void testCreateWallet()  throws AssertionError{

        //Clear text fields
        solo.clearEditText(emailAddressView);
        solo.clearEditText(walletPasswordView);
        solo.clearEditText(walletPasswordConfirmView);

        //Enter details
        String email = "qatest@qatest.com";
        String pw = "!Very>Str0ng?Pass80*%";
        solo.enterText(emailAddressView, email);
        solo.enterText(walletPasswordView, pw);
        solo.enterText(walletPasswordConfirmView, pw);

        //Complete
        solo.clickOnView(solo.getView(R.id.command_next));

        //Test result
        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));
    }
}
