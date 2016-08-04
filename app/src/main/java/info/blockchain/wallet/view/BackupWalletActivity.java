package info.blockchain.wallet.view;

import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;

import info.blockchain.wallet.access.AccessState;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityBackupWalletBinding;

public class BackupWalletActivity extends AppCompatActivity {

    public static final String BACKUP_DATE_KEY = "BACKUP_DATE_KEY";

    private ActivityBackupWalletBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_backup_wallet);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.backup_wallet));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new BackupWalletFragment1())
                .addToBackStack("backup_start")
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AccessState.getInstance().stopLogoutTimer();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() <= 1)
            finish();
        else
            getFragmentManager().popBackStack();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        AccessState.getInstance().startLogoutTimer();
        super.onPause();
    }
}