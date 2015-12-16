package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;

import piuk.blockchain.android.R;

/**
 * Created by riaanvos on 18/09/15.
 */
public class UnpairWalletTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static boolean loggedIn = false;
    private Solo solo = null;

    public UnpairWalletTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {

        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());

        if (!loggedIn) {
            UiUtil.getInstance(getActivity()).enterPin(solo, solo.getString(R.string.qa_test_pin1));
            try {
                solo.sleep(4000);
            } catch (Exception e) {
            }
            loggedIn = true;
        }

        UiUtil.getInstance(getActivity()).openNavigationDrawer(solo);
        try {
            solo.sleep(500);
        } catch (Exception e) {
        }
        solo.clickOnText(getActivity().getString(R.string.unpair_wallet));
        try {
            solo.sleep(500);
        } catch (Exception e) {
        }
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testA_UnpairCancel() throws AssertionError {

        solo.clickOnText(getActivity().getString(R.string.dialog_cancel));
        assertTrue(solo.waitForDialogToClose());
    }

    public void testB_UnpairConfirm() throws AssertionError {

        solo.clickOnText(getActivity().getString(R.string.unpair));
        assertTrue(solo.waitForText(getActivity().getString(R.string.CREATE)));
    }
}