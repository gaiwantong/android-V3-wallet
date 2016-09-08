package info.blockchain.wallet.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import info.blockchain.wallet.model.RecipientModel;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.viewModel.TransactionDetailViewModel;

import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityTransactionDetailsBinding;

public class TransactionDetailActivity extends BaseAuthActivity implements TransactionDetailViewModel.DataListener {

    private TransactionDetailViewModel mViewModel;
    private ActivityTransactionDetailsBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_details);
        mViewModel = new TransactionDetailViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.transaction_detail_title));
        setSupportActionBar(toolbar);

        mViewModel.onViewReady();
    }

    @Override
    public void pageFinish() {
        finish();
    }

    @Override
    public void setTransactionType(String type) {
        if (type.equals(MultiAddrFactory.MOVED))
            mBinding.transactionType.setText(getResources().getString(R.string.MOVED));
        if (type.equals(MultiAddrFactory.RECEIVED))
            mBinding.transactionType.setText(getResources().getString(R.string.RECEIVED));
            mBinding.transactionFee.setVisibility(View.GONE);
        if (type.equals(MultiAddrFactory.SENT))
            mBinding.transactionType.setText(getResources().getString(R.string.SENT));
    }

    @Override
    public void setTransactionColour(@ColorRes int colour) {
        mBinding.transactionAmount.setTextColor(ResourcesCompat.getColor(getResources(), colour, getTheme()));
        mBinding.transactionType.setTextColor(ResourcesCompat.getColor(getResources(), colour, getTheme()));
    }

    @Override
    public void setTransactionValue(String value) {
        mBinding.transactionAmount.setText(value);
    }

    @Override
    public void setToAddress(List<RecipientModel> addresses) {
        if (addresses.size() > 1) {
            mBinding.toDropdown.setVisibility(View.VISIBLE);
            // TODO: 08/09/2016
        } else {
            mBinding.toAddress.setText(addresses.get(0).getAddress());
            mBinding.toDropdown.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setFromAddress(String address) {
        mBinding.fromAddress.setText(address);
    }

    @Override
    public void setStatus(String status) {
        mBinding.status.setText(status);
        mBinding.status.setOnClickListener(v -> {
            // TODO: 08/09/2016 Chrome tab
        });
    }

    @Override
    public void setFee(String fee) {
        mBinding.transactionFee.setText(String.format(Locale.getDefault(), getString(R.string.transaction_detail_fee), fee));
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.destroy();
    }
}
