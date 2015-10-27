package info.blockchain.wallet.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;

import info.blockchain.wallet.payload.PayloadFactory;

//import android.util.Log;

public class WebSocketService extends android.app.Service	{

    private static final long checkIfNotConnectedDelay = 15000L;

    private WebSocketHandler webSocketHandler = null;

    Timer timer = new Timer();

    private final Handler handler = new Handler();

    private NotificationManager nm;
    private static final int NOTIFICATION_ID_CONNECTED = 0;
    public static boolean isRunning = false;

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

        isRunning = true;

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

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

        int nbLegacy = PayloadFactory.getInstance().get().getLegacyAddresses().size();
        final String[] addrs = new String[nbLegacy];
        for(int i = 0; i < nbLegacy; i++) {
            String s = PayloadFactory.getInstance().get().getLegacyAddresses().get(i).getAddress();
            if(s != null && s.length() > 0) {
                addrs[i] = PayloadFactory.getInstance().get().getLegacyAddresses().get(i).getAddress();
            }
        }

        webSocketHandler = new WebSocketHandler(getApplicationContext(), PayloadFactory.getInstance().get().getGuid(), xpubs, addrs);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectToWebsocketIfNotConnected();
                    }
                });
            }
        }, 5000, checkIfNotConnectedDelay);

    }

    public void connectToWebsocketIfNotConnected()
    {
        try {
            if(webSocketHandler != null && !webSocketHandler.isConnected()) {
                webSocketHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {

        timer.cancel();

        try {
            if(webSocketHandler != null)	{
                webSocketHandler.stop();
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy()
    {
        isRunning = false;

        stop();

        handler.removeCallbacksAndMessages(null);

        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                nm.cancel(NOTIFICATION_ID_CONNECTED);
            }
        }, 2000);

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