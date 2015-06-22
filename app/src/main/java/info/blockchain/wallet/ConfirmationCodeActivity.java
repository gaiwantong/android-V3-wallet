package info.blockchain.wallet;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import info.blockchain.wallet.access.AccessFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;

import piuk.blockchain.android.R;

public class ConfirmationCodeActivity extends ActionBarActivity implements TextWatcher{

    private TextView tvInstructions = null;

    private EditText etConfirmBox0 = null;
    private EditText etConfirmBox1 = null;
    private EditText etConfirmBox2 = null;
    private EditText etConfirmBox3 = null;
    private EditText etConfirmBox4 = null;
    private ProgressDialog progress = null;

    private String code = null;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_code);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.confirmation_code));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        tvInstructions = (TextView)findViewById(R.id.tvInstructions);
        tvInstructions.setText(tvInstructions.getText().toString().replace("[--email_address--]",PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_EMAIL,"'unknown'")));

        etConfirmBox0 = (EditText)findViewById(R.id.confirmBox0);
        etConfirmBox1 = (EditText)findViewById(R.id.confirmBox1);
        etConfirmBox2 = (EditText)findViewById(R.id.confirmBox2);
        etConfirmBox3 = (EditText)findViewById(R.id.confirmBox3);
        etConfirmBox4 = (EditText)findViewById(R.id.confirmBox4);

        etConfirmBox0.addTextChangedListener(this);
        etConfirmBox1.addTextChangedListener(this);
        etConfirmBox2.addTextChangedListener(this);
        etConfirmBox3.addTextChangedListener(this);
        etConfirmBox4.addTextChangedListener(this);

        etConfirmBox0.requestFocus();
    }

    public void resendClicked(View view) {

        final String email = PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_EMAIL,"");

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getResources().getString(R.string.please_wait)+"...");
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                boolean sent = false;
                try {
                    sent = AccessFactory.getInstance(ConfirmationCodeActivity.this).resendEmailConfirmation(email);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    clearBoxes();
                }finally {
                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
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

    public void verifyClicked(final String done) {

        if(code==null)return;
        if(code.length()!=5 )return;

        final Handler handler = new Handler();

        if(progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getResources().getString(R.string.please_wait)+"...");
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                String response = "";
                try {
                    response = AccessFactory.getInstance(ConfirmationCodeActivity.this).verifyEmail(done);
                    if (response != null && response.equals("Email successfully verified")) {
                        AppUtil.getInstance(ConfirmationCodeActivity.this).restartApp("verified", true);
                    }else {
                        ToastCustom.makeText(ConfirmationCodeActivity.this, response, ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        clearBoxes();
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                    ToastCustom.makeText(ConfirmationCodeActivity.this, response, ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    clearBoxes();
                }finally {
                    if(progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
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

    private void clearBoxes(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                etConfirmBox0.setText("");
                etConfirmBox1.setText("");
                etConfirmBox2.setText("");
                etConfirmBox3.setText("");
                etConfirmBox4.setText("");
                etConfirmBox0.requestFocus();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        if (etConfirmBox0.hasFocus())etConfirmBox1.requestFocus();
        else if (etConfirmBox1.hasFocus())etConfirmBox2.requestFocus();
        else if (etConfirmBox2.hasFocus())etConfirmBox3.requestFocus();
        else if (etConfirmBox3.hasFocus())etConfirmBox4.requestFocus();

        String a = etConfirmBox0.getText().toString();
        String b = etConfirmBox1.getText().toString();
        String c = etConfirmBox2.getText().toString();
        String d = etConfirmBox3.getText().toString();
        String e = etConfirmBox4.getText().toString();

        code = a+b+c+d+e;

        verifyClicked(code);
    }

    public void forgetClicked(View view) {
        AppUtil.getInstance(this).clearCredentialsAndRestart();
    }
}
