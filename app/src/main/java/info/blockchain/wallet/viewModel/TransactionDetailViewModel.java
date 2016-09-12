package info.blockchain.wallet.viewModel;

import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;

import info.blockchain.wallet.datamanagers.TransactionListDataManager;
import info.blockchain.wallet.model.RecipientModel;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.view.helpers.TransactionHelper;

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

public class TransactionDetailViewModel implements ViewModel {

    private static final int REQUIRED_CONFIRMATIONS = 3;

    private DataListener mDataListener;
    private MonetaryUtil mMonetaryUtil;
    @Inject TransactionHelper mTransactionHelper;
    @Inject PrefsUtil mPrefsUtil;
    @Inject PayloadManager mPayloadManager;
    @Inject info.blockchain.wallet.util.StringUtils mStringUtils;
    @Inject TransactionListDataManager mTransactionListDataManager;
    @Inject ExchangeRateFactory mExchangeRateFactory;

    private double mBtcExchangeRate;
    private String mFiatType;

    @VisibleForTesting Tx mTransaction;
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

        void onDataLoaded();

    }

    public TransactionDetailViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
        mMonetaryUtil = new MonetaryUtil(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        mFiatType = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        mBtcExchangeRate = mExchangeRateFactory.getLastPrice(mFiatType);
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
                            Pair<HashMap<String, Long>, HashMap<String, Long>> pair =
                                    mTransactionHelper.filterNonChangeAddresses(result, transaction);

                            // From address
                            HashMap<String, Long> inputMap = pair.first;
                            ArrayList<String> labelList = new ArrayList<>();
                            Set<Map.Entry<String, Long>> entrySet = inputMap.entrySet();
                            for (Map.Entry<String, Long> set : entrySet) {
                                String label = mTransactionHelper.addressToLabel(set.getKey());
                                if (!labelList.contains(label))
                                    labelList.add(label);
                            }

                            String inputMapString = StringUtils.join(labelList.toArray(), "\n");
                            mDataListener.setFromAddress(mTransactionHelper.addressToLabel(inputMapString));

                            // To Address
                            HashMap<String, Long> outputMap = pair.second;
                            ArrayList<RecipientModel> recipients = new ArrayList<>();

                            for (Map.Entry<String, Long> item : outputMap.entrySet()) {
                                RecipientModel recipientModel = new RecipientModel(
                                        mTransactionHelper.addressToLabel(item.getKey()),
                                        mMonetaryUtil.getDisplayAmountWithFormatting(item.getValue()),
                                        getDisplayUnits());
                                recipients.add(recipientModel);
                            }

                            mDataListener.setToAddresses(recipients);
                            setFee(result);
                            mDataListener.onDataLoaded();

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

    @VisibleForTesting
    void setConfirmationStatus(Tx transaction) {
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

    @VisibleForTesting
    void setTransactionColor(Tx transaction) {
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
}
