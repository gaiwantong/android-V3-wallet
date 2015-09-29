package info.blockchain.wallet.service;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
//import android.util.Log;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import info.blockchain.wallet.EventListeners;
import info.blockchain.wallet.HDPayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.NotificationsFactory;

import com.neovisionaries.ws.client.*;

import info.blockchain.wallet.util.ToastCustom;

public class WebSocketHandler {

    private boolean isRunning = true;
    private WebSocket mConnection = null;

    private static String guid = null;
    private static String[] xpubs = null;
    private static String[] addrs = null;

    private static Context context = null;

    final private EventListeners.EventListener walletEventListener = new EventListeners.EventListener() {
        @Override
        public String getDescription() {
            return "Websocket Listener";
        }

        @Override
        public void onWalletDidChange() {
            try {
                if(isRunning) {
                    start();
                } else if(isConnected()) {
                    // Disconnect and reconnect
                    // To resubscribe
                    subscribe();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public WebSocketHandler(Context ctx, String guid, String[] xpubs, String[] addrs) {
        this.context = ctx;
        this.guid = guid;
        this.xpubs = xpubs;
        this.addrs = addrs;
    }

    public void send(String message) {
        try {
            if(mConnection != null && mConnection.isOpen()) {
                mConnection.sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe() {

        if(guid == null) {
            return;
        }

        send("{\"op\":\"blocks_sub\"}");
        send("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");
//		Log.i("WebSocketHandler", "Websocket subscribe:" + "{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");

        for(int i = 0; i < xpubs.length; i++) {
            if(xpubs[i] != null && xpubs[i].length() > 0) {
                send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpubs[i] + "\"}");
//				Log.i("WebSocketHandler", "Websocket subscribe:" + "{\"op\":\"xpub_sub\", \"xpub\":\"" + xpubs[i] + "\"}");
            }
        }

        for(int i = 0; i < addrs.length; i++) {
            if(addrs[i] != null && addrs[i].length() > 0) {
                send("{\"op\":\"addr_sub\", \"addr\":\"" + addrs[i] + "\"}");
//				Log.i("WebSocketHandler", "Websocket subscribe:" + "{\"op\":\"addr_sub\", \"addr\":\"" + addrs[i] + "\"}");
            }
        }

    }

    public boolean isConnected() {
        return  mConnection != null && mConnection.isOpen();
    }

    public void stop() {
        if(mConnection != null && mConnection.isOpen()) {
            mConnection.disconnect();
        }

        EventListeners.removeEventListener(walletEventListener);

        this.isRunning = false;
    }

    public void start() {

        this.isRunning = true;

        try {
            stop();
            connect();
        }
        catch (IOException | com.neovisionaries.ws.client.WebSocketException e) {
            e.printStackTrace();
        }

    }

    /**
     * Connect to the server.
     */
    private void connect() throws IOException, WebSocketException
    {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    mConnection = new WebSocketFactory()
                            .createSocket("wss://ws.blockchain.info/inv")
                            .addHeader("Origin", "https://blockchain.info").recreate()
                            .addListener(new WebSocketAdapter() {

                                public void onTextMessage(WebSocket websocket, String message) {

                                    if (guid == null) {
                                        return;
                                    }

                                    try {
//						Map<String, Object> top = (Map<String, Object>) JSONValue.parse(message);
                                        JSONObject jsonObject = null;
                                        try {
                                            jsonObject = new JSONObject(message);
                                        } catch (JSONException je) {
//											Log.i("WebSocketHandler", "JSONException:" + je.getMessage());
                                            jsonObject = null;
                                        }

                                        if (jsonObject == null) {
//											Log.i("WebSocketHandler", "jsonObject is null");
                                            return;
                                        }

//										Log.i("WebSocketHandler", jsonObject.toString());

                                        String op = (String) jsonObject.get("op");
                                        if (op.equals("utx") && jsonObject.has("x")) {

                                            JSONObject objX = (JSONObject) jsonObject.get("x");

                                            long value = 0L;
                                            long total_value = 0L;
                                            long ts = 0L;
                                            String in_addr = null;

                                            if (objX.has("time")) {
                                                ts = objX.getLong("time");
                                            }

                                            if (objX.has("inputs")) {
                                                JSONArray inputArray = (JSONArray) objX.get("inputs");
                                                JSONObject inputObj = null;
                                                for (int j = 0; j < inputArray.length(); j++) {
                                                    inputObj = (JSONObject) inputArray.get(j);
                                                    if (inputObj.has("prev_out")) {
                                                        JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                                                        if (prevOutObj.has("value")) {
                                                            value = prevOutObj.getLong("value");
                                                        }
                                                        if (prevOutObj.has("xpub")) {
                                                            total_value -= value;
                                                        } else if (prevOutObj.has("addr")) {
                                                            if (PayloadFactory.getInstance().get().containsLegacyAddress((String) prevOutObj.get("addr"))) {
                                                                total_value -= value;
                                                            } else if (in_addr == null) {
                                                                in_addr = (String) prevOutObj.get("addr");
                                                            } else {
                                                                ;
                                                            }
                                                        } else {
                                                            ;
                                                        }
                                                    }
                                                }
                                            }

                                            if (objX.has("out")) {
                                                JSONArray outArray = (JSONArray) objX.get("out");
                                                JSONObject outObj = null;
                                                for (int j = 0; j < outArray.length(); j++) {
                                                    outObj = (JSONObject) outArray.get(j);
                                                    if (outObj.has("value")) {
                                                        value = outObj.getLong("value");
                                                    }
                                                    if (outObj.has("xpub")) {
                                                        total_value += value;
                                                    } else if (outObj.has("addr")) {
                                                        if (PayloadFactory.getInstance().get().containsLegacyAddress((String) outObj.get("addr"))) {
                                                            total_value += value;
                                                        }
                                                    } else {
                                                        ;
                                                    }
                                                }
                                            }

                                            String title = context.getString(R.string.app_name);
                                            if (total_value > 0L) {
                                                String marquee = context.getString(R.string.received_bitcoin) + " " + MonetaryUtil.getInstance().getBTCFormat().format((double) total_value / 1e8) + " BTC";
                                                String text = marquee;
                                                if (total_value > 0) {
                                                    text += " from " + in_addr;
                                                }

                                                NotificationsFactory.getInstance(context).setNotification(title, marquee, text, R.drawable.ic_notification_transparent, R.drawable.ic_launcher, info.blockchain.wallet.MainActivity.class, 1000);
                                            }

                                            new Thread() {
                                                public void run() {

                                                    Looper.prepare();

                                                    try {
                                                        HDPayloadBridge.getInstance().getBalances();
                                                    } catch (JSONException | IOException | DecoderException | AddressFormatException
                                                            | MnemonicException.MnemonicChecksumException
                                                            | MnemonicException.MnemonicLengthException
                                                            | MnemonicException.MnemonicWordException e) {
                                                        e.printStackTrace();
                                                    }

                                                    Intent intent = new Intent("info.blockchain.wallet.BalanceFragment.REFRESH");
                                                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                                                    Looper.loop();
                                                }
                                            }.start();

                                        } else if (op.equals("on_change")) {

//											Log.i("WalletSocketHandler", "on_change:" + message);

                                            if(PayloadFactory.getInstance().getTempPassword() != null)	{
                                                HDPayloadBridge.getInstance(context).init(PayloadFactory.getInstance().getTempPassword());
                                                ToastCustom.makeText(context, context.getString(R.string.wallet_updated), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                            }

                                        } else if (op.equals("block")) {
                                            ;
                                        } else {
                                            ;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                    mConnection.connect();

                    subscribe();

                }
                catch(Exception e)	{
                    e.printStackTrace();
                }

                Looper.loop();

            }
        }).start();

    }

}
