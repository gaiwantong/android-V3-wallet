package info.blockchain.wallet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.ToastCustom;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;

public class AccountEditActivity extends AppCompatActivity {

    private TextView tvLabelTitle = null;
    private TextView tvLabel = null;

    private TextView tvXpub = null;

    private TextView tvArchiveHeading = null;
    private TextView tvArchiveDescription = null;

    private Account account = null;
    private LegacyAddress legacyAddress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_edit);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        initToolbar();
        getIntentData();
        setupViews();
    }

    private void setupViews() {

        tvLabelTitle = (TextView) findViewById(R.id.tv_label);
        if(account != null){
            tvLabelTitle.setText(getString(R.string.name));//V3
        }else{
            tvLabelTitle.setText(getString(R.string.label));//V2
        }

        tvLabel = (TextView) findViewById(R.id.account_name);

        tvXpub = (TextView)findViewById(R.id.tv_xpub);

        tvArchiveHeading = (TextView) findViewById(R.id.tv_archive);
        tvArchiveDescription = (TextView) findViewById(R.id.tv_archive_description);

        if (account != null) {

            tvLabel.setText(account.getLabel());
            tvXpub.setText(R.string.extended_public_key);
            setArchive(account.isArchived());

        }else if (legacyAddress != null){

            tvLabel.setText(legacyAddress.getLabel());
            tvXpub.setText(R.string.address);
            setArchive(legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS);
        }
    }

    private void getIntentData() {

        int accountIndex = getIntent().getIntExtra("account_index", -1);
        int addressIndex = getIntent().getIntExtra("address_index", -1);

        if (accountIndex >= 0) {

            //V3
            List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();

            //Remove "All"
            List<Account> accountClone = new ArrayList<Account>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            account = accountClone.get(accountIndex);

        } else if (addressIndex >= 0) {

            //V2
            ImportedAccount iAccount = null;
            if (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0) {
                iAccount = new ImportedAccount(getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            }
            if (iAccount != null) {

                List<LegacyAddress> legacy = iAccount.getLegacyAddresses();
                legacyAddress = legacy.get(addressIndex);
            }
        }
    }

    private void initToolbar() {

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.edit));
        setSupportActionBar(toolbar);
    }

    private void setArchive(boolean isArchived){

        if(isArchived){
            tvArchiveHeading.setText(R.string.unarchive);
            tvArchiveDescription.setText(R.string.archived_description);

            findViewById(R.id.label_container).setAlpha(0.5f);
            findViewById(R.id.label_container).setClickable(false);
            findViewById(R.id.xpub_container).setAlpha(0.5f);
            findViewById(R.id.xpub_container).setClickable(false);
        }else{
            tvArchiveHeading.setText(R.string.archive);
            tvArchiveDescription.setText(R.string.not_archived_description);

            findViewById(R.id.label_container).setAlpha(1.0f);
            findViewById(R.id.label_container).setClickable(true);
            findViewById(R.id.xpub_container).setAlpha(1.0f);
            findViewById(R.id.xpub_container).setClickable(true);
        }
    }

    private void showXpubSharingWarning(){

        new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.xpub_sharing_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        showAddressDetails();
                    }

                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void showAddressDetails(){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_show_extended_public_key, null);
        dialogBuilder.setView(dialogView);

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        TextView tvHeading = (TextView)dialogView.findViewById(R.id.tv_warning_heading);
        TextView tvXpubNote = (TextView)dialogView.findViewById(R.id.tv_xpub_note);
        final TextView tvXpub = (TextView)dialogView.findViewById(R.id.tv_extended_xpub);
        ImageView ivQr = (ImageView)dialogView.findViewById(R.id.iv_qr);

        String qrString = null;

        if (account != null) {

            tvHeading.setText(R.string.extended_public_key);
            tvXpubNote.setText(R.string.scan_this_code);
            tvXpub.setText(R.string.copy_xpub);
            tvXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
            qrString = account.getXpub();

        }else if (legacyAddress != null){

            tvHeading.setText(R.string.address);
            tvXpubNote.setText(legacyAddress.getAddress());
            tvXpub.setText(R.string.copy_address);
            tvXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
            qrString = legacyAddress.getAddress();
        }

        final String finalQrString = qrString;
        tvXpub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) AccountEditActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = null;
                clip = android.content.ClipData.newPlainText("Send address", finalQrString);
                ToastCustom.makeText(AccountEditActivity.this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                clipboard.setPrimaryClip(clip);
            }
        });

        Bitmap bitmap = null;
        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        ivQr.setImageBitmap(bitmap);

        TextView confirmCancel = (TextView) dialogView.findViewById(R.id.confirm_cancel);
        confirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.cancel();
                }
            }
        });

        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    public void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    public void changeLabelClicked(View view) {

        final EditText etLabel = new EditText(this);
        etLabel.setInputType(InputType.TYPE_CLASS_TEXT);
        etLabel.setPadding(46, 16, 46, 16);
        new AlertDialog.Builder(this)
                .setTitle(R.string.name)
                .setMessage(R.string.assign_display_name)
                .setView(etLabel)
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                if (!ConnectivityStatus.hasConnectivity(AccountEditActivity.this)) {
                                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } else {
                                    if (etLabel != null && etLabel.getText().toString().trim().length() > 0) {

                                        String newLabel = etLabel.getText().toString().trim();

                                        new AsyncTask<String, Void, Void>() {

                                            ProgressDialog progress;

                                            @Override
                                            protected void onPreExecute() {
                                                super.onPreExecute();
                                                progress = new ProgressDialog(AccountEditActivity.this);
                                                progress.setTitle(R.string.app_name);
                                                progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                                                progress.show();
                                            }

                                            @Override
                                            protected void onPostExecute(Void aVoid) {
                                                super.onPostExecute(aVoid);
                                                if (progress != null && progress.isShowing()) {
                                                    progress.dismiss();
                                                    progress = null;
                                                }
                                            }

                                            @Override
                                            protected Void doInBackground(final String... params) {
                                                String revertLabel = account.getLabel();
                                                account.setLabel(params[0]);
                                                if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            tvLabel.setText(params[0]);
                                                            setResult(RESULT_OK);
                                                        }
                                                    });
                                                } else {
                                                    account.setLabel(revertLabel);//Remote save not successful - revert
                                                }
                                                return null;
                                            }
                                        }.execute(newLabel);


                                    } else {
                                        ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.label_cant_be_empty), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }
                                }
                            }
                        }
                ).
                setNegativeButton(R.string.cancel, null)
                .show();

    }

    public void revealExtendedXPub(View view) {
        if (account != null) {
            showXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    public void archiveClicked(View view) {

        String title = getResources().getString(R.string.archive);
        String subTitle = getResources().getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived()) || (legacyAddress != null && legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)) {
            title = getResources().getString(R.string.unarchive);
            subTitle = getResources().getString(R.string.unarchive_are_you_sure);
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(subTitle)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                if (!ConnectivityStatus.hasConnectivity(AccountEditActivity.this)) {
                                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } else {

                                    new AsyncTask<Void, Void, Void>() {

                                        ProgressDialog progress;

                                        @Override
                                        protected void onPreExecute() {
                                            super.onPreExecute();
                                            progress = new ProgressDialog(AccountEditActivity.this);
                                            progress.setTitle(R.string.app_name);
                                            progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
                                            progress.show();
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            super.onPostExecute(aVoid);
                                            if (progress != null && progress.isShowing()) {
                                                progress.dismiss();
                                                progress = null;
                                            }
                                        }

                                        @Override
                                        protected Void doInBackground(final Void... params) {

                                            if (account != null) {
                                                if (account.isArchived()) {
                                                    account.setArchived(false);
                                                } else {
                                                    account.setArchived(true);
                                                }
                                            } else {
                                                if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
                                                    legacyAddress.setTag(PayloadFactory.NORMAL_ADDRESS);
                                                } else {
                                                    legacyAddress.setTag(PayloadFactory.ARCHIVED_ADDRESS);
                                                }
                                            }

                                            if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        if ((account != null && account.isArchived()) || (legacyAddress != null && legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS)) {
                                                            setArchive(true);
                                                        } else {
                                                            setArchive(false);
                                                        }

                                                        setResult(RESULT_OK);
                                                    }
                                                });
                                            }
                                            return null;
                                        }
                                    }.execute();
                                }
                            }
                        }
                ).
                setNegativeButton(R.string.no, null)
                .show();
    }
}
