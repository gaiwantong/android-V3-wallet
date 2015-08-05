package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import info.blockchain.wallet.LandingActivity;
import piuk.blockchain.android.R;

public class PairingTest extends ActivityInstrumentationTestCase2<LandingActivity> {

    private Solo solo = null;

    public PairingTest() {
        super(LandingActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());

        //Navigate to create wallet
        solo.clickOnView(solo.getView(R.id.login));
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

//    public void testFlashLight()  throws AssertionError{
//
//        //Observe manually - no assertion test
//
//        solo.clickOnView(solo.getView(R.id.command_scan));
//        try{solo.wait(1000);}catch (Exception e){}
//
//        //On
//        solo.clickOnView(solo.getView(R.id.action_flash_light));
//        try{solo.wait(1000);}catch (Exception e){}
//
//        //Off
//        solo.clickOnView(solo.getView(R.id.action_flash_light));
//        try{solo.wait(1000);}catch (Exception e){}
//
//        //On
//        solo.clickOnView(solo.getView(R.id.action_flash_light));
//        try{solo.wait(1000);}catch (Exception e){}
//
//        //Off
//        solo.clickOnView(solo.getView(R.id.action_flash_light));
//        try{solo.wait(1000);}catch (Exception e){}
//    }
//
//    public void testBackButton()  throws AssertionError{
//
//        solo.goBack();
//        TestCase.assertEquals(true, solo.waitForActivity(LandingActivity.class));
//    }
//
//    public void testManuallyV2()  throws AssertionError{
//
//        solo.clickOnView(solo.getView(R.id.command_manual));
//
//        solo.enterText(solo.getEditText(R.id.wallet_id), "694af790-7af2-46cb-b748-f1078b440c59");
//        solo.enterText(solo.getEditText(R.id.wallet_pass),"!Very>Str0ng?Pass80*%");
//
//        solo.clickOnView(solo.getView(R.id.command_next));
//    }
//
//    public void testManuallyV3()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV2()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV2_2nd_password()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV2_2fa()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV2_2nd_password_and_2fa()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV3()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV3_2nd_password()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV3_2fa()  throws AssertionError{
//
//    }
//
//    public void testAutomaticallyV3_2nd_password_and_2fa()  throws AssertionError{
//
//    }
}
