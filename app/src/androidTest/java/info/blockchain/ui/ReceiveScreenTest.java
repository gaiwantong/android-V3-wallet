package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.util.PrefsUtil;
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

    public void testA_NavigateToReceive() throws AssertionError{

        assertTrue(solo.waitForText(getActivity().getString(R.string.receive_bitcoin)));
    }

    public void testB_SelectToAccounts() throws AssertionError{

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

    public void testC_Conversion() throws AssertionError{

        double allowedFluctuation = 0.01;//only 1% fluctuation allowed for volatility

        EditText btcEt = (EditText)solo.getView(R.id.amount1);
        EditText fiatEt = (EditText)solo.getView(R.id.amount2);

        double fiatTestAmount = 500.0;
        solo.enterText(fiatEt, fiatTestAmount+"");

        String fiatFormat = info.blockchain.wallet.util.PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT,"USD");

        //Test fiat converted to btc
        double expectedBtc = 0.0;
        try {
            expectedBtc = Double.parseDouble(info.blockchain.wallet.util.WebUtil.getInstance().getURL("https://blockchain.info/tobtc?currency=" + fiatFormat + "&value=" + fiatEt.getText().toString()));
            double displayedBtc = Double.parseDouble(btcEt.getText().toString());

            assertTrue("Expected btc= "+expectedBtc+", UI shows btc="+ displayedBtc +" ("+(allowedFluctuation*100)+"% fluctuation allowed.)",displayedBtc >= expectedBtc * (1.0-allowedFluctuation) && displayedBtc <= expectedBtc * (1.0+allowedFluctuation));

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Clear text
        solo.enterText(btcEt, "");

        //Test btc converted to fiat
        solo.enterText(btcEt, expectedBtc + "");
        double displayedFiat = Double.parseDouble(fiatEt.getText().toString());

        assertTrue("Expected fiat= " + fiatTestAmount + ", UI shows fiat=" + displayedFiat + " (" + (allowedFluctuation * 100) + "% fluctuation allowed.)", displayedFiat >= fiatTestAmount * (1.0 - allowedFluctuation) && displayedFiat <= fiatTestAmount * (1.0 + allowedFluctuation));

    }

    public void testD_EnterInvalidCharacters() throws AssertionError{

        final EditText btcEt = (EditText)solo.getView(R.id.amount1);
        final EditText fiatEt = (EditText)solo.getView(R.id.amount2);

        //BTC field
        //Test invalid chars
        solo.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btcEt.requestFocus();
            }
        });
        solo.sendKey(KeyEvent.KEYCODE_A);
        solo.sendKey(KeyEvent.KEYCODE_A);
        assertTrue(!btcEt.getText().toString().toLowerCase().contains("a"));
        solo.enterText(btcEt, "");

        //Test no multiple decimals excepted
        solo.sendKey(KeyEvent.KEYCODE_0);
        solo.sendKey(KeyEvent.KEYCODE_PERIOD);
        solo.sendKey(KeyEvent.KEYCODE_0);
        solo.sendKey(KeyEvent.KEYCODE_PERIOD);
        solo.sendKey(KeyEvent.KEYCODE_1);
        solo.sendKey(KeyEvent.KEYCODE_PERIOD);
        assertTrue(btcEt.getText().toString().equals("0.01"));
        solo.enterText(btcEt, "");


        //FIAT field
        //Test invalid chars
        solo.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fiatEt.requestFocus();
            }
        });
        solo.sendKey(KeyEvent.KEYCODE_A);
        solo.sendKey(KeyEvent.KEYCODE_A);
        assertTrue(!fiatEt.getText().toString().toLowerCase().contains("a"));
        solo.enterText(fiatEt, "");

        //Test no multiple decimals excepted
        solo.sendKey(KeyEvent.KEYCODE_0);
        solo.sendKey(KeyEvent.KEYCODE_PERIOD);
        solo.sendKey(KeyEvent.KEYCODE_0);
        solo.sendKey(KeyEvent.KEYCODE_PERIOD);
        solo.sendKey(KeyEvent.KEYCODE_1);
        solo.sendKey(KeyEvent.KEYCODE_PERIOD);
        assertTrue(fiatEt.getText().toString().equals("0.01"));
        solo.enterText(fiatEt, "");

        exitApp();
    }
}
