package info.blockchain.wallet.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import piuk.blockchain.android.R;

public class AccountsUtil {

    private static Context   context  = null;
    private static AccountsUtil instance = null;

	private static int currentSpinnerIndex = 0;//The current selected spinner index for balance/receive/send fragments

	//Balance Screen
	private static LinkedHashMap<Integer, Account> balanceAccountMap = null;
	private static LinkedHashMap<Integer, Integer> balanceAccountIndexResolver = null;

	//Send/Receive Screen
	private static LinkedHashMap<Integer, Account> sendReceiveAccountMap = null;
	private static LinkedHashMap<Integer, Integer> sendReceiveAccountIndexResolver = null;
	private static List<String> sendReceiveAccountList = null;//used for spinner
	private static int lastHDIndex = 0;
	private static List<LegacyAddress> legacyAddresses;

    public static AccountsUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            initAccountMaps();
            instance = new AccountsUtil();
        }

        return instance;
    }

	public void setCurrentSpinnerIndex(int currentSpinnerIndex){
		this.currentSpinnerIndex = currentSpinnerIndex;
	}

	public int getCurrentSpinnerIndex(){
		return currentSpinnerIndex;
	}

	public static void initAccountMaps(){
		initBalanceAccountMap();
		initSendReceiveAccountMap();
	}

	public static void initBalanceAccountMap(){

		balanceAccountMap = new LinkedHashMap<Integer, Account>();
		balanceAccountIndexResolver = new LinkedHashMap<Integer, Integer>();
		int accountIndex = 0;
		int spinnerIndex = 0;

        HDWallet hdWallet = PayloadFactory.getInstance().get().getHdWallet();
		List<Account> accounts = new ArrayList<>();
        if(hdWallet!=null && hdWallet.getAccounts()!=null)accounts = hdWallet.getAccounts();

		List<LegacyAddress> legacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

		//All Account - if multiple accounts or contains legacy address
		if(accounts != null && accounts.size() > 1 || legacyAddresses.size() > 0) {

            if(PayloadFactory.getInstance(context).get().isUpgraded()) {
                Account all = new Account();
                all.setLabel(context.getResources().getString(R.string.all_accounts));
                balanceAccountMap.put(-1, all);
            }else{
                ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.total_funds), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), 0l);
                accounts.add(iAccount);
                getLegacyTotalFunds(iAccount);
            }
		}

        //Add Legacy addresses to accounts
        if (PayloadFactory.getInstance(context).get().isUpgraded()) {
            if (accounts != null && accounts.size() > 0
                    && !(accounts.get(accounts.size() - 1) instanceof ImportedAccount)
                    && (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0)) {

                ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
                accounts.add(iAccount);
            }
        } else {

            List<LegacyAddress> list = PayloadFactory.getInstance().get().getLegacyAddresses();

            if(list!=null) {
                for (int j = 0; j < list.size(); j++) {
                    LegacyAddress leg = list.get(j);
                    if(leg.getLabel()==null || leg.getLabel().length() == 0)leg.setLabel(leg.getAddress());

                    List<LegacyAddress> legList = new ArrayList<>();
                    legList.add(leg);
                    ImportedAccount imp = new ImportedAccount(leg.getLabel(), legList, new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance(leg.getAddress()));
                    imp.setLabel(leg.getLabel());
                    accounts.add(imp);
                }
            }
        }

		for (Account item : accounts){

			if(!item.isArchived()) {

				if(item.getLabel().length() == 0)item.setLabel("Account: "+accountIndex);

				balanceAccountMap.put(accountIndex, item);
				balanceAccountIndexResolver.put(spinnerIndex,accountIndex);
				spinnerIndex++;
			}

			accountIndex++;
		}
	}

	public static void initSendReceiveAccountMap() {

		sendReceiveAccountMap = new LinkedHashMap<Integer, Account>();
		sendReceiveAccountIndexResolver = new LinkedHashMap<Integer, Integer>();
        sendReceiveAccountList = new ArrayList<String>();
		int accountIndex = 0;
		int spinnerIndex = 0;

        if(PayloadFactory.getInstance().get().isUpgraded()) {
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
            for (Account item : accounts){

                if(item instanceof ImportedAccount)continue;
                if(!item.isArchived()) {

                    if(item.getLabel().length() == 0)item.setLabel("Account: "+accountIndex);

                    sendReceiveAccountMap.put(accountIndex, item);
                    sendReceiveAccountIndexResolver.put(spinnerIndex,accountIndex);
                    spinnerIndex++;
                }

                accountIndex++;
            }

            sendReceiveAccountList = new ArrayList<String>();
            for (Account account : sendReceiveAccountMap.values()) {
                sendReceiveAccountList.add(account.getLabel());
            }
            lastHDIndex = sendReceiveAccountList.size();
        }

		//Add individual legacy addresses
		ImportedAccount iAccount = null;
		if(PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
			iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
		}
		if(iAccount != null) {
			legacyAddresses = iAccount.getLegacyAddresses();
			for(int j = 0; j < legacyAddresses.size(); j++) {
				sendReceiveAccountList.add((legacyAddresses.get(j).getLabel() == null || legacyAddresses.get(j).getLabel().length() == 0) ? legacyAddresses.get(j).getAddress() : legacyAddresses.get(j).getLabel());
			}
		}
	}

	public LinkedHashMap<Integer, Account> getBalanceAccountMap(){
		return balanceAccountMap;
	}

	public LinkedHashMap<Integer, Integer> getBalanceAccountIndexResolver(){
		return balanceAccountIndexResolver;
	}

	public LinkedHashMap<Integer, Integer> getSendReceiveAccountIndexResolver(){
		return sendReceiveAccountIndexResolver;
	}

	public static LinkedHashMap<Integer, Account> getSendReceiveAccountMap() {
		return sendReceiveAccountMap;
	}

	public static List<String> getSendReceiveAccountList() {
		return sendReceiveAccountList;
	}

	public static int getLastHDIndex() {
		return lastHDIndex;
	}

	public static LegacyAddress getLegacyAddress(int position){
		return legacyAddresses.get(position);
	}

	public static List<LegacyAddress> getLegacyAddresses(){
		return legacyAddresses;
	}

    private static void getLegacyTotalFunds(final ImportedAccount iAccount){
        new Thread(new Runnable() {
            @Override
            public void run() {
                long balance = 0l;
                for(LegacyAddress legacyAddress : iAccount.getLegacyAddresses()){

                    JSONObject info = AddressInfo.getInstance().getAddressInfo(legacyAddress.getAddress());
                    long add = 0l;
                    try {
                        add = info.getLong("final_balance");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        add = 0l;
                    }
                    balance+=add;
                }
                iAccount.setAmount(balance);
                MultiAddrFactory.getInstance().setLegacyBalance(balance);

                Intent intent = new Intent("info.blockchain.wallet.BalanceFragment.REFRESH");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }).start();
    }
}
