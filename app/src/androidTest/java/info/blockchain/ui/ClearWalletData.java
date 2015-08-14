package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;

public class ClearWalletData extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    public ClearWalletData() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    public void testClear() throws AssertionError{
        UiUtil.getInstance(solo.getCurrentActivity()).wipeWallet();
    }
}
