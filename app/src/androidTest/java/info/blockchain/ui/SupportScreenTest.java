package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class SupportScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private static boolean loggedIn = false;

    public SupportScreenTest() {
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
        solo.clickOnText(getActivity().getString(R.string.support));
        try{solo.sleep(500);}catch (Exception e){}
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    @FlakyTest(tolerance = 2)
    public void testA_CopyGUI() throws AssertionError{

        solo.clickOnText(getActivity().getString(R.string.my_wallet_id));
        solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 200);
        solo.clickOnText(getActivity().getString(R.string.no));

        solo.clickOnText(getActivity().getString(R.string.my_wallet_id));
        solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 200);
        solo.clickOnText(getActivity().getString(R.string.yes));

        assertTrue(UiUtil.getInstance(getActivity()).getClipboardText().equals(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_GUID, "")));

        UiUtil.getInstance(getActivity()).exitApp(solo);
    }

}
