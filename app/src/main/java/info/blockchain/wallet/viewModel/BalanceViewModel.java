package info.blockchain.wallet.viewModel;

import com.google.common.collect.HashBiMap;

import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v4.util.Pair;
import android.util.Log;

import info.blockchain.wallet.model.BalanceModel;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

    public Pair<HashMap<String,Long>, HashMap<String,Long>> filterNonChangeAddresses(Transaction transactionDetails, Tx transaction){

        HashMap<String, String> addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();

        HashMap<String,Long> inputMap = new HashMap<>();
        HashMap<String,Long> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        //Inputs / From field
        if (transaction.getDirection().equals(MultiAddrFactory.RECEIVED) && transactionDetails.getInputs().size() > 0) {//only 1 addr for receive
            inputMap.put(transactionDetails.getInputs().get(0).addr, transactionDetails.getInputs().get(0).value);

        } else {

            for (Transaction.xPut input : transactionDetails.getInputs()) {

                if (!transaction.getDirection().equals(MultiAddrFactory.RECEIVED)){

                    //Move or Send
                    //The address belongs to us
                    String xpub = addressToXpubMap.get(input.addr);

                    //Address belongs to xpub we own
                    if(xpub != null) {
                        //Only add xpub once
                        if (!inputXpubList.contains(xpub)) {
                            inputMap.put(input.addr, input.value);
                            inputXpubList.add(xpub);
                        }
                    }else{
                        //Legacy Address we own
                        inputMap.put(input.addr, input.value);
                    }

                }else{
                    //Receive
                    inputMap.put(input.addr, input.value);
                }
            }
        }

        //Outputs / To field
        for (Transaction.xPut output : transactionDetails.getOutputs()) {

            if (MultiAddrFactory.getInstance().isOwnHDAddress(output.addr)) {

                //If output address belongs to an xpub we own - we have to check if it's change
                String xpub = addressToXpubMap.get(output.addr);
                if(inputXpubList.contains(xpub)){
                    continue;//change back to same xpub
                }

                //Receiving to same address multiple times?
                if (outputMap.containsKey(output.addr)) {
                    long prevAmount = outputMap.get(output.addr) + output.value;
                    outputMap.put(output.addr, prevAmount);
                } else {
                    outputMap.put(output.addr, output.value);
                }

            } else if(payloadManager.getPayload().getLegacyAddressStrings().contains(output.addr) ||
                    payloadManager.getPayload().getWatchOnlyAddressStrings().contains(output.addr)){

                //If output address belongs to a legacy address we own - we have to check if it's change

                //If it goes back to same address AND if it's not the total amount sent (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if(inputMap.containsKey(output.addr) && output.value != transaction.getAmount()){
                    continue;//change back to same input address
                }

                //Output more than tx amount - change
                if(output.value > transaction.getAmount()){
                    continue;
                }

                outputMap.put(output.addr, output.value);

            } else {

                //Address does not belong to us
                if (transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {
                    continue;//ignore other person's change
                }else{
                    outputMap.put(output.addr, output.value);
                }
            }
        }

        return new Pair<>(inputMap,outputMap);
    }

    public String addressToLabel(String address){

        HDWallet hdWallet = payloadManager.getPayload().getHdWallet();
        List<Account> accountList = new ArrayList<>();
        if(hdWallet != null && hdWallet.getAccounts() != null)
            accountList = hdWallet.getAccounts();

        HashMap<String, String> addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();

        //If address belongs to owned xpub
        if (MultiAddrFactory.getInstance().isOwnHDAddress(address)){

            String xpub = addressToXpubMap.get(address);
            if(xpub != null) {
                //eventhough it looks like this shouldn't happen,
                //it sometimes happens with transfers if user clicks to view details immediately.
                //TODO - see if isOwnHDAddress could be updated to solve this
                int accIndex = payloadManager.getPayload().getXpub2Account().get(xpub);
                String label = accountList.get(accIndex).getLabel();
                if (label != null && !label.isEmpty())
                    return label;
            }

            //If address one of owned legacy addresses
        }else if (payloadManager.getPayload().getLegacyAddressStrings().contains(address) ||
                payloadManager.getPayload().getWatchOnlyAddressStrings().contains(address)){

            Payload payload = payloadManager.getPayload();

            String label = payload.getLegacyAddresses().get(payload.getLegacyAddressStrings().indexOf(address)).getLabel();
            if (label != null && !label.isEmpty())
                return label;
        }

        return address;
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
        if (activeAccounts != null && activeAccounts.size() > 1 || activeLegacyAddresses.size() > 0) {

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
                iAccount.setTags(Arrays.asList(TAG_ALL));
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
            iAccount.setTags(Arrays.asList(TAG_IMPORTED_ADDRESSES));
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

    public List<Tx> getTransactionList(){
        return transactionList;
    }

    //TODO refactor isBTC out
    public void updateBalanceAndTransactionList(Intent intent, int accountSpinnerPosition, boolean isBTC) {
        ArrayList<Tx> unsortedTransactionList = new ArrayList<>();//We will sort this list by date shortly
        double btc_balance = 0.0;
        double fiat_balance = 0.0;

        Object object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);//the current selected item in dropdown (Account or Legacy Address)

        //If current selected item gets edited by another platform object might become null
        if(object == null){
            dataListener.onAccountSizeChange();
            object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);
        }

        if(object instanceof Account){
            //V3
            Account account = ((Account) object);

            //V3 - All
            if(account.getTags().contains(TAG_ALL)){
                if (payloadManager.getPayload().isUpgraded()) {
                    //Total for accounts
                    List<Tx> allTransactions = getAllXpubAndLegacyTxs();
                    if(allTransactions != null)unsortedTransactionList.addAll(allTransactions);

                    //Balance = all xpubs + all legacy address balances
                    btc_balance = ((double) MultiAddrFactory.getInstance().getXpubBalance()) + ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());

                }else{
                    //Total for legacyAddresses
                    List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
                    if(allLegacyTransactions != null)unsortedTransactionList.addAll(allLegacyTransactions);
                    //Balance = all legacy address balances
                    btc_balance = ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());
                }
            }else if(account.getTags().contains(TAG_IMPORTED_ADDRESSES)){
                //V3 - Imported Addresses
                List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
                if(allLegacyTransactions != null)unsortedTransactionList.addAll(allLegacyTransactions);
                btc_balance = ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());

            }else{
                //V3 - Individual
                String xpub = account.getXpub();
                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    List<Tx> xpubTransactions = MultiAddrFactory.getInstance().getXpubTxs().get(xpub);
                    if(xpubTransactions != null)unsortedTransactionList.addAll(xpubTransactions);
                    HashMap<String, Long> xpubAmounts = MultiAddrFactory.getInstance().getXpubAmounts();
                    Long bal = (xpubAmounts.get(xpub) == null ? 0l : xpubAmounts.get(xpub));
                    btc_balance = ((double) (bal));
                }
            }

        }else{
            //V2
            LegacyAddress legacyAddress = ((LegacyAddress) object);
            List<Tx> legacyTransactions = MultiAddrFactory.getInstance().getAddressLegacyTxs(legacyAddress.getAddress());
            if (legacyTransactions != null)
                unsortedTransactionList.addAll(legacyTransactions);//V2 get single address' transactionList
            btc_balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());

        }

        //Returning from SendFragment the following will happen
        //After sending btc we create a "placeholder" tx until websocket handler refreshes list
        if (intent != null && intent.getExtras() != null) {
            long amount = intent.getLongExtra("queued_bamount", 0);
            String strNote = intent.getStringExtra("queued_strNote");
            String direction = intent.getStringExtra("queued_direction");
            long time = intent.getLongExtra("queued_time", System.currentTimeMillis() / 1000);

            Tx tx = new Tx("", strNote, direction, amount, time, new HashMap<Integer, String>());
            unsortedTransactionList.add(0, tx);
        } else if (unsortedTransactionList != null && unsortedTransactionList.size() > 0) {
            if (unsortedTransactionList.get(0).getHash().isEmpty()) unsortedTransactionList.remove(0);
        }

        //Sort transactionList as server does not return sorted transactionList
        transactionList.clear();
        Collections.sort(unsortedTransactionList, new TxDateComparator());
        transactionList.addAll(unsortedTransactionList);

        //Update Balance
        String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double btc_fx = ExchangeRateFactory.getInstance().getLastPrice(context, strFiat);
        fiat_balance = btc_fx * (btc_balance / 1e8);

        String balanceTotal = "";
        if (isBTC) {
            balanceTotal = (getMonetaryUtil().getDisplayAmountWithFormatting(btc_balance) + " " + getDisplayUnits());
        } else {
            balanceTotal = (getMonetaryUtil().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat);
        }

        setBalance(balanceTotal);
        dataListener.onRefreshBalanceAndTransactions();
    }

    public List<Tx> getAllXpubAndLegacyTxs(){

        //Remove duplicate txs
        HashMap<String, Tx> consolidatedTxsList = new HashMap<String, Tx>();

        List<Tx> allXpubTransactions = MultiAddrFactory.getInstance().getAllXpubTxs();
        for(Tx tx : allXpubTransactions){
            if(!consolidatedTxsList.containsKey(tx.getHash()))
                consolidatedTxsList.put(tx.getHash(), tx);
        }

        List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
        for(Tx tx : allLegacyTransactions){
            if(!consolidatedTxsList.containsKey(tx.getHash()))
                consolidatedTxsList.put(tx.getHash(), tx);
        }

        return new ArrayList(consolidatedTxsList.values());
    }

    public String getDisplayUnits() {
        return (String) getMonetaryUtil().getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    private class TxDateComparator implements Comparator<Tx> {

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

    public void startWebSocketService(){
        if (!osUtil.isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            context.startService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        } else {
            context.stopService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
            context.startService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }
}
