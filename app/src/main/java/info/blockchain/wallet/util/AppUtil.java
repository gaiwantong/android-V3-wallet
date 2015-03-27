package info.blockchain.wallet.util;
 
import info.blockchain.wallet.MainActivity;
import android.content.Context;
import android.content.Intent;

public class AppUtil {
	
	private static AppUtil instance = null;
	private static Context context = null;
	
	private AppUtil() { ; }

	public static AppUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
			instance = new AppUtil();
		}
		
		return instance;
	}

	public void wipeApp() {
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

}
