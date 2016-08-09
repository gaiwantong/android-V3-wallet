package piuk.blockchain.android.di;

import info.blockchain.api.Access;

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
    Access provideAccess() {
        return new Access();
    }
}
