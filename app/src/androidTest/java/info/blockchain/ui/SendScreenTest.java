package info.blockchain.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.robotium.solo.Solo;

import info.blockchain.ui.util.UiUtil;
import info.blockchain.wallet.HDPayloadBridge;
import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.util.AccountsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import piuk.blockchain.android.R;

public class SendScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo = null;

    private static boolean loggedIn = false;

    public SendScreenTest() {
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
        solo.clickOnText(getActivity().getString(R.string.send_bitcoin));
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

    public void testA_SelectFromAccounts() throws AssertionError{

        Spinner mSpinner = solo.getView(Spinner.class, 0);
        int itemCount = mSpinner.getAdapter().getCount();
        View spinnerView = solo.getView(Spinner.class, 0);

        for(int i = 0; i < itemCount; i++){

            solo.clickOnView(spinnerView);
            solo.scrollToTop();
            TextView tvMax = (TextView)solo.getView(LinearLayout.class, i).findViewById(R.id.receive_account_balance);
            solo.clickOnView(solo.getView(LinearLayout.class, i));
            try{solo.sleep(500);}catch (Exception e){}

            //Test if amount available correct
            long amount = 0L;

            //Index of last hd account in spinner
            int hdAccountsIdx = AccountsUtil.getInstance(getActivity()).getLastHDIndex();

            //Legacy addresses
            if(i >= hdAccountsIdx) {
                amount = MultiAddrFactory.getInstance().getLegacyBalance(AccountsUtil.getInstance(getActivity()).getLegacyAddress(i - hdAccountsIdx).getAddress());
            }
            else {
                //HD Accounts
                String xpub = account2Xpub(i);
                if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                }
                else {
                    amount = 0L;
                }
            }

            String expectedTextTotal = "0.0";
            String expectedTextAvailable = getActivity().getResources().getString(R.string.no_funds_available);
            long amount_available = amount - SendFactory.bFee.longValue();
            long amount_total = amount;

            if(amount_available > 0L) {
                double btc_balance = (((double)amount_available) / 1e8);
                expectedTextAvailable = getActivity().getResources().getText(R.string.max_available) + " " + MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_balance));
            }

            if(amount_total > 0L) {
                double btc_total = (((double)amount_total) / 1e8);
                expectedTextTotal = MonetaryUtil.getInstance().getBTCFormat().format(MonetaryUtil.getInstance(getActivity()).getDenominatedAmount(btc_total));
            }


            String spinnerTextTotal = tvMax.getText().toString();
            spinnerTextTotal = spinnerTextTotal.replace("(", "").trim();
            spinnerTextTotal = spinnerTextTotal.replace(")", "").trim();
            CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
            for(CharSequence unit : units){
                spinnerTextTotal = spinnerTextTotal.replace(unit.toString(), "").trim();
            }

            TextView tvMax2 = (TextView)solo.getCurrentActivity().findViewById(R.id.max);
            String available = tvMax2.getText().toString();
            for(CharSequence unit : units)
                available = available.replace(unit.toString(), "").trim();

            //Test if spinner totals matches actual account total
            assertTrue(expectedTextTotal.equals(spinnerTextTotal));

            //Test if max available matches actual available for account (total - fee)
            assertTrue(expectedTextAvailable.equals(available));
        }
    }

    private String account2Xpub(int sel) {

        Account hda = AccountsUtil.getInstance(getActivity()).getSendReceiveAccountMap().get(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        String xpub = null;
        if(hda instanceof ImportedAccount) {
            xpub = null;
        }
        else {
            xpub = HDPayloadBridge.getInstance(getActivity()).account2Xpub(AccountsUtil.getInstance(getActivity()).getSendReceiveAccountIndexResolver().get(sel));
        }

        return xpub;
    }

    public void testB_InputToAddressManually() throws AssertionError{

        smartAccountSelect();

        EditText toAddress = (EditText)solo.getView(R.id.destination);
        solo.enterText(toAddress, "");
        solo.enterText(toAddress, getActivity().getString(R.string.qa_test_address_1));

        EditText amount1 = (EditText)solo.getView(R.id.amount1);
        solo.enterText(amount1, "0.0001");
        solo.clickOnView(solo.getView(R.id.action_send));

        assertTrue("Ensure wallet has sufficient funds!", solo.waitForText(getActivity().getString(R.string.confirm_details),1,500));
        solo.goBack();

    }

    public void testC_InputToAddressFromDropdown() throws AssertionError{

        Spinner mSpinner = solo.getView(Spinner.class, 1);
        int itemCount = mSpinner.getAdapter().getCount();
        View spinnerView = solo.getView(Spinner.class, 1);

        String prevAddress = null;
        for(int i = 0; i < itemCount; i++){

            solo.clickOnView(spinnerView);
            solo.scrollToTop();
            solo.clickOnView(solo.getView(LinearLayout.class, i));
            try{solo.sleep(500);}catch (Exception e){}

            EditText destination = (EditText)solo.getView(R.id.destination);
            String selectedAddress = destination.getText().toString();

            //Test destination address is not empty, and ensure it changes for each account selected
            if(prevAddress!=null)
                assertTrue("Address not populated or address same for different accounts.", !selectedAddress.isEmpty() && !prevAddress.equals(selectedAddress));

            prevAddress = selectedAddress;
        }

    }

    public void testD_InputInvalidAddress() throws AssertionError{

        EditText toAddress = (EditText)solo.getView(R.id.destination);
        solo.enterText(toAddress, "aaaaaaaaaaaaaaaaaaaa");

        EditText amount1 = (EditText)solo.getView(R.id.amount1);
        solo.enterText(amount1, "0.0001");
        solo.clickOnView(solo.getView(R.id.action_send));

        assertTrue("Ensure wallet has sufficient funds!", solo.waitForText(getActivity().getString(R.string.invalid_bitcoin_address), 1, 500));
        solo.goBack();

    }

    public void testE_Conversion() throws AssertionError{

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

    public void testF_SendMoreThanAvailable() throws AssertionError{

        smartAccountSelect();

        TextView maxTv = (TextView)solo.getView(R.id.max);
        String max = maxTv.getText().toString().replace(getActivity().getResources().getText(R.string.max_available) + " ", "");
        CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        for(CharSequence unit : units)
            max = max.replace(unit.toString(), "").trim();

        EditText amount1 = (EditText)solo.getView(R.id.amount1);
        solo.enterText(amount1, "1" + max);
        solo.clickOnView(solo.getView(R.id.action_send));

        assertTrue("Expected a 'Insufficient funds' toast.", solo.waitForText(getActivity().getString(R.string.insufficient_funds), 1, 500));
    }

    public void testF_SendLessThanAvailable() throws AssertionError{

        smartAccountSelect();

        EditText amount1 = (EditText)solo.getView(R.id.amount1);
        solo.enterText(amount1, (SendFactory.bFee.longValue() / 1e8) + "");
        solo.clickOnView(solo.getView(R.id.action_send));

        assertTrue("Expected a '" + getActivity().getString(R.string.confirm_details) + "' dialog.", solo.waitForText(getActivity().getString(R.string.confirm_details), 1, 500));

        solo.clickOnText(getActivity().getString(R.string.SEND));

        assertTrue("Expected a '" + getActivity().getString(R.string.transaction_submitted) + "' toast.", solo.waitForText(getActivity().getString(R.string.transaction_submitted), 1, 5000));

    }


    public void testG_ConfirmDetails() throws AssertionError{

        smartAccountSelect();

        TextView maxTv = (TextView) solo.getView(R.id.max);
        String max = maxTv.getText().toString().replace(getActivity().getResources().getText(R.string.max_available) + " ", "");
        CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        for (CharSequence unit : units)
            max = max.replace(unit.toString(), "").trim();

        EditText amount1 = (EditText) solo.getView(R.id.amount1);
        solo.enterText(amount1, "");
        solo.enterText(amount1, max);
        solo.clickOnView(solo.getView(R.id.action_send));
        solo.waitForText(getActivity().getString(R.string.confirm_details));

        TextView totalToSend = (TextView) solo.getView(R.id.confirm_total_to_send);
        double totalToSendD = Double.parseDouble(totalToSend.getText().toString().split(" ")[0]);
        TextView confirm_fee = (TextView) solo.getView(R.id.confirm_fee);
        double confirm_feeD = Double.parseDouble(confirm_fee.getText().toString().split(" ")[0]);
        double amount1D = Double.parseDouble(max);

        EditText destination = (EditText) solo.getView(R.id.destination);
        TextView confirm_to = (TextView) solo.getView(R.id.confirm_to);

        assertTrue("Expected a '" + getActivity().getString(R.string.confirm_details) + "' dialog.", solo.waitForText(getActivity().getString(R.string.confirm_details), 1, 1000));

        assertTrue("Destination address doesn't match confirm modal - Expected '"+destination.getText().toString()+"' - Got '"+confirm_to.getText().toString()+"'", destination.getText().toString().equals(confirm_to.getText().toString()));

        assertTrue("Total to Send not equal to input - Expected '"+totalToSendD+"' - Got '"+(confirm_feeD + amount1D)+"'", totalToSendD == (confirm_feeD + amount1D));

        solo.clickOnText(getActivity().getString(R.string.dialog_cancel));

    }

    public void testH_SendAllAvailable() throws AssertionError{

        smartAccountSelect();

        TextView maxTv = (TextView)solo.getView(R.id.max);
        String max = maxTv.getText().toString().replace(getActivity().getResources().getText(R.string.max_available) + " ", "");
        CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        for(CharSequence unit : units)
            max = max.replace(unit.toString(), "").trim();

        EditText amount1 = (EditText)solo.getView(R.id.amount1);
        solo.enterText(amount1, max);
        solo.clickOnView(solo.getView(R.id.action_send));

        assertTrue("Expected a '" + getActivity().getString(R.string.confirm_details) + "' dialog.", solo.waitForText(getActivity().getString(R.string.confirm_details), 1, 500));

        solo.clickOnText(getActivity().getString(R.string.SEND));

        assertTrue("Expected a '" + getActivity().getString(R.string.transaction_submitted) + "' toast.", solo.waitForText(getActivity().getString(R.string.transaction_submitted), 1, 6000));

    }

    public void testI_EnterInvalidCharacters() throws AssertionError{

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

    private void smartAccountSelect(){

        Spinner mSpinner = solo.getView(Spinner.class, 0);
        int itemCount = mSpinner.getAdapter().getCount();
        View spinnerView = solo.getView(Spinner.class, 0);

        int highestAccountIndex = 0;
        int smallestAccountIndex = 0;
        double highestAccountBalance = -1;
        double smallestAccountBalance = -1;
        for(int i = 0; i < itemCount; i++){

            solo.clickOnView(spinnerView);
            solo.scrollToTop();
            TextView tvMax = (TextView)solo.getView(LinearLayout.class, i).findViewById(R.id.receive_account_balance);
            solo.clickOnView(solo.getView(LinearLayout.class, i));
            try{solo.sleep(500);}catch (Exception e){}

            String spinnerTextTotal = tvMax.getText().toString();
            spinnerTextTotal = spinnerTextTotal.replace("(", "").trim();
            spinnerTextTotal = spinnerTextTotal.replace(")", "").trim();
            CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
            for(CharSequence unit : units){
                spinnerTextTotal = spinnerTextTotal.replace(unit.toString(), "").trim();
            }

            double available = Double.parseDouble(spinnerTextTotal);

            if(highestAccountBalance==-1 || highestAccountBalance < available) {
                highestAccountIndex = i;
                highestAccountBalance = available;
            }

            if(smallestAccountBalance==-1 || smallestAccountBalance > available) {
                smallestAccountIndex = i;
                smallestAccountBalance = available;
            }
        }

        //Select from account with highest balance
        solo.clickOnView(spinnerView);
        solo.scrollToTop();
        solo.clickOnView(solo.getView(LinearLayout.class, highestAccountIndex));

        //Select destination account with smallest balance
        solo.clickOnView(solo.getView(Spinner.class, 1));
        solo.scrollToTop();
        solo.clickOnView(solo.getView(LinearLayout.class, smallestAccountIndex));
    }
}
