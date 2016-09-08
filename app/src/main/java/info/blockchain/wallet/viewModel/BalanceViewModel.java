package info.blockchain.wallet.viewModel;

import com.google.common.collect.HashBiMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;

import info.blockchain.wallet.datamanagers.TransactionListDataManager;
import info.blockchain.wallet.model.BalanceModel;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.BR;
import piuk.blockchain.android.R;
import piuk.blockchain.android.di.Injector;

public class BalanceViewModel extends BaseObservable implements ViewModel {

    private Context context;
    private DataListener dataListener;
    private BalanceModel model;

    private ArrayList<String> activeAccountAndAddressList = null;
    private HashBiMap<Object, Integer> activeAccountAndAddressBiMap = null;
    private final String TAG_ALL = "TAG_ALL";
    private final String TAG_IMPORTED_ADDRESSES = "TAG_IMPORTED_ADDRESSES";
    private List<Tx> transactionList;
    private OSUtil osUtil;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected PayloadManager payloadManager;
    @Inject protected TransactionListDataManager transactionListDataManager;

    @Bindable
    public String getBalance(){
        return model.getBalance();
    }

    public void setBalance(String balance){
        model.setBalance(balance);
        notifyPropertyChanged(BR.balance);
    }

    public interface DataListener {
        void onRefreshAccounts();
        void onAccountSizeChange();
        void onRefreshBalanceAndTransactions();
    }

    public BalanceViewModel(Context context, DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.context = context;
        this.dataListener = dataListener;
        this.model = new BalanceModel();

        this.activeAccountAndAddressList = new ArrayList<>();
        this.activeAccountAndAddressBiMap = HashBiMap.create();
        this.transactionList = new ArrayList<>();
        this.osUtil = new OSUtil(context);
    }

    public MonetaryUtil getMonetaryUtil() {
        return new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    @Override
    public void destroy() {
        context = null;
        dataListener = null;
    }

    public ArrayList<String> getActiveAccountAndAddressList(){
        return activeAccountAndAddressList;
    }

    public void updateAccountList(){

        //activeAccountAndAddressList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        activeAccountAndAddressList.clear();
        activeAccountAndAddressBiMap.clear();

        int spinnerIndex = 0;

        //All accounts/addresses
        List<Account> allAccounts = null;
        List<LegacyAddress> allLegacyAddresses = payloadManager.getPayload().getLegacyAddresses();

        //Only active accounts/addresses (exclude archived)
        List<Account> activeAccounts = new ArrayList<>();
        if (payloadManager.getPayload().isUpgraded()) {

            allAccounts = payloadManager.getPayload().getHdWallet().getAccounts();//V3

            for (Account item : allAccounts) {
                if (!item.isArchived()) {
                    activeAccounts.add(item);
                }
            }
        }
        List<LegacyAddress> activeLegacyAddresses = new ArrayList<>();
        for (LegacyAddress item : allLegacyAddresses) {
            if (item.getTag() != PayloadManager.ARCHIVED_ADDRESS){
                activeLegacyAddresses.add(item);
            }
        }

        //"All" - total balance
        if (activeAccounts.size() > 1 || activeLegacyAddresses.size() > 0) {

            if (payloadManager.getPayload().isUpgraded()) {

                //Only V3 will display "All"
                Account all = new Account();
                all.setLabel(context.getResources().getString(R.string.all_accounts));
                all.setTags(Arrays.asList(TAG_ALL));
                activeAccountAndAddressList.add(all.getLabel());
                activeAccountAndAddressBiMap.put(all, spinnerIndex);
                spinnerIndex++;

            } else if (activeLegacyAddresses.size() > 1) {

                //V2 "All" at top of accounts spinner if wallet contains multiple legacy addresses
                ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.total_funds), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
                iAccount.setTags(Collections.singletonList(TAG_ALL));
                activeAccountAndAddressList.add(iAccount.getLabel());
                activeAccountAndAddressBiMap.put(iAccount, spinnerIndex);
                spinnerIndex++;
            }
        }

        //Add accounts to map
        int accountIndex = 0;
        for (Account item : activeAccounts) {

            if (item.getLabel().trim().length() == 0)item.setLabel("Account: " + accountIndex);//Give unlabeled account a label

            activeAccountAndAddressList.add(item.getLabel());
            activeAccountAndAddressBiMap.put(item, spinnerIndex);
            spinnerIndex++;
            accountIndex++;
        }

        //Add "Imported Addresses" or "Total Funds" to map
        if (payloadManager.getPayload().isUpgraded() && activeLegacyAddresses.size() > 0) {

            //Only V3 - Consolidate and add Legacy addresses to "Imported Addresses" at bottom of accounts spinner
            ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            iAccount.setTags(Collections.singletonList(TAG_IMPORTED_ADDRESSES));
            activeAccountAndAddressList.add(iAccount.getLabel());
            activeAccountAndAddressBiMap.put(iAccount, spinnerIndex);
            spinnerIndex++;

        }else{
            for (LegacyAddress legacyAddress : activeLegacyAddresses) {

                //If address has no label, we'll display address
                String labelOrAddress = legacyAddress.getLabel() == null || legacyAddress.getLabel().trim().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();

                //Prefix "watch-only"
                if (legacyAddress.isWatchOnly()) {
                    labelOrAddress = context.getString(R.string.watch_only_label) + " " + labelOrAddress;
                }

                activeAccountAndAddressList.add(labelOrAddress);
                activeAccountAndAddressBiMap.put(legacyAddress, spinnerIndex);
                spinnerIndex++;
            }
        }

        //If we have multiple accounts/addresses we will show dropdown in toolbar, otherwise we will only display a static text
        dataListener.onRefreshAccounts();
    }

    public PayloadManager getPayloadManager() {
        return payloadManager;
    }

    public List<Tx> getTransactionList(){
        return transactionList;
    }

    //TODO refactor isBTC out
    public void updateBalanceAndTransactionList(Intent intent, int accountSpinnerPosition, boolean isBTC) {
        double btc_balance;
        double fiat_balance;

        Object object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);//the current selected item in dropdown (Account or Legacy Address)

        //If current selected item gets edited by another platform object might become null
        if (object == null) {
            dataListener.onAccountSizeChange();
            object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);
        }

        transactionListDataManager.clearTransactionList();
        transactionListDataManager.generateTransactionList(object);
        transactionList = transactionListDataManager.getTransactionList();
        btc_balance = transactionListDataManager.getBtcBalance(object);

        // Returning from SendFragment the following will happen
        // After sending btc we create a "placeholder" tx until websocket handler refreshes list
        if (intent != null && intent.getExtras() != null) {
            long amount = intent.getLongExtra("queued_bamount", 0);
            String strNote = intent.getStringExtra("queued_strNote");
            String direction = intent.getStringExtra("queued_direction");
            long time = intent.getLongExtra("queued_time", System.currentTimeMillis() / 1000);

            @SuppressLint("UseSparseArrays")
            Tx tx = new Tx("", strNote, direction, amount, time, new HashMap<>());

            transactionList = transactionListDataManager.insertTransactionIntoListAndReturnSorted(tx);
        } else if (transactionList.size() > 0) {
            if (transactionList.get(0).getHash().isEmpty()) transactionList.remove(0);
        }

        //Update Balance
        String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double btc_fx = ExchangeRateFactory.getInstance().getLastPrice(strFiat);
        fiat_balance = btc_fx * (btc_balance / 1e8);

        String balanceTotal;
        if (isBTC) {
            balanceTotal = (getMonetaryUtil().getDisplayAmountWithFormatting(btc_balance) + " " + getDisplayUnits());
        } else {
            balanceTotal = (getMonetaryUtil().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat);
        }

        setBalance(balanceTotal);
        dataListener.onRefreshBalanceAndTransactions();
    }

    public String getDisplayUnits() {
        return (String) getMonetaryUtil().getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    public void startWebSocketService(){
        if (!osUtil.isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            context.startService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        } else {
            context.stopService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
            context.startService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }
}
