package info.blockchain.wallet;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import piuk.blockchain.android.R;

public class AccountManagerActivity extends AppCompatActivity {

    private TextView tvSave = null;
    private TextView tvCancel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_accounts);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        initToolbar();

        tvSave = (TextView) findViewById(R.id.confirm_save);
        tvSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
                finish();
            }
        });

        tvCancel = (TextView) findViewById(R.id.confirm_cancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //TODO catch intent
        getIntent().getIntExtra("address_index", -1);
        getIntent().getIntExtra("account_index",-1);
    }

    private void initToolbar() {

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.rename_address));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        setSupportActionBar(toolbar);
    }

    private void saveChanges() {

//        LegacyAddress legacyAddress = legacy.get(position - HEADERS.length - hdAccountsIdx);
//
//            if (legacyAddress.getTag() == PayloadFactory.ARCHIVED_ADDRESS) {
//                ToastCustom.makeText(MyAccountsActivity.this, getString(R.string.archived_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
//                return;
//            } else if (legacyAddress.isWatchOnly()) {
//                ToastCustom.makeText(MyAccountsActivity.this, getString(R.string.watchonly_address), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
//                return;
//            } else {
//                currentSelectedAddress = legacyAddress.getAddress();
//            }
//        legacyAddress.setLabel("");


        //OR


//        PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIndexResover.get(position)).setLabel("Hello 1");
//        PayloadBridge.getInstance(context).remoteSaveThread();

    }
}
