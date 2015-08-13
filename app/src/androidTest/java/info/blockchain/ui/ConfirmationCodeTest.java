package info.blockchain.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.google.bitcoin.crypto.MnemonicException;
import com.robotium.solo.Solo;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
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

    public void enterPin() throws AssertionError{

        String pin = solo.getString(R.string.qa_test_pin1);

        ArrayList<Integer> pinSequence = new ArrayList<>();
        pinSequence.add(Integer.parseInt(pin.substring(0, 1)));
        pinSequence.add(Integer.parseInt(pin.substring(1, 2)));
        pinSequence.add(Integer.parseInt(pin.substring(2, 3)));
        pinSequence.add(Integer.parseInt(pin.substring(3, 4)));

        for(int i : pinSequence){

            switch (i){
                case 0:solo.clickOnView(solo.getView(R.id.button0));break;
                case 1:solo.clickOnView(solo.getView(R.id.button1));break;
                case 2:solo.clickOnView(solo.getView(R.id.button2));break;
                case 3:solo.clickOnView(solo.getView(R.id.button3));break;
                case 4:solo.clickOnView(solo.getView(R.id.button4));break;
                case 5:solo.clickOnView(solo.getView(R.id.button5));break;
                case 6:solo.clickOnView(solo.getView(R.id.button6));break;
                case 7:solo.clickOnView(solo.getView(R.id.button7));break;
                case 8:solo.clickOnView(solo.getView(R.id.button8));break;
                case 9:solo.clickOnView(solo.getView(R.id.button9));break;
            }
            try{solo.sleep(500);}catch (Exception e){}
        }
    }

//    public void testA_CreateValidWallet()  throws AssertionError{
//        navigateToCreate();
//        //Clear text fields
//        solo.clearEditText(emailAddressView);
//        solo.clearEditText(walletPasswordView);
//        solo.clearEditText(walletPasswordConfirmView);
//
//        //Enter details
//        String email = solo.getString(R.string.qa_test_email);
//        String pw = solo.getString(R.string.qa_test_password1);
//        solo.enterText(emailAddressView, email);
//        solo.enterText(walletPasswordView, pw);
//        solo.enterText(walletPasswordConfirmView, pw);
//
//        //Complete
//        solo.clickOnView(solo.getView(R.id.command_next));
//
//        //Test result
//        TestCase.assertEquals(true, solo.waitForActivity(PinEntryActivity.class));
//
//        try{solo.sleep(3000);}catch (Exception e){}
//        enterPin();//create
//        try{solo.sleep(1000);}catch (Exception e){}
//        enterPin();//confirm
//        try{solo.sleep(4000);}catch (Exception e){}
//    }
//
//    public void testB_EnterIncorrectCode() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {
//
//        try{solo.sleep(1000);}catch (Exception e){}
//        enterPin();
//
//        solo.enterText((EditText) solo.getView(R.id.confirmBox0), "A");
//        solo.enterText((EditText) solo.getView(R.id.confirmBox1), "A");
//        solo.enterText((EditText) solo.getView(R.id.confirmBox2), "A");
//        solo.enterText((EditText) solo.getView(R.id.confirmBox3), "A");
//        solo.enterText((EditText) solo.getView(R.id.confirmBox4), "A");
//
//        TestCase.assertTrue(solo.waitForText("Invalid Response Email Verification Code Incorrect"));//message generated server side
//    }
//
//    public void testC_ResendEmail() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {
//
//        try{solo.sleep(1000);}catch (Exception e){}
//        enterPin();
//
//        solo.clickOnText(getActivity().getString(R.string.resend_email_confirmation));
//
//        //Confirm email received needs to be done manually by tester - so we'll make a sound to alert tester
//        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
//        if (audioManager!=null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
//            MediaPlayer mp;
//            mp = MediaPlayer.create(getActivity(), R.raw.alert);
//            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    mp.reset();
//                    mp.release();
//                }
//
//            });
//            mp.start();
//        }
//    }
//
//    public void testD_ForgetWallet() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {
//
//        try{solo.sleep(1000);}catch (Exception e){}
//        enterPin();
//
//        solo.clickOnText(getActivity().getString(R.string.wipe_wallet));
//        try{solo.sleep(1000);}catch (Exception e){}
//
//        //Test wiped
//        TestCase.assertTrue(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_GUID, "").isEmpty());
//        TestCase.assertTrue(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty());
//    }

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

        try{solo.sleep(3000);}catch (Exception e){}
        enterPin();//create
        try{solo.sleep(1000);}catch (Exception e){}
        enterPin();//confirm
        try{solo.sleep(4000);}catch (Exception e){}
    }

    public void testF_EnterCorrectCode() throws AssertionError, IOException, MnemonicException.MnemonicLengthException {

        try{solo.sleep(1000);}catch (Exception e){}
        enterPin();

        //Confirm email received needs to be done manually by tester - so we'll make a sound to alert tester
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager!=null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            MediaPlayer mp;
            mp = MediaPlayer.create(getActivity(), R.raw.alert);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mp.release();
                }

            });
            mp.start();
        }

        TestCase.assertTrue(solo.waitForText(getActivity().getString(R.string.my_bitcoin_wallet), 1, 240000));
    }
}
