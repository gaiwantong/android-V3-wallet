package info.blockchain.wallet.viewModel;

import com.google.common.collect.HashBiMap;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.SparseIntArray;

import info.blockchain.wallet.datamanagers.ReceiveDataManager;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.StringUtils;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;
import rx.subscriptions.CompositeSubscription;

public class ReceiveViewModel implements ViewModel {

    private static final int DIMENSION_QR_CODE = 260;

    private DataListener mDataListener;
    @Inject protected PayloadManager mPayloadManager;
    @Inject protected AppUtil mAppUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected StringUtils mStringUtils;
    @Inject protected ReceiveDataManager mDataManager;
    private MonetaryUtil mMonetaryUtil;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    private List<String> mReceiveToList;
    private HashBiMap<Integer, Object> mAccountMap;
    private SparseIntArray mSpinnerIndexMap;
    private Locale mLocale;

    public interface DataListener {

        void onSpinnerDataChanged();

        void showQrLoading();

        void showQrCode(@Nullable Bitmap bitmap);

        void updateFiatTextField(String text);

        void updateBtcTextField(String text);

    }

    public ReceiveViewModel(DataListener listener, Locale locale) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
        mCompositeSubscription = new CompositeSubscription();
        mMonetaryUtil = new MonetaryUtil(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        mReceiveToList = new ArrayList<>();
        mAccountMap = HashBiMap.create();
        mSpinnerIndexMap = new SparseIntArray();
        mLocale = locale;
    }

    public void onViewReady() {
        // No-op
        updateSpinnerList();
    }

    @NonNull
    public List<String> getReceiveToList() {
        return mReceiveToList;
    }

    public void updateSpinnerList() {
        mReceiveToList.clear();
        mAccountMap.clear();
        mSpinnerIndexMap.clear();

        int spinnerIndex = 0;

        if (isUpgraded()) {
            // V3
            List<Account> accounts = mPayloadManager.getPayload().getHdWallet().getAccounts();
            int accountIndex = 0;
            for (Account item : accounts) {

                mSpinnerIndexMap.put(spinnerIndex, accountIndex);
                accountIndex++;

                if (item.isArchived())
                    // Skip archived account
                    continue;

                mReceiveToList.add(item.getLabel());
                mAccountMap.put(spinnerIndex, item);
                spinnerIndex++;
            }
        }

        // Legacy Addresses
        List<LegacyAddress> legacyAddresses = mPayloadManager.getPayload().getLegacyAddresses();
        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)
                // Skip archived address
                continue;

            // If address has no label, display address instead
            String labelOrAddress = legacyAddress.getLabel() == null
                    || legacyAddress.getLabel().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();

            // Prefix "watch-only"
            if (legacyAddress.isWatchOnly()) {
                labelOrAddress = mStringUtils.getString(R.string.watch_only_label) + " " + labelOrAddress;
            }

            mReceiveToList.add(labelOrAddress);
            mAccountMap.put(spinnerIndex, legacyAddress);
            spinnerIndex++;
        }

        mDataListener.onSpinnerDataChanged();
    }

    public void generateQrCode(String uri) {
        mDataListener.showQrLoading();

        mCompositeSubscription.add(
                mDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                        .subscribe(qrCode -> {
                            mDataListener.showQrCode(qrCode);
                        }, throwable -> {
                            mDataListener.showQrCode(null);
                        }));
    }

    public String getQrFileName() {
        return mAppUtil.getReceiveQRFilename();
    }

    public int getDefaultSpinnerPosition() {
        if (isUpgraded()) {
            return mAccountMap.inverse().get(getDefaultAccount());
        } else {
            return 0;
        }
    }

    private Account getDefaultAccount() {
        return mPayloadManager.getPayload().getHdWallet().getAccounts().get(
                mPayloadManager.getPayload().getHdWallet().getDefaultIndex());
    }

    @Nullable
    public Object getAccountItemForPosition(int position) {
        return mAccountMap.get(position);
    }

    public boolean isUpgraded() {
        return mPayloadManager.getPayload().isUpgraded();
    }

    public boolean warnWatchOnlySpend() {
        return mPrefsUtil.getValue("WARN_WATCH_ONLY_SPEND", true);
    }

    public void setWarnWatchOnlySpend(boolean warn) {
        mPrefsUtil.getValue("WARN_WATCH_ONLY_SPEND", warn);
    }

    public String getBtcUnit() {
        return mMonetaryUtil.getBTCUnit(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    public String getFiatUnit() {
        return mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    public int getMaxBtcLength() {
        int unit = mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        int maxLength;
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                maxLength = 2;
                break;
            case MonetaryUtil.MILLI_BTC:
                maxLength = 4;
                break;
            default:
                maxLength = 8;
                break;
        }
        return maxLength;
    }

    public BigInteger getUndenominatedAmount(long amount) {
        return mMonetaryUtil.getUndenominatedAmount(amount);
    }

    private Double getUndenominatedAmount(double amount) {
        return mMonetaryUtil.getUndenominatedAmount(amount);
    }

    private Double getDenominatedAmount(double amount) {
        return mMonetaryUtil.getDenominatedAmount(amount);
    }

    public boolean getIfAmountInvalid(BigInteger amount) {
        return amount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1;
    }

    private double getLastPrice() {
        return ExchangeRateFactory.getInstance().getLastPrice(getFiatUnit());
    }

    private String getFormattedFiatString(double amount) {
        return mMonetaryUtil.getFiatFormat(getFiatUnit()).format(amount);
    }

    private String getFormattedBtcString(double amount) {
        return mMonetaryUtil.getBTCFormat().format(getDenominatedAmount(amount));
    }

    public long getLongAmount(String amount) {
        try {
            return Math.round(NumberFormat.getInstance(mLocale).parse(amount).doubleValue() * 1e8);
        } catch (ParseException e) {
            return 0L;
        }
    }

    private double getDoubleAmount(String amount) {
        try {
            return NumberFormat.getInstance(mLocale).parse(amount).doubleValue();
        } catch (ParseException e) {
            return 0D;
        }
    }

    public void updateFiatTextField(String bitcoin) {
        if (bitcoin.isEmpty()) bitcoin = "0";
        double btcAmount = getUndenominatedAmount(getDoubleAmount(bitcoin));
        double fiatAmount = getLastPrice() * btcAmount;
        mDataListener.updateFiatTextField(getFormattedFiatString(fiatAmount));
    }

    public void updateBtcTextField(String fiat) {
        if (fiat.isEmpty()) fiat = "0";
        double fiatAmount = getDoubleAmount(fiat);
        double btcAmount = fiatAmount / getLastPrice();
        mDataListener.updateBtcTextField(getFormattedBtcString(btcAmount));
    }

    @Nullable
    public String getV3ReceiveAddress(Account account) {
        try {
            int spinnerIndex = mAccountMap.inverse().get(account);
            int accountIndex = mSpinnerIndexMap.get(spinnerIndex);
            return mPayloadManager.getReceiveAddress(accountIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
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
