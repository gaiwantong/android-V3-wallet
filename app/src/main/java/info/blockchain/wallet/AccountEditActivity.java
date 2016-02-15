package info.blockchain.wallet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.service.WebSocketService;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.ToastCustom;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;

public class AccountEditActivity extends AppCompatActivity {

    private final int SCAN_PRIVX = 302;
    private final int ADDRESS_LABEL_MAX_LENGTH = 32;

    private TextView tvLabelTitle = null;
    private TextView tvLabel = null;

    private TextView tvXpub = null;
    private TextView tvExtendedXpubDescription = null;

    private TextView tvArchiveHeading = null;
    private TextView tvArchiveDescription = null;

    private Account account = null;
    private LegacyAddress legacyAddress = null;

    private int accountIndex;

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
        tvExtendedXpubDescription = (TextView)findViewById(R.id.tv_extended_xpub_description);

        tvArchiveHeading = (TextView) findViewById(R.id.tv_archive);
        tvArchiveDescription = (TextView) findViewById(R.id.tv_archive_description);

        if (account != null) {

            tvLabel.setText(account.getLabel());
            tvXpub.setText(R.string.extended_public_key);
            setArchive(account.isArchived());

            LinearLayout defaultContainer = (LinearLayout)findViewById(R.id.default_container);
            defaultContainer.setVisibility(View.VISIBLE);

            if(isDefault(account)){
                defaultContainer.setVisibility(View.GONE);
            }else{
                defaultContainer.setVisibility(View.VISIBLE);
            }

            findViewById(R.id.privx_container).setVisibility(View.GONE);

        }else if (legacyAddress != null){

            tvLabel.setText(legacyAddress.getLabel());
            tvXpub.setText(R.string.address);
            tvExtendedXpubDescription.setVisibility(View.GONE);
            setArchive(legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS);
            findViewById(R.id.default_container).setVisibility(View.GONE);//No default for V2

            if(legacyAddress.isWatchOnly()){
                findViewById(R.id.privx_container).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.privx_container).setVisibility(View.GONE);
            }
        }
    }

    private boolean isDefault(Account account){

        //TODO account.getRealIdx() always returns -1
//        if(account.getRealIdx() == PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex())

        int defaultIndex = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
        List<Account> accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();

        int accountIndex = 0;
        for(Account acc : accounts){

            if(acc.getXpub().equals(account.getXpub())){
                this.accountIndex = accountIndex;//sets this account index

                if(accountIndex == defaultIndex){//this is current default already
                    return true;
                }
            }

            accountIndex++;
        }
        return false;
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
            findViewById(R.id.privx_container).setAlpha(0.5f);
            findViewById(R.id.privx_container).setClickable(false);
            findViewById(R.id.default_container).setAlpha(0.5f);
            findViewById(R.id.default_container).setClickable(false);
        }else{

            //Don't allow archiving of default account
            if(isArchivable()){
                findViewById(R.id.archive_container).setAlpha(1.0f);
                findViewById(R.id.archive_container).setClickable(true);
                tvArchiveDescription.setText(R.string.not_archived_description);
            }else{
                findViewById(R.id.archive_container).setAlpha(0.5f);
                findViewById(R.id.archive_container).setClickable(false);
                tvArchiveDescription.setText(getString(R.string.default_account_description));
            }

            tvArchiveHeading.setText(R.string.archive);

            findViewById(R.id.label_container).setAlpha(1.0f);
            findViewById(R.id.label_container).setClickable(true);
            findViewById(R.id.xpub_container).setAlpha(1.0f);
            findViewById(R.id.xpub_container).setClickable(true);
            findViewById(R.id.privx_container).setAlpha(1.0f);
            findViewById(R.id.privx_container).setClickable(true);
        }
    }
    
    private boolean isArchivable(){

        if (PayloadFactory.getInstance().get().isUpgraded()) {
            //V3 - can't archive default account
            int defaultIndex = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
            Account defaultAccount = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(defaultIndex);

            if(defaultAccount == account)
                return false;
        }else{
            //V2 - must have a single unarchived address
            List<LegacyAddress> allActiveLegacyAddresses = PayloadFactory.getInstance().get().getActiveLegacyAddresses();
            return (allActiveLegacyAddresses.size() > 1);
        }

        return true;
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
                                                String revertLabel = null;
                                                if (account != null) {
                                                    revertLabel = account.getLabel();
                                                    account.setLabel(params[0]);
                                                } else {
                                                    revertLabel = legacyAddress.getLabel();
                                                    legacyAddress.setLabel(params[0]);
                                                }

                                                if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            tvLabel.setText(params[0]);
                                                            setResult(RESULT_OK);
                                                        }
                                                    });
                                                } else {
                                                    //Remote save not successful - revert
                                                    if (account != null) {
                                                        account.setLabel(revertLabel);
                                                    } else {
                                                        legacyAddress.setLabel(revertLabel);
                                                    }
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

    public void defaultClicked(View view) {

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

                final int revertDefault = PayloadFactory.getInstance().get().getHdWallet().getDefaultIndex();
                PayloadFactory.getInstance().get().getHdWallet().setDefaultIndex(accountIndex);

                if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            findViewById(R.id.default_container).setVisibility(View.GONE);

                            setResult(RESULT_OK);
                        }
                    });
                } else {
                    //Remote save not successful - revert
                    PayloadFactory.getInstance().get().getHdWallet().setDefaultIndex(revertDefault);
                }
                return null;
            }
        }.execute();

    }

    public void scanXPrivClicked(View view) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.privx_required)
                .setMessage(getString(R.string.watch_only_spend_instructions).replace("[--address--]",legacyAddress.getAddress()))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Intent intent = new Intent(AccountEditActivity.this, CaptureActivity.class);
                        startActivityForResult(intent, SCAN_PRIVX);
                    }
                }).setNegativeButton(R.string.cancel, null).show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SCAN_PRIVX && resultCode == Activity.RESULT_OK){
            String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);

            try {
                String format = PrivateKeyFactory.getInstance().getFormat(scanData);
                if (format != null) {
                    if (!format.equals(PrivateKeyFactory.BIP38)) {
                        importNonBIP38Address(format, scanData);
                    } else {
                        importBIP38Address(scanData);
                    }
                } else {
                    ToastCustom.makeText(this, getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void importBIP38Address(final String data) {

        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        new AsyncTask<Void, Void, Void>(){

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
                            protected Void doInBackground(Void... params) {

                                try {
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
                                    final ECKey key = bip38.decrypt(pw);

                                    if (key != null && key.hasPrivKey()) {

                                        final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                                        if(legacyAddress.getAddress().equals(keyAddress)) {
                                            importAddressPrivateKey(key);
                                        }else{

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new AlertDialog.Builder(AccountEditActivity.this)
                                                            .setTitle(R.string.warning)
                                                            .setMessage(getString(R.string.private_key_not_matching_address).replace("[--address--]",legacyAddress.getAddress()).replace("[--new_address--]",keyAddress))
                                                            .setCancelable(false)
                                                            .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            importUnmatchedPrivateKey(key);
                                                                        }
                                                                    });
                                                                }
                                                            }).setNegativeButton(R.string.cancel, null).show();
                                                }
                                            });
                                        }

                                    } else {
                                        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.invalid_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }
                                } catch (Exception e) {
                                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                }

                                return null;
                            }
                        }.execute();
                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void importNonBIP38Address(final String format, final String data) {

        new AsyncTask<Void, Void, Void>(){

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
            protected Void doInBackground(Void... params) {

                try {
                    final ECKey key = PrivateKeyFactory.getInstance().getKey(format, data);
                    if (key != null && key.hasPrivKey()) {

                        final String keyAddress = key.toAddress(MainNetParams.get()).toString();
                        if (legacyAddress.getAddress().equals(keyAddress)) {
                            importAddressPrivateKey(key);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(AccountEditActivity.this)
                                            .setTitle(R.string.warning)
                                            .setMessage(getString(R.string.private_key_not_matching_address).replace("[--address--]",legacyAddress.getAddress()).replace("[--new_address--]",keyAddress))
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            importUnmatchedPrivateKey(key);
                                                        }
                                                    });
                                                }
                                            }).setNegativeButton(R.string.cancel, null).show();
                                }
                            });
                        }
                    } else {
                        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.invalid_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }

                } catch (Exception e) {
                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }

    private void importAddressPrivateKey(ECKey key){

        //if double encrypted, save encrypted in payload
        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            legacyAddress.setWatchOnly(false);
        } else {
            String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
            legacyAddress.setWatchOnly(false);
        }

        if (PayloadBridge.getInstance(AccountEditActivity.this).remoteSaveThreadLocked()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.privx_container).setVisibility(View.GONE);
                    setResult(RESULT_OK);
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    new AlertDialog.Builder(AccountEditActivity.this)
                            .setTitle(R.string.success)
                            .setMessage(R.string.private_key_successfully_imported)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }).show();
                    Looper.loop();

                }
            }).start();
        }
    }

    private void importUnmatchedPrivateKey(ECKey key){

        final LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", "");
            /*
             * if double encrypted, save encrypted in payload
             */
        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
        } else {
            String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, PayloadFactory.getInstance().get().getSharedKey(), PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(), PayloadFactory.getInstance().get().getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
        }

        final EditText address_label = new EditText(AccountEditActivity.this);
        address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

        new AlertDialog.Builder(AccountEditActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.label_address)
                .setView(address_label)
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String label = address_label.getText().toString();
                        if (label != null && label.trim().length() > 0) {
                            legacyAddress.setLabel(label);
                        } else {
                            legacyAddress.setLabel(legacyAddress.getAddress());
                        }
                        remoteSaveUnmatchedPrivateKey(legacyAddress);

                    }
                }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                legacyAddress.setLabel(legacyAddress.getAddress());
                remoteSaveUnmatchedPrivateKey(legacyAddress);

            }
        }).show();
    }

    private void remoteSaveUnmatchedPrivateKey(final LegacyAddress legacyAddress){

        new AsyncTask<Void, Void, Void>(){

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
            protected Void doInBackground(Void... params) {

                Payload updatedPayload = PayloadFactory.getInstance().get();
                List<LegacyAddress> updatedLegacyAddresses = updatedPayload.getLegacyAddresses();
                updatedLegacyAddresses.add(legacyAddress);
                updatedPayload.setLegacyAddresses(updatedLegacyAddresses);
                PayloadFactory.getInstance().set(updatedPayload);

                if (PayloadFactory.getInstance().put()) {
                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.remote_save_ok), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                    ToastCustom.makeText(getApplicationContext(), legacyAddress.getAddress(), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(""));
                    List<String> legacyAddressList = PayloadFactory.getInstance().get().getLegacyAddressStrings();
                    MultiAddrFactory.getInstance().getLegacy(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);

                    //Subscribe to new address only if successfully created
                    Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                    intent.putExtra("address", legacyAddress.getAddress());
                    LocalBroadcastManager.getInstance(AccountEditActivity.this).sendBroadcast(intent);

                    setResult(RESULT_OK);
                    finish();

                } else {
                    ToastCustom.makeText(AccountEditActivity.this, getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

                return null;
            }
        }.execute();
    }
}
