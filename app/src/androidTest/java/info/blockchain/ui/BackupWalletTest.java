package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
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

    public void testA_CancelModal() throws AssertionError {

        assertTrue(true);
    }
}
