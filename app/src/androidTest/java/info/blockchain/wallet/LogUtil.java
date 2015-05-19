package info.blockchain.wallet;

import android.util.Log;

public class LogUtil {

    private static LogUtil instance = null;

    private static boolean LOGGING_ON = true;

    private LogUtil() { ; }

    public static LogUtil getInstance() {

        if(instance == null) {
            instance = new LogUtil();
        }

        return instance;
    }

    public boolean isOn() {
        return LOGGING_ON;
    }

    public void turnOn(boolean logging) {
        LOGGING_ON = logging;
    }

    public void log(String tag, String msg) {
        Log.i(tag, msg);
    }

}
