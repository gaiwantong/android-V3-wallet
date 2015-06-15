package info.blockchain.wallet;

import android.graphics.drawable.Drawable;

public class MyAccountItem {

    String title;
    Drawable icon;
    String amount;

    public MyAccountItem(String title, Drawable icon, String amount) {
        this.title = title;
        this.icon = icon;
        this.amount = amount;
    }

    public MyAccountItem(String title,  String amount, Drawable icon) {
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

    public String getAmount(){
        return amount;
    }
}