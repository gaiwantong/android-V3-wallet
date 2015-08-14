package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.PinEntryActivity;
import piuk.blockchain.android.R;

public class PairValidWallet extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    public PairValidWallet() {
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

    public void testPairValidWallet()  throws AssertionError{

        solo.clickOnView(solo.getView(R.id.login));
        solo.clickOnView(solo.getView(R.id.command_manual));
        try{solo.sleep(1000);}catch (Exception e){}
        solo.enterText((EditText)solo.getView(R.id.wallet_id), solo.getString(R.string.qa_test_guid1));
        solo.enterText((EditText) solo.getView(R.id.wallet_pass), solo.getString(R.string.qa_test_password1));

        solo.clickOnText(solo.getString(R.string.dialog_continue));

        //Confirm email needs to be done manually by tester - so we'll make a sound to alert tester
        UiUtil.getInstance(getActivity()).soundAlert();

        solo.waitForActivity(PinEntryActivity.class, 120000);
        try{solo.sleep(2000);}catch (Exception e){}
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        try{solo.sleep(1000);}catch (Exception e){}
        UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
        try{solo.sleep(4000);}catch (Exception e){}
    }
}
