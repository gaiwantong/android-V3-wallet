package info.blockchain.wallet.view.customviews;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import piuk.blockchain.android.R;

public class MaterialProgressDialog {

    private AlertDialog mAlertDialog;

    public MaterialProgressDialog(Context context, String message) {
        this(context, message, false);
    }

    public MaterialProgressDialog(Context context, String message, boolean cancellable) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.progress_dialog_compat, null);

        TextView textView = (TextView) layout.findViewById(R.id.message);
        textView.setText(message);

        mAlertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(context.getString(R.string.app_name))
                .setCancelable(cancellable)
                .setView(layout)
                .create();
    }

    public void show() {
        if (mAlertDialog != null) {
            mAlertDialog.show();
        }
    }

    public void dismiss() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }
}
