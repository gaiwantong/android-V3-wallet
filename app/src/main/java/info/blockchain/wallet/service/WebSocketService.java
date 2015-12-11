package info.blockchain.wallet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import info.blockchain.wallet.payload.PayloadFactory;

//import android.util.Log;

public class WebSocketService extends android.app.Service	{

    private WebSocketHandler webSocketHandler = null;

    public static final String ACTION_INTENT = "info.blockchain.wallet.WebSocketService.SUBSCRIBE_TO_ADDRESS";

    public class LocalBinder extends Binder
    {
        public WebSocketService getService()
        {
            return WebSocketService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(final Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        String[] addrs = getAddresses();
        String[] xpubs = getXpubs();

        webSocketHandler = new WebSocketHandler(getApplicationContext(), PayloadFactory.getInstance().get().getGuid(), xpubs, addrs);
        webSocketHandler.start();

    }

    private String[] getXpubs(){

        int nbAccounts = 0;
        if(PayloadFactory.getInstance().get().isUpgraded())    {
            try {
                nbAccounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts().size();
            }
            catch(java.lang.IndexOutOfBoundsException e) {
                nbAccounts = 0;
            }
        }

        final String[] xpubs = new String[nbAccounts];
        for(int i = 0; i < nbAccounts; i++) {
            String s = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(i).getXpub();
            if(s != null && s.length() > 0) {
                xpubs[i] = s;
            }
        }

        return xpubs;
    }

    private String[] getAddresses(){

        int nbLegacy = PayloadFactory.getInstance().get().getLegacyAddresses().size();
        final String[] addrs = new String[nbLegacy];
        for(int i = 0; i < nbLegacy; i++) {
            String s = PayloadFactory.getInstance().get().getLegacyAddresses().get(i).getAddress();
            if(s != null && s.length() > 0) {
                addrs[i] = PayloadFactory.getInstance().get().getLegacyAddresses().get(i).getAddress();
            }
        }

        return addrs;
    }

    @Override
    public void onDestroy()
    {
        webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

        if (ACTION_INTENT.equals(intent.getAction())) {
            webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
        }
        }
    };
}