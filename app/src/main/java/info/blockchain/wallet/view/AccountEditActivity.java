package info.blockchain.wallet.view;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FeeUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.WebUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.websocket.WebSocketService;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityAccountEditBinding;
import piuk.blockchain.android.databinding.AlertShowExtendedPublicKeyBinding;
import piuk.blockchain.android.databinding.AlertTransferFundsBinding;

public class AccountEditActivity extends AppCompatActivity {

    private final int SCAN_PRIVX = 302;
    private final int ADDRESS_LABEL_MAX_LENGTH = 17;

    private Account account = null;
    private LegacyAddress legacyAddress = null;

    private int accountIndex;
    private MonetaryUtil monetaryUtil;
    private PrefsUtil prefsUtil;
    private PayloadManager payloadManager;

    private ActivityAccountEditBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_edit);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        payloadManager = PayloadManager.getInstance();
        prefsUtil = new PrefsUtil(this);
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        initToolbar();
        getIntentData();
        setupViews();

        updateAllFields();
    }

    private void setupViews() {

        if(account != null){
            binding.tvLabel.setText(getString(R.string.name));//V3
        }else{
            binding.tvLabel.setText(getString(R.string.label));//V2
        }

        if (account != null) {

            binding.privxContainer.setVisibility(View.GONE);

        }else if (legacyAddress != null){

            if(legacyAddress.isWatchOnly()){
                binding.privxContainer.setVisibility(View.VISIBLE);
            }else{
                binding.privxContainer.setVisibility(View.GONE);
            }
        }
    }

    private void updateAllFields(){
        updateTransferField();
        updateLabelField();
        updateDefaultField();
        updateArchivedField();
        updateXpubField();
    }

    private void updateTransferField(){
        if (account != null) {
            binding.transferContainer.setVisibility(View.GONE);

        }else if (legacyAddress != null && payloadManager.getPayload().isUpgraded()){

            long balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
            //Subtract fee
            long balanceAfterFee = (balance - FeeUtil.AVERAGE_FEE.longValue());

            if(balanceAfterFee > SendCoins.bDust.longValue() && !legacyAddress.isWatchOnly()){
                binding.transferContainer.setVisibility(View.VISIBLE);
            }else{
                //No need to show 'transfer' if funds are less than dust amount
                binding.transferContainer.setVisibility(View.GONE);
            }
        }else{
            //No transfer option for V2
            binding.transferContainer.setVisibility(View.GONE);
        }
    }

    private void updateLabelField(){
        if (account != null) {
            binding.accountName.setText(account.getLabel());
        }else if (legacyAddress != null){
            binding.accountName.setText(legacyAddress.getLabel());
        }
    }

    private void updateDefaultField(){
        if (account != null) {
            if(isDefault(account)){
                binding.defaultContainer.setVisibility(View.GONE);
                binding.defaultContainer.setClickable(false);
            }else{
                binding.defaultContainer.setVisibility(View.VISIBLE);
                binding.tvDefault.setText(getString(R.string.make_default));
                binding.tvDefault.setTextColor(getResources().getColor(R.color.blockchain_blue));
                binding.defaultContainer.setClickable(true);
            }

        }else if (legacyAddress != null){
            binding.defaultContainer.setVisibility(View.GONE);//No default for V2
        }
    }

    private void updateArchivedField(){

        if (account != null) {
            setArchive(account.isArchived());
        }else if (legacyAddress != null){
            setArchive(legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS);
        }
    }

    private void updateXpubField(){
        if (account != null) {

            binding.tvXpub.setText(R.string.extended_public_key);
        }else if (legacyAddress != null){

            binding.tvXpub.setText(R.string.address);
            binding.tvExtendedXpubDescription.setVisibility(View.GONE);
        }
    }

    private boolean isDefault(Account account){

        //TODO account.getRealIdx() always returns -1
//        if(account.getRealIdx() == payloadManager.get().getHdWallet().getDefaultIndex())

        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();

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

    private void toggleArchived(){
        if (account != null) {
            account.setArchived(!account.isArchived());
        } else {
            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS) {
                legacyAddress.setTag(PayloadManager.NORMAL_ADDRESS);
            } else {
                legacyAddress.setTag(PayloadManager.ARCHIVED_ADDRESS);
            }
        }
    }

    private void getIntentData() {

        int accountIndex = getIntent().getIntExtra("account_index", -1);
        int addressIndex = getIntent().getIntExtra("address_index", -1);

        if (accountIndex >= 0) {

            //V3
            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();

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
            if (payloadManager.getPayload().getLegacyAddresses().size() > 0) {
                iAccount = new ImportedAccount(getString(R.string.imported_addresses), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            }
            if (iAccount != null) {

                List<LegacyAddress> legacy = iAccount.getLegacyAddresses();
                legacyAddress = legacy.get(addressIndex);
            }
        }
    }

    private void initToolbar() {

        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.edit));
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);
    }

    private void setArchive(boolean isArchived){

        if(isArchived){
            binding.tvArchive.setText(R.string.unarchive);
            binding.tvArchiveDescription.setText(R.string.archived_description);

            binding.labelContainer.setAlpha(0.5f);
            binding.labelContainer.setClickable(false);
            binding.xpubContainer.setAlpha(0.5f);
            binding.xpubContainer.setClickable(false);
            binding.privxContainer.setAlpha(0.5f);
            binding.privxContainer.setClickable(false);
            binding.defaultContainer.setAlpha(0.5f);
            binding.defaultContainer.setClickable(false);
            binding.transferContainer.setAlpha(0.5f);
            binding.transferContainer.setClickable(false);
        }else{

            //Don't allow archiving of default account
            if(isArchivable()){
                binding.archiveContainer.setAlpha(1.0f);
                binding.archiveContainer.setClickable(true);
                binding.tvArchiveDescription.setText(R.string.not_archived_description);
            }else{
                binding.archiveContainer.setAlpha(0.5f);
                binding.archiveContainer.setClickable(false);
                binding.tvArchiveDescription.setText(getString(R.string.default_account_description));
            }

            binding.tvArchive.setText(R.string.archive);

            binding.labelContainer.setAlpha(1.0f);
            binding.labelContainer.setClickable(true);
            binding.xpubContainer.setAlpha(1.0f);
            binding.xpubContainer.setClickable(true);
            binding.privxContainer.setAlpha(1.0f);
            binding.privxContainer.setClickable(true);
            binding.defaultContainer.setAlpha(1.0f);
            binding.defaultContainer.setClickable(true);
            binding.transferContainer.setAlpha(1.0f);
            binding.transferContainer.setClickable(true);
        }
    }

    private boolean isArchivable(){

        if (payloadManager.getPayload().isUpgraded()) {
            //V3 - can't archive default account
            int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
            Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);

            if(defaultAccount == account)
                return false;
        }else{
            //V2 - must have a single unarchived address
            List<LegacyAddress> allActiveLegacyAddresses = payloadManager.getPayload().getActiveLegacyAddresses();
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
        AlertShowExtendedPublicKeyBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_show_extended_public_key, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        String qrString = null;

        if (account != null) {

            dialogBinding.tvWarningHeading.setText(R.string.extended_public_key);
            dialogBinding.tvXpubNote.setText(R.string.scan_this_code);
            dialogBinding.tvExtendedXpub.setText(R.string.copy_xpub);
            dialogBinding.tvExtendedXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
            qrString = account.getXpub();

        }else if (legacyAddress != null){

            dialogBinding.tvWarningHeading.setText(R.string.address);
            dialogBinding.tvXpubNote.setText(legacyAddress.getAddress());
            dialogBinding.tvExtendedXpub.setText(R.string.copy_address);
            dialogBinding.tvExtendedXpub.setTextColor(getResources().getColor(R.color.blockchain_blue));
            qrString = legacyAddress.getAddress();
        }

        final String finalQrString = qrString;
        dialogBinding.tvExtendedXpub.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) AccountEditActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = null;
            clip = android.content.ClipData.newPlainText("Send address", finalQrString);
            ToastCustom.makeText(AccountEditActivity.this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
            clipboard.setPrimaryClip(clip);
        });

        Bitmap bitmap = null;
        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        dialogBinding.ivQr.setImageBitmap(bitmap);

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.cancel();
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
        AccessState.getInstance(this).stopLogoutTimer();
    }

    @Override
    public void onPause() {
        AccessState.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    public void changeLabelClicked(View view) {

        final EditText etLabel = new EditText(this);
        etLabel.setInputType(InputType.TYPE_CLASS_TEXT);
        etLabel.setPadding(46, 16, 46, 16);
        etLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});
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

                                                if (payloadManager.savePayloadToServer()) {

                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            updateLabelField();
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

    public void extendedXPubClicked(View view) {
        if (account != null) {
            showXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    public void archiveClicked(View view) {

        String title = getResources().getString(R.string.archive);
        String subTitle = getResources().getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived()) || (legacyAddress != null && legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)) {
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
                                            progress.setCancelable(false);
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

                                            toggleArchived();

                                            if (payloadManager.savePayloadToServer()) {

                                                try {
                                                    payloadManager.updateBalancesAndTransactions();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        updateAllFields();
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

                final int revertDefault = payloadManager.getPayload().getHdWallet().getDefaultIndex();
                payloadManager.getPayload().getHdWallet().setDefaultIndex(accountIndex);

                if (payloadManager.savePayloadToServer()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            updateAllFields();
                            setResult(RESULT_OK);
                        }
                    });
                } else {
                    //Remote save not successful - revert
                    payloadManager.getPayload().getHdWallet().setDefaultIndex(revertDefault);
                }
                return null;
            }
        }.execute();

    }

    public void scanXPrivClicked(View view) {

        if (payloadManager.getPayload().isDoubleEncrypted()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.privx_required)
                    .setMessage(String.format(getString(R.string.watch_only_spend_instructionss), legacyAddress.getAddress()))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            final EditText password = new EditText(AccountEditActivity.this);
                            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                            new AlertDialog.Builder(AccountEditActivity.this)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.enter_double_encryption_pw)
                                    .setView(password)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            final String pw = password.getText().toString();

                                            if (DoubleEncryptionFactory.getInstance().validateSecondPassword(payloadManager.getPayload().getDoublePasswordHash(), payloadManager.getPayload().getSharedKey(), new CharSequenceX(pw), payloadManager.getPayload().getDoubleEncryptionPbkdf2Iterations())) {
                                                payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(pw));
                                                if (ContextCompat.checkSelfPermission(AccountEditActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                                    PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, AccountEditActivity.this);
                                                }else{
                                                    startScanActivity();
                                                }
                                            } else {
                                                ToastCustom.makeText(AccountEditActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                            }

                                        }
                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ;
                                }
                            }).show();

                        }
                    }).setNegativeButton(R.string.cancel, null).show();
        }else{
            if (ContextCompat.checkSelfPermission(AccountEditActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, AccountEditActivity.this);
            }else{
                startScanActivity();
            }
        }
    }

    private void startScanActivity(){
        if (!new AppUtil(AccountEditActivity.this).isCameraOpen()) {
            Intent intent = new Intent(AccountEditActivity.this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_PRIVX);
        } else {
            ToastCustom.makeText(AccountEditActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
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
                                        if (!legacyAddress.getAddress().equals(keyAddress)) {
                                            //Private key does not match this address - warn user but import nevertheless
                                            importUnmatchedPrivateKey(key);
                                        }else{
                                            importAddressPrivateKey(key);
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
                        if (!legacyAddress.getAddress().equals(keyAddress)) {
                            //Private key does not match this address - warn user but import nevertheless
                            importUnmatchedPrivateKey(key);
                        }else{
                            importAddressPrivateKey(key);
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
        if (!payloadManager.getPayload().isDoubleEncrypted()) {
            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            legacyAddress.setWatchOnly(false);
        } else {
            String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, payloadManager.getPayload().getSharedKey(), payloadManager.getTempDoubleEncryptPassword().toString(), payloadManager.getPayload().getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
            legacyAddress.setWatchOnly(false);
        }

        if (payloadManager.savePayloadToServer()) {

            //Reset double encrypt
            payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.privxContainer.setVisibility(View.GONE);
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

        if (payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
            //Wallet already contains private key - silently avoid duplicating
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    AlertDialog.Builder alert = new AlertDialog.Builder(AccountEditActivity.this);
                    alert.setTitle(getString(R.string.warning));
                    alert.setMessage(getString(R.string.private_key_successfully_imported)+"\n\n"+getString(R.string.private_key_not_matching_address));
                    alert.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            scanXPrivClicked(null);
                        }
                    });
                    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                    alert.show();
                    Looper.loop();

                }
            }).start();
        }else{
            final LegacyAddress legacyAddress = new LegacyAddress(null, System.currentTimeMillis() / 1000L, key.toAddress(MainNetParams.get()).toString(), "", 0L, "android", "");
            /*
             * if double encrypted, save encrypted in payload
             */
            if (!payloadManager.getPayload().isDoubleEncrypted()) {
                legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
            } else {
                String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
                String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey, payloadManager.getPayload().getSharedKey(), payloadManager.getTempDoubleEncryptPassword().toString(), payloadManager.getPayload().getOptions().getIterations());
                legacyAddress.setEncryptedKey(encrypted2);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    AlertDialog.Builder alert = new AlertDialog.Builder(AccountEditActivity.this);
                    alert.setTitle(getString(R.string.warning));
                    alert.setMessage(getString(R.string.private_key_successfully_imported)+"\n\n"+getString(R.string.private_key_not_matching_address));
                    alert.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            scanXPrivClicked(null);
                        }
                    });
                    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
                    alert.show();
                    Looper.loop();

                }
            }).start();

            remoteSaveUnmatchedPrivateKey(legacyAddress);
        }
    }

    private void remoteSaveUnmatchedPrivateKey(final LegacyAddress legacyAddress){

        Payload updatedPayload = payloadManager.getPayload();
        List<LegacyAddress> updatedLegacyAddresses = updatedPayload.getLegacyAddresses();
        updatedLegacyAddresses.add(legacyAddress);
        updatedPayload.setLegacyAddresses(updatedLegacyAddresses);
        payloadManager.setPayload(updatedPayload);

        if (payloadManager.savePayloadToServer()) {

            payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));
            List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStrings();
            try {
                MultiAddrFactory.getInstance().refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Subscribe to new address only if successfully created
            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
            intent.putExtra("address", legacyAddress.getAddress());
            LocalBroadcastManager.getInstance(AccountEditActivity.this).sendBroadcast(intent);
            setResult(RESULT_OK);
        } else {
            ToastCustom.makeText(AccountEditActivity.this, getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    public void transferClicked(View view) {

        //Only funded legacy address' will see this option
        final PendingSpend pendingSpend = new PendingSpend();
        pendingSpend.fromLegacyAddress = legacyAddress;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertTransferFundsBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_transfer_funds, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        //From
        String label = pendingSpend.fromLegacyAddress.getLabel();
        if(label == null || label.isEmpty())label = pendingSpend.fromLegacyAddress.getAddress();
        dialogBinding.confirmFrom.setText(label);

        //To default
        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);
        pendingSpend.destination = payloadManager.getReceiveAddress(defaultIndex);

        String toLabel = defaultAccount.getLabel();
        if(toLabel ==null || toLabel.isEmpty())toLabel = pendingSpend.destination;
        dialogBinding.confirmTo.setText(toLabel+" ("+getResources().getString(R.string.default_label)+")");

        //Fee
        pendingSpend.bigIntFee = FeeUtil.AVERAGE_FEE;
        dialogBinding.confirmFee.setText(monetaryUtil.getDisplayAmount(pendingSpend.bigIntFee.longValue()) + " BTC");

        //Total
        long balance = MultiAddrFactory.getInstance().getLegacyBalance(pendingSpend.fromLegacyAddress.getAddress());
        long balanceAfterFee = (balance - FeeUtil.AVERAGE_FEE.longValue());
        pendingSpend.bigIntAmount = BigInteger.valueOf(balanceAfterFee);
        double btc_balance = (((double) balanceAfterFee) / 1e8);
        dialogBinding.confirmTotalToSend.setText(monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(btc_balance)) + " " + " BTC");

        dialogBinding.confirmCancel.setOnClickListener(v -> alertDialog.dismiss());

        dialogBinding.confirmSend.setOnClickListener(v -> {

            if(FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)){
                if (!payloadManager.getPayload().isDoubleEncrypted() || DoubleEncryptionFactory.getInstance().isActivated()) {
                    sendPayment(pendingSpend);
                } else {
                    alertDoubleEncrypted(pendingSpend);
                }
            }else{
                //This should never happen
                ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }

            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private class PendingSpend {

        LegacyAddress fromLegacyAddress;
        String destination;
        BigInteger bigIntFee;
        BigInteger bigIntAmount;
    }

    private void alertDoubleEncrypted(final PendingSpend pendingSpend){
        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_double_encryption_pw)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (DoubleEncryptionFactory.getInstance().validateSecondPassword(payloadManager.getPayload().getDoublePasswordHash(), payloadManager.getPayload().getSharedKey(), new CharSequenceX(pw), payloadManager.getPayload().getDoubleEncryptionPbkdf2Iterations())) {

                            payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(pw));
                            sendPayment(pendingSpend);

                        } else {
                            ToastCustom.makeText(AccountEditActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void sendPayment(final PendingSpend pendingSpend){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                UnspentOutputsBundle unspents = null;
                try {

                    String unspentApiResponse = WebUtil.getInstance().getURL(WebUtil.UNSPENT_OUTPUTS_URL + pendingSpend.fromLegacyAddress.getAddress());
                    unspents = SendFactory.getInstance(AccountEditActivity.this).prepareSend(pendingSpend.fromLegacyAddress.getAddress(), pendingSpend.bigIntAmount.add(FeeUtil.AVERAGE_FEE), BigInteger.ZERO, unspentApiResponse);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (unspents != null) {

                    //Warn user of unconfirmed funds - but don't block payment
                    if(unspents.getNotice() != null){
                        ToastCustom.makeText(AccountEditActivity.this, pendingSpend.fromLegacyAddress.getAddress()+" - "+unspents.getNotice(), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }

                    executeSend(pendingSpend, unspents);

                } else {

                    payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));
                    ToastCustom.makeText(AccountEditActivity.this, pendingSpend.fromLegacyAddress.getAddress()+" - "+getString(R.string.no_confirmed_funds), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                }

                Looper.loop();
            }
        }).start();
    }

    private void executeSend(final PendingSpend pendingSpend, final UnspentOutputsBundle unspents){

        final ProgressDialog progress;
        progress = new ProgressDialog(AccountEditActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(AccountEditActivity.this.getResources().getString(R.string.please_wait));
        progress.setCancelable(false);
        progress.show();

        SendFactory.getInstance(this).execSend(-1, unspents.getOutputs(), pendingSpend.destination, pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, null, false, new OpCallback() {

            public void onSuccess() {
            }

            @Override
            public void onSuccess(final String hash) {

                ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                //Update v2 balance immediately after spend - until refresh from server
                MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));
                PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                    @Override
                    public void onSaveSuccess() {
                    }

                    @Override
                    public void onSaveFail() {
                        ToastCustom.makeText(AccountEditActivity.this, getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.transferContainer.setVisibility(View.GONE);
                    }
                });

                setResult(RESULT_OK);

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

            public void onFail(String error) {

                //Reset double encrypt for V2
                payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));
                ToastCustom.makeText(AccountEditActivity.this, getResources().getString(R.string.send_failed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

            @Override
            public void onFailPermanently(String error) {

                //Reset double encrypt for V2
                payloadManager.setTempDoubleEncryptPassword(new CharSequenceX(""));
                ToastCustom.makeText(AccountEditActivity.this, error, ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity();
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
