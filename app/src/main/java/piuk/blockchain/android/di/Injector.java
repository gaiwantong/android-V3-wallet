package piuk.blockchain.android.di;

import android.app.Application;
import android.content.Context;

/**
 * Created by adambennett on 08/08/2016.
 */

public enum Injector {

    INSTANCE;

    private ApplicationComponent mAppComponent;

    public static Injector getInstance() {
        return INSTANCE;
    }

    public void init(Context applicationContext) {

        ApplicationModule applicationModule = new ApplicationModule((Application) applicationContext);
        ApiModule apiModule = new ApiModule();

        initAppComponent(applicationModule, apiModule);
    }

    protected void initAppComponent(ApplicationModule applicationModule, ApiModule apiModule) {

        mAppComponent = DaggerApplicationComponent.builder()
                .applicationModule(applicationModule)
                .apiModule(apiModule)
                .build();
    }

    public ApplicationComponent getAppComponent() {
        return mAppComponent;
    }
}
