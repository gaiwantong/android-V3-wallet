package info.blockchain.wallet.payload;

import info.blockchain.wallet.crypto.AESUtil;

import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Options {

    private int iterations = AESUtil.PasswordPBKDF2Iterations;
    private int fee_policy = 0;
    private boolean html5Notifs = false;
    private long logout_time = 600000L;
    private int tx_display = 0;
    private boolean always_keep_local_backup = false;
    private int tx_per_page = 30;
    private List<String> additionalSeeds = null;

    public Options() { additionalSeeds = new ArrayList<String>(); }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public void setFeePolicy(int policy) {
        this.fee_policy = policy;
    }

    public void setHtmlNotifs(boolean notifs) {
        this.html5Notifs = notifs;
    }

    public void setLogoutTime(long logout_time) {
        this.logout_time = logout_time;
    }

    public void setTxDisplay(int tx_display) {
        this.tx_display = tx_display;
    }

    public void setKeepLocalBackup(boolean keep) {
        this.always_keep_local_backup = keep;
    }

    public void setTxPerPage(int tx_per_page) {
        this.tx_per_page = tx_per_page;
    }

    public void setAdditionalSeeds(List<String> seeds) {
        this.additionalSeeds = seeds;
    }

    public int getIterations() {
        return iterations;
    }

    public int getFee_policy() {
        return fee_policy;
    }

    public boolean isHtml5Notifs() {
        return html5Notifs;
    }

    public long getLogoutTime() {
        return logout_time;
    }

    public int getTxDisplay() {
        return tx_display;
    }

    public boolean isKeepLocalBackup() {
        return always_keep_local_backup;
    }

    public int getTxPerPage() {
        return tx_per_page;
    }

    public List<String> getAdditionalSeeds() {
        return additionalSeeds;
    }

    public JSONObject dumpJSON() throws JSONException	 {

        JSONObject obj = new JSONObject();

        obj.put("pbkdf2_iterations", iterations);
        obj.put("fee_policy", fee_policy);
        obj.put("html5_notifications", html5Notifs);
        obj.put("logout_time", logout_time);
        obj.put("tx_display", tx_display);
        obj.put("always_keep_local_backup", always_keep_local_backup);
        obj.put("transactions_per_page", tx_per_page);

        JSONArray seeds = new JSONArray();
        for(String seed : additionalSeeds) {
            seeds.put(seed);
        }
//        obj.put("additional_seeds", seeds);

        return obj;
    }

}
