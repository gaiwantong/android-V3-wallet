package info.blockchain.wallet.util;

import android.util.Log;
 
public class TimeOutUtil {
	
	private static long TIMEOUT_DELAY = 1000 * 60 * 5;
	
    private static long lastPin = 0L;
    private static TimeOutUtil instance = null;

	private TimeOutUtil() { ; }
	
	public static TimeOutUtil getInstance() {
		
		if(instance == null) {
			instance = new TimeOutUtil();
		}
		
		return instance;
	}

	public void updatePin() {
		lastPin = System.currentTimeMillis();
	}

	public boolean isTimedOut() {
		return (System.currentTimeMillis() - lastPin) > TIMEOUT_DELAY;
	}

}
