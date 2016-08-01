package info.blockchain.wallet.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by riaanvos on 01/08/16.
 */
public class ItemSendAddress {

    public String label;
    public String balance;
    public String tag;

    public Object accountObject;

    public ItemSendAddress(@NonNull String label, @NonNull String balance, @NonNull String tag, @Nullable Object accountObject) {
        this.label = label;
        this.balance = balance;
        this.tag = tag;
        this.accountObject = accountObject;
    }
}
