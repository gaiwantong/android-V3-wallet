package info.blockchain.wallet.send;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import info.blockchain.api.PushTx;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.ui.helpers.ToastCustom;

import org.bitcoinj.core.Transaction;
import org.spongycastle.util.encoders.Hex;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import piuk.blockchain.android.R;

public class TxQueue {

    private static PushTx pushTxApi;
    private static Timer timer = null;
    private static Handler handler = null;
    private static Context context = null;
    private static ConcurrentLinkedQueue<Spendable> queue = null;
    private static TxQueue instance = null;
    private static PayloadManager payloadManager;

    private TxQueue() {
        ;
    }

    public static TxQueue getInstance(Context ctx) {

        context = ctx.getApplicationContext();

        if (instance == null) {

            queue = new ConcurrentLinkedQueue<Spendable>();

            instance = new TxQueue();
            pushTxApi = new PushTx();
            payloadManager = PayloadManager.getInstance();
        }

        return instance;
    }

    public void add(Spendable sp) {
        queue.add(sp);
        doTimer();
    }

    public Spendable peek() {
        return queue.peek();
    }

    public Spendable poll() {
        return queue.poll();
    }

    private boolean contains(Transaction tx) {

        boolean ret = false;

        if (queue.size() > 0) {

            ConcurrentLinkedQueue<Spendable> _queue = queue;

            Spendable spq = null;
            while (queue.peek() != null) {
                spq = queue.poll();
                if (tx.getHashAsString().equals(spq.getTx().getHashAsString())) {
                    ret = true;
                    break;
                }
            }

            queue = _queue;
        }

        return ret;

    }

    private void doTimer() {

        if (timer == null) {
            timer = new Timer();
            handler = new Handler();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            if (ConnectivityStatus.hasConnectivity(context)) {

                                Spendable sp = poll();

                                Log.i("TxQueue", sp == null ? "sp is null" : "sp is not null");

                                if (sp != null) {
                                    try {
                                        String hexString = new String(Hex.encode(sp.getTx().bitcoinSerialize()));
                                        String response = pushTxApi.submitTransaction(hexString);
//					Log.i("Send response", response);
                                        if (response.contains("Transaction Submitted")) {

                                            sp.getOpCallback().onSuccess(sp.getTx().getHashAsString());

                                            if (sp.getNote() != null && sp.getNote().length() > 0) {
                                                Map<String, String> notes = payloadManager.getPayload().getNotes();
                                                notes.put(sp.getTx().getHashAsString(), sp.getNote());
                                                payloadManager.getPayload().setNotes(notes);
                                            }

                                            if (sp.isHD() && sp.sentChange()) {
                                                // increment change address counter
                                                payloadManager.getPayload().getHdWallet().getAccounts().get(sp.getAccountIdx()).incChange();
                                            }

                                            if (queue.size() == 0) {
                                                if (timer != null) {
                                                    timer.cancel();
                                                    timer = null;
                                                }
                                            }

                                        } else {
                                            add(sp);

                                            ToastCustom.makeText(context, response, ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
//                                            sp.getOpCallback().onFail();
                                        }

                                    } catch (Exception e) {
                                        add(sp);

                                        e.printStackTrace();
//                                        sp.getOpCallback().onFail();
                                    }
                                }

                            } else {
                                ToastCustom.makeText(context, context.getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            }

                        }
                    });

                }
            }, 1000, 10000);

        }

    }

}
