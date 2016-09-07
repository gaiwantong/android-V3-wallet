package info.blockchain.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import info.blockchain.api.BaseApi;
import info.blockchain.wallet.connectivity.ConnectivityStatus;

import org.thoughtcrime.ssl.pinning.util.PinningHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import piuk.blockchain.android.R;

// openssl s_client -showcerts -connect blockchain.info:443

public class SSLVerifyUtil {

    private Context context;
    private AlertDialog alertDialog;

    public SSLVerifyUtil(Context context) {
        this.context = context;
    }

    public void validateSSL() {
        if (ConnectivityStatus.hasConnectivity(context)) {
            new PinSSLTask(context, new CertificateCheckInterface() {
                @Override
                public void onSuccess() {
                    Log.i(SSLVerifyUtil.class.getSimpleName(), "SSL pinning completed successfully");
                }

                @Override
                public void onServerDown() {
                    showAlertDialog(context.getString(R.string.ssl_no_connection), false);
                }

                @Override
                public void onPinningFail() {
                    showAlertDialog(context.getString(R.string.ssl_pinning_invalid), true);
                }
            })
            .execute();
        } else {
            //On connection issue: 2 choices - retry or exit
            showAlertDialog(context.getString(R.string.ssl_no_connection), false);
        }
    }

    private void showAlertDialog(final String message, final boolean forceExit) {
        if (!((Activity) context).isFinishing()) {
            if (alertDialog != null) alertDialog.dismiss();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            builder.setMessage(message);
            builder.setCancelable(false);

            if (!forceExit) {
                builder.setPositiveButton(R.string.retry,
                        (d, id) -> {
                            //Retry
                            validateSSL();
                        });
            }

            builder.setNegativeButton(R.string.exit,
                    (d, id) -> ((Activity) context).finish());

            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    interface CertificateCheckInterface {

        void onSuccess();

        void onServerDown();

        void onPinningFail();
    }

    private static class PinSSLTask extends AsyncTask<Void, Void, Void> {

        private final Context context;
        private final CertificateCheckInterface listener;

        PinSSLTask(Context context, CertificateCheckInterface listener) {
            super();
            this.context = context;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // DER encoded public key:
            // 30820122300d06092a864886f70d01010105000382010f003082010a0282010100bff56f562096307165320b0f04ff30e3f7d7e7a2813a35c16bfbe549c23f2a5d0388818fc0f9326a9679322fd7a6d4a1f2c4d45129c8641f6a3e7d9175938f050352a1cf09440399a36a358a846e4b5ef43baafbcb6af9f3615a7a49aae497cfeaaeb943e0175bab546abacc60b29c9bb7f588c62ac81e21038e760f044c07fe6d8a1cba4f8b5e9835bb8eddec79d506dc47fd73030630bf1af7bd70352ced281efae1675e70a6918d98645ebc389d2169ff72a82c7ff7a6328f0cd337197d87e208d2bc8cdd21182157fcb12a6db697dbd62b76800debef8feea2da2a5e074feea56af52f4300c17892018f7584eb5d4946c10156a85746ae8eacc5ebe112df0203010001
            String[] pins = new String[]{"10902ad9c6fb7d84c133b8682a7e7e30a5b6fb90"};    // SHA-1 hash of DER encoded public key byte array

            URL url = null;
            try {
                url = new URL(BaseApi.PROTOCOL + BaseApi.SERVER_ADDRESS);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpsURLConnection connection = null;
            try {
                connection = PinningHelper.getPinnedHttpsURLConnection(context, pins, url);
            } catch (IOException e) {
                e.printStackTrace();
                listener.onServerDown();
            }

            try {
                byte[] data = new byte[4096];
                connection.getInputStream().read(data);
                listener.onSuccess();

            } catch (IOException e) {
                e.printStackTrace();

                if (e instanceof SSLHandshakeException) {
                    listener.onPinningFail();
                } else {
                    listener.onServerDown();
                }
            }
            return null;
        }
    }
}