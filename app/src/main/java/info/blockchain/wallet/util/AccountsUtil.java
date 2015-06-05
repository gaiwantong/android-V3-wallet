package info.blockchain.wallet.util;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import info.blockchain.wallet.R;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;

public class AccountsUtil {

    private static Context   context  = null;
    private static AccountsUtil instance = null;

	private static int currentSpinnerIndex = 0;

	//Balance Screen
	private static LinkedHashMap<Integer, Account> balanceAccountMap = null;
	private static LinkedHashMap<Integer, Integer> balanceAccountIndexResolver = null;
	private ArrayList<String> balanceSpinnerList = null;

	//Receive Screen
	private static LinkedHashMap<Integer, Account> receiveAccountMap = null;
	private static HashMap<Integer, Integer> receiveAccountIndexResolver = null;
	private ArrayList<String> receiveSpinnerList = null;

	//Send Screen
	private static LinkedHashMap<Integer, Account> sendAccountMap = null;
	private static HashMap<Integer, Integer> sendAccountIndexResolver = null;
	private ArrayList<String> sendSpinnerList = null;

    public static AccountsUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
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

	public void initBalanceAccounts(){

		balanceAccountMap = new LinkedHashMap<Integer, Account>();
		balanceAccountIndexResolver = new LinkedHashMap<Integer, Integer>();
		int accountIndex = 0;
		int spinnerIndex = 0;

		List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
		List<LegacyAddress> legacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

		//All Account - if multiple accounts or contains legacy address
		if(accounts != null && accounts.size() > 1 || legacyAddresses.size() > 0) {

			Account all = new Account();
			all.setLabel(context.getResources().getString(R.string.all_accounts));
			balanceAccountMap.put(-1, all);
		}

		//Add Legacy addresses to accounts
		if(accounts != null && accounts.size() > 0
				&& !(accounts.get(accounts.size() - 1) instanceof ImportedAccount)
				&& (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0)) {

			ImportedAccount iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
			accounts.add(iAccount);
		}

		for (Account item : accounts){

			if(!item.isArchived()) {

				if(item.getLabel().length() == 0)item.setLabel("Account: "+accountIndex);

				balanceAccountMap.put(accountIndex, item);
				balanceAccountIndexResolver.put(spinnerIndex, accountIndex);
				spinnerIndex++;
			}

			accountIndex++;
		}

		Log.v("","---------initBalanceAccounts------------");
		for(Map.Entry<Integer, Account> item : balanceAccountMap.entrySet()){

			Log.v("","balanceAccountMap: "+item.getKey()+" - "+item.getValue().getLabel());
		}
		for(Map.Entry<Integer, Integer> item : balanceAccountIndexResolver.entrySet()){

			Log.v("","spinnerKey: "+item.getKey()+" - label:"+balanceAccountMap.get(item.getValue()).getLabel());
		}

		setBalanceSpinnerList();
	}

	private void setBalanceSpinnerList(){

		balanceSpinnerList = new ArrayList<String>();
		for (Account item : balanceAccountMap.values()){
			balanceSpinnerList.add(item.getLabel());
		}

		Log.v("","-------------getBalanceSpinnerList-----------");
		for(String item : balanceSpinnerList){
			Log.v("", "item: " + item);
		}
	}

	public LinkedHashMap<Integer, Account> getBalanceAccountMap(){
		return balanceAccountMap;
	}

	public ArrayList<String> getBalanceSpinnerList(){
		return balanceSpinnerList;
	}

	public Account getAccountFromSpinnerIndex(int currentSpinnerIndex){
		return balanceAccountMap.get(balanceAccountIndexResolver.get(currentSpinnerIndex));
	}

	public int getAccountIndexFromSpinnerIndex(int currentSpinnerIndex){
		return balanceAccountIndexResolver.get(currentSpinnerIndex);
	}
}
