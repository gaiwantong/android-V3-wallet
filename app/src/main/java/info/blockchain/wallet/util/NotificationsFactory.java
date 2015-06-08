package info.blockchain.wallet.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import info.blockchain.wallet.R;

public class NotificationsFactory {

    public static NotificationManager mNotificationManager;
    private static Context context = null;
    private static NotificationsFactory instance = null;
	private static int notificationCount = 0;

    private NotificationsFactory()	{
    	;
    }

    public static NotificationsFactory getInstance(Context ctx) {
    	
    	context = ctx;
    	
    	if(instance == null) {
    		instance = new NotificationsFactory();
    	}
    	
    	return instance;
    }

    public void clearNotification(int id) {
		resetNotificationCounter();
        mNotificationManager.cancel(id);
    }

    public void setNotification(String title, String marquee, String text, int drawablePostLollipop, int drawablePreLollipop, Class cls, int id) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationCount ++;

		int drawableCompat = drawablePreLollipop;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			drawableCompat = drawablePostLollipop;

        Intent notifyIntent = new Intent(context, cls);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification mBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(drawableCompat)
				.setColor(context.getResources().getColor(R.color.blockchain_blue))
				.setContentTitle(title)
				.setContentIntent(intent)
				.setWhen(System.currentTimeMillis())
				.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.alert))
				.setNumber(notificationCount)
				.setTicker(marquee)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setDefaults(Notification.DEFAULT_ALL)
				.setContentText(text).build();

		mNotificationManager.notify(id, mBuilder);
	}

	public static void resetNotificationCounter(){
		notificationCount = 0;
	}
}
