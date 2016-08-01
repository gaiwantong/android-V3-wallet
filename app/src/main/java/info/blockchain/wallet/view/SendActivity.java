package info.blockchain.wallet.view;

import com.google.zxing.client.android.CaptureActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.callbacks.CustomKeypadCallback;
import info.blockchain.wallet.model.ItemSendAddress;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.view.helpers.CustomKeypad;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.SendViewModel;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivitySendBinding;

public class SendActivity extends AppCompatActivity implements SendViewModel.DataListener, CustomKeypadCallback {

    private final String TAG = getClass().getSimpleName();
    public static final int SCAN_URI = 2007;

    private ActivitySendBinding binding;
    private SendViewModel viewModel;

    private static CustomKeypad customKeypad;
    private String defaultSeparator;//Decimal separator based on locale

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (BalanceFragment.ACTION_INTENT.equals(intent.getAction())) {
                ((SendAddressAdapter) binding.accounts.spinner.getAdapter()).updateData(viewModel.getAddressList());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_send);
        viewModel = new SendViewModel(this, this);
        binding.setViewModel(viewModel);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setupToolbar();

        defaultSeparator = getDefaultDecimalSeparator();
        setCustomKeypad();

        setupViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessState.getInstance(this).stopLogoutTimer();

        IntentFilter filter = new IntentFilter(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        AccessState.getInstance(this).startLogoutTimer();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    private void setupToolbar() {
        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.send_bitcoin));
        setSupportActionBar(binding.toolbarContainer.toolbarGeneral);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_qr:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromActivity(binding.getRoot(), this);
                } else {
                    startScanActivity();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanActivity() {
        if (!new AppUtil(this).isCameraOpen()) {
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivityForResult(intent, SCAN_URI);
        } else {
            ToastCustom.makeText(this, getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
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

    @Override
    public void onKeypadClose() {
        customKeypad.setNumpadVisibility(View.GONE);
    }

    private void setCustomKeypad() {

        customKeypad = new CustomKeypad(this, (binding.keypad.numericPad));
        customKeypad.setDecimalSeparator(defaultSeparator);

        //Enable custom keypad and disables default keyboard from popping up
        customKeypad.enableOnView(binding.amountRow.amountBtc);
        customKeypad.enableOnView(binding.amountRow.amountFiat);
        customKeypad.enableOnView(binding.customFee);

        binding.amountRow.amountBtc.setText("");
        binding.amountRow.amountBtc.requestFocus();
    }

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    private void setupViews() {

        setupDestinationView();
        setupSendFromView();
        setupReceiveToView();
    }

    private void setupDestinationView(){
        binding.destination.setHorizontallyScrolling(false);
        binding.destination.setLines(3);
        binding.destination.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && customKeypad != null)
                customKeypad.setNumpadVisibility(View.GONE);
        });
    }

    private void setupSendFromView() {

        binding.accounts.spinner.setAdapter(new SendAddressAdapter(this, R.layout.spinner_item, viewModel.getAddressList()));

        //Set drop down width equal to clickable view
        binding.accounts.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    binding.accounts.spinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.accounts.spinner.setDropDownWidth(binding.accounts.spinner.getWidth());
                }
            }
        });

        binding.accounts.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ItemSendAddress selectedItem = (ItemSendAddress) binding.accounts.spinner.getSelectedItem();

                Log.v(TAG, "label: " + selectedItem.label);
                Log.v(TAG, "accountObject: " + selectedItem.accountObject);
                Log.v(TAG, "tag: " + selectedItem.tag);
                Log.v(TAG, "balance: " + selectedItem.balance);

//                        unspentsCoinsBundle = null;
//                        unspentApiResponse = null;
//                        binding.max.setVisibility(View.GONE);
//                        binding.progressBarMaxAvailable.setVisibility(View.VISIBLE);
//                        if(btSend != null)btSend.setEnabled(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onSingleFromAddress() {
        binding.fromRow.setVisibility(View.GONE);
    }

    @Override
    public void onSingleToAddress() {
        binding.spDestination.setVisibility(View.GONE);
    }

    private void setupReceiveToView() {
        binding.spDestination.setAdapter(new SendAddressAdapter(this, R.layout.spinner_item, viewModel.getAddressList()));

        //Set drop down width equal to clickable view
        binding.spDestination.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.spDestination.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    binding.spDestination.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if(binding.accounts.spinner.getWidth() > 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        binding.spDestination.setDropDownWidth(binding.accounts.spinner.getWidth());
                    }
            }
        });


        binding.spDestination.setOnItemSelectedEvenIfUnchangedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        ItemSendAddress selectedItem = (ItemSendAddress) binding.spDestination.getSelectedItem();

                        Log.v(TAG, "label: " + selectedItem.label);
                        Log.v(TAG, "accountObject: " + selectedItem.accountObject);
                        Log.v(TAG, "tag: " + selectedItem.tag);
                        Log.v(TAG, "balance: " + selectedItem.balance);

//                        final Object object = receiveToBiMap.inverse().get(binding.spDestination.getSelectedItemPosition());
//
//                        if (object instanceof LegacyAddress) {
//
//                            //V2
//                            if (((LegacyAddress) object).isWatchOnly() && prefsUtil.getValue("WARN_WATCH_ONLY_SPEND", true)) {
//
//                                promptWatchOnlySpendWarning(object);
//
//                            } else {
//                                binding.destination.setText(((LegacyAddress) object).getAddress());
//                            }
//                        } else if(object instanceof Account){
//                            //V3
//                            //TODO - V3 no watch only yet
//                            binding.destination.setText(getV3ReceiveAddress((Account) object));
//
//                        } else if (object instanceof AddressBookEntry){
//                            //Address book
//                            binding.destination.setText(((AddressBookEntry) object).getAddress());
//                        }
//
//                        spDestinationSelected = true;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }
}