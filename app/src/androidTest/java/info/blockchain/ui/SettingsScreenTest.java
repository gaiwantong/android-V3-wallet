package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class SettingsScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private static boolean loggedIn = false;

    public SettingsScreenTest() {
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
        solo.clickOnText(getActivity().getString(R.string.action_settings));
        try{solo.sleep(500);}catch (Exception e){}
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testA_CopyGUI() throws AssertionError{
        solo.clickOnText(getActivity().getString(R.string.options_id));
        solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 200);
        solo.clickOnText(getActivity().getString(R.string.no));

        solo.clickOnText(getActivity().getString(R.string.options_id));
        solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 200);
        solo.clickOnText(getActivity().getString(R.string.yes));

        assertTrue(UiUtil.getInstance(getActivity()).getClipboardText().equals(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_GUID, "")));
    }

    public void testB_SelectBTCUnits() throws AssertionError{

        PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, 0);
        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();

        for(CharSequence unit : units) {

            if(solo.searchText(getActivity().getString(R.string.options_units)))
                solo.clickOnText(getActivity().getString(R.string.options_units));

            solo.waitForText(getActivity().getString(R.string.select_units), 1, 500);
            solo.clickOnText(unit.toString());
            try{solo.sleep(500);}catch (Exception e){}
            int sel = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, 0);
            assertTrue("Found: '"+units[sel]+"' - Expected: '"+unit+"'",units[sel].equals(unit));
        }
    }
}
