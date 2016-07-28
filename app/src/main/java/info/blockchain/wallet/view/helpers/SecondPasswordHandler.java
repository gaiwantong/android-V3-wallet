package info.blockchain.wallet.view.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;

import org.apache.commons.lang3.StringUtils;

import piuk.blockchain.android.R;

public class SecondPasswordHandler {

    private Context context;
    private PayloadManager payloadManager;

    public SecondPasswordHandler(Context context) {
        this.context = context;
        this.payloadManager = PayloadManager.getInstance();
    }

    public interface ResultListener{
        void onNoSecondPassword();
        void onSecondPasswordValidated(String validateSecondPassword);
    }

    public void validate(final ResultListener listener){

        if(!payloadManager.getPayload().isDoubleEncrypted()){
            listener.onNoSecondPassword();
        }else {

            final EditText double_encrypt_password = new EditText(context);
            double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            new AlertDialog.Builder(context)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.enter_double_encryption_pw)
                    .setView(double_encrypt_password)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> {

                        String secondPassword = double_encrypt_password.getText().toString();

                        if (secondPassword != null &&
                                secondPassword.length() > 0 &&
                                DoubleEncryptionFactory.getInstance().validateSecondPassword(
                                        payloadManager.getPayload().getDoublePasswordHash(),
                                        payloadManager.getPayload().getSharedKey(),
                                        new CharSequenceX(secondPassword),
                                        payloadManager.getPayload().getOptions().getIterations()) &&
                                !StringUtils.isEmpty(secondPassword)) {

                            listener.onSecondPasswordValidated(secondPassword);

                        } else {
                            ToastCustom.makeText(context, context.getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }
                    }).setNegativeButton(R.string.cancel, null).show();
        }
    }
}
