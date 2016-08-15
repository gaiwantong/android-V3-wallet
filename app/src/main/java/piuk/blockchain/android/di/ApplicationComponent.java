package piuk.blockchain.android.di;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.viewModel.ManualPairingViewModel;
import info.blockchain.wallet.viewModel.PasswordRequiredViewModel;

import javax.inject.Singleton;

import dagger.Component;
import piuk.blockchain.android.LauncherViewModel;
import piuk.blockchain.android.exceptions.LoggingExceptionHandler;

/**
 * Created by adambennett on 08/08/2016.
 */

@Singleton
@Component(modules = {ApplicationModule.class, ApiModule.class, DataManagerModule.class} )
public interface ApplicationComponent {

    void inject(AccessState accessState);

    void inject(LauncherViewModel launcherViewModel);

    void inject(PasswordRequiredViewModel passwordRequiredViewModel);

    void inject(AppUtil appUtil);

    void inject(LoggingExceptionHandler loggingExceptionHandler);

    void inject(ManualPairingViewModel manualPairingViewModel);

    void inject(AuthDataManager authDataManager);
}
