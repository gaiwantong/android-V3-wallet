package info.blockchain.wallet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import piuk.blockchain.android.R;

public class PairWalletFragment extends Fragment {

    TextView commandScan;
    TextView commandManual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pair_wallet, container, false);

        getActivity().setTitle(getResources().getString(R.string.pair_your_wallet));

        commandScan = (TextView)rootView.findViewById(R.id.command_scan);
        commandScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new ScanPairingFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
            }
        });

        commandManual = (TextView)rootView.findViewById(R.id.command_manual);
        commandManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new ManualPairingFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
            }
        });

        return rootView;
    }


}
