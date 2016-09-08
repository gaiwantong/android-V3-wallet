package info.blockchain.wallet.view.helpers;

import android.support.annotation.NonNull;

import info.blockchain.wallet.model.ItemAccount;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.AddressBookEntry;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;

public class WalletAccountHelper {

    @Inject PayloadManager mPayloadManager;
    @Inject PrefsUtil mPrefsUtil;
    @Inject StringUtils mStringUtils;
    @Inject ExchangeRateFactory mExchangeRateFactory;
    private AddressBalanceHelper mAddressBalanceHelper;
    private double mBtcExchangeRate;
    private String mFiatUnit;
    private String mBtcUnit;

    public WalletAccountHelper() {
        Injector.getInstance().getAppComponent().inject(this);
        int btcUnit = mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        MonetaryUtil monetaryUtil = new MonetaryUtil(btcUnit);
        mBtcUnit = monetaryUtil.getBTCUnit(btcUnit);
        mFiatUnit = mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        mBtcExchangeRate = mExchangeRateFactory.getLastPrice(mFiatUnit);

        mAddressBalanceHelper = new AddressBalanceHelper(monetaryUtil);
    }

    @NonNull
    public List<ItemAccount> getAccountItems(boolean isBtc) {

        ArrayList<ItemAccount> accountArrayList = new ArrayList<>();

        // V3
        if (mPayloadManager.getPayload().isUpgraded()) {

            List<Account> accounts = mPayloadManager.getPayload().getHdWallet().getAccounts();
            for (Account account : accounts) {

                if (account.isArchived())
                    // Skip archived account
                    continue;

                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(account.getXpub())) {
                    accountArrayList.add(new ItemAccount(
                            account.getLabel(),
                            mAddressBalanceHelper.getAccountBalance(account, isBtc, mBtcExchangeRate, mFiatUnit, mBtcUnit),
                            null,
                            account));
                }
            }
        }

        // V2
        List<LegacyAddress> legacyAddresses = mPayloadManager.getPayload().getLegacyAddresses();
        for (LegacyAddress legacyAddress : legacyAddresses) {

            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)
                // Skip archived account
                continue;

            // If address has no label, we'll display address
            String labelOrAddress = legacyAddress.getLabel();
            if (labelOrAddress == null || labelOrAddress.trim().isEmpty()) {
                labelOrAddress = legacyAddress.getAddress();
            }

            // Watch-only tag - we'll ask for xpriv scan when spending from
            String tag = null;
            if (legacyAddress.isWatchOnly()) {
                tag = mStringUtils.getString(R.string.watch_only);
            }

            accountArrayList.add(new ItemAccount(
                    labelOrAddress,
                    mAddressBalanceHelper.getAddressBalance(legacyAddress, isBtc, mBtcExchangeRate, mFiatUnit, mBtcUnit),
                    tag,
                    legacyAddress));

        }

        return accountArrayList;
    }

    @NonNull
    public List<ItemAccount> getAddressBookEntries() {
        ArrayList<ItemAccount> accountArrayList = new ArrayList<>();

        List<AddressBookEntry> addressBookEntries = mPayloadManager.getPayload().getAddressBookEntries();
        for (AddressBookEntry addressBookEntry : addressBookEntries) {

            // If address has no label, we'll display address
            String labelOrAddress = addressBookEntry.getLabel() == null || addressBookEntry.getLabel().length() == 0 ? addressBookEntry.getAddress() : addressBookEntry.getLabel();

            accountArrayList.add(new ItemAccount(labelOrAddress, "", mStringUtils.getString(R.string.address_book_label), addressBookEntry));
        }

        return accountArrayList;
    }
}
