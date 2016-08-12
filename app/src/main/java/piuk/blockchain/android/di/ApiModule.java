package piuk.blockchain.android.di;

import info.blockchain.api.Access;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApiModule {

    @Provides
    @Singleton
    protected Access provideAccess() {
        return new Access();
    }

    @Provides
    @Singleton
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }
}
