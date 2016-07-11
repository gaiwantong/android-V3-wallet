package info.blockchain.wallet.payload;

import android.os.Looper;

/**
 * PayloadBridge.java : singleton class for remote save of payload
 */
public class PayloadBridge {

    private static PayloadBridge instance;
    private static PayloadManager payloadManager;

    private PayloadBridge() {
    }

    public static PayloadBridge getInstance(){

        if(instance == null){
            instance = new PayloadBridge();
            payloadManager = PayloadManager.getInstance();
        }

        return instance;
    }

    public interface PayloadSaveListener{
        void onSaveSuccess();
        void onSaveFail();
    }

    /**
     * Thread for remote save of payload to server.
     */
    public void remoteSaveThread(PayloadSaveListener listener) {

        new Thread(() -> {
            Looper.prepare();

            if (payloadManager.savePayloadToServer()) {
                listener.onSaveSuccess();
            } else {
                listener.onSaveFail();
            }

            Looper.loop();

        }).start();
    }
}