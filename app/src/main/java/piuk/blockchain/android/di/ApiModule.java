package piuk.blockchain.android.di;

import info.blockchain.api.PinStore;
import info.blockchain.api.WalletPayload;
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
    protected WalletPayload provideAccess() {
        return new WalletPayload();
    }

    @Provides
    @Singleton
    protected PinStore providePinStore() {
        return new PinStore();
    }

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }
}
