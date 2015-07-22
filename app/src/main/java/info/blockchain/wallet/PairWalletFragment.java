package info.blockchain.wallet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ToastCustom;
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
                if(!AppUtil.getInstance(getActivity()).isCameraOpen())    {
                    Intent intent = new Intent(getActivity(), ScanActivity.class);
                    getActivity().startActivityForResult(intent, PairOrCreateWalletActivity.PAIRING_QR);
                }
                else    {
                    ToastCustom.makeText(getActivity(), getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
            }
        });

        commandManual = (TextView)rootView.findViewById(R.id.command_manual);
        commandManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PairOrCreateWalletActivity.fragment = new ManualPairingFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.content_frame, PairOrCreateWalletActivity.fragment).addToBackStack(null).commit();
            }
        });

        return rootView;
    }


}
