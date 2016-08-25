package info.blockchain.wallet.view;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;

public class SettingsActivity extends BaseAuthActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupToolbar();
    }

    private void setupToolbar(){
        //TODO - don't use NoActionBar in styles.xml (affects BalanceFragment, so don't just edit styles.xml)
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.action_settings));
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
