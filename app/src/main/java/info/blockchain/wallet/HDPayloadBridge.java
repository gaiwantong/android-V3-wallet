package info.blockchain.wallet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;
//import android.util.Log;

import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.core.AddressFormatException;

import org.apache.commons.codec.DecoderException;

import org.json.JSONObject;
import org.json.JSONException;

import info.blockchain.wallet.hd.*;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.*;
import info.blockchain.wallet.util.AddressFactory;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrefsUtil;

public class HDPayloadBridge	{
	
	private static Context context = null;
	private static HDPayloadBridge instance = null;

    private HDPayloadBridge()	{ ; }

    public static HDPayloadBridge getInstance(Context ctx) {
    	
    	context = ctx;

        if (instance == null) {
            instance = new HDPayloadBridge();
        }

        return instance;
    }

    public static HDPayloadBridge getInstance() {
    	
        if (instance == null) {
            instance = new HDPayloadBridge();
        }

        return instance;
    }

    public boolean init(CharSequenceX password) throws JSONException, IOException, DecoderException, AddressFormatException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicWordException	{
    	
        PayloadFactory.getInstance().get(PrefsUtil.getInstance(context).getValue(PrefsUtil.GUID, ""),
        		PrefsUtil.getInstance(context).getValue(PrefsUtil.SHARED_KEY, ""),
				password);

        if(PayloadFactory.getInstance().get() == null || PayloadFactory.getInstance().get().getJSON() == null) {
        	return false;
        }

        //
    	// create HD wallet if not present and sync w/ payload, replace this w/ user prompt + setup
        //
        if(PayloadFactory.getInstance().get().getHdWallets() == null || PayloadFactory.getInstance().get().getHdWallets().size() == 0) {
        	HD_WalletFactory.getInstance(context).newWallet(12, "", 1);
        	HDWallet hdw = new HDWallet();
        	hdw.setSeedHex(HD_WalletFactory.getInstance(context).get().getSeedHex());
        	List<Account> accounts = new ArrayList<Account>();
        	accounts.add(new Account());
        	accounts.get(0).setXpub(HD_WalletFactory.getInstance(context).get().getAccount(0).xpubstr());
        	accounts.get(0).setXpriv(HD_WalletFactory.getInstance(context).get().getAccount(0).xprvstr());
        	hdw.setAccounts(accounts);
        	PayloadFactory.getInstance().get().setHdWallets(hdw);
    		PayloadFactory.getInstance(context).remoteSaveThread();
        }

        PayloadFactory.getInstance().store();

        getBalances();

        // update highest idxs here, they were just updated above in getBalances();
        List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
        for(Account a : accounts) {
            a.setNbReceiveAddresses(MultiAddrFactory.getInstance().getHighestTxReceiveIdx(a.getXpub()));
            a.setNbChangeAddresses(MultiAddrFactory.getInstance().getHighestTxChangeIdx(a.getXpub()));
        }

        return true;
     }

    public void getBalances() throws JSONException, IOException, DecoderException, AddressFormatException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicWordException	{
    	
        String[] _xpubs = null;
		String[] addr = null;
		
		if(PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0)	{
			addr = getLegacyAddresses();
			MultiAddrFactory.getInstance().getLegacy(addr, false);
		}

        _xpubs = getXPUBs();
		JSONObject xpubObj = MultiAddrFactory.getInstance().getXPUB(_xpubs);

    }

    public void addAccount() throws IOException, MnemonicException.MnemonicLengthException {

    	String xpub = null;
    	String xpriv = null;
    	
    	if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
    		int before = HD_WalletFactory.getInstance(context).get().getAccounts().size();
    		HD_WalletFactory.getInstance(context).get().addAccount();
    		int after = HD_WalletFactory.getInstance(context).get().getAccounts().size();
        	Toast.makeText(context, "Created account: " + HD_WalletFactory.getInstance(context).get().getAccount(HD_WalletFactory.getInstance(context).get().getAccounts().size() - 1).getLabel() + ". Wallet had " + before + " accounts, now has " + after + ".", Toast.LENGTH_SHORT).show();

        	xpub = HD_WalletFactory.getInstance(context).get().getAccounts().get(HD_WalletFactory.getInstance(context).get().getAccounts().size() - 1).xpubstr();
        	xpriv = HD_WalletFactory.getInstance(context).get().getAccounts().get(HD_WalletFactory.getInstance(context).get().getAccounts().size() - 1).xprvstr();
    	}
    	else {
    		int before = HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().size();
    		HD_WalletFactory.getInstance(context).getWatchOnlyWallet().addAccount();
    		int after = HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().size();
        	Toast.makeText(context, "Created account: " + HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccount(HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().size() - 1).getLabel() + ". Wallet had " + before + " accounts, now has " + after + ".", Toast.LENGTH_SHORT).show();

        	xpub = HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().get(HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().size() - 1).xpubstr();
        	xpriv = HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().get(HD_WalletFactory.getInstance(context).getWatchOnlyWallet().getAccounts().size() - 1).xprvstr();
    	}

    	List<Tx> txs = new ArrayList<Tx>();
    	MultiAddrFactory.getInstance().getXpubTxs().put(xpub, txs);
    	MultiAddrFactory.getInstance().getXpubAmounts().put(xpub, 0L);

    	List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
        Account account = new Account("Account " + (int)(accounts.size() + 1));

    	account.setXpub(xpub);
    	if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
        	account.setXpriv(xpriv);
    	}
    	else {
    		String encrypted_xpriv = DoubleEncryptionFactory.getInstance().encrypt(
        			xpriv,
        			PayloadFactory.getInstance().get().getSharedKey(),
    	        	PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
        			PayloadFactory.getInstance().get().getIterations());
        	account.setXpriv(encrypted_xpriv);
    	}

        if(accounts.get(accounts.size() - 1) instanceof ImportedAccount) {
        	accounts.add(accounts.size() - 1, account);
        }
        else {
        	accounts.add(account);
        }

        PayloadFactory.getInstance().get().getHdWallet().setAccounts(accounts);
		PayloadFactory.getInstance(context).remoteSaveThread();

    }

    public String getHDSeed() throws IOException, MnemonicException.MnemonicLengthException {
    	return HD_WalletFactory.getInstance(context).get().getSeedHex();
    }
    
    public String getHDMnemonic() throws IOException, MnemonicException.MnemonicLengthException {
    	return HD_WalletFactory.getInstance(context).get().getMnemonic();
    }

    public String getHDPassphrase() throws IOException, MnemonicException.MnemonicLengthException {
    	return HD_WalletFactory.getInstance(context).get().getPassphrase();
    }

    public ReceiveAddress getReceiveAddress(int accountIdx) throws DecoderException, IOException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicLengthException, AddressFormatException {

        if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            return AddressFactory.getInstance(context, null).get(accountIdx, 0);
        }
        else {
            return AddressFactory.getInstance(context, getXPUBs()).get(accountIdx, 0);
        }

    }

	public String account2Xpub(int accountIdx)	{

		return PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getXpub();

	}

	public void createHDWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException	{
    	HD_WalletFactory.getInstance(context).newWallet(12, passphrase, 1);
    	PayloadFactory.getInstance(context).createBlockchainWallet(HD_WalletFactory.getInstance(context).get());
	}

	public void restoreHDWallet(String seed, String passphrase, int nbAccounts) throws IOException, AddressFormatException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException	{
    	HD_WalletFactory.getInstance(context).restoreWallet(seed, passphrase, 1);
    	PayloadFactory.getInstance(context).createBlockchainWallet(HD_WalletFactory.getInstance(context).get());
	}

	//
	//
	//
	private String[] getXPUBs()	throws IOException, DecoderException, AddressFormatException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicChecksumException, MnemonicException.MnemonicWordException {
		
		ArrayList<String> xpubs = new ArrayList<String>();

		if(!PayloadFactory.getInstance().get().isDoubleEncrypted()) {

			HD_Wallet hd_wallet = null;
			
	        if(PayloadFactory.getInstance().get().getHdWallet() != null) {
	    		hd_wallet = HD_WalletFactory.getInstance(context).restoreWallet(PayloadFactory.getInstance().get().getHdWallet().getSeedHex(), "", PayloadFactory.getInstance().get().getHdWallet().getAccounts().size());
	        }

		}

		int nb_accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts().size();
		for(int i = 0; i < nb_accounts; i++) {
			String s = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(i).getXpub();
			if(s != null && s.length() > 0) {
				xpubs.add(s);
			}
		}

		return xpubs.toArray(new String[xpubs.size()]);
	}

	private String[] getLegacyAddresses()	{
		ArrayList<String> addresses = new ArrayList<String>();
		List<LegacyAddress> legacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();
		for(LegacyAddress legacyAddr : legacyAddresses) {
			if(legacyAddr.getTag() == 0L) {
				addresses.add(legacyAddr.getAddress());
			}
		}
		return addresses.toArray(new String[addresses.size()]);
	}

}
