package info.blockchain.wallet.view;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

public class AboutDialog extends AppCompatDialogFragment {

    private static final String strMerchantPackage = "info.blockchain.merchant";

    public AboutDialog() {
        // No-op
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_about, null);

        TextView about = (TextView) view.findViewById(R.id.about);
        TextView rateUs = (TextView) view.findViewById(R.id.rate_us);
        TextView freeWallet = (TextView) view.findViewById(R.id.free_wallet);

        about.setText(getString(R.string.about, BuildConfig.VERSION_NAME, "2015"));

        rateUs.setOnClickListener(v -> {
            String appPackageName = getActivity().getPackageName();
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(marketIntent);
        });

        if (hasWallet()) {
            freeWallet.setVisibility(View.GONE);
        } else {
            freeWallet.setOnClickListener(v -> {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + strMerchantPackage));
                startActivity(marketIntent);
            });
        }

        return view;
    }

    private boolean hasWallet() {
        PackageManager pm = getActivity().getPackageManager();
        try {
            pm.getPackageInfo(strMerchantPackage, 0);
            return true;
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }
}
