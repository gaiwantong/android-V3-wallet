package piuk.blockchain.android.di;

import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.datamanagers.ReceiveDataManager;
import info.blockchain.wallet.view.helpers.WalletAccountHelper;

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

    // TODO: 01/09/2016 This needs to move to a more appropriate place once we've restructured the app
    @Provides
    protected WalletAccountHelper provideWalletAccountHelper() {
        return new WalletAccountHelper();
    }
}
