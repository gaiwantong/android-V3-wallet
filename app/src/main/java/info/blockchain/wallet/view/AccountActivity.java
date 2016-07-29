package info.blockchain.wallet.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.account_manager.AccountAdapter;
import info.blockchain.wallet.account_manager.AccountItem;
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
import info.blockchain.wallet.util.AddressInfo;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FeeUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.PrivateKeyFactory;
import info.blockchain.wallet.util.WebUtil;
import info.blockchain.wallet.view.helpers.RecyclerItemClickListener;
import info.blockchain.wallet.view.helpers.SecondPasswordHandler;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.websocket.WebSocketService;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityAccountsBinding;
import piuk.blockchain.android.databinding.AlertPromptTransferFundsBinding;
import piuk.blockchain.android.databinding.AlertTransferFundsBinding;

//import android.util.Log;

public class AccountActivity extends AppCompatActivity {

    public static final String ACTION_INTENT = BalanceFragment.ACTION_INTENT;
    private static final int IMPORT_PRIVATE_REQUEST_CODE = 2006;
    private static final int EDIT_ACTIVITY_REQUEST_CODE = 2007;

    private static int ADDRESS_LABEL_MAX_LENGTH = 17;

    private static String[] HEADERS;
    public static String IMPORTED_HEADER;

    private AppUtil appUtil;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {

                AccountActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateAccountsList();
                    }
                });

            }
        }
    };
    private LinearLayoutManager layoutManager = null;
    private ArrayList<AccountItem> accountsAndImportedList = null;
    private AccountAdapter accountsAdapter = null;
    private ArrayList<Integer> headerPositions;
    private int hdAccountsIdx;
    private List<LegacyAddress> legacy = null;
    private ProgressDialog progress = null;
    private Context context = null;
    private MenuItem transferFundsMenuItem = null;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    private PayloadManager payloadManager;

    private ActivityAccountsBinding binding;
    private String secondPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        prefsUtil = new PrefsUtil(context);
        appUtil = new AppUtil(context);
        payloadManager = PayloadManager.getInstance();
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

        binding = DataBindingUtil.setContentView(this, R.layout.activity_accounts);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        initToolbar();

        setupViews();

        setFab();
    }

    private void initToolbar(){

        if (!payloadManager.isNotUpgraded()) {
            binding.toolbarContainer.toolbarGeneral.setTitle("");//TODO - empty header for V3 for now - awaiting product
        } else {
            binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.my_addresses));
        }
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);
    }

    private void setupViews(){

        IMPORTED_HEADER = getResources().getString(R.string.imported_addresses);

        if (!payloadManager.isNotUpgraded())
            HEADERS = new String[]{IMPORTED_HEADER};
        else
            HEADERS = new String[0];

        layoutManager = new LinearLayoutManager(this);
        binding.accountsList.setLayoutManager(layoutManager);

        accountsAndImportedList = new ArrayList<>();
        updateAccountsList();
        accountsAdapter = new AccountAdapter(accountsAndImportedList);
        binding.accountsList.setAdapter(accountsAdapter);

        binding.accountsList.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(final View view, int position) {

                        if (!payloadManager.isNotUpgraded())
                            if (headerPositions.contains(position)) return;//headers unclickable

                        onRowClick(position);
                    }
                })
        );
    }

    private void setFab(){

        //First icon when fab expands
        FloatingActionButton actionA = new FloatingActionButton(getBaseContext());
        actionA.setColorNormal(getResources().getColor(R.color.blockchain_transfer_blue));
        actionA.setSize(FloatingActionButton.SIZE_MINI);
        actionA.setIconDrawable(getResources().getDrawable(R.drawable.icon_accounthd));
        actionA.setColorPressed(getResources().getColor(R.color.blockchain_dark_blue));

        if (!payloadManager.isNotUpgraded()) {
            //V3
            actionA.setTitle(getResources().getString(R.string.create_new));
            actionA.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewAccount();
                    if(binding.multipleActions.isExpanded())
                        binding.multipleActions.collapse();
                }
            });
        }else {
            //V2
            actionA.setTitle(getResources().getString(R.string.create_new_address));
            actionA.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewAddress();
                    if(binding.multipleActions.isExpanded())
                        binding.multipleActions.collapse();
                }
            });
        }

        //Second icon when fab expands
        FloatingActionButton actionB = new FloatingActionButton(getBaseContext());
        actionB.setColorNormal(getResources().getColor(R.color.blockchain_transfer_blue));
        actionB.setSize(FloatingActionButton.SIZE_MINI);
        actionB.setIconDrawable(getResources().getDrawable(R.drawable.icon_imported));
        actionB.setColorPressed(getResources().getColor(R.color.blockchain_dark_blue));
        actionB.setTitle(getResources().getString(R.string.import_address));
        actionB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importAddress();
                if(binding.multipleActions.isExpanded())
                    binding.multipleActions.collapse();
            }
        });

        //Add buttons to expanding fab
        binding.multipleActions.addButton(actionA);
        binding.multipleActions.addButton(actionB);
    }

    private void onRowClick(int position){

        Intent intent = new Intent(this, AccountEditActivity.class);
        if (position - HEADERS.length >= hdAccountsIdx) {//2 headers before imported
            intent.putExtra("address_index", position - HEADERS.length - hdAccountsIdx);
        } else {
            intent.putExtra("account_index", position);
        }
        startActivityForResult(intent, EDIT_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_activity_actions, menu);
        transferFundsMenuItem = menu.findItem(R.id.action_transfer_funds);

        if(hasTransferableFunds() && payloadManager.getPayload().isUpgraded()){

            transferFundsMenuItem.setVisible(true);

            if(prefsUtil.getValue("WARN_TRANSFER_ALL", true)){
                promptToTransferFunds(true);
            }

        }else{
            transferFundsMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_transfer_funds:
                promptToTransferFunds(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity(){
        if (!appUtil.isCameraOpen()) {
            Intent intent = new Intent(AccountActivity.this, CaptureActivity.class);
            intent.putExtra(Intents.Scan.FORMATS, EnumSet.allOf(BarcodeFormat.class));
            intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
            startActivityForResult(intent, IMPORT_PRIVATE_REQUEST_CODE);
        } else {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
    }

    private void importAddress() {
        if (ContextCompat.checkSelfPermission(AccountActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.requestCameraPermissionFromActivity(binding.mainLayout, AccountActivity.this);
        } else {
            new SecondPasswordHandler(AccountActivity.this).validate(new SecondPasswordHandler.ResultListener() {
                @Override
                public void onNoSecondPassword() {
                    startScanActivity();
                }

                @Override
                public void onSecondPasswordValidated(String validateSecondPassword) {
                    secondPassword = validateSecondPassword;
                    startScanActivity();
                }
            });
        }
    }

    private void createNewAccount() {

        new SecondPasswordHandler(AccountActivity.this).validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                promptForAccountLabel(null);
            }

            @Override
            public void onSecondPasswordValidated(String validateSecondPassword) {
                secondPassword = validateSecondPassword;
                promptForAccountLabel(validateSecondPassword);
            }
        });
    }

    private void promptForAccountLabel(@Nullable final String validatedSecondPassword){
        final EditText etLabel = new EditText(this);
        etLabel.setInputType(InputType.TYPE_CLASS_TEXT);
        etLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});
        new AlertDialog.Builder(this)
                .setTitle(R.string.label)
                .setMessage(R.string.assign_display_name)
                .setView(etLabel)
                .setCancelable(false)
                .setPositiveButton(R.string.save_name, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        if (etLabel != null && etLabel.getText().toString().trim().length() > 0) {
                            addAccount(etLabel.getText().toString().trim(), validatedSecondPassword);
                        } else {
                            ToastCustom.makeText(AccountActivity.this, getResources().getString(R.string.label_cant_be_empty), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void addAccount(final String accountLabel, @Nullable final String secondPassword) {

        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else {

            new AsyncTask<Void, Void, Void>() {

                ProgressDialog progress;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                    progress = new ProgressDialog(AccountActivity.this);
                    progress.setTitle(R.string.app_name);
                    progress.setMessage(AccountActivity.this.getResources().getString(R.string.please_wait));
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
                        payloadManager.addAccount(accountLabel, secondPassword, new PayloadManager.AccountAddListener() {
                            @Override
                            public void onAccountAddSuccess(Account account) {
                                ToastCustom.makeText(AccountActivity.this,
                                        AccountActivity.this.getString(R.string.remote_save_ok),
                                        ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);

                                //Subscribe to new xpub only if successfully created
                                Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                                intent.putExtra("xpub", account.getXpub());
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                                //Update adapter list
                                updateAccountsList();
                            }

                            @Override
                            public void onSecondPasswordFail() {
                                ToastCustom.makeText(AccountActivity.this,
                                        AccountActivity.this.getString(R.string.double_encryption_password_error),
                                        ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                            }

                            @Override
                            public void onPayloadSaveFail() {
                                ToastCustom.makeText(AccountActivity.this,
                                        AccountActivity.this.getString(R.string.remote_save_ko),
                                        ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);

                            }
                        });
                        }catch (Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }
    }

    private void createNewAddress(){

        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else {

            new SecondPasswordHandler(AccountActivity.this).validate(new SecondPasswordHandler.ResultListener() {
                @Override
                public void onNoSecondPassword() {
                    addAddress();
                }

                @Override
                public void onSecondPasswordValidated(String validateSecondPassword) {
                    secondPassword = validateSecondPassword;
                    addAddress();
                }
            });
        }

    }

    private void updateAccountsList() {

        headerPositions = new ArrayList<Integer>();

        //accountsAndImportedList is linked to AccountAdapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        accountsAndImportedList.clear();

        int i = 0;
        if (payloadManager.getPayload().isUpgraded()) {

            List<Account> accounts = payloadManager.getPayload().getHdWallet().getAccounts();
            List<Account> accountClone = new ArrayList<Account>(accounts.size());
            accountClone.addAll(accounts);

            if (accountClone.get(accountClone.size() - 1) instanceof ImportedAccount) {
                accountClone.remove(accountClone.size() - 1);
            }

            int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
            Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);

            int archivedCount = 0;
            for (; i < accountClone.size(); i++) {

                String label = accountClone.get(i).getLabel();
                String balance = getAccountBalance(i);

                if (label == null || label.length() == 0) label = "Account: " + (i + 1);

                accountsAndImportedList.add(new AccountItem(label, balance, getResources().getDrawable(R.drawable.icon_accounthd), accountClone.get(i).isArchived(), false, defaultAccount.getXpub().equals(accountClone.get(i).getXpub())));
            }
            hdAccountsIdx = accountClone.size() - archivedCount;
        }

        ImportedAccount iAccount = null;
        if (payloadManager.getPayload().getLegacyAddresses().size() > 0) {
            iAccount = new ImportedAccount(getString(R.string.imported_addresses), payloadManager.getPayload().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        }
        if (iAccount != null) {

            if (!payloadManager.isNotUpgraded()) {
                //Imported Header Position
                headerPositions.add(accountsAndImportedList.size());
                accountsAndImportedList.add(new AccountItem(HEADERS[0], "", getResources().getDrawable(R.drawable.icon_accounthd), false, false, false));
            }

            legacy = iAccount.getLegacyAddresses();
            for (int j = 0; j < legacy.size(); j++) {

                String label = legacy.get(j).getLabel();
                String balance = getAddressBalance(j);
                if (label == null || label.length() == 0) label = legacy.get(j).getAddress();

                accountsAndImportedList.add(new AccountItem(label, balance, getResources().getDrawable(R.drawable.icon_imported), legacy.get(j).getTag() == PayloadManager.ARCHIVED_ADDRESS, legacy.get(j).isWatchOnly(), false));
            }
        }

        AccountActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(accountsAdapter != null)accountsAdapter.notifyDataSetChanged();
            }
        });
    }

    private String getAccountBalance(int index) {

        String address = payloadManager.getXpubFromAccountIndex(index);
        Long amount = MultiAddrFactory.getInstance().getXpubAmounts().get(address);
        if (amount == null) amount = 0l;

        String unit = (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

        return monetaryUtil.getDisplayAmount(amount) + " " + unit;
    }

    private String getAddressBalance(int index) {

        String address = legacy.get(index).getAddress();
        Long amount = MultiAddrFactory.getInstance().getLegacyBalance(address);
        if (amount == null) amount = 0l;
        String unit = (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

        return monetaryUtil.getDisplayAmount(amount) + " " + unit;
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(AccountActivity.this).registerReceiver(receiver, filter);

        AccessState.getInstance(this).stopLogoutTimer();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(AccountActivity.this).unregisterReceiver(receiver);
        AccessState.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == IMPORT_PRIVATE_REQUEST_CODE
                && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
            try {
                final String strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT);
                String format = PrivateKeyFactory.getInstance().getFormat(strResult);
                if (format != null) {
                    //Private key scanned
                    if (!format.equals(PrivateKeyFactory.BIP38)) {
                        importNonBIP38Address(format, strResult);
                    } else {
                        importBIP38Address(strResult);
                    }
                } else {
                    //Watch-only address scanned
                    importWatchOnly(strResult);
                }
            } catch (Exception e) {
                ToastCustom.makeText(AccountActivity.this, getString(R.string.privkey_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == IMPORT_PRIVATE_REQUEST_CODE) {
            ;
        } else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {

            updateAccountsList();

        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {

        }
    }

    private void importBIP38Address(final String data) {

        final List<LegacyAddress> rollbackLegacyAddresses = payloadManager.getPayload().getLegacyAddresses();

        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(password)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();

                        if (progress != null && progress.isShowing()) {
                            progress.dismiss();
                            progress = null;
                        }
                        progress = new ProgressDialog(AccountActivity.this);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(AccountActivity.this.getResources().getString(R.string.please_wait));
                        progress.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                Looper.prepare();

                                try {
                                    BIP38PrivateKey bip38 = new BIP38PrivateKey(MainNetParams.get(), data);
                                    final ECKey key = bip38.decrypt(pw);

                                    if (key != null && key.hasPrivKey() && payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

                                        //A private key to an existing address has been scanned
                                        setPrivateKey(key);

                                    } else if (key != null && key.hasPrivKey() && !payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {
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
                                                    secondPassword.toString(),
                                                    payloadManager.getPayload().getOptions().getIterations());
                                            legacyAddress.setEncryptedKey(encrypted2);
                                        }

                                        final EditText address_label = new EditText(AccountActivity.this);
                                        address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

                                        new AlertDialog.Builder(AccountActivity.this)
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

                                                        remoteSaveNewAddress(legacyAddress);

                                                    }
                                                }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                legacyAddress.setLabel(legacyAddress.getAddress());
                                                remoteSaveNewAddress(legacyAddress);

                                            }
                                        }).show();

                                    } else {
                                        ToastCustom.makeText(getApplicationContext(), getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                    }
                                } catch (Exception e) {
                                    ToastCustom.makeText(AccountActivity.this, getString(R.string.bip38_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                } finally {
                                    if (progress != null && progress.isShowing()) {
                                        progress.dismiss();
                                        progress = null;
                                    }
                                }

                                Looper.loop();

                            }
                        }).start();

                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void importNonBIP38Address(final String format, final String data) {

        ECKey key = null;

        try {
            key = PrivateKeyFactory.getInstance().getKey(format, data);
        } catch (Exception e) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            e.printStackTrace();
            return;
        }

        if (key != null && key.hasPrivKey() && payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

            //A private key to an existing address has been scanned
            setPrivateKey(key);

        } else if (key != null && key.hasPrivKey() && !payloadManager.getPayload().getLegacyAddressStrings().contains(key.toAddress(MainNetParams.get()).toString())) {

            final List<LegacyAddress> rollbackLegacyAddresses = payloadManager.getPayload().getLegacyAddresses();

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
                        secondPassword.toString(),
                        payloadManager.getPayload().getOptions().getIterations());
                legacyAddress.setEncryptedKey(encrypted2);
            }

            final EditText address_label = new EditText(AccountActivity.this);
            address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

            final ECKey scannedKey = key;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    new AlertDialog.Builder(AccountActivity.this)
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

                                    remoteSaveNewAddress(legacyAddress);

                                }
                            }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            legacyAddress.setLabel(legacyAddress.getAddress());
                            remoteSaveNewAddress(legacyAddress);

                        }
                    }).show();

                    Looper.loop();
                }
            }).start();

        } else {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.no_private_key), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

    }

    private void setPrivateKey(ECKey key){

        Payload payload = payloadManager.getPayload();
        int index = payload.getLegacyAddressStrings().indexOf(key.toAddress(MainNetParams.get()).toString());
        LegacyAddress legacyAddress = payload.getLegacyAddresses().get(index);
        if (!payload.isDoubleEncrypted()) {
            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
        } else {
            String encryptedKey = new String(Base58.encode(key.getPrivKeyBytes()));
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                    payload.getSharedKey(),
                    secondPassword.toString(),
                    payload.getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
        }
        legacyAddress.setWatchOnly(false);
        payloadManager.setPayload(payload);
        PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
            @Override
            public void onSaveSuccess() {
                ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.private_key_successfully_imported), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                updateAccountsList();
            }

            @Override
            public void onSaveFail() {
                ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                //TODO revert on fail
            }
        });
    }

    private void importWatchOnly(String address){

        // check for poorly formed BIP21 URIs
        if (address.startsWith("bitcoin://") && address.length() > 10) {
            address = "bitcoin:" + address.substring(10);
        }

        if (FormatsUtil.getInstance().isBitcoinUri(address)) {
            address = FormatsUtil.getInstance().getBitcoinAddress(address);
        }

        if(!FormatsUtil.getInstance().isValidBitcoinAddress(address)){
            ToastCustom.makeText(AccountActivity.this, getString(R.string.invalid_bitcoin_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }else if (payloadManager.getPayload().getLegacyAddressStrings().contains(address)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.address_already_in_wallet), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } else {

            final String finalAddress = address;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setCancelable(false)
                    .setMessage(getString(R.string.watch_only_import_warning))
                    .setPositiveButton(R.string.dialog_continue, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            final LegacyAddress legacyAddress = new LegacyAddress();
                            legacyAddress.setAddress(finalAddress);
                            legacyAddress.setCreatedDeviceName("android");
                            legacyAddress.setCreated(System.currentTimeMillis());
                            legacyAddress.setCreatedDeviceVersion(BuildConfig.VERSION_NAME);
                            legacyAddress.setWatchOnly(true);

                            final EditText address_label = new EditText(AccountActivity.this);
                            address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Looper.prepare();

                                    new AlertDialog.Builder(AccountActivity.this)
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

                                                    remoteSaveNewAddress(legacyAddress);

                                                }
                                            }).setNegativeButton(R.string.polite_no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            legacyAddress.setLabel(legacyAddress.getAddress());
                                            remoteSaveNewAddress(legacyAddress);

                                        }
                                    }).show();

                                    Looper.loop();
                                }
                            }).start();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            }).show();
        }
    }

    private void addAddressAndUpdateList(final LegacyAddress legacyAddress) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                JSONObject info = AddressInfo.getInstance().getAddressInfo(legacyAddress.getAddress(), "&limit=0");//limit 0 tx, since we only want final balance

                long balance = 0l;
                if (info != null)
                    try {
                        balance = info.getLong("final_balance");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                MultiAddrFactory.getInstance().setLegacyBalance(legacyAddress.getAddress(), balance);
                MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() + balance);

                updateAccountsList();

                Looper.loop();

            }
        }).start();
    }

    private void addAddress() {

        final Handler mHandler = new Handler();

        final ProgressDialog progress = new ProgressDialog(AccountActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.please_wait));
        progress.setCancelable(false);

        new AsyncTask<Void, Void, LegacyAddress>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress.show();
            }

            @Override
            protected LegacyAddress doInBackground(Void... params) {

                new AppUtil(context).applyPRNGFixes();
                return payloadManager.generateLegacyAddress("android", BuildConfig.VERSION_NAME, secondPassword.toString());
            }

            @Override
            protected void onPostExecute(LegacyAddress legacyAddress) {
                super.onPostExecute(legacyAddress);

                if(legacyAddress != null){
                    new Thread(() -> {
                        try {
                            mHandler.post(() -> {
                                final EditText address_label = new EditText(AccountActivity.this);
                                address_label.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH)});

                                new AlertDialog.Builder(AccountActivity.this)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.label_address2)
                                        .setView(address_label)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.save_name, (dialog, whichButton) -> {
                                            String label = address_label.getText().toString();
                                            if (label != null && label.trim().length() > 0) {
                                                ;
                                            } else {
                                                label = legacyAddress.getAddress();
                                            }

                                            legacyAddress.setLabel(label);
                                            remoteSaveNewAddress(legacyAddress);

                                        }).setNegativeButton(R.string.polite_no, (dialog, whichButton) -> {

                                            legacyAddress.setLabel(legacyAddress.getAddress());
                                            remoteSaveNewAddress(legacyAddress);

                                        }).show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }else{
                    ToastCustom.makeText(context, context.getString(R.string.cannot_create_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

                progress.dismiss();
            }
        }.execute();
    }

    private void remoteSaveNewAddress(final LegacyAddress legacy) {

        if (!ConnectivityStatus.hasConnectivity(AccountActivity.this)) {
            ToastCustom.makeText(AccountActivity.this, getString(R.string.check_connectivity_exit), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        final ProgressDialog progress = new ProgressDialog(AccountActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(getString(R.string.saving_address));
        progress.setCancelable(false);
        progress.show();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                if(payloadManager.addLegacyAddress(legacy)){
                    ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.remote_save_ok), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                    ToastCustom.makeText(getApplicationContext(), legacy.getAddress(), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);

                    List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStrings();
                    try {
                        MultiAddrFactory.getInstance().refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Subscribe to new address only if successfully created
                    Intent intent = new Intent(WebSocketService.ACTION_INTENT);
                    intent.putExtra("address", legacy.getAddress());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    addAddressAndUpdateList(legacy);

                }else{
                    ToastCustom.makeText(AccountActivity.this, AccountActivity.this.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    appUtil.restartApp();
                }

                progress.dismiss();

                return null;
            }
        }.execute();
    }

    private boolean hasTransferableFunds(){
        List<LegacyAddress> legacyAddresses = payloadManager.getPayload().getLegacyAddresses();
        for(LegacyAddress legacyAddress : legacyAddresses){

            if(!legacyAddress.isWatchOnly()){
                long balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());
                if(balance - FeeUtil.AVERAGE_FEE.longValue() > SendCoins.bDust.longValue()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void promptToTransferFunds(boolean isPopup){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertPromptTransferFundsBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.alert_prompt_transfer_funds, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        if(!isPopup){
            dialogBinding.confirmDontAskAgain.setVisibility(View.GONE);
        }

        dialogBinding.confirmCancel.setOnClickListener(v -> {
            if(dialogBinding.confirmDontAskAgain.isChecked()) prefsUtil.setValue("WARN_TRANSFER_ALL", false);
            alertDialog.dismiss();
        });

        dialogBinding.confirmSend.setOnClickListener(v -> {
            if(dialogBinding.confirmDontAskAgain.isChecked()) prefsUtil.setValue("WARN_TRANSFER_ALL", false);
            checkTransferFunds();
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    /*
    Check if user should be alerted to transfer his funds from any address to default account for safety.
     */
    private void checkTransferFunds(){

        ArrayList<PendingSpend> pendingSpendList = new ArrayList<>();
        long totalToSend = 0;

        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();

        List<LegacyAddress> legacyAddresses = payloadManager.getPayload().getLegacyAddresses();
        for(LegacyAddress legacyAddress : legacyAddresses){

            if(!legacyAddress.isWatchOnly() && legacyAddress.getTag() != PayloadManager.ARCHIVED_ADDRESS){

                long balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());

                if(balance - FeeUtil.AVERAGE_FEE.longValue() > (SendCoins.bDust.longValue())) {
                    final PendingSpend pendingSpend = new PendingSpend();
                    pendingSpend.fromLegacyAddress = legacyAddress;
                    pendingSpend.bigIntFee = FeeUtil.AVERAGE_FEE;
                    Long totalToSendAfterFee = (balance - FeeUtil.AVERAGE_FEE.longValue());
                    totalToSend += totalToSendAfterFee;
                    pendingSpend.bigIntAmount = BigInteger.valueOf(totalToSendAfterFee);
                    pendingSpend.destination = payloadManager.getReceiveAddress(defaultIndex);//assign new receive address for each transfer
                    pendingSpendList.add(pendingSpend);
                }
            }
        }

        transferSpendableFunds(pendingSpendList, totalToSend);
    }

    public void transferSpendableFunds(final ArrayList<PendingSpend> pendingSpendList, final long totalBalance) {

        //Only funded legacy address' will see this option
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertTransferFundsBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.alert_transfer_funds, null, false);
        dialogBuilder.setView(dialogBinding.getRoot());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        dialogBinding.tvTransferFunds.setVisibility(View.GONE);

        //From
        dialogBinding.confirmFrom.setText(pendingSpendList.size()+" "+getResources().getString(R.string.spendable_addresses));

        //To default
        int defaultIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultIndex);
        dialogBinding.confirmTo.setText(defaultAccount.getLabel()+" ("+getResources().getString(R.string.default_label)+")");

        //Fee
        dialogBinding.confirmFee.setText(monetaryUtil.getDisplayAmount(FeeUtil.AVERAGE_FEE.longValue() * pendingSpendList.size()) + " BTC");

        //Total
        dialogBinding.confirmTotalToSend.setText(monetaryUtil.getDisplayAmount(
                totalBalance + (FeeUtil.AVERAGE_FEE.longValue() * pendingSpendList.size())) + " " + " BTC");

        dialogBinding.confirmCancel.setOnClickListener(v -> alertDialog.dismiss());

        dialogBinding.confirmSend.setOnClickListener(v -> {

            new SecondPasswordHandler(AccountActivity.this).validate(new SecondPasswordHandler.ResultListener() {
                @Override
                public void onNoSecondPassword() {
                    sendPayment(pendingSpendList);
                }

                @Override
                public void onSecondPasswordValidated(String validateSecondPassword) {
                    secondPassword = validateSecondPassword;
                    sendPayment(pendingSpendList);
                }
            });

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

    private void sendPayment(final ArrayList<PendingSpend> pendingSpendList){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                int sendCount = 1;
                for(PendingSpend pendingSpend : pendingSpendList){

                    UnspentOutputsBundle unspents = null;
                    try {
                        String unspentApiResponse = WebUtil.getInstance().getURL(WebUtil.UNSPENT_OUTPUTS_URL + pendingSpend.fromLegacyAddress.getAddress());
                        unspents = SendFactory.getInstance(AccountActivity.this).prepareSend(pendingSpend.fromLegacyAddress.getAddress(), pendingSpend.bigIntAmount.add(FeeUtil.AVERAGE_FEE), BigInteger.ZERO, unspentApiResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    boolean isLastSpend = false;
                    if(pendingSpendList.size() == sendCount)isLastSpend = true;
                    if(unspents != null){

                        //Warn user of unconfirmed funds - but don't block payment
                        if(unspents.getNotice() != null){
                            ToastCustom.makeText(AccountActivity.this, pendingSpend.fromLegacyAddress.getAddress()+" - "+unspents.getNotice(), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        executeSend(pendingSpend, unspents, isLastSpend);

                    } else {
                        ToastCustom.makeText(AccountActivity.this, pendingSpend.fromLegacyAddress.getAddress()+" - "+getString(R.string.no_confirmed_funds), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }

                    sendCount++;
                }

                Looper.loop();
            }
        }).start();
    }

    private void executeSend(final PendingSpend pendingSpend, final UnspentOutputsBundle unspents, final boolean isLastSpend){

        final ProgressDialog progress;
        progress = new ProgressDialog(AccountActivity.this);
        progress.setTitle(R.string.app_name);
        progress.setMessage(AccountActivity.this.getResources().getString(R.string.please_wait));
        progress.setCancelable(false);
        progress.show();

        SendFactory.getInstance(this).execSend(false, -1, unspents.getOutputs(), pendingSpend.destination,
                pendingSpend.bigIntAmount, pendingSpend.fromLegacyAddress,
                pendingSpend.bigIntFee, null, false, secondPassword, new OpCallback() {

            public void onSuccess() {
            }

            @Override
            public void onSuccess(final String hash) {

                MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingSpend.bigIntAmount.longValue() + pendingSpend.bigIntFee.longValue()));

                if(isLastSpend){
                    ToastCustom.makeText(context, getResources().getString(R.string.transaction_submitted), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
                    PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                        @Override
                        public void onSaveSuccess() {
                        }

                        @Override
                        public void onSaveFail() {
                            ToastCustom.makeText(context, context.getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }
                    });

                    runOnUiThread(() -> {
                        updateAccountsList();
                        transferFundsMenuItem.setVisible(false);
                    });
                }

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

            public void onFail(String error) {

                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            }

            @Override
            public void onFailPermanently(String error) {

                ToastCustom.makeText(AccountActivity.this, error, ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
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
