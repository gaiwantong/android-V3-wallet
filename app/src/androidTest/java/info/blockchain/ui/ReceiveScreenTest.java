package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import piuk.blockchain.android.R;

public class ReceiveScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private static boolean loggedIn = false;

    public ReceiveScreenTest() {
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

        solo.clickOnView(solo.getView(R.id.btActivateBottomSheet));
        try{solo.sleep(500);}catch (Exception e){}
        solo.clickOnText(getActivity().getString(R.string.receive_bitcoin));
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    private void exitApp(){
        solo.goBack();
        solo.goBack();
        solo.goBack();
    }

    public void testA_SelectToAccounts() throws AssertionError{

        Spinner mSpinner = solo.getView(Spinner.class, 0);
        int itemCount = mSpinner.getAdapter().getCount();
        View spinnerView = solo.getView(Spinner.class, 0);

        for(int i = 0; i < itemCount; i++){

            solo.clickOnView(spinnerView);
            solo.scrollToTop();
            solo.clickOnView(solo.getView(TextView.class, i));
            try{solo.sleep(500);}catch (Exception e){}

            //Test copy address from text
            EditText receivingAddress = (EditText)solo.getView(R.id.receiving_address);
            solo.clickLongOnView(receivingAddress);

            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
            clipboard.setText(receivingAddress.getText().toString());

            assertTrue("Copy to clipboard fail",getClipboardText().toString().equals(receivingAddress.getText().toString()));

            //Test copy address from qr
            ImageView qr = (ImageView)solo.getView(R.id.qr);
            solo.clickOnView(qr);
            solo.waitForText(getActivity().getString(R.string.receive_address_to_share),1,500);
            solo.clickOnText(getActivity().getString(R.string.yes));

            assertTrue("Copy to clipboard fail", getClipboardText().toString().equals(receivingAddress.getText().toString()));

        }
    }

    private String getClipboardText(){
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager)getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
        return clipboard.getText().toString();
    }
}
