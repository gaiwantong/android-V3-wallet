package info.blockchain.wallet.payload;

import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Account {

    protected boolean isArchived = false;
    protected int nbChangeAddresses = 0;
    protected int nbReceiveAddresses = 0;
    protected String strLabel = null;
    protected List<ReceiveAddress> receiveAddresses = null;
    protected List<String> tags = null;
    protected long amount = 0L;
    protected String strXpub = null;
    protected String strXpriv = null;

    public Account() {
        receiveAddresses = new ArrayList<ReceiveAddress>();
        tags = new ArrayList<String>();
        strXpub = "";
        strXpriv = "";
    }

    /*
    public Account(boolean isArchived, int change, int receive, String label, List<ReceiveAddress> receiveAddresses, List<String> tags, long amount) {
        this.isArchived = isArchived;
        this.nbChangeAddresses = change;
        this.nbReceiveAddresses = receive;
        this.strLabel = label;
        this.receiveAddresses = receiveAddresses;
        this.tags = tags;
        this.amount = amount;
    }
    */

    public Account(boolean isArchived, int change, int receive, String label) {
        this.isArchived = isArchived;
        this.nbChangeAddresses = change;
        this.nbReceiveAddresses = receive;
        this.strLabel = label;
        receiveAddresses = new ArrayList<ReceiveAddress>();
        tags = new ArrayList<String>();
        strXpub = "";
        strXpriv = "";
    }

    public Account(String label) {
        this.isArchived = false;
        this.nbChangeAddresses = 0;
        this.nbReceiveAddresses = 0;
        this.strLabel = label;
        this.receiveAddresses = new ArrayList<ReceiveAddress>();
        tags = new ArrayList<String>();
        strXpub = "";
        strXpriv = "";
    }

    public void incChange() {
        nbChangeAddresses++;
    }

    public void incReceive() {
        nbReceiveAddresses++;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public int getNbChangeAddresses() {
        return nbChangeAddresses;
    }

    public void setNbChangeAddresses(int nbChangeAddresses) {
        this.nbChangeAddresses = nbChangeAddresses;
    }

    public int getNbReceiveAddresses() {
        return nbReceiveAddresses;
    }

    public void setNbReceiveAddresses(int nbReceiveAddresses) {
        this.nbReceiveAddresses = nbReceiveAddresses;
    }

    public String getLabel() {
    	if(strLabel == null) {
            return "";
    	}
    	else if(strLabel.length() > 32) {
            return strLabel.replaceAll("\n", " ").substring(0, 32) + "...";
    	}
    	else {
            return strLabel.replaceAll("\n", " ");
    	}
    }

    public String getFullLabel() {
    	if(strLabel == null) {
    		return "";
    	}
    	else {
            return strLabel;
    	}
    }

    public void setLabel(String strLabel) {
        this.strLabel = strLabel;
    }

    public List<ReceiveAddress> getReceiveAddresses() {
        return receiveAddresses;
    }

    public void setReceiveAddresses(List<ReceiveAddress> receiveAddresses) {
        this.receiveAddresses = receiveAddresses;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getXpub() {
        return strXpub;
    }

    public void setXpub(String strXpub) {
        this.strXpub = strXpub;
    }

    public String getXpriv() {
        return strXpriv;
    }

    public void setXpriv(String strXpriv) {
        this.strXpriv = strXpriv;
    }

    public JSONObject dumpJSON() throws JSONException {

        JSONObject obj = new JSONObject();

        obj.put("archived", isArchived);
        obj.put("change_addresses", nbChangeAddresses);
        obj.put("receive_addresses_count", nbReceiveAddresses);
        obj.put("label", strLabel == null ? "" : strLabel);
        obj.put("xpub", strXpub == null ? "" : strXpub);
        obj.put("xpriv", strXpriv == null ? "" : strXpriv);

        JSONArray receives = new JSONArray();
        for(ReceiveAddress receiveAddress : receiveAddresses) {
            receives.put(receiveAddress.dumpJSON());
        }
        /*
         * temporary: do not write out receive addresses/payment requests until further notice
         */
//        obj.put("receive_addresses", receives);

        /*
        JSONArray _tags = new JSONArray();
        for(String tag : tags) {
            _tags.put(tag);
        }
        obj.put("tags", _tags);

        obj.put("amount", amount);
        */

        return obj;
    }

}
