package info.blockchain.wallet.payload;

import android.os.Looper;
import android.support.annotation.Nullable;

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
    public void remoteSaveThread(@Nullable PayloadSaveListener listener) {

        new Thread(() -> {
            Looper.prepare();

            if (payloadManager.savePayloadToServer()) {
                if(listener != null)listener.onSaveSuccess();
            } else {
                if(listener != null)listener.onSaveFail();
            }

            Looper.loop();

        }).start();
    }
}