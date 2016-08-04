package piuk.blockchain.android;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.multidex.MultiDex;

import info.blockchain.wallet.access.AccessState;

/**
 * Created by adambennett on 04/08/2016.
 */

public class BlockchainApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AccessState.getInstance().initAccessState(base);

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MultiDex.install(base);
        }
    }
}
