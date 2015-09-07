package info.blockchain.wallet.util;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Looper;

import java.io.File;

import org.bitcoinj.core.bip44.WalletFactory;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadFactory;
import piuk.blockchain.android.R;

public class AppUtil {

    private static String REGEX_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static AppUtil instance = null;
	private static Context context = null;

    private static boolean DEBUG = false;

    private static long TIMEOUT_DELAY = 1000L * 60L * 5L;
    private static long lastPin = 0L;

    private static String strReceiveQRFilename = null;
    private static Thread lockThread = null;

    private static long UPGRADE_REMINDER_DELAY = 1000L * 60L * 60L * 24L * 14L;
    private static boolean newlyCreated = false;
    private static boolean isBackgrounded = false;
    private static boolean isLocked = false;

    private static boolean isClosed = false;

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
        WalletFactory.getInstance().set(null);
        PayloadFactory.getInstance().wipe();
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

    public void closeApp() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("EXIT_APP", true);
        context.startActivity(intent);
    }

    public boolean isDEBUG() {
        return DEBUG;
    }

    public void setDEBUG(boolean debug) {
        DEBUG = debug;
    }

    public void updatePinEntryTime() {

        if(isClosed){
            if(lockThread!=null)lockThread.interrupt();
            return;
        }

        lastPin = System.currentTimeMillis();
        if(lockThread!=null){
            lockThread.interrupt();
        }
        lockThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try{
                    Thread.sleep(TIMEOUT_DELAY);

                    if(isLocked){
                        if(lockThread!=null)lockThread.interrupt();
                        return;
                    }

                    KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                    if( myKM.inKeyguardRestrictedInputMode()) {
                        //screen is locked, time is up - lock app
                        ToastCustom.makeText(context, context.getString(R.string.logging_out_automatically), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                        restartApp();
                        clearPinEntryTime();
                        if(lockThread!=null)lockThread.interrupt();
                    }else if(isBackgrounded) {
                        //app is in background, time is up - lock app
                        ((Activity) context).finish();
                        clearPinEntryTime();
                    }else{
                        //screen not locked and app is not in background, sleep some more
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
            lastReminder = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L);
        }
        catch(NumberFormatException nfe) {
            lastReminder = 0L;
        }
        return (System.currentTimeMillis() - lastReminder) > UPGRADE_REMINDER_DELAY;
    }

    public void setUpgradeReminder(long ts) {
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, ts);
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }

    public boolean isSane() {

        String guid = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_GUID, "");

        if(!guid.matches(REGEX_UUID))  {
            return false;
        }

        String encryptedPassword = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        String pinID = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");

        if(encryptedPassword.length() == 0 || pinID.length() == 0)  {
            return false;
        }

        return true;
    }

    public boolean isBackgrounded() {
        return isBackgrounded;
    }

    public void setIsBackgrounded(boolean isBackgrounded) {
        AppUtil.isBackgrounded = isBackgrounded;
        if(!isLocked)updatePinEntryTime();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        AppUtil.isLocked = isLocked;
        updatePinEntryTime();
    }

    public void setIsClosed(boolean isClosed) {
        AppUtil.isClosed = isClosed;
    }

    public boolean isCameraOpen() {

        Camera camera = null;

        try {
            camera = Camera.open();
        }
        catch (RuntimeException e) {
            return true;
        }
        finally {
            if (camera != null) {
                camera.release();
            }
        }

        return false;
    }

    public String getSharedKey()   {
        if(PrefsUtil.getInstance(context).has(PrefsUtil.KEY_SHARED_KEY))    {
            String sharedKey = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY, "");
            setSharedKey(sharedKey);
            PrefsUtil.getInstance(context).removeValue(PrefsUtil.KEY_SHARED_KEY);
            return sharedKey;
        }
        else    {
            return AESUtil.decrypt(PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY_X, ""), PayloadFactory.getInstance().getTempPassword(), AESUtil.PasswordPBKDF2Iterations);
        }

    }

    public String getSharedKey(CharSequenceX password)   {
        if(PrefsUtil.getInstance(context).has(PrefsUtil.KEY_SHARED_KEY))    {
            String sharedKey = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY, "");
            setSharedKey(sharedKey);
            PrefsUtil.getInstance(context).removeValue(PrefsUtil.KEY_SHARED_KEY);
            return sharedKey;
        }
        else    {
            return AESUtil.decrypt(PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_SHARED_KEY_X, ""), password, AESUtil.PasswordPBKDF2Iterations);
        }

    }

    public void setSharedKey(String sharedKey)   {
        String sharedKeyEncrypted = AESUtil.encrypt(sharedKey, PayloadFactory.getInstance().getTempPassword(), AESUtil.PasswordPBKDF2Iterations);
        PrefsUtil.getInstance(context).setValue(PrefsUtil.KEY_SHARED_KEY_X, sharedKeyEncrypted);
    }

}
