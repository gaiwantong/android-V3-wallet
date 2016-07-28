package info.blockchain.wallet.viewModel;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.model.AccountEditModel;
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
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FeeUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
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

public class AccountEditViewModel implements ViewModel{

    private Context context;
    private DataListener dataListener;

    private PayloadManager payloadManager;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;

    private Account account = null;
    private LegacyAddress legacyAddress = null;
    private int accountIndex;

    public AccountEditModel accountModel;
    private PendingSpend pendingSpend;
    private String secondPassword;

    public interface DataListener {
        void onPromptAccountLabel();
        void onConnectivityLoss();
        void onToast(String errorMessage, String type);
        void onSetResult(int resultCode);
        void onStartScanActivity();
        void onPromptPrivateKey(String message);
        void onPromptTransferFunds(String fromLabel, String toLabel, String fee, String totalToSend);
        void onPromptArchive(String title, String message);
        void onPromptBIP38Password(String data);
        void onPrivateKeyImportMismatch();
        void onPrivateKeyImportSuccess();
        void onPromptSecondPasswordForTransfer();
        void onShowXpubSharingWarning();
        void onShowAddressDetails(String heading, String note, String copy, Bitmap bitmap, String qrString);
    }

    public AccountEditViewModel(AccountEditModel accountModel, Context context, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;

        this.payloadManager = PayloadManager.getInstance();
        prefsUtil = new PrefsUtil(context);
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        this.accountModel = accountModel;
    }

    public void setAccountModel(AccountEditModel accountModel){
        this.accountModel = accountModel;
    }

    @Override
    public void destroy() {
        context = null;
        dataListener = null;
    }

    public void setDataFromIntent(Intent intent) {
        int accountIndex = intent.getIntExtra("account_index", -1);
        int addressIndex = intent.getIntExtra("address_index", -1);

        if (accountIndex >= 0) {

            //V3
            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();

            //Remove "All"
            List<Account> accountClone = new ArrayList<Account>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            this.account = accountClone.get(accountIndex);

            accountModel.setLabel(account.getLabel());
            accountModel.setLabelHeader(context.getString(R.string.name));
            accountModel.setScanPrivateKeyVisibility(View.GONE);
            accountModel.setXpubDescriptionVisibility(View.VISIBLE);
            accountModel.setXpubText(context.getString(R.string.extended_public_key));
            accountModel.setTransferFundsVisibility(View.GONE);
            setArchive(account.isArchived());
            setDefault(isDefault(account));

        } else if (addressIndex >= 0) {

            //V2
            ImportedAccount iAccount = null;
            if (payloadManager.getPayload().getLegacyAddresses().size() > 0) {
                iAccount = new ImportedAccount(context.getString(R.string.imported_addresses), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            }

            if (iAccount != null) {

                List<LegacyAddress> legacy = iAccount.getLegacyAddresses();
                legacyAddress = legacy.get(addressIndex);


                accountModel.setLabel(legacyAddress.getLabel());
                accountModel.setLabelHeader(context.getString(R.string.name));
                accountModel.setXpubDescriptionVisibility(View.GONE);
                accountModel.setXpubText(context.getString(R.string.address));
                accountModel.setDefaultAccountVisibility(View.GONE);//No default for V2
                setArchive(legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS);

                if(legacyAddress.isWatchOnly()){
                    accountModel.setScanPrivateKeyVisibility(View.VISIBLE);
                }else{
                    accountModel.setScanPrivateKeyVisibility(View.GONE);
                }

                if(payloadManager.getPayload().isUpgraded()){
                    long balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
                    //Subtract fee
                    long balanceAfterFee = (balance - FeeUtil.AVERAGE_FEE.longValue());

                    if(balanceAfterFee > SendCoins.bDust.longValue() && !legacyAddress.isWatchOnly()){
                        accountModel.setTransferFundsVisibility(View.VISIBLE);
                    }else{
                        //No need to show 'transfer' if funds are less than dust amount
                        accountModel.setTransferFundsVisibility(View.GONE);
                    }
                }else{
                    //No transfer option for V2
                    accountModel.setTransferFundsVisibility(View.GONE);
                }
            }
        }
    }

    private void setDefault(boolean isDefault){
        if(isDefault){
            accountModel.setDefaultAccountVisibility(View.GONE);
            accountModel.setArchiveAlpha(0.5f);
            accountModel.setArchiveText(context.getString(R.string.default_account_description));
            accountModel.setArchiveClickable(false);
        }else{
            accountModel.setDefaultAccountVisibility(View.VISIBLE);
            accountModel.setDefaultText(context.getString(R.string.make_default));
            accountModel.setDefaultTextColor(context.getResources().getColor(R.color.blockchain_blue));
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

    private void setArchive(boolean isArchived){

        if(isArchived){
            accountModel.setArchiveHeader(context.getString(R.string.unarchive));
            accountModel.setArchiveText(context.getString(R.string.archived_description));
            accountModel.setArchiveAlpha(1.0f);
            accountModel.setArchiveVisibility(View.VISIBLE);
            accountModel.setArchiveClickable(true);

            accountModel.setLabelAlpha(0.5f);
            accountModel.setLabelClickable(false);
            accountModel.setXpubAlpha(0.5f);
            accountModel.setXpubClickable(false);
            accountModel.setXprivAlpha(0.5f);
            accountModel.setXprivClickable(false);
            accountModel.setDefaultAlpha(0.5f);
            accountModel.setDefaultClickable(false);
            accountModel.setTransferFundsAlpha(0.5f);
            accountModel.setTransferFundsClickable(false);
        }else{

            //Don't allow archiving of default account
            if(isArchivable()){
                accountModel.setArchiveAlpha(1.0f);
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveText(context.getString(R.string.not_archived_description));
                accountModel.setArchiveClickable(true);
            }else{
                accountModel.setArchiveVisibility(View.VISIBLE);
                accountModel.setArchiveAlpha(0.5f);
                accountModel.setArchiveText(context.getString(R.string.default_account_description));
                accountModel.setArchiveClickable(false);
            }

            accountModel.setArchiveHeader(context.getString(R.string.archive));

            accountModel.setLabelAlpha(1.0f);
            accountModel.setLabelClickable(true);
            accountModel.setXpubAlpha(1.0f);
            accountModel.setXpubClickable(true);
            accountModel.setXprivAlpha(1.0f);
            accountModel.setXprivClickable(true);
            accountModel.setDefaultAlpha(1.0f);
            accountModel.setDefaultClickable(true);
            accountModel.setTransferFundsAlpha(1.0f);
            accountModel.setTransferFundsClickable(true);
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

    private class PendingSpend {

        LegacyAddress fromLegacyAddress;
        String destination;
        BigInteger bigIntFee;
        BigInteger bigIntAmount;
    }

    public void onClickTransferFunds(View view) {

        //Only funded legacy address' will see this option
        pendingSpend = new PendingSpend();
        pendingSpend.fromLegacyAddress = legacyAddress;

        //To default
        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);
        pendingSpend.destination = payloadManager.getReceiveAddress(defaultIndex);
        pendingSpend.bigIntFee = FeeUtil.AVERAGE_FEE;

        //From
        String fromLabel = legacyAddress.getLabel();
        if(fromLabel == null || fromLabel.isEmpty())fromLabel = legacyAddress.getAddress();

        //To
        String toLabel = defaultAccount.getLabel();

        //Total
        long balance = MultiAddrFactory.getInstance().getLegacyBalance(pendingSpend.fromLegacyAddress.getAddress());
        long balanceAfterFee = (balance - FeeUtil.AVERAGE_FEE.longValue());
        pendingSpend.bigIntAmount = BigInteger.valueOf(balanceAfterFee);
        double btc_balance = (((double) balanceAfterFee) / 1e8);

        dataListener.onPromptTransferFunds(fromLabel,
                toLabel+" ("+context.getResources().getString(R.string.default_label)+")",
                monetaryUtil.getDisplayAmount(pendingSpend.bigIntFee.longValue()) + " BTC",
                monetaryUtil.getBTCFormat().format(monetaryUtil.getDenominatedAmount(btc_balance)) + " " + " BTC"
        );
    }

    public void onClickTransferFunds(){
        if(FormatsUtil.getInstance().isValidBitcoinAddress(pendingSpend.destination)){
            if (!payloadManager.getPayload().isDoubleEncrypted()) {
                sendPayment();
            } else {
                dataListener.onPromptSecondPasswordForTransfer();
            }
        }else{
            //This should never happen
            dataListener.onToast(context.getString(R.string.invalid_bitcoin_address), ToastCustom.TYPE_ERROR);
        }
    }

    public void updateAccountLabel(String newLabel) {

        if (!ConnectivityStatus.hasConnectivity(context)) {
            dataListener.onConnectivityLoss();
        } else {

            newLabel = newLabel.trim();

            if (newLabel != null && newLabel.length() > 0) {

                final String finalNewLabel = newLabel;
                new AsyncTask<String, Void, Void>() {

                    ProgressDialog progress;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        progress = new ProgressDialog(context);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(context.getResources().getString(R.string.please_wait));
                        progress.show();
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
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
                            dataListener.onSetResult(Activity.RESULT_OK);
                            accountModel.setLabel(finalNewLabel);
                        } else {
                            //Remote save not successful - revert
                            if (account != null) {
                                account.setLabel(revertLabel);
                            } else {
                                legacyAddress.setLabel(revertLabel);
                            }
                            accountModel.setLabel(revertLabel);
                            dataListener.onToast(context.getString(R.string.remote_save_ko), ToastCustom.TYPE_ERROR);
                        }
                        return null;
                    }
                }.execute(newLabel);


            } else {
                dataListener.onToast(context.getResources().getString(R.string.label_cant_be_empty), ToastCustom.TYPE_ERROR);
            }
        }
    }

    public void onClickChangeLabel(View view) {
        dataListener.onPromptAccountLabel();
    }

    public void onClickDefault(View view) {
        new AsyncTask<String, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(context);
                progress.setTitle(R.string.app_name);
                progress.setMessage(context.getResources().getString(R.string.please_wait));
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
                    setDefault(isDefault(account));
                    dataListener.onSetResult(Activity.RESULT_OK);
                } else {
                    //Remote save not successful - revert
                    payloadManager.getPayload().getHdWallet().setDefaultIndex(revertDefault);
                }
                return null;
            }
        }.execute();
    }

    public void onClickScanXpriv(View view) {
        if (payloadManager.getPayload().isDoubleEncrypted()) {
            dataListener.onPromptPrivateKey(String.format(context.getString(R.string.watch_only_spend_instructionss), legacyAddress.getAddress()));
        }else{
            dataListener.onStartScanActivity();
        }
    }

    public void onClickShowXpub(View view) {
        if (account != null) {
            dataListener.onShowXpubSharingWarning();
        } else {
            showAddressDetails();
        }
    }

    public void onClickArchive(View view) {
        String title = context.getResources().getString(R.string.archive);
        String subTitle = context.getResources().getString(R.string.archive_are_you_sure);

        if ((account != null && account.isArchived()) || (legacyAddress != null && legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS)) {
            title = context.getResources().getString(R.string.unarchive);
            subTitle = context.getResources().getString(R.string.unarchive_are_you_sure);
        }

        dataListener.onPromptArchive(title, subTitle);
    }

    private boolean toggleArchived(){

        if (account != null) {
            account.setArchived(!account.isArchived());

            return account.isArchived();

        } else {
            if (legacyAddress.getTag() == PayloadManager.ARCHIVED_ADDRESS) {
                legacyAddress.setTag(PayloadManager.NORMAL_ADDRESS);
                return false;
            } else {
                legacyAddress.setTag(PayloadManager.ARCHIVED_ADDRESS);
                return true;
            }
        }
    }

    private void importNonBIP38Address(final String format, final String data) {

        new AsyncTask<Void, Void, Void>(){

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(context);
                progress.setTitle(R.string.app_name);
                progress.setMessage(context.getResources().getString(R.string.please_wait));
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
                        dataListener.onToast(context.getString(R.string.invalid_private_key), ToastCustom.TYPE_ERROR);
                    }

                } catch (Exception e) {
                    dataListener.onToast(context.getString(R.string.no_private_key), ToastCustom.TYPE_ERROR);
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
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                    payloadManager.getPayload().getSharedKey(),
                    secondPassword,
                    payloadManager.getPayload().getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
            legacyAddress.setWatchOnly(false);
        }

        if (payloadManager.savePayloadToServer()) {

            dataListener.onSetResult(Activity.RESULT_OK);
            accountModel.setScanPrivateKeyVisibility(View.GONE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    dataListener.onPrivateKeyImportSuccess();
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
                    dataListener.onPrivateKeyImportMismatch();
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
                String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                        payloadManager.getPayload().getSharedKey(),
                        secondPassword,
                        payloadManager.getPayload().getOptions().getIterations());
                legacyAddress.setEncryptedKey(encrypted2);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    dataListener.onPrivateKeyImportMismatch();
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

            List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStrings();
            try {
                MultiAddrFactory.getInstance().refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Subscribe to new address only if successfully created
            Intent intent = new Intent(WebSocketService.ACTION_INTENT);
            intent.putExtra("address", legacyAddress.getAddress());
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            dataListener.onSetResult(Activity.RESULT_OK);
        } else {
            dataListener.onToast(context.getString(R.string.remote_save_ko), ToastCustom.TYPE_ERROR);
        }
    }

    public void sendPayment(){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                UnspentOutputsBundle unspents = null;
                try {

                    String unspentApiResponse = WebUtil.getInstance().getURL(WebUtil.UNSPENT_OUTPUTS_URL + pendingSpend.fromLegacyAddress.getAddress());
                    unspents = SendFactory.getInstance(context).prepareSend(pendingSpend.fromLegacyAddress.getAddress(), pendingSpend.bigIntAmount.add(FeeUtil.AVERAGE_FEE), BigInteger.ZERO, unspentApiResponse);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (unspents != null) {

                    //Warn user of unconfirmed funds - but don't block payment
                    if(unspents.getNotice() != null){
                        dataListener.onToast(pendingSpend.fromLegacyAddress.getAddress()+" - "+unspents.getNotice(), ToastCustom.TYPE_ERROR);
                    }

                    executeSend(pendingSpend, unspents);

                } else {

                    dataListener.onToast(pendingSpend.fromLegacyAddress.getAddress()+" - "+context.getString(R.string.no_confirmed_funds), ToastCustom.TYPE_ERROR);
                }

                Looper.loop();
            }
        }).start();
    }

    private void executeSend(final PendingSpend pendingSpend, final UnspentOutputsBundle unspents){

        final ProgressDialog progress;
        progress = new ProgressDialog(context);
        progress.setTitle(R.string.app_name);
        progress.setMessage(context.getResources().getString(R.string.please_wait));
        progress.setCancelable(false);
        progress.show();

        SendFactory.getInstance(context).execSend(-1, unspents.getOutputs(),
                pendingSpend.destination, pendingSpend.bigIntAmount,
                pendingSpend.fromLegacyAddress, pendingSpend.bigIntFee, null, false, secondPassword, new OpCallback() {

            public void onSuccess() {
            }

            @Override
            public void onSuccess(final String hash) {

                dataListener.onToast(context.getResources().getString(R.string.transaction_submitted), ToastCustom.TYPE_OK);

                //Update v2 balance immediately after spend - until refresh from server
                MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));
                PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                    @Override
                    public void onSaveSuccess() {
                    }

                    @Override
                    public void onSaveFail() {
                        dataListener.onToast(context.getString(R.string.remote_save_ko), ToastCustom.TYPE_ERROR);
                    }
                });

                accountModel.setTransferFundsVisibility(View.GONE);
                dataListener.onSetResult(Activity.RESULT_OK);
                onProgressDismiss(progress);
            }

            public void onFail(String error) {

                dataListener.onToast(context.getResources().getString(R.string.send_failed), ToastCustom.TYPE_ERROR);
                onProgressDismiss(progress);
            }

            @Override
            public void onFailPermanently(String error) {

                dataListener.onToast(error, ToastCustom.TYPE_ERROR);
                onProgressDismiss(progress);
            }

            private void onProgressDismiss(ProgressDialog progress){
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }
        });
    }

    public void showAddressDetails(){

        String heading = null;
        String note = null;
        String copy = null;
        String qrString = null;
        Bitmap bitmap = null;

        if (account != null) {

            heading = context.getString(R.string.extended_public_key);
            note = context.getString(R.string.scan_this_code);
            copy = context.getString(R.string.copy_xpub);
            qrString = account.getXpub();

        }else if (legacyAddress != null){

            heading = context.getString(R.string.address);
            note = legacyAddress.getAddress();
            copy = context.getString(R.string.copy_address);
            qrString = legacyAddress.getAddress();
        }

        int qrCodeDimension = 260;
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrString, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        dataListener.onShowAddressDetails(heading, note, copy, bitmap, qrString);
    }

    public void handleIncomingScanIntent(Intent data) {
        String scanData = data.getStringExtra(CaptureActivity.SCAN_RESULT);

        try {
            String format = PrivateKeyFactory.getInstance().getFormat(scanData);
            if (format != null) {
                if (!format.equals(PrivateKeyFactory.BIP38)) {
                    importNonBIP38Address(format, scanData);
                } else {
                    dataListener.onPromptBIP38Password(scanData);
                }
            } else {
                dataListener.onToast(context.getString(R.string.privkey_error), ToastCustom.TYPE_ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSecondPassword(String secondPassword) {
        this.secondPassword = secondPassword;
    }

    public void archiveAccount(){
        if (!ConnectivityStatus.hasConnectivity(context)) {
            dataListener.onConnectivityLoss();
        } else {

            new AsyncTask<Void, Void, Void>() {

                ProgressDialog progress;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progress = new ProgressDialog(context);
                    progress.setTitle(R.string.app_name);
                    progress.setMessage(context.getResources().getString(R.string.please_wait));
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

                    boolean isArchived = toggleArchived();

                    if (payloadManager.savePayloadToServer()) {

                        try {
                            payloadManager.updateBalancesAndTransactions();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        setArchive(isArchived);
                        dataListener.onSetResult(Activity.RESULT_OK);
                    }
                    return null;
                }
            }.execute();
        }
    }

    public void importBIP38Address(final String data, final String pw) {
        new AsyncTask<Void, Void, Void>(){

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(context);
                progress.setTitle(R.string.app_name);
                progress.setMessage(context.getResources().getString(R.string.please_wait));
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
                        dataListener.onToast(context.getString(R.string.invalid_private_key), ToastCustom.TYPE_ERROR);
                    }
                } catch (Exception e) {
                    dataListener.onToast(context.getString(R.string.bip38_error), ToastCustom.TYPE_ERROR);
                }

                return null;
            }
        }.execute();
    }
}
