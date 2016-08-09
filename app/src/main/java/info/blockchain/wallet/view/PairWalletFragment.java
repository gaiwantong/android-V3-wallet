package info.blockchain.wallet.view;

import com.google.zxing.client.android.CaptureActivity;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PermissionUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentPairWalletBinding;

public class PairWalletFragment extends Fragment implements FragmentCompat.OnRequestPermissionsResultCallback {

    private FragmentPairWalletBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pair_wallet, container, false);

        getActivity().setTitle(getResources().getString(R.string.pair_your_wallet));

        binding.commandScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                PermissionUtil.requestCameraPermissionFromFragment(binding.mainLayout, getActivity(), this);
            } else {
                startScanActivity();
            }
        });

        binding.commandManual.setOnClickListener(v -> {
            Fragment fragment = new ManualPairingFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        return binding.getRoot();
    }

    private void startScanActivity() {
        if (!new AppUtil(getActivity()).isCameraOpen()) {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            intent.putExtra("SCAN_FORMATS", "QR_CODE");
            getActivity().startActivityForResult(intent, PairOrCreateWalletActivity.PAIRING_QR);
        } else {
            ToastCustom.makeText(getActivity(), getString(R.string.camera_unavailable), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
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
}
