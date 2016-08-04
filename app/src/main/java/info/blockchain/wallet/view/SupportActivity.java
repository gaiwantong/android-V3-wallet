package info.blockchain.wallet.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.view.helpers.ToastCustom;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivitySupportBinding;

public class SupportActivity extends AppCompatActivity {

    private ActivitySupportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_support);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        //TODO - don't use NoActionBar in styles.xml (affects BalanceFragment, so don't just edit styles.xml)
        binding.toolbarContainer.toolbarGeneral.setTitle(getResources().getString(R.string.contact_support));
        binding.toolbarContainer.toolbarGeneral.setTitleTextColor(getResources().getColor(R.color.white));
        binding.toolbarContainer.toolbarGeneral.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        binding.toolbarContainer.toolbarGeneral.setNavigationOnClickListener(v -> onBackPressed());

        final String guid = PayloadManager.getInstance().getPayload().getGuid();
        binding.walletId.setText(guid);

        binding.walletIdContainer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(SupportActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.guid_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) SupportActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("guid", guid);
                                clipboard.setPrimaryClip(clip);
                                ToastCustom.makeText(SupportActivity.this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();
            }
        });

        binding.emailAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(SupportActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(getString(R.string.support_guide))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                emailIntent(guid);
                            }

                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

            }
        });
    }

    private void emailIntent(String guid) {

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "support@blockchain.zendesk.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.email_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT,
                "Dear Blockchain Support," +
                        "\n\n" +
                        "" +
                        "\n\n" +
                        "--\n" +
                        "App: " + getString(R.string.app_name) + ", Version " + BuildConfig.VERSION_NAME + " \n" +
                        "System: " + Build.MANUFACTURER + "\n" +
                        "Model: " + Build.MODEL + "\n" +
                        "Version: " + Build.VERSION.RELEASE);
        startActivity(Intent.createChooser(emailIntent, SupportActivity.this.getResources().getText(R.string.email_chooser)));

    }

    @Override
    protected void onResume() {
        super.onResume();
        AccessState.getInstance().stopLogoutTimer();
    }

    @Override
    protected void onPause() {
        AccessState.getInstance().startLogoutTimer();
        super.onPause();
    }
}
