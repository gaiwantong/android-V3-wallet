package piuk.blockchain.android.di;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.datamanagers.AuthDataManager;
import info.blockchain.wallet.datamanagers.TransactionListDataManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.view.helpers.ReceiveCurrencyHelper;
import info.blockchain.wallet.view.helpers.WalletAccountHelper;
import info.blockchain.wallet.viewModel.AccountEditViewModel;
import info.blockchain.wallet.viewModel.BalanceViewModel;
import info.blockchain.wallet.viewModel.MainViewModel;
import info.blockchain.wallet.viewModel.ManualPairingViewModel;
import info.blockchain.wallet.viewModel.PairingViewModel;
import info.blockchain.wallet.viewModel.PasswordRequiredViewModel;
import info.blockchain.wallet.viewModel.PinEntryViewModel;
import info.blockchain.wallet.viewModel.ReceiveViewModel;
import info.blockchain.wallet.viewModel.RecoverFundsViewModel;
import info.blockchain.wallet.viewModel.SendViewModel;
import info.blockchain.wallet.viewModel.TransactionDetailViewModel;

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

    void inject(SendViewModel sendViewModel);

    void inject(PinEntryViewModel pinEntryViewModel);

    void inject(MainViewModel mainViewModel);

    void inject(BalanceViewModel balanceViewModel);

    void inject(PairingViewModel pairingViewModel);

    void inject(AccountEditViewModel accountEditViewModel);

    void inject(RecoverFundsViewModel recoverFundsViewModel);

    void inject(ReceiveViewModel receiveViewModel);

    void inject(ExchangeRateFactory exchangeRateFactory);

    void inject(ReceiveCurrencyHelper receiveCurrencyHelper);

    void inject(WalletAccountHelper walletAccountHelper);

    void inject(TransactionListDataManager transactionDataManager);

    void inject(TransactionDetailViewModel transactionDetailViewModel);
}
