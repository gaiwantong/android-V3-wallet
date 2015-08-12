package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.PrefsUtil;

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
        HD_WalletFactory.getInstance(solo.getCurrentActivity()).set(null);
        PayloadFactory.getInstance().wipe();
        PrefsUtil.getInstance(solo.getCurrentActivity()).clear();
    }
}
