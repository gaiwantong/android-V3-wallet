package info.blockchain.wallet.viewModel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.address.AddressFactory;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.HDPayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.RootUtil;
import info.blockchain.wallet.util.SSLVerifyUtil;
import info.blockchain.wallet.util.WebUtil;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.WalletFactory;

import java.util.Arrays;
import java.util.List;

public class MainViewModel implements ViewModel{

    private Context context;
    private DataListener dataListener;
    private PrefsUtil prefs;
    private OSUtil osUtil;

    private int exitClickCount = 0;
    private int exitClickCooldown = 2;//seconds

    public interface DataListener {
        void onRooted();
        void onConnectivityFail();
        void onNoGUID();
        void onRequestPin();
        void onCorruptPayload();
        void onRequestUpgrade();
        void onFetchTransactionsStart();
        void onFetchTransactionCompleted();
        void onScanInput(String strUri);
        void onStartBalanceFragment();
        void onExitConfirmToast();
        void onRequestBackup();
    }

    public MainViewModel(Context context, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;
        this.prefs = new PrefsUtil(context);
        this.osUtil = new OSUtil(context);

        AppUtil.getInstance(context).applyPRNGFixes();

        checkIntent();
        checkRooted();
        checkConnectivity();
    }

    private void checkIntent(){
        // Log out if started with the logout intent
        if (((Activity)context).getIntent().getAction() != null && AppUtil.LOGOUT_ACTION.equals(((Activity)context).getIntent().getAction())) {
            ((Activity)context).finish();
            System.exit(0);
            return;
        }
    }

    private void checkRooted(){
        if (new RootUtil().isDeviceRooted() &&
                !prefs.getValue("disable_root_warning", false)) {
            this.dataListener.onRooted();
        }
    }

    private void checkConnectivity(){

        if(ConnectivityStatus.hasConnectivity(context)) {
            preLaunchChecks();
        }else{
            this.dataListener.onConnectivityFail();
        }
    }

    private void preLaunchChecks(){
        exchangeRateThread();
        new SSLVerifyUtil(context).validateSSLThread();

        boolean isPinValidated = false;
        Bundle extras = ((Activity)context).getIntent().getExtras();
        if (extras != null && extras.containsKey("verified")) {
            isPinValidated = extras.getBoolean("verified");
        }

        String action = ((Activity)context).getIntent().getAction();
        String scheme = ((Activity)context).getIntent().getScheme();
        if (action != null && Intent.ACTION_VIEW.equals(action) && scheme.equals("bitcoin")) {
            prefs.setValue(PrefsUtil.KEY_SCHEME_URL, ((Activity)context).getIntent().getData().toString());
        }

        // No GUID? Treat as new installation
        if (prefs.getValue(PrefsUtil.KEY_GUID, "").length() < 1) {
            PayloadFactory.getInstance().setTempPassword(new CharSequenceX(""));
            this.dataListener.onNoGUID();
        }
        // No PIN ID? Treat as installed app without confirmed PIN
        else if (prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").length() < 1) {
            this.dataListener.onRequestPin();
        }
        // Installed app, check sanity
        else if (!AppUtil.getInstance(context).isSane()) {
            this.dataListener.onCorruptPayload();
        }
        // Legacy app has not been prompted for upgrade
        else if (isPinValidated && !PayloadFactory.getInstance().get().isUpgraded() && prefs.getValue(PrefsUtil.KEY_HD_UPGRADED_LAST_REMINDER, 0L) == 0L) {
            AccessFactory.getInstance(context).setIsLoggedIn(true);
            this.dataListener.onRequestUpgrade();
        }
        // App has been PIN validated
        else if (isPinValidated || (AccessFactory.getInstance(context).isLoggedIn())) {
            AccessFactory.getInstance(context).setIsLoggedIn(true);

            this.dataListener.onFetchTransactionsStart();

            new Thread(() -> {

                Looper.prepare();

                try {
                    HDPayloadBridge.getInstance(context).updateBalancesAndTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                dataListener.onFetchTransactionCompleted();

                if (prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").length() > 0) {
                    String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                    prefs.setValue(PrefsUtil.KEY_SCHEME_URL, "");
                    dataListener.onScanInput(strUri);
                } else {
                    dataListener.onStartBalanceFragment();
                }

                Looper.loop();
            }).start();
        } else {
            this.dataListener.onRequestPin();
        }
    }

    @Override
    public void destroy() {
        AppUtil.getInstance(context).deleteQR();
        context = null;
        dataListener = null;
    }

    private void exchangeRateThread() {

        List<String> currencies = Arrays.asList(ExchangeRateFactory.getInstance(context).getCurrencies());
        String strCurrentSelectedFiat = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, "");
        if (!currencies.contains(strCurrentSelectedFiat)) {
            prefs.setValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        }

        new Thread(() -> {
            Looper.prepare();

            String response = null;
            try {
                response = WebUtil.getInstance().getURL(WebUtil.EXCHANGE_URL);

                ExchangeRateFactory.getInstance(context).setData(response);
                ExchangeRateFactory.getInstance(context).updateFxPricesForEnabledCurrencies();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Looper.loop();

        }).start();
    }

    public void unpair(){
        WalletFactory.getInstance().set(null);
        WalletFactory.getInstance().setWatchOnlyWallet(null);
        PayloadFactory.getInstance().wipe();
        MultiAddrFactory.getInstance().wipe();
        prefs.clear();

        try {
            AddressFactory.getInstance(context, null).wipe();
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        AppUtil.getInstance(context).restartApp();
    }

    public void onBackPressed(){

        exitClickCount++;
        if (exitClickCount == 2) {
            AppUtil.getInstance(context).logout();
        } else {
            dataListener.onExitConfirmToast();
        }

        new Thread(() -> {
            for (int j = 0; j <= exitClickCooldown; j++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (j >= exitClickCooldown) exitClickCount = 0;
            }
        }).start();

    }

    public void startWebSocketService(){
        if (!osUtil.isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            context.startService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }

    public void stopWebSocketService(){
        if (!osUtil.isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            context.stopService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }
}
