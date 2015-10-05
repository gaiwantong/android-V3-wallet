package info.blockchain.wallet.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import piuk.blockchain.android.R;

public class SSLVerifierThreadUtil {

    private static SSLVerifierThreadUtil instance = null;
    private static Context context = null;
    private int strike = 0;

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

                        strike++;
                        if(strike < 3){

                            //Possible connection issue, retry  ssl verify
                            try{Thread.sleep(500);}catch (Exception e){}
                            validateSSLThread();
                            return;

                        }else{

                            //ssl verify failed 3 time - something is wrong, warn user
                            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

                            final String message = context.getString(R.string.ssl_hostname_invalid);

                            if(!((Activity) context).isFinishing()) {
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
                    }

                    if(!SSLVerifierUtil.getInstance(context).certificatePinned()) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

                        final String message = context.getString(R.string.ssl_pinning_invalid);

                        if(!((Activity) context).isFinishing()) {
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

                    strike = 0;
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
