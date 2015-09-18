package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;
import android.widget.TextView;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.util.AppUtil;
import piuk.blockchain.android.R;

public class BackupWalletTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private static boolean loggedIn = false;

    public BackupWalletTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {

        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());

        if(!loggedIn){
            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
            try{solo.sleep(4000);}catch (Exception e){}
            loggedIn = true;
        }

        UiUtil.getInstance(getActivity()).openNavigationDrawer(solo);
        try{solo.sleep(500);}catch (Exception e){}
        solo.clickOnText(getActivity().getString(R.string.backup_wallet));
        try{solo.sleep(500);}catch (Exception e){}
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testA_Reveal() throws AssertionError {
        if(AppUtil.getInstance(getActivity()).isLegacy())assertTrue(true);

        solo.clickOnText(getActivity().getString(R.string.BACKUP_WALLET));
        solo.clickOnText(getActivity().getString(R.string.START));

        TextView currentWordTv = (TextView) solo.getView(R.id.tv_current_word);
        TextView hiddenTv = (TextView)solo.getView(R.id.tv_press_reveal);

        for(int i = 0; i < 11; i ++){

            //Test word count increments
            int c = Integer.parseInt(currentWordTv.getText().toString().split(" ")[1]);
            assertTrue(c == (i+1));

            //Test word reveals
            solo.clickLongOnView(solo.getView(R.id.card_layout));
            assertTrue(solo.waitForText(hiddenTv.getText().toString()));

            solo.clickOnText(getActivity().getString(R.string.NEXT_WORD));
            try{solo.sleep(500);}catch (Exception e){}
        }

        for(int i = 11; i > 0; i --){

            solo.clickOnScreen(50f,50f);
            try{solo.sleep(500);}catch (Exception e){}

            //Test word count increments
            int c = Integer.parseInt(currentWordTv.getText().toString().split(" ")[1]);
            assertTrue(c + " != " + i, c == i);

            //Test word reveals
            solo.clickLongOnView(solo.getView(R.id.card_layout));
            assertTrue(solo.waitForText(hiddenTv.getText().toString()));
        }
    }

    public void testB_IncorrectMnemonic() throws AssertionError {
        if(AppUtil.getInstance(getActivity()).isLegacy())assertTrue(true);

        solo.clickOnText(getActivity().getString(R.string.BACKUP_WALLET));
        solo.clickOnText(getActivity().getString(R.string.START));

        for(int i = 0; i < 11; i ++)
            solo.clickOnText(getActivity().getString(R.string.NEXT_WORD));

        solo.clickOnText(getActivity().getString(R.string.VERIFY));
        try{solo.sleep(500);}catch (Exception e){}

        solo.enterText((EditText) solo.getView(R.id.etFirstRequest),"aaaa");
        solo.enterText((EditText) solo.getView(R.id.etSecondRequest),"aaaa");
        solo.enterText((EditText) solo.getView(R.id.etThirdRequest),"aaaa");

        solo.clickOnText(getActivity().getString(R.string.VERIFY_BACKUP));

        assertTrue(solo.waitForText(getActivity().getString(R.string.backup_word_mismatch)));
    }
}
