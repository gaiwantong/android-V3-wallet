package info.blockchain.wallet.viewModel;

import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;

import info.blockchain.wallet.datamanagers.TransactionListDataManager;
import info.blockchain.wallet.model.RecipientModel;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.subscriptions.CompositeSubscription;

import static info.blockchain.wallet.view.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings("WeakerAccess")
public class TransactionDetailViewModel implements ViewModel {

    private static final int REQUIRED_CONFIRMATIONS = 3;

    private DataListener mDataListener;
    private MonetaryUtil mMonetaryUtil;
    @Inject PrefsUtil mPrefsUtil;
    @Inject PayloadManager mPayloadManager;
    @Inject info.blockchain.wallet.util.StringUtils mStringUtils;
    @Inject TransactionListDataManager mTransactionListDataManager;

    private Tx mTransaction;
    private double mBtcExchangeRate;
    private String mFiatType;

    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    public interface DataListener {

        Intent getPageIntent();

        void pageFinish();

        void setTransactionType(String type);

        void setTransactionValueBtc(String value);

        void setTransactionValueFiat(String fiat);

        void setToAddresses(List<RecipientModel> addresses);

        void setFromAddress(String address);

        void setStatus(String status, String hash);

        void setFee(String fee);

        void setDate(String date);

        void setDescription(String description);

        void setTransactionColour(@ColorRes int colour);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onLoaded();

    }

    public TransactionDetailViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
        mMonetaryUtil = new MonetaryUtil(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        mFiatType = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        mBtcExchangeRate = ExchangeRateFactory.getInstance().getLastPrice(mFiatType);
    }

    public void onViewReady() {
        if (mDataListener.getPageIntent() != null
                && mDataListener.getPageIntent().hasExtra(KEY_TRANSACTION_LIST_POSITION)) {

            int transactionPosition = mDataListener.getPageIntent().getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1);
            if (transactionPosition == -1) {
                mDataListener.pageFinish();
            } else {
                mTransaction = mTransactionListDataManager.getTransactionList().get(transactionPosition);
                updateUiFromTransaction(mTransaction);
            }
        } else {
            mDataListener.pageFinish();
        }
    }

    public void updateTransactionNote(String description) {
        mCompositeSubscription.add(
                mTransactionListDataManager.updateTransactionNotes(mTransaction.getHash(), description)
                        .subscribe(aBoolean -> {
                            if (!aBoolean) {
                                // Save unsuccessful
                                mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                            } else {
                                mDataListener.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                            }
                        }, throwable -> {
                            mDataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                        }));
    }

    private void updateUiFromTransaction(Tx transaction) {
        mDataListener.setTransactionType(transaction.getDirection());
        setTransactionColor(transaction);
        setTransactionAmount(transaction);
        setConfirmationStatus(transaction);
        setTransactionNote(transaction);
        setDate(transaction);

        mCompositeSubscription.add(
                mTransactionListDataManager.getTransactionFromHash(transaction.getHash())
                        .subscribe(result -> {
                            // Filter non-change addresses
                            Pair<HashMap<String, Long>, HashMap<String, Long>> pair = filterNonChangeAddresses(result, transaction);

                            // From address
                            HashMap<String, Long> inputMap = pair.first;
                            ArrayList<String> labelList = new ArrayList<>();
                            Set<Map.Entry<String, Long>> entrySet = inputMap.entrySet();
                            for (Map.Entry<String, Long> set : entrySet) {
                                String label = addressToLabel(set.getKey());
                                if (!labelList.contains(label))
                                    labelList.add(label);
                            }

                            String inputMapString = StringUtils.join(labelList.toArray(), "\n");
                            mDataListener.setFromAddress(addressToLabel(inputMapString));

                            // To Address
                            HashMap<String, Long> outputMap = pair.second;
                            ArrayList<RecipientModel> recipients = new ArrayList<>();

                            for (Map.Entry<String, Long> item : outputMap.entrySet()) {
                                RecipientModel recipientModel = new RecipientModel(
                                        addressToLabel(item.getKey()),
                                        mMonetaryUtil.getDisplayAmountWithFormatting(item.getValue()),
                                        getDisplayUnits());
                                recipients.add(recipientModel);
                            }

                            mDataListener.setToAddresses(recipients);
                            setFee(result);
                            mDataListener.onLoaded();

                        }, throwable -> {
                            mDataListener.pageFinish();
                        }));
    }

    private void setFee(Transaction result) {
        String fee = (mMonetaryUtil.getDisplayAmountWithFormatting(result.getFee()) + " " + getDisplayUnits());
        mDataListener.setFee(fee);
    }

    private void setTransactionAmount(Tx transaction) {
        String amountBtc = (
                mMonetaryUtil.getDisplayAmountWithFormatting(
                        Math.abs(transaction.getAmount()))
                        + " "
                        + getDisplayUnits());

        String amountFiat = (
                mMonetaryUtil.getFiatFormat(mFiatType).format(mBtcExchangeRate * (Math.abs(transaction.getAmount()) / 1e8))
                        + " "
                        + mFiatType);

        mDataListener.setTransactionValueBtc(amountBtc);
        mDataListener.setTransactionValueFiat(
                mStringUtils.getString(R.string.transaction_detail_value) + amountFiat);
    }

    private void setTransactionNote(Tx transaction) {
        String notes = mPayloadManager.getPayload().getNotes().get(transaction.getHash());
        mDataListener.setDescription(notes);
    }

    private void setConfirmationStatus(Tx transaction) {
        long confirmations = transaction.getConfirmations();

        if (confirmations >= REQUIRED_CONFIRMATIONS) {
            mDataListener.setStatus(mStringUtils.getString(R.string.transaction_detail_confirmed), transaction.getHash());
        } else {
            String pending = mStringUtils.getString(R.string.transaction_detail_pending);
            pending = String.format(Locale.getDefault(), pending, confirmations, REQUIRED_CONFIRMATIONS);
            mDataListener.setStatus(pending, transaction.getHash());
        }
    }

    private void setDate(Tx transaction) {
        long epochTime = transaction.getTS() * 1000;

        Date date = new Date(epochTime);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG);
        DateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String dateText = dateFormat.format(date);
        String timeText = timeFormat.format(date);

        mDataListener.setDate(dateText + " @ " + timeText);
    }

    private void setTransactionColor(Tx transaction) {
        double btcBalance = transaction.getAmount() / 1e8;
        if (transaction.isMove()) {
            mDataListener.setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue);
        } else if (btcBalance < 0.0) {
            mDataListener.setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.blockchain_red_50 : R.color.blockchain_send_red);
        } else {
            mDataListener.setTransactionColour(transaction.getConfirmations() < REQUIRED_CONFIRMATIONS
                    ? R.color.blockchain_green_50 : R.color.blockchain_receive_green);
        }
    }

    private String getDisplayUnits() {
        return (String) mMonetaryUtil.getBTCUnits()[mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    @Override
    public void destroy() {
        // Clear all subscriptions so that:
        // 1) all processes are cancelled
        // 2) processes don't try to update a null View
        // 3) background processes don't leak memory
        mCompositeSubscription.clear();
    }

    private Pair<HashMap<String, Long>, HashMap<String, Long>> filterNonChangeAddresses(Transaction transactionDetails, Tx transaction) {

        HashMap<String, String> addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();

        HashMap<String, Long> inputMap = new HashMap<>();
        HashMap<String, Long> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        // Inputs / From field
        if (transaction.getDirection().equals(MultiAddrFactory.RECEIVED) && transactionDetails.getInputs().size() > 0) {
            // Only 1 addr for receive
            inputMap.put(transactionDetails.getInputs().get(0).addr, transactionDetails.getInputs().get(0).value);
        } else {
            for (Transaction.xPut input : transactionDetails.getInputs()) {
                if (!transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {
                    // Move or Send
                    // The address belongs to us
                    String xpub = addressToXpubMap.get(input.addr);

                    // Address belongs to xpub we own
                    if (xpub != null) {
                        // Only add xpub once
                        if (!inputXpubList.contains(xpub)) {
                            inputMap.put(input.addr, input.value);
                            inputXpubList.add(xpub);
                        }
                    } else {
                        // Legacy Address we own
                        inputMap.put(input.addr, input.value);
                    }
                } else {
                    // Receive
                    inputMap.put(input.addr, input.value);
                }
            }
        }

        // Outputs / To field
        for (Transaction.xPut output : transactionDetails.getOutputs()) {

            if (MultiAddrFactory.getInstance().isOwnHDAddress(output.addr)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                String xpub = addressToXpubMap.get(output.addr);
                if (inputXpubList.contains(xpub)) {
                    continue;// change back to same xpub
                }

                // Receiving to same address multiple times?
                if (outputMap.containsKey(output.addr)) {
                    long prevAmount = outputMap.get(output.addr) + output.value;
                    outputMap.put(output.addr, prevAmount);
                } else {
                    outputMap.put(output.addr, output.value);
                }

            } else if (mPayloadManager.getPayload().getLegacyAddressStrings().contains(output.addr)
                    || mPayloadManager.getPayload().getWatchOnlyAddressStrings().contains(output.addr)) {
                // If output address belongs to a legacy address we own - we have to check if it's change
                // If it goes back to same address AND if it's not the total amount sent (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if (inputMap.containsKey(output.addr) && output.value != transaction.getAmount()) {
                    continue;// change back to same input address
                }

                // Output more than tx amount - change
                if (output.value > transaction.getAmount()) {
                    continue;
                }

                outputMap.put(output.addr, output.value);
            } else {
                // Address does not belong to us
                if (!transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {
                    outputMap.put(output.addr, output.value);
                }
            }
        }

        return new Pair<>(inputMap, outputMap);
    }

    private String addressToLabel(String address) {
        HDWallet hdWallet = mPayloadManager.getPayload().getHdWallet();
        List<Account> accountList = new ArrayList<>();
        if (hdWallet != null && hdWallet.getAccounts() != null) {
            accountList = hdWallet.getAccounts();
        }

        HashMap<String, String> addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();

        // If address belongs to owned xpub
        if (MultiAddrFactory.getInstance().isOwnHDAddress(address)) {
            String xpub = addressToXpubMap.get(address);
            if (xpub != null) {
                // Even though it looks like this shouldn't happen, it sometimes happens with
                // transfers if user clicks to view details immediately.
                // TODO - see if isOwnHDAddress could be updated to solve this
                int accIndex = mPayloadManager.getPayload().getXpub2Account().get(xpub);
                String label = accountList.get(accIndex).getLabel();
                if (label != null && !label.isEmpty())
                    return label;
            }
            // If address one of owned legacy addresses
        } else if (mPayloadManager.getPayload().getLegacyAddressStrings().contains(address)
                || mPayloadManager.getPayload().getWatchOnlyAddressStrings().contains(address)) {

            Payload payload = mPayloadManager.getPayload();

            String label = payload.getLegacyAddresses().get(payload.getLegacyAddressStrings().indexOf(address)).getLabel();
            if (label != null && !label.isEmpty())
                return label;
        }

        return address;
    }
}
