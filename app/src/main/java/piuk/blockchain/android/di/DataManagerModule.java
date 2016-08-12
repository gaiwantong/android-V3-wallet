package piuk.blockchain.android.di;

import info.blockchain.wallet.datamanagers.AuthDataManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by adambennett on 12/08/2016.
 */

@Module
public class DataManagerModule {

    @Provides
    @Singleton
    protected AuthDataManager provideAuthDataManager() {
        return new AuthDataManager();
    }
}
