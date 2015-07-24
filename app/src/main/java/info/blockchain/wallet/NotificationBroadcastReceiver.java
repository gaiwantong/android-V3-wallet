package info.blockchain.wallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import info.blockchain.wallet.util.NotificationsFactory;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals("notification_cancelled"))
            NotificationsFactory.getInstance(context).resetNotificationCounter();
    }
}