package piuk.blockchain.android.di;

import info.blockchain.wallet.access.AccessState;

import javax.inject.Singleton;

import dagger.Component;
import piuk.blockchain.android.LauncherViewModel;

/**
 * Created by adambennett on 08/08/2016.
 */

@Singleton
@Component(modules = {ApplicationModule.class, ApiModule.class} )
public interface ApplicationComponent {

    void inject(AccessState accessState);

    void inject(LauncherViewModel launcherViewModel);
}
