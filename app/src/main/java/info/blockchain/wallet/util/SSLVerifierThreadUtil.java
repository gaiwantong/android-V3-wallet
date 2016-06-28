package info.blockchain.wallet.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import info.blockchain.wallet.connectivity.ConnectivityStatus;

import org.thoughtcrime.ssl.pinning.util.PinningHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import piuk.blockchain.android.R;

// openssl s_client -showcerts -connect blockchain.info:443

public class SSLVerifierThreadUtil {

    private static SSLVerifierThreadUtil instance = null;
    private static Context context = null;

    public static final int STATUS_POTENTIAL_SERVER_DOWN = 0;
    public static final int STATUS_PINNING_FAIL = 1;
    public static final int STATUS_PINNING_SUCCESS = 2;

    private AlertDialog alertDialog = null;

    private SSLVerifierThreadUtil() {
        ;
    }

    public static SSLVerifierThreadUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new SSLVerifierThreadUtil();
        }

        return instance;
    }

    public void validateSSLThread() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if (ConnectivityStatus.hasConnectivity(context)) {

                    //Pin SSL certificate
                    switch (certificatePinned()) {
                        case STATUS_POTENTIAL_SERVER_DOWN:
                            //On connection issue: 2 choices - retry or exit
                            showAlertDialog(context.getString(R.string.ssl_no_connection), false);
                            break;
                        case STATUS_PINNING_FAIL:
                            //On fail: only choice is to exit app
                            showAlertDialog(context.getString(R.string.ssl_pinning_invalid), true);
                            break;
                        case STATUS_PINNING_SUCCESS:
                            //Certificate pinning successful: safe to continue
                            break;
                    }
                } else {
                    //On connection issue: 2 choices - retry or exit
                    showAlertDialog(context.getString(R.string.ssl_no_connection), false);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ;
                    }
                });

                Looper.loop();

            }
        }).start();
    }

    private void showAlertDialog(final String message, final boolean forceExit){

        if (!((Activity) context).isFinishing()) {

            if(alertDialog != null)alertDialog.dismiss();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message);
            builder.setCancelable(false);

            if(!forceExit) {
                builder.setPositiveButton(R.string.retry,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.dismiss();
                                //Retry
                                validateSSLThread();
                            }
                        });
            }

            builder.setNegativeButton(R.string.exit,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int id) {
                            d.dismiss();
                            ((Activity) context).finish();
                        }
                    });

            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public int certificatePinned() {

        // DER encoded public key:
        // 30820122300d06092a864886f70d01010105000382010f003082010a0282010100bff56f562096307165320b0f04ff30e3f7d7e7a2813a35c16bfbe549c23f2a5d0388818fc0f9326a9679322fd7a6d4a1f2c4d45129c8641f6a3e7d9175938f050352a1cf09440399a36a358a846e4b5ef43baafbcb6af9f3615a7a49aae497cfeaaeb943e0175bab546abacc60b29c9bb7f588c62ac81e21038e760f044c07fe6d8a1cba4f8b5e9835bb8eddec79d506dc47fd73030630bf1af7bd70352ced281efae1675e70a6918d98645ebc389d2169ff72a82c7ff7a6328f0cd337197d87e208d2bc8cdd21182157fcb12a6db697dbd62b76800debef8feea2da2a5e074feea56af52f4300c17892018f7584eb5d4946c10156a85746ae8eacc5ebe112df0203010001
        String[] pins = new String[]{"10902ad9c6fb7d84c133b8682a7e7e30a5b6fb90"};    // SHA-1 hash of DER encoded public key byte array

        URL url = null;
        try {
            url = new URL(WebUtil.VALIDATE_SSL_URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpsURLConnection connection = null;
        try {
            connection = PinningHelper.getPinnedHttpsURLConnection(context, pins, url);
        } catch (IOException e) {
            e.printStackTrace();
            return STATUS_POTENTIAL_SERVER_DOWN;
        }

        try {
            byte[] data = new byte[4096];
            connection.getInputStream().read(data);
            return STATUS_PINNING_SUCCESS;

        } catch (IOException e) {
            e.printStackTrace();

            if(e instanceof SSLHandshakeException){
                return STATUS_PINNING_FAIL;
            }else{
                return STATUS_POTENTIAL_SERVER_DOWN;
            }
        }
    }
}