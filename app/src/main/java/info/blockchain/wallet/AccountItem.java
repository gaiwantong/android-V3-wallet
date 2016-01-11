package info.blockchain.wallet;

import android.graphics.drawable.Drawable;

public class AccountItem {

    String title;
    Drawable icon;
    String amount;

    public AccountItem(String title, Drawable icon, String amount) {
        this.title = title;
        this.icon = icon;
        this.amount = amount;
    }

    public AccountItem(String title, String amount, Drawable icon) {
        this.title = title;
        this.amount = amount;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getAmount() {
        return amount;
    }
}