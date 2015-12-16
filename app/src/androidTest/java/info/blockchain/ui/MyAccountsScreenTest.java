package info.blockchain.ui;

import android.support.v7.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;

import piuk.blockchain.android.R;

public class MyAccountsScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static boolean loggedIn = false;
    private Solo solo = null;

    public MyAccountsScreenTest() {
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
        solo.clickOnText(getActivity().getString(R.string.my_accounts));
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    private float toPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getActivity().getResources().getDisplayMetrics());
    }

    public void testA_ContainsAccounts() throws AssertionError {

        final RecyclerView accList = (RecyclerView) solo.getView(R.id.accountsList);
        int itemCount = accList.getAdapter().getItemCount();

        float extendedBarHeight = toPx(124);
        float headerHeight = toPx(48);
        float rowHeight = toPx(72);
        float startY = (float) (rowHeight / 2.0);
        float yd = 0f;

        if (itemCount > 0) {

            for (int i = 0; i < itemCount; i++) {

                solo.clickOnScreen(50, extendedBarHeight + headerHeight + startY + yd);
                try {
                    solo.sleep(200);
                } catch (Exception e) {
                }

                ImageView qrView = (ImageView) solo.getView(R.id.qrr);
                if (qrView.getVisibility() == View.VISIBLE) {

                    solo.clickLongOnView(solo.getView(R.id.qrr));
                    solo.waitForText(getActivity().getString(R.string.receive_address_to_clipboard), 1, 100);
                    solo.clickOnText(getActivity().getString(R.string.yes));
                    assertTrue(solo.waitForText(getActivity().getString(R.string.copied_to_clipboard), 1, 100));

                }

                solo.clickOnScreen(50, extendedBarHeight + headerHeight + startY + yd);
                yd += rowHeight;
            }
        }
    }

    public void testB_OpenCamera() throws AssertionError {

        solo.clickOnView(solo.getView(R.id.menu_import));
        solo.clickOnText(getActivity().getString(R.string.import_address));

        assertTrue(solo.waitForText(getActivity().getString(R.string.scan_qr), 1, 500));
        solo.clickOnActionBarHomeButton();

        UiUtil.getInstance(getActivity()).exitApp(solo);
    }
}
