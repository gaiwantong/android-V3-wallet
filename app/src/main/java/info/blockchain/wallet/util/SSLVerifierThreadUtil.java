package info.blockchain.wallet.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
//import android.util.Log;

import piuk.blockchain.android.R;

public class SSLVerifierThreadUtil {

    private static SSLVerifierThreadUtil instance = null;
    private static Context context = null;

    private SSLVerifierThreadUtil() { ; }

    public static SSLVerifierThreadUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
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

                if(ConnectivityStatus.hasConnectivity(context)) {

                    if(!SSLVerifierUtil.getInstance(context).isValidHostname()) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

                        final String message = context.getString(R.string.ssl_hostname_invalid);

                        builder.setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton(R.string.dialog_continue,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface d, int id) {
                                                d.dismiss();
                                            }
                                        });

                        builder.create().show();

                    }

                    if(!SSLVerifierUtil.getInstance(context).certificatePinned()) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

                        final String message = context.getString(R.string.ssl_pinning_invalid);

                        builder.setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton(R.string.dialog_continue,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface d, int id) {
                                                d.dismiss();
                                            }
                                        });

                        builder.create().show();
                    }

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

}
