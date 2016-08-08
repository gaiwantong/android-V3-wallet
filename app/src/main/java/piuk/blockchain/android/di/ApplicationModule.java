package piuk.blockchain.android.di;

import android.app.Application;
import android.content.Context;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApplicationModule {

    private final Application mApplication;

    ApplicationModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return mApplication;
    }

    @Provides
    @Singleton
    PrefsUtil providePrefsUtil() {
        return new PrefsUtil(mApplication);
    }

    @Provides
    @Singleton
    AppUtil provideAppUtil() {
        return new AppUtil(mApplication);
    }
}
