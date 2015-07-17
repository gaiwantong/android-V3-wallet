package info.blockchain.wallet.payload;

import java.util.*;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class HDWallet {

    private String strSeedHex = null;
    private List<Account> accounts = null;
    private String strPassphrase = "";
    private boolean mnemonic_verified = false;
    private int default_account_idx = 0;
    private Map<String,PaidTo> paidTo = null;

    public HDWallet() {
        accounts = new ArrayList<Account>();
        paidTo = new HashMap<String,PaidTo>();
    }

    public HDWallet(String seed, List<Account> accounts, String passphrase) {
        this.strSeedHex = seed;
        this.accounts = accounts;
        this.strPassphrase = passphrase;
        paidTo = new HashMap<String,PaidTo>();
    }

    public HDWallet(String seed, List<Account> accounts, String passphrase, HashMap<String,PaidTo> paidTo) {
        this.strSeedHex = seed;
        this.accounts = accounts;
        this.strPassphrase = passphrase;
        this.paidTo = paidTo;
    }

    public String getSeedHex() {
        return strSeedHex;
    }

    public void setSeedHex(String seed) {
        this.strSeedHex = seed;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public String getPassphrase() {
        return strPassphrase;
    }

    public void setPassphrase(String passphrase) {
        this.strPassphrase = passphrase;
    }

    public boolean isMnemonicVerified() {
        return mnemonic_verified;
    }

    public void mnemonic_verified(boolean verified) {
        this.mnemonic_verified = verified;
    }

    public int getDefaultIndex() {
        return default_account_idx;
    }

    public void setDefaultIndex(int idx) {
        this.default_account_idx = idx;
    }

    public Map<String, PaidTo> getPaidTo() {
        return paidTo;
    }

    public void setPaidTo(Map<String, PaidTo> paidTo) {
        this.paidTo = paidTo;
    }

    public JSONObject dumpJSON() throws JSONException {

        JSONObject obj = new JSONObject();

        obj.put("seed_hex", strSeedHex);
        obj.put("passphrase", strPassphrase);
        obj.put("default_account_idx", default_account_idx);
        obj.put("mnemonic_verified", mnemonic_verified);

        JSONObject paidToObj = new JSONObject();
        Set<String> pkeys = paidTo.keySet();
        for(String key : pkeys)  {
            PaidTo pto = paidTo.get(key);
            JSONObject pobj = new JSONObject();
            pobj.put("email", pto.getEmail());
            pobj.put("mobile", pto.getMobile());
            pobj.put("redeemedAt", pto.getRedeemedAt());
            pobj.put("address", pto.getAddress());
            paidToObj.put(key, pobj);
        }
        obj.put("paidTo", paidToObj);

        JSONArray accs = new JSONArray();
        for(Account account : accounts) {
        	if(!(account instanceof ImportedAccount)) {
                accs.put(account.dumpJSON());
        	}
        }
        obj.put("accounts", accs);

        return obj;
    }

}
