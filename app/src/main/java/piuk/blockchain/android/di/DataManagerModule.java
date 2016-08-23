package piuk.blockchain.android.di;

import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.datamanagers.ReceiveDataManager;

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

    @Provides
    @Singleton
    protected ReceiveDataManager provideReceiveDataManager() {
        return new ReceiveDataManager();
    }
}
