package info.blockchain.wallet.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ItemAccount {

    public String label;
    public String balance;
    public String tag;

    public Object accountObject;

    public ItemAccount(@NonNull String label, @NonNull String balance, @Nullable String tag, @Nullable Object accountObject) {
        this.label = label;
        this.balance = balance;
        this.tag = tag;
        this.accountObject = accountObject;
    }
}
