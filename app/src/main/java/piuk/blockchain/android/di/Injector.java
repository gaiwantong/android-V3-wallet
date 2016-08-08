package piuk.blockchain.android.di;

import android.app.Application;
import android.content.Context;

/**
 * Created by adambennett on 08/08/2016.
 */

public enum Injector {

    INSTANCE;

    private ApplicationComponent mAppComponent;
    private ApiComponent mApiComponent;

    public static Injector getInstance() {
        return INSTANCE;
    }

    public void init(Context applicationContext) {

        ApplicationModule applicationModule = new ApplicationModule((Application) applicationContext);
        ApiModule apiModule = new ApiModule();

        initAppComponent(applicationModule);
        initApiComponent(apiModule);
    }

    protected void initAppComponent(ApplicationModule applicationModule) {

        mAppComponent = DaggerApplicationComponent.builder()
                .applicationModule(applicationModule)
                .build();
    }

    protected void initApiComponent(ApiModule apiModule) {

        mApiComponent = DaggerApiComponent.builder()
                .apiModule(apiModule)
                .build();
    }


    public ApplicationComponent getAppComponent() {
        return mAppComponent;
    }

    public ApiComponent getApiComponent() {
        return mApiComponent;
    }
}
