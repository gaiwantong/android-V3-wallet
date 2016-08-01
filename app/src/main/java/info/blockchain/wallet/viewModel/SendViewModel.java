package info.blockchain.wallet.viewModel;

import android.content.Context;

import info.blockchain.wallet.model.ItemSendAddress;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.AddressBookEntry;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;

public class SendViewModel {

    private DataListener dataListener;
    private Context context;

    private PayloadManager payloadManager;
    private MonetaryUtil monetaryUtil;
    private PrefsUtil prefsUtil;
    private String btcUnit;

    public SendViewModel(Context context, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;
        this.payloadManager = PayloadManager.getInstance();
        this.prefsUtil = new PrefsUtil(context);
        this.monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        this.btcUnit = monetaryUtil.getBTCUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    public interface DataListener {
        void onSingleFromAddress();
        void onSingleToAddress();
    }

    public List<ItemSendAddress> getAddressList() {

        ArrayList<ItemSendAddress> result = new ArrayList<>();

        //V3
        if (payloadManager.getPayload().isUpgraded()) {

            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
            for (Account account : accounts) {

                if (account.isArchived())
                    continue;//skip archived account

                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(account.getXpub())) {
                    long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(account.getXpub());
                    String balance = "(" + monetaryUtil.getDisplayAmount(amount) + " " + btcUnit + ")";
                    result.add(new ItemSendAddress(account.getLabel(), balance, null, account));
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
            String balance = "(" + monetaryUtil.getDisplayAmount(amount) + " " + btcUnit + ")";
            result.add(new ItemSendAddress(labelOrAddress, balance, tag, legacyAddress));
        }

        if(result.size() == 1){
            //Only a single account/address available in wallet
            dataListener.onSingleFromAddress();
        }

        //Address Book
        List<AddressBookEntry> addressBookEntries = payloadManager.getPayload().getAddressBookEntries();
        for(AddressBookEntry addressBookEntry : addressBookEntries){

            //If address has no label, we'll display address
            String labelOrAddress = addressBookEntry.getLabel() == null || addressBookEntry.getLabel().length() == 0 ? addressBookEntry.getAddress() : addressBookEntry.getLabel();

            result.add(new ItemSendAddress(labelOrAddress, "", context.getResources().getString(R.string.address_book_label), addressBookEntry));
        }

        if(result.size() == 1){
            //Only a single account/address available in wallet and no addressBook entries
            dataListener.onSingleToAddress();
        }

        return result;
    }
}
