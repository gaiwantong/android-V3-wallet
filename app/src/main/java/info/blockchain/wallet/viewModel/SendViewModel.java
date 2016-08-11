package info.blockchain.wallet.viewModel;

import android.content.Context;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;

import info.blockchain.api.DynamicFee;
import info.blockchain.api.Unspent;
import info.blockchain.util.FeeUtil;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.model.ItemAccount;
import info.blockchain.wallet.model.PaymentConfirmationDetails;
import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.model.SendModel;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.AddressBookEntry;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;
import info.blockchain.wallet.payment.data.SuggestedFee;
import info.blockchain.wallet.payment.data.SweepBundle;
import info.blockchain.wallet.payment.data.UnspentOutputs;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.view.helpers.SecondPasswordHandler;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.R;

public class SendViewModel implements ViewModel {

    private final String TAG = getClass().getSimpleName();

    private DataListener dataListener;
    private Context context;

    private PayloadManager payloadManager;
    private MonetaryUtil monetaryUtil;

    private Payment payment;
    public SendModel sendModel;

    public SendViewModel(SendModel sendModel, Context context, double exchangeRate, int btcUnit, String fiatUnit, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;
        this.payloadManager = PayloadManager.getInstance();
        this.monetaryUtil = new MonetaryUtil(btcUnit);
        this.payment = new Payment();

        this.sendModel = sendModel;
        this.sendModel.pendingTransaction = new PendingTransaction();
        this.sendModel.btcUnit = monetaryUtil.getBTCUnit(btcUnit);
        this.sendModel.fiatUnit = fiatUnit;
        this.sendModel.btcUniti = btcUnit;
        this.sendModel.defaultSeparator = getDefaultDecimalSeparator();
        this.sendModel.exchangeRate = exchangeRate;
        this.sendModel.unspentApiResponse = new HashMap<>();

        dataListener.onUpdateBtcUnit(this.sendModel.btcUnit);
        dataListener.onUpdateFiatUnit(this.sendModel.fiatUnit);
        getSuggestedFee();
    }

    @Override
    public void destroy() {
        context = null;
        dataListener = null;
    }

    public String getDefaultSeparator() {
        return sendModel.defaultSeparator;
    }

    public interface DataListener {
        void onHideSendingAddressField();
        void onHideReceivingAddressField();

        void onRemoveBtcTextChangeListener();
        void onRemoveFiatTextChangeListener();

        void onAddBtcTextChangeListener();
        void onAddFiatTextChangeListener();

        void onUpdateBtcAmount(String amount);
        void onUpdateFiatAmount(String amount);
        void onUpdateBtcUnit(String unit);
        void onUpdateFiatUnit(String unit);

        void onSetBtcUnit(int unitBtc);
        void onSetSpendAllAmount(String textFromSatoshis);

        void onShowInvalidAmount();
        void onShowSpendFromWatchOnly(String address);
        void onShowPaymentDetails(PaymentConfirmationDetails confirmationDetails, boolean isLargeTransaction);
        void onShowReceiveToWatchOnlyWarning(String address);
        void onShowAlterFee(String absoluteFeeSuggested,String body, int positiveAction, int negativeAction);
        void onShowErrorMessage(String message);
        void onShowTransactionSuccess();
        void onShowBIP38PassphrasePrompt(String scanData);
    }

    public int getDefaultAccount(){

        int result = 0;
        if (payloadManager.getPayload().isUpgraded()) {
            result = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        }
        return Math.max(result,0);
    }

    public List<ItemAccount> getAddressList(boolean includeAddressBookEntries) {

        ArrayList<ItemAccount> result = new ArrayList<>();

        //V3
        if (payloadManager.getPayload().isUpgraded()) {

            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
            for (Account account : accounts) {

                if (account.isArchived())
                    continue;//skip archived account

                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(account.getXpub())) {
                    long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(account.getXpub());
                    String balance = "(" + monetaryUtil.getDisplayAmount(amount) + " " + sendModel.btcUnit + ")";
                    result.add(new ItemAccount(account.getLabel(), balance, null, account));
                }
            }
        }

        //V2
        List<LegacyAddress> legacyAddresses = payloadManager.getPayload().getLegacyAddresses();
        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)
                continue;//skip archived

            //If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel();
            if(labelOrAddress == null || labelOrAddress.trim().isEmpty()){
                labelOrAddress = legacyAddress.getAddress();
            }

            //Watch-only tag - we'll ask for xpriv scan when spending from
            String tag = null;
            if(legacyAddress.isWatchOnly()){
                tag = context.getResources().getString(R.string.watch_only);
            }

            long amount = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
            String balance = "(" + monetaryUtil.getDisplayAmount(amount) + " " + sendModel.btcUnit + ")";
            result.add(new ItemAccount(labelOrAddress, balance, tag, legacyAddress));
        }

        if(result.size() == 1){
            //Only a single account/address available in wallet
            dataListener.onHideSendingAddressField();
        }

        //Address Book (only included in receiving)
        if(includeAddressBookEntries) {
            List<AddressBookEntry> addressBookEntries = payloadManager.getPayload().getAddressBookEntries();
            for (AddressBookEntry addressBookEntry : addressBookEntries) {

                //If address has no label, we'll display address
                String labelOrAddress = addressBookEntry.getLabel() == null || addressBookEntry.getLabel().length() == 0 ? addressBookEntry.getAddress() : addressBookEntry.getLabel();

                result.add(new ItemAccount(labelOrAddress, "", context.getResources().getString(R.string.address_book_label), addressBookEntry));
            }
        }

        if (result.size() == 1) {
            //Only a single account/address available in wallet and no addressBook entries
            dataListener.onHideReceivingAddressField();
        }

        return result;
    }

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    public void checkMaximumBTCAmount(String btc) {
        long lamount = 0L;
        try {
            //Long is safe to use, but double can lead to ugly rounding issues..
            Double btcDouble = Double.parseDouble(btc);
            double undenominatedAmount = monetaryUtil.getUndenominatedAmount(btcDouble);
            lamount = (BigDecimal.valueOf(undenominatedAmount).multiply(BigDecimal.valueOf(100000000)).longValue());

            if (BigInteger.valueOf(lamount).compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
                dataListener.onShowInvalidAmount();
                return;
            }
        } catch (NumberFormatException nfe) {
            ;
        }
    }

    public void afterBtcTextChanged(String btcAmountText) {

        checkMaximumBTCAmount(btcAmountText);

        dataListener.onRemoveBtcTextChangeListener();

        int max_len;
        NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
        switch (sendModel.btcUniti) {
            case MonetaryUtil.MICRO_BTC:
                max_len = 2;
                break;
            case MonetaryUtil.MILLI_BTC:
                max_len = 4;
                break;
            default:
                max_len = 8;
                break;
        }
        btcFormat.setMaximumFractionDigits(max_len + 1);
        btcFormat.setMinimumFractionDigits(0);

        try {
            if (btcAmountText.indexOf(sendModel.defaultSeparator) != -1) {
                String dec = btcAmountText.substring(btcAmountText.indexOf(sendModel.defaultSeparator));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > max_len) {
                        dataListener.onUpdateBtcAmount(btcAmountText.substring(0, btcAmountText.length() - 1));
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            ;
        }

        dataListener.onAddBtcTextChangeListener();

        if (sendModel.textChangeAllowed) {
            sendModel.textChangeAllowed = false;

            if(btcAmountText.isEmpty())btcAmountText = "0";
            double btc_amount;
            try {
                btc_amount = monetaryUtil.getUndenominatedAmount(NumberFormat.getInstance(Locale.getDefault()).parse(btcAmountText).doubleValue());
            } catch (NumberFormatException nfe) {
                btc_amount = 0.0;
            } catch (ParseException pe) {
                btc_amount = 0.0;
            }

            double fiat_amount = sendModel.exchangeRate * btc_amount;
            dataListener.onUpdateFiatAmount(monetaryUtil.getFiatFormat(sendModel.fiatUnit).format(fiat_amount));

            sendModel.textChangeAllowed = true;
        }
    }

    public void afterFiatTextChanged(String fiatAmountText) {

        dataListener.onRemoveFiatTextChangeListener();

        int max_len = 2;
        NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
        fiatFormat.setMaximumFractionDigits(max_len + 1);
        fiatFormat.setMinimumFractionDigits(0);

        try {
            if (fiatAmountText.indexOf(sendModel.defaultSeparator) != -1) {
                String dec = fiatAmountText.substring(fiatAmountText.indexOf(sendModel.defaultSeparator));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > max_len) {
                        dataListener.onUpdateFiatAmount(fiatAmountText.substring(0, fiatAmountText.length() - 1));
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            ;
        }

        dataListener.onAddFiatTextChangeListener();

        if (sendModel.textChangeAllowed) {
            sendModel.textChangeAllowed = false;

            if(fiatAmountText.isEmpty())fiatAmountText = "0";
            double fiat_amount;
            try {
                fiat_amount = NumberFormat.getInstance(Locale.getDefault()).parse(fiatAmountText).doubleValue();
            } catch (NumberFormatException | ParseException e) {
                fiat_amount = 0.0;
            }
            double btc_amount = fiat_amount / sendModel.exchangeRate;
            dataListener.onUpdateBtcAmount(monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(btc_amount)));
            sendModel.textChangeAllowed = true;
        }
    }

    public void handleIncomingQRScan(String scanData){

        scanData = scanData.trim();

        String btcAddress = null;
        String btcAmount = null;

        // check for poorly formed BIP21 URIs
        if (scanData.startsWith("bitcoin://") && scanData.length() > 10) {
            scanData = "bitcoin:" + scanData.substring(10);
        }

        if (FormatsUtil.getInstance().isValidBitcoinAddress(scanData)) {
            btcAddress = scanData;
        } else if (FormatsUtil.getInstance().isBitcoinUri(scanData)) {
            btcAddress = FormatsUtil.getInstance().getBitcoinAddress(scanData);
            btcAmount = FormatsUtil.getInstance().getBitcoinAmount(scanData);

            //Convert to correct units
            try {
                btcAmount = monetaryUtil.getDisplayAmount(Long.parseLong(btcAmount));
            }catch (Exception e){
                btcAmount = null;
            }

        } else {
            dataListener.onShowErrorMessage(context.getString(R.string.invalid_bitcoin_address));
            return;
        }

        if (!btcAddress.equals("")) {
            sendModel.setDestinationAddress(btcAddress);
            sendModel.pendingTransaction.receivingObject = null;
        }
        if (btcAmount != null && !btcAmount.equals("")) {
            dataListener.onRemoveBtcTextChangeListener();
            dataListener.onRemoveFiatTextChangeListener();

            dataListener.onUpdateBtcAmount(btcAmount);

            double btc_amount;

            try {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
                Number btcNumber = numberFormat.parse(btcAmount);
                btc_amount = monetaryUtil.getUndenominatedAmount(btcNumber.doubleValue());
            } catch (NumberFormatException e) {
                btc_amount = 0.0;
            } catch (ParseException e) {
                btc_amount = 0.0;
            }

            sendModel.exchangeRate = ExchangeRateFactory.getInstance().getLastPrice(context, sendModel.fiatUnit);

            double fiat_amount = sendModel.exchangeRate * btc_amount;

            dataListener.onUpdateFiatAmount(monetaryUtil.getFiatFormat(sendModel.fiatUnit).format(fiat_amount));

            //QR scan comes in as BTC - set current btc unit
            dataListener.onSetBtcUnit(MonetaryUtil.UNIT_BTC);

            dataListener.onUpdateBtcUnit(sendModel.btcUnit);
            dataListener.onUpdateFiatUnit(sendModel.fiatUnit);

            dataListener.onAddBtcTextChangeListener();
            dataListener.onAddFiatTextChangeListener();
        }
    }

    public void getSuggestedFee(){

        DynamicFee dynamicFeeApi = new DynamicFee();

        //Use default fee per kb until suggestion retrieved
        sendModel.suggestedFee = dynamicFeeApi.getDefaultFee();

        new Thread(() -> {
            Looper.prepare();
            sendModel.suggestedFee = dynamicFeeApi.getDynamicFee();
            Looper.loop();
        }).start();
    }

    public void spendAllClicked(ItemAccount sendAddressItem, String customFeeText) {
        calculateTransactionAmounts(true, sendAddressItem, null, customFeeText, null);
    }

    public void calculateTransactionAmounts(ItemAccount sendAddressItem, String amountToSendText, String customFeeText, TransactionDataListener listener) {
        calculateTransactionAmounts(false, sendAddressItem, amountToSendText, customFeeText, listener);
    }

    public interface TransactionDataListener {
        void onReady();
    }

    private void calculateTransactionAmounts(boolean spendAll, ItemAccount sendAddressItem,
                                             String amountToSendText, String customFeeText, TransactionDataListener listener) {

        sendModel.setMaxAvailableProgressVisibility(View.VISIBLE);
        sendModel.setMaxAvailableVisibility(View.GONE);

        String address;

        if(sendAddressItem.accountObject instanceof Account){
            //xpub
            address = ((Account)sendAddressItem.accountObject).getXpub();

        }else{
            //legacy address
            address = ((LegacyAddress)sendAddressItem.accountObject).getAddress();
        }

        new Thread(() -> {
            Looper.prepare();

            try {
                JSONObject unspentResponse = getUnspentApiResponse(address);

                if(unspentResponse != null){

                    final UnspentOutputs coins = payment.getCoins(unspentResponse);
                    BigInteger amountToSend = getSatoshisFromText(amountToSendText);
                    BigInteger customFee = BigInteger.ZERO;
                    BigInteger feePerKb = BigInteger.ZERO;
                    long balanceAfterFee = 0l;

                    //Check customized fee
                    customFee = getSatoshisFromText(customFeeText);
                    if(!customFeeText.isEmpty()) {
                        //Fee has been customized, use absolute fee
                        SweepBundle sweepBundle = payment.getSweepBundle(coins, BigInteger.ZERO);
                        balanceAfterFee = sweepBundle.getSweepAmount().longValue();
                        balanceAfterFee -= customFee.longValue();

                        amountToSend = amountToSend.add(customFee);

                        SpendableUnspentOutputs unspentOutputBundle = payment.getSpendableCoins(coins,
                                amountToSend,
                                BigInteger.ZERO);
                        //Include custom fee in amount sent
                        setPendingTransactionAmounts(unspentOutputBundle, amountToSend, customFee);

                    }else{
                        //Use default fee per kb
                        SweepBundle sweepBundle = payment.getSweepBundle(coins, sendModel.suggestedFee.defaultFeePerKb);
                        balanceAfterFee = sweepBundle.getSweepAmount().longValue();

                        feePerKb = sendModel.suggestedFee.defaultFeePerKb;

                        SpendableUnspentOutputs unspentOutputBundle = payment.getSpendableCoins(coins,
                                amountToSend,
                                feePerKb);
                        sendModel.absoluteFeeSuggested = unspentOutputBundle.getAbsoluteFee();

                        setPendingTransactionAmounts(unspentOutputBundle, amountToSend, BigInteger.valueOf(-1));
                    }

                    if(spendAll){
                        dataListener.onSetSpendAllAmount(getTextFromSatoshis(balanceAfterFee));
                    }
                    updateMaxAvailable(balanceAfterFee);

                    if(sendModel.suggestedFee != null && sendModel.suggestedFee.estimateList != null){
                        updateEstimateConfirmationTime(amountToSend, customFee.longValue(), coins);
                    }

                    //Warn user of unconfirmed funds - but don't block payment
                    if(coins.getNotice() != null){
                        dataListener.onShowErrorMessage(coins.getNotice());
                    }

                }else{
                    updateMaxAvailable(0);
                    sendModel.pendingTransaction.unspentOutputBundle = null;
                }

                if(listener != null)listener.onReady();

            } catch (Exception e) {
                e.printStackTrace();
                updateMaxAvailable(0);
                dataListener.onShowErrorMessage(context.getString(R.string.api_fail));
            }

            Looper.loop();
        }).start();

    }

    private void setPendingTransactionAmounts(SpendableUnspentOutputs unspentOutputBundle, BigInteger amountToSend, BigInteger absoluteCustomFee){

        sendModel.pendingTransaction.bigIntAmount = amountToSend;
        sendModel.pendingTransaction.unspentOutputBundle = unspentOutputBundle;

        if(absoluteCustomFee.compareTo(BigInteger.ZERO) > 0) {
            sendModel.pendingTransaction.bigIntFee = absoluteCustomFee;
        }else {
            sendModel.pendingTransaction.bigIntFee = sendModel.pendingTransaction.unspentOutputBundle.getAbsoluteFee();
        }
    }

    private void updateMaxAvailable(long balanceAfterFee){
        sendModel.setMaxAvailableProgressVisibility(View.GONE);
        sendModel.setMaxAvailableVisibility(View.VISIBLE);

        if(balanceAfterFee <= 0){
            sendModel.setMaxAvailableColor(ContextCompat.getColor(context, R.color.blockchain_send_red));
        }else{
            sendModel.setMaxAvailableColor(ContextCompat.getColor(context, R.color.textColorPrimary));
        }

        //Format for display
        final double balanceAfterFeeD = Math.max(balanceAfterFee / 1e8, 0.0);
        String btcAmountFormatted = monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(balanceAfterFeeD));
        sendModel.setMaxAviable(context.getString(R.string.max_available) + " " + btcAmountFormatted + " " + sendModel.btcUnit);
    }

    private BigInteger[] getEstimatedBlocks(BigInteger amountToSend, ArrayList<SuggestedFee.Estimates> estimates, UnspentOutputs coins) {
        BigInteger[] absoluteFeeSuggestedEstimates = new BigInteger[estimates.size()];

        for(int i = 0; i < absoluteFeeSuggestedEstimates.length; i++){

            BigInteger feePerKb = estimates.get(i).fee;
            SpendableUnspentOutputs unspentOutputBundle = payment.getSpendableCoins(coins, amountToSend, feePerKb);

            if(unspentOutputBundle != null){
                absoluteFeeSuggestedEstimates[i] = unspentOutputBundle.getAbsoluteFee();
            }
        }

        return absoluteFeeSuggestedEstimates;
    }

    private JSONObject getUnspentApiResponse(String address) throws Exception {
        if(sendModel.unspentApiResponse.containsKey(address)) {
            return sendModel.unspentApiResponse.get(address);
        }else{
            JSONObject unspentResponse = new Unspent().getUnspentOutputs(address);
            sendModel.unspentApiResponse.put(address, unspentResponse);
            return unspentResponse;
        }
    }

    private BigInteger getSatoshisFromText(String text){

        if(text == null || text.isEmpty())return BigInteger.ZERO;

        //Format string to parsable double
        String amountToSend = text.trim().replace(" ","").replace(sendModel.defaultSeparator, ".");

        double amount;
        try{
            amount = Double.parseDouble(amountToSend);
        }catch (NumberFormatException nfe){
            amount = 0.0;
        }

        long amountL = (BigDecimal.valueOf(monetaryUtil.getUndenominatedAmount(amount)).multiply(BigDecimal.valueOf(100000000)).longValue());
        return BigInteger.valueOf(amountL);
    }

    private String getTextFromSatoshis(long satoshis){

        String displayAmount = monetaryUtil.getDisplayAmount(satoshis);
        displayAmount = displayAmount.replace(".", sendModel.defaultSeparator);
        return displayAmount;
    }

    private String updateEstimateConfirmationTime(BigInteger amountToSend, long fee, UnspentOutputs coins){

        sendModel.absoluteFeeSuggestedEstimates = getEstimatedBlocks(amountToSend, sendModel.suggestedFee.estimateList, coins);

        String likelyToConfirmMessage = context.getText(R.string.estimate_confirm_block_count).toString();
        String unlikelyToConfirmMessage = context.getText(R.string.fee_too_low_no_confirm).toString();

        long minutesPerBlock = 10;
        Arrays.sort(sendModel.absoluteFeeSuggestedEstimates, Collections.reverseOrder());

        String estimateText = unlikelyToConfirmMessage;

        for(int i = 0; i < sendModel.absoluteFeeSuggestedEstimates.length; i++){
            if(fee >= sendModel.absoluteFeeSuggestedEstimates[i].longValue()){
                estimateText = likelyToConfirmMessage;
                estimateText = String.format(estimateText, ((i+1) * minutesPerBlock), (i+1));
                break;
            }
        }

        sendModel.setEstimate(estimateText);

        if (estimateText.equals(unlikelyToConfirmMessage)) {
            sendModel.setEstimateColor(ContextCompat.getColor(context, R.color.blockchain_send_red));
        } else {
            sendModel.setEstimateColor(ContextCompat.getColor(context, R.color.blockchain_blue));
        }

        return estimateText;
    }

    public void sendClicked(boolean bypassFeeCheck, String address) {

        if(FormatsUtil.getInstance().isValidBitcoinAddress(address)){
            //Receiving address manual or scanned input
            sendModel.pendingTransaction.receivingAddress = address;
        }

        if(bypassFeeCheck || isFeeAdequate()) {

            //Check connectivity before we spend
            if (ConnectivityStatus.hasConnectivity(context)) {

                if (isValidSpend(sendModel.pendingTransaction)) {

                    //Currently only v2 has watch-only
                    if (!sendModel.pendingTransaction.isHD() &&
                            ((LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject).isWatchOnly()) {

                        dataListener.onShowSpendFromWatchOnly(((LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject).getAddress());

                    } else if(sendModel.verifiedSecondPassword != null) {
                        confirmPayment();

                    }else{
                        new SecondPasswordHandler(context).validate(new SecondPasswordHandler.ResultListener() {
                            @Override
                            public void onNoSecondPassword() {
                                confirmPayment();
                            }

                            @Override
                            public void onSecondPasswordValidated(String validatedSecondPassword) {
                                payloadManager.decryptDoubleEncryptedWallet(validatedSecondPassword);
                                sendModel.verifiedSecondPassword = validatedSecondPassword;
                                confirmPayment();
                            }
                        });
                    }
                }

            } else {
                dataListener.onShowErrorMessage(context.getString(R.string.check_connectivity_exit));
            }
        }
    }

    private boolean isFeeAdequate(){

        if(sendModel.suggestedFee !=null && sendModel.suggestedFee.estimateList != null){

            if (sendModel.absoluteFeeSuggestedEstimates != null
                    && sendModel.pendingTransaction.bigIntFee.compareTo(sendModel.absoluteFeeSuggestedEstimates[0]) > 0) {

                String message = String.format(context.getString(R.string.high_fee_not_necessary_info),
                        monetaryUtil.getDisplayAmount(sendModel.pendingTransaction.bigIntFee.longValue()) + " " + sendModel.btcUnit,
                        monetaryUtil.getDisplayAmount(sendModel.absoluteFeeSuggestedEstimates[0].longValue()) + " " + sendModel.btcUnit);

                dataListener.onShowAlterFee(
                        getTextFromSatoshis(sendModel.absoluteFeeSuggestedEstimates[0].longValue()),
                        message,
                        R.string.lower_fee,
                        R.string.keep_high_fee);

                return false;
            }

            if (sendModel.absoluteFeeSuggestedEstimates != null
                    && sendModel.pendingTransaction.bigIntFee.compareTo(sendModel.absoluteFeeSuggestedEstimates[5]) < 0) {

                String message = String.format(context.getString(R.string.low_fee_suggestion),
                        monetaryUtil.getDisplayAmount(sendModel.pendingTransaction.bigIntFee.longValue()) + " " + sendModel.btcUnit,
                        monetaryUtil.getDisplayAmount(sendModel.absoluteFeeSuggestedEstimates[5].longValue()) + " " + sendModel.btcUnit);

                dataListener.onShowAlterFee(
                        getTextFromSatoshis(sendModel.absoluteFeeSuggestedEstimates[5].longValue()),
                        message,
                        R.string.raise_fee,
                        R.string.keep_low_fee);

                return false;
            }


        }

        return true;
    }

    private void confirmPayment(){

        PendingTransaction pendingTransaction = sendModel.pendingTransaction;

        PaymentConfirmationDetails details = new PaymentConfirmationDetails();
        details.fromLabel = pendingTransaction.sendingObject.label;
        if(pendingTransaction.receivingObject != null
                && pendingTransaction.receivingObject.label != null
                && !pendingTransaction.receivingObject.label.isEmpty()) {
            details.toLabel = pendingTransaction.receivingObject.label;
        }else{
            details.toLabel = pendingTransaction.receivingAddress;
        }
        details.btcAmount = getTextFromSatoshis(pendingTransaction.bigIntAmount.longValue());
        details.btcFee = getTextFromSatoshis(pendingTransaction.bigIntFee.longValue());
        details.btcSuggestedFee = getTextFromSatoshis(sendModel.absoluteFeeSuggested.longValue());
        details.btcUnit = sendModel.btcUnit;
        details.fiatUnit = sendModel.fiatUnit;
        details.btcTotal = getTextFromSatoshis(pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee).longValue());

        details.fiatFee = (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (pendingTransaction.bigIntFee.doubleValue() / 1e8)));

        details.fiatAmount = (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (pendingTransaction.bigIntAmount.doubleValue() / 1e8)));

        BigInteger totalFiat = (pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee));
        details.fiatTotal = (monetaryUtil.getFiatFormat(sendModel.fiatUnit)
                .format(sendModel.exchangeRate * (totalFiat.doubleValue() / 1e8)));

        dataListener.onShowPaymentDetails(details, isLargeTransaction());
    }

    public boolean isLargeTransaction(){

        int txSize = FeeUtil.estimatedSize(sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(), 2);//assume change
        double relativeFee = sendModel.absoluteFeeSuggested.doubleValue()/sendModel.pendingTransaction.bigIntAmount.doubleValue()*100.0;

        if(sendModel.absoluteFeeSuggested.longValue() > SendModel.LARGE_TX_FEE
                && txSize > SendModel.LARGE_TX_SIZE
                && relativeFee > SendModel.LARGE_TX_PERCENTAGE){

            return true;
        }else{
            return false;
        }
    }

    private boolean isValidSpend(PendingTransaction pendingTransaction) {

        //Validate amount
        if(!isValidAmount(pendingTransaction.bigIntAmount)){
            dataListener.onShowInvalidAmount();
            return false;
        }

        //Validate sufficient funds
        if(pendingTransaction.unspentOutputBundle == null || pendingTransaction.unspentOutputBundle.getSpendableOutputs() == null){
            dataListener.onShowErrorMessage(context.getString(R.string.no_confirmed_funds));
            return false;
        }
        long amountToSendIncludingFee = pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue();
        if(pendingTransaction.isHD()){

            String xpub = ((Account)pendingTransaction.sendingObject.accountObject).getXpub();
            if(!hasSufficientFunds(xpub, null, amountToSendIncludingFee)){
                dataListener.onShowErrorMessage(context.getString(R.string.insufficient_funds));
                sendModel.setCustomFeeColor(R.color.blockchain_send_red);
                return false;
            }else{
                sendModel.setCustomFeeColor(R.color.primary_text_default_material_light);
            }

        }else{

            if(!hasSufficientFunds(null, ((LegacyAddress)pendingTransaction.sendingObject.accountObject).getAddress(), amountToSendIncludingFee)){
                dataListener.onShowErrorMessage(context.getString(R.string.insufficient_funds));
                sendModel.setCustomFeeColor(R.color.blockchain_send_red);
                return false;
            }else{
                sendModel.setCustomFeeColor(R.color.primary_text_default_material_light);
            }

        }

        //Validate addresses
        if(!FormatsUtil.getInstance().isValidBitcoinAddress(pendingTransaction.receivingAddress)){
            dataListener.onShowErrorMessage(context.getString(R.string.invalid_bitcoin_address));
            return false;
        }

        //Validate send and receive not same addresses
        if(pendingTransaction.sendingObject == pendingTransaction.receivingObject){
            dataListener.onShowErrorMessage(context.getString(R.string.send_to_same_address_warning));
            return false;
        }

        //Validate sufficient fee
        //TODO - minimum on push tx = 1000 per kb, unless it has sufficient priority
        if(pendingTransaction.bigIntFee.longValue() < 1000){
            dataListener.onShowErrorMessage(context.getString(R.string.insufficient_fee));
            return false;
        }

        if(pendingTransaction.unspentOutputBundle == null){
            dataListener.onShowErrorMessage(context.getString(R.string.no_confirmed_funds));
            return false;
        }

        if(pendingTransaction.unspentOutputBundle.getSpendableOutputs().size() == 0){
            dataListener.onShowErrorMessage(context.getString(R.string.insufficient_funds));
            return false;
        }

        if(sendModel.pendingTransaction.receivingObject.accountObject == sendModel.pendingTransaction.sendingObject.accountObject){
            dataListener.onShowErrorMessage(context.getString(R.string.send_to_same_address_warning));
            return false;
        }

        return true;
    }

    public void setSendingAddress(ItemAccount selectedItem) {
        sendModel.pendingTransaction.sendingObject = selectedItem;
    }

    /*
    Account or legacyAddress selected
     */
    public void setReceivingAddress(ItemAccount selectedItem) {

        sendModel.pendingTransaction.receivingObject = selectedItem;

        if(selectedItem.accountObject instanceof Account) {

            //V3
            Account account = ((Account)selectedItem.accountObject);
            sendModel.pendingTransaction.receivingAddress = payloadManager.getReceiveAddress(account.getRealIdx());

        }else if(selectedItem.accountObject instanceof LegacyAddress){

            //V2
            LegacyAddress legacyAddress = ((LegacyAddress)selectedItem.accountObject);
            sendModel.pendingTransaction.receivingAddress = legacyAddress.getAddress();

            if(legacyAddress.isWatchOnly())
                if (legacyAddress.isWatchOnly()) {
                    dataListener.onShowReceiveToWatchOnlyWarning(legacyAddress.getAddress());
                }
        }else{

            //Address book
            AddressBookEntry addressBook = ((AddressBookEntry)selectedItem.accountObject);
            sendModel.pendingTransaction.receivingAddress = addressBook.getAddress();
        }
    }

    private boolean isValidAmount(BigInteger bAmount){

        //Test that amount is more than dust
        if (bAmount.compareTo(SendCoins.bDust) == -1) {
            return false;
        }

        //Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1) {
            dataListener.onUpdateBtcAmount("0");
            return false;
        }

        //Test that amount is not zero
        if (!(bAmount.compareTo(BigInteger.ZERO) >= 0)) {
            return false;
        }

        return true;
    }

    private boolean hasSufficientFunds(String xpub, String legacyAddress, long amountToSendIncludingFee){

        if (xpub != null) {
            //HD
            if (xpub != null && MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                long xpubBalance = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
                if (amountToSendIncludingFee > xpubBalance) {
                    return false;
                }
            }
        } else {
            //Legacy
            long legacyAddressBalance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress);
            if (amountToSendIncludingFee > legacyAddressBalance) {
                return false;
            }
        }

        return true;
    }

    public void submitPayment(AlertDialog alertDialog) {

        new Thread(() -> {
            try {

                if (ConnectivityStatus.hasConnectivity(context)) {

                    boolean isWatchOnly = false;

                    String changeAddress;
                    Account account = null;
                    LegacyAddress legacyAddress = null;

                    if (sendModel.pendingTransaction.isHD()) {
                        account = ((Account) sendModel.pendingTransaction.sendingObject.accountObject);
                        changeAddress = payloadManager.getChangeAddress(account.getRealIdx());
                    } else {
                        legacyAddress = ((LegacyAddress) sendModel.pendingTransaction.sendingObject.accountObject);
                        changeAddress = legacyAddress.getAddress();
                    }

                    payment.submitPayment(sendModel.pendingTransaction.unspentOutputBundle,
                            account,
                            legacyAddress,
                            sendModel.pendingTransaction.receivingAddress,
                            changeAddress,
                            sendModel.pendingTransaction.note,
                            sendModel.pendingTransaction.bigIntFee,
                            sendModel.pendingTransaction.bigIntAmount,
                            isWatchOnly,
                            sendModel.verifiedSecondPassword,
                            new Payment.SubmitPaymentListener() {
                                @Override
                                public void onSuccess(String s) {

                                    if(alertDialog != null && alertDialog.isShowing())alertDialog.dismiss();
                                    updateInternalBalances();
                                    PayloadBridge.getInstance().remoteSaveThread(null);
                                    dataListener.onShowTransactionSuccess();

                                }

                                @Override
                                public void onFail(String s) {
                                    dataListener.onShowErrorMessage(context.getString(R.string.transaction_failed));
                                }
                            });

                } else {
                    dataListener.onShowErrorMessage(context.getString(R.string.check_connectivity_exit));
                    // Queue tx
//                    Spendable spendable = new Spendable(tx, opc, note, isHD, sentChange, accountIdx);
//                    TxQueue.getInstance(context).add(spendable);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();

    }

    /*
    Update balance immediately after spend - until refresh from server
     */
    private void updateInternalBalances(){

        BigInteger totalSent = sendModel.pendingTransaction.bigIntAmount.add(sendModel.pendingTransaction.bigIntFee);

        if (sendModel.pendingTransaction.isHD()) {

            Account account = (Account)sendModel.pendingTransaction.sendingObject.accountObject;


            long updatedBalance = MultiAddrFactory.getInstance().getXpubBalance() - totalSent.longValue();

            //Set total balance
            MultiAddrFactory.getInstance().setXpubBalance(updatedBalance);

            //Set individual xpub balance
            MultiAddrFactory.getInstance().setXpubAmount(
                    account.getXpub(),
                    MultiAddrFactory.getInstance().getXpubAmounts().get(account.getXpub()) - updatedBalance);

        } else {
            MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - totalSent.longValue());
        }
    }

    public void handleScannedDataForWatchOnlySpend(String scanData) {
        try {
            final String format = PrivateKeyFactory.getInstance().getFormat(scanData);
            if (format != null) {
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    spendFromWatchOnlyNonBIP38(format, scanData);
                } else {
                    //BIP38 needs passphrase
                    dataListener.onShowBIP38PassphrasePrompt(scanData);
                }
            } else {
                dataListener.onShowErrorMessage(context.getString(R.string.privkey_error));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spendFromWatchOnlyNonBIP38(final String format, final String scanData){

        try {
            ECKey key = PrivateKeyFactory.getInstance().getKey(format, scanData);
            LegacyAddress legacyAddress = (LegacyAddress)sendModel.pendingTransaction.sendingObject.accountObject;
            setTempLegacyAddressPrivateKey(legacyAddress, key);

        } catch (Exception e) {
            dataListener.onShowErrorMessage(context.getString(R.string.no_private_key));
            e.printStackTrace();
        }
    }

    private void setTempLegacyAddressPrivateKey(LegacyAddress legacyAddress, ECKey key){
        if (key != null && key.hasPrivKey() && legacyAddress.getAddress().equals(key.toAddress(MainNetParams.get()).toString())) {

            //Create copy, otherwise pass by ref will override private key in wallet payload
            LegacyAddress tempLegacyAddress = new LegacyAddress();
            tempLegacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            tempLegacyAddress.setAddress(key.toAddress(MainNetParams.get()).toString());
            tempLegacyAddress.setLabel(legacyAddress.getLabel());
            sendModel.pendingTransaction.sendingObject.accountObject = tempLegacyAddress;

            confirmPayment();
        } else {
            dataListener.onShowErrorMessage(context.getString(R.string.invalid_private_key));
        }
    }

    public void spendFromWatchOnlyBIP38(String pw, String scanData) {
        new Thread(() -> {

            Looper.prepare();

            try {
                BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), scanData);
                final ECKey key = bip38.decrypt(pw);

                LegacyAddress legacyAddress = (LegacyAddress)sendModel.pendingTransaction.sendingObject.accountObject;
                setTempLegacyAddressPrivateKey(legacyAddress, key);

            } catch (Exception e) {
                dataListener.onShowErrorMessage(context.getString(R.string.bip38_error));
            }

            Looper.loop();

        }).start();
    }
}
