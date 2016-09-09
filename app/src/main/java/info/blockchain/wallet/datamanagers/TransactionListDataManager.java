package info.blockchain.wallet.datamanagers;

import android.support.annotation.NonNull;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.rxjava.RxUtil;
import info.blockchain.wallet.util.WebUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.di.Injector;
import rx.Observable;
import rx.schedulers.Schedulers;

public class TransactionListDataManager {

    private final String TAG_ALL = "TAG_ALL";
    private final String TAG_IMPORTED_ADDRESSES = "TAG_IMPORTED_ADDRESSES";
    private List<Tx> mTransactionList = new ArrayList<>();

    @SuppressWarnings("WeakerAccess")
    @Inject
    PayloadManager mPayloadManager;

    public TransactionListDataManager() {
        Injector.getInstance().getAppComponent().inject(this);
    }

    public void clearTransactionList() {
        mTransactionList.clear();
    }

    @NonNull
    public void generateTransactionList(Object object) {
        if (object instanceof Account) {
            // V3
            mTransactionList.addAll(getV3Transactions((Account) object));
        } else if (object instanceof LegacyAddress) {
            // V2
            mTransactionList.addAll(MultiAddrFactory.getInstance().getAddressLegacyTxs(((LegacyAddress) object).getAddress()));
        } else {
            throw new IllegalArgumentException("Object must be instance of Account.class or LegacyAddress.class");
        }

        Collections.sort(mTransactionList, new TxDateComparator());
    }

    @NonNull
    public List<Tx> getTransactionList() {
        return mTransactionList;
    }

    @NonNull
    public List<Tx> insertTransactionIntoListAndReturnSorted(Tx transaction) {
        mTransactionList.add(transaction);
        Collections.sort(mTransactionList, new TxDateComparator());
        return mTransactionList;
    }

    private List<Tx> getV3Transactions(Account account) {
        List<Tx> transactions = new ArrayList<>();

        if (account.getTags().contains(TAG_ALL)) {
            if (mPayloadManager.getPayload().isUpgraded()) {
                transactions.addAll(getAllXpubAndLegacyTxs());
            } else {
                transactions.addAll(MultiAddrFactory.getInstance().getLegacyTxs());
            }

        } else if (account.getTags().contains(TAG_IMPORTED_ADDRESSES)) {
            // V3 - Imported Addresses
            transactions.addAll(MultiAddrFactory.getInstance().getLegacyTxs());
        } else {
            // V3 - Individual
            String xpub = account.getXpub();
            if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                transactions.addAll(MultiAddrFactory.getInstance().getXpubTxs().get(xpub));
            }
        }

        return transactions;
    }

    public double getBtcBalance(Object object) {
        // Update Balance
        double balance = 0D;
        if (object instanceof Account) {
            //V3
            Account account = ((Account) object);
            // V3 - All
            if (account.getTags().contains(TAG_ALL)) {
                if (mPayloadManager.getPayload().isUpgraded()) {
                    // Balance = all xpubs + all legacy address balances
                    balance = ((double) MultiAddrFactory.getInstance().getXpubBalance())
                            + ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());
                } else {
                    // Balance = all legacy address balances
                    balance = ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());
                }
            } else if (account.getTags().contains(TAG_IMPORTED_ADDRESSES)) {
                balance = ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());
            } else {
                // V3 - Individual
                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(account.getXpub())) {
                    HashMap<String, Long> xpubAmounts = MultiAddrFactory.getInstance().getXpubAmounts();
                    Long bal = (xpubAmounts.get(account.getXpub()) == null ? 0L : xpubAmounts.get(account.getXpub()));
                    balance = ((double) (bal));
                }
            }
        } else if (object instanceof LegacyAddress) {
            // V2
            LegacyAddress legacyAddress = ((LegacyAddress) object);
            balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
        } else {
            throw new IllegalArgumentException("Object must be instance of Account.class or LegacyAddress.class");
        }

        return balance;
    }

    private List<Tx> getAllXpubAndLegacyTxs() {

        // Remove duplicate txs
        HashMap<String, Tx> consolidatedTxsList = new HashMap<>();

        List<Tx> allXpubTransactions = MultiAddrFactory.getInstance().getAllXpubTxs();
        for (Tx tx : allXpubTransactions) {
            if (!consolidatedTxsList.containsKey(tx.getHash()))
                consolidatedTxsList.put(tx.getHash(), tx);
        }

        List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
        for (Tx tx : allLegacyTransactions) {
            if (!consolidatedTxsList.containsKey(tx.getHash()))
                consolidatedTxsList.put(tx.getHash(), tx);
        }

        return new ArrayList<>(consolidatedTxsList.values());
    }

    public Observable<Transaction> getTransactionFromHash(String transactionHash) {
        return getTransactionResultString(transactionHash)
                .flatMap(this::getTransactionFromJsonString)
                .compose(RxUtil.applySchedulers());
    }

    private Observable<Transaction> getTransactionFromJsonString(String json) {
        return Observable.fromCallable(() -> new Transaction(new JSONObject(json)));
    }

    private Observable<String> getTransactionResultString(String transactionHash) {
        return Observable.fromCallable(() -> WebUtil.getInstance().getURL(
                WebUtil.TRANSACTION + transactionHash + "?format=json"))
                .observeOn(Schedulers.io());
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     * @param transactionHash   The hash of the transaction to be updated
     * @param notes             Transaction notes
     * @return                  Successful or not
     */
    public Observable<Boolean> updateTransactionNotes(String transactionHash, String notes) {
        mPayloadManager.getPayload().getNotes().put(transactionHash, notes);
        return Observable.fromCallable(() -> mPayloadManager.savePayloadToServer())
                .compose(RxUtil.applySchedulers());
    }

    private class TxDateComparator implements Comparator<Tx> {

        TxDateComparator() {
        }

        public int compare(Tx t1, Tx t2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (t2.getTS() < t1.getTS()) {
                return BEFORE;
            } else if (t2.getTS() > t1.getTS()) {
                return AFTER;
            } else {
                return EQUAL;
            }
        }
    }
}
