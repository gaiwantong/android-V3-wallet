package info.blockchain.wallet;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import net.sourceforge.zbar.Symbol;

/**
 * Created by riaanvos on 23/03/15.
 */
public class ScanPairingFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.include_setup0, container, false);//random view for now until zbar implemented

        rootView.setFilterTouchesWhenObscured(true);

        getActivity().setTitle(getResources().getString(R.string.scan_pairing_code));

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        launchQRScan();
    }

    private void launchQRScan() {

        //For now start scanner in new activity... need to wrap this in container still
        Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        getActivity().startActivityForResult(intent, Setup00Activity.PAIRING_QR);
    }
}
