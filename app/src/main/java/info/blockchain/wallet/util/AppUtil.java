package info.blockchain.wallet.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import java.io.File;

import info.blockchain.wallet.MainActivity;
import piuk.blockchain.android.R;

public class AppUtil {
	
	private static AppUtil instance = null;
	private static Context context = null;

    private static boolean DEBUG = false;

    private static long TIMEOUT_DELAY = 1000L * 60L * 5L;
    private static long lastPin = 0L;

    private static String strReceiveQRFilename = null;
    private static Thread lockThread = null;

    private static long UPGRADE_REMINDER_DELAY = 1000L * 60L * 60L * 24L * 14L;

	private AppUtil() { ; }

	public static AppUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
            strReceiveQRFilename = context.getExternalCacheDir() + File.separator + "qr.png";
			instance = new AppUtil();
		}
		
		return instance;
	}

	public void clearCredentialsAndRestart() {
		PrefsUtil.getInstance(context).clear();
		restartApp();
	}

	public void restartApp() {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public void restartApp(String name, boolean value) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		if(name != null) {
    		intent.putExtra(name, value);
		}
		context.startActivity(intent);
	}

    public static boolean isDEBUG() {
        return DEBUG;
    }

    public static void setDEBUG(boolean debug) {
        DEBUG = debug;
    }

    public void updatePinEntryTime() {
        lastPin = System.currentTimeMillis();

        if(lockThread!=null)lockThread.interrupt();
        lockThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try{
                    Thread.sleep(TIMEOUT_DELAY);

                    KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                    if( myKM.inKeyguardRestrictedInputMode()) {
                        //screen is locked, time is up - lock app
                        ToastCustom.makeText(context, context.getString(R.string.logging_out_automatically), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                        restartApp();
                        clearPinEntryTime();
                    } else {
                        //screen not locked, sleep some more
                        updatePinEntryTime();
                    }
                }catch (Exception e){
                }
                Looper.loop();
            }
        });
        lockThread.start();
    }

	public void clearPinEntryTime() {
		lastPin = 0L;
        if(lockThread!=null)lockThread.interrupt();
	}

    public boolean isTimedOut() {
        return (System.currentTimeMillis() - lastPin) > TIMEOUT_DELAY;
    }

    public String getReceiveQRFilename(){
        return strReceiveQRFilename;
    }

    public void deleteQR(){
        File file = new File(strReceiveQRFilename);
        if(file.exists()) {
            file.delete();
        }
    }

    public boolean isTimeForUpgradeReminder() {
        long lastReminder = 0L;
        try {
            lastReminder = Long.parseLong(PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, "0L"));
        }
        catch(NumberFormatException nfe) {
            lastReminder = 0L;
        }
        return (System.currentTimeMillis() - lastReminder) > UPGRADE_REMINDER_DELAY;
    }

    public void setUpgradeReminder(long ts) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, Long.toString(ts));
    }

}
