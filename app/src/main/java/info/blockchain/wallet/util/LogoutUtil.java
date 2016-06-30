package info.blockchain.wallet.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import info.blockchain.wallet.ui.MainActivity;

public class LogoutUtil {

    private static LogoutUtil instance = null;
    private static Context context = null;

    private static final long LOGOUT_TIMEOUT = 1000L * 30L; // 30 seconds in milliseconds
    public static final String LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT";
    private static PendingIntent logoutPendingIntent;

    private LogoutUtil() {
    }

    public static LogoutUtil getInstance(Context ctx) {
        context = ctx;

        if (instance == null) {
            instance = new LogoutUtil();

            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction(LOGOUT_ACTION);
            logoutPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        }

        return instance;
    }

    /**
     * Called from all activities' onPause
     */
    public void startLogoutTimer() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT, logoutPendingIntent);
    }

    /**
     * Called from all activities' onResume
     */
    public void stopLogoutTimer() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(logoutPendingIntent);
    }

    public static void logout() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LOGOUT_ACTION);
        context.startActivity(intent);
    }
}
