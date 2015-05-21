package info.blockchain.wallet.util;
 
import android.content.Context;
import android.content.Intent;

import java.io.File;

import info.blockchain.wallet.MainActivity;

public class AppUtil {
	
	private static AppUtil instance = null;
	private static Context context = null;

    private static boolean DEBUG = false;

    private static long TIMEOUT_DELAY = 1000 * 60 * 5;
    private static long lastPin = 0L;

    private static String strReceiveQRFilename = null;

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
    }

	public void clearPinEntryTime() {
		lastPin = 0L;
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

}
