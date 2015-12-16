package info.blockchain.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.Display;
//import android.util.Log;

public class DeviceUtil {

    private static DeviceUtil instance = null;
    private static Context context = null;

    private static float REG_RES = 2.0f;
    private static float scale = 0.0f;
    private static Display display = null;

    private DeviceUtil() {
        ;
    }

    public static DeviceUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Resources resources = context.getResources();
            scale = resources.getDisplayMetrics().density;
            instance = new DeviceUtil();
        }

        return instance;
    }

    public float getScale() {
        return scale;
    }

    public boolean isHiRes() {
        return (scale > REG_RES);
    }

    public int getHeight() {
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    public int getWidth() {
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public boolean isSmallScreen() {
        Point size = new Point();
        display.getSize(size);
        int height = size.y;

        if (height <= 800) {
            return true;
        } else {
            return false;
        }
    }

}
