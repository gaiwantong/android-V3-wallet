package info.blockchain.wallet.util;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.R;

public class AppUtil {
	
	private static AppUtil instance = null;
	private static Context context = null;

    private static boolean DEBUG = false;

    private static long TIMEOUT_DELAY = 1000 * 60 * 5;
    private static long lastPin = 0L;

    private static String strReceiveQRFilename = null;
    private static Thread lockThread = null;

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
                    Toast.makeText(context,context.getResources().getString(R.string.logging_out_automatically),Toast.LENGTH_SHORT).show();
                    restartApp();
                    clearPinEntryTime();

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

}
