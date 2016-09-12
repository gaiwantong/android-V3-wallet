package info.blockchain.wallet.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import info.blockchain.wallet.model.RecipientModel;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.view.adapters.RecipientAdapter;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.TransactionDetailViewModel;

import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.BaseAuthActivity;
import piuk.blockchain.android.R;
import piuk.blockchain.android.annotations.Thunk;
import piuk.blockchain.android.databinding.ActivityTransactionDetailsBinding;

public class TransactionDetailActivity extends BaseAuthActivity implements TransactionDetailViewModel.DataListener {

    // TODO: 12/09/2016 Use new API for getting Fiat value at time of transaction
    public static final String KEY_TRANSACTION_URL = "key_transaction_url";
    @Thunk ActivityTransactionDetailsBinding mBinding;
    private TransactionDetailViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_details);
        mViewModel = new TransactionDetailViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.transaction_detail_title));
        setSupportActionBar(toolbar);

        mBinding.editIcon.setOnClickListener(v -> mBinding.descriptionField.requestFocus());
        mBinding.descriptionField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mViewModel.updateTransactionNote(mBinding.descriptionField.getText().toString());
                hideKeyboard();
                mBinding.descriptionField.clearFocus();
            }
            return true;
        });

        mViewModel.onViewReady();
    }

    @Override
    public void setTransactionType(String type) {
        switch (type) {
            case MultiAddrFactory.MOVED:
                mBinding.transactionType.setText(getResources().getString(R.string.MOVED));
                break;
            case MultiAddrFactory.RECEIVED:
                mBinding.transactionType.setText(getResources().getString(R.string.RECEIVED));
                mBinding.transactionFee.setVisibility(View.GONE);
                break;
            case MultiAddrFactory.SENT:
                mBinding.transactionType.setText(getResources().getString(R.string.SENT));
                break;
        }
    }

    @Override
    public void onDataLoaded() {
        mBinding.mainLayout.setVisibility(View.VISIBLE);
        mBinding.loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void setTransactionColour(@ColorRes int colour) {
        mBinding.transactionAmount.setTextColor(ResourcesCompat.getColor(getResources(), colour, getTheme()));
        mBinding.transactionType.setTextColor(ResourcesCompat.getColor(getResources(), colour, getTheme()));
    }

    @Override
    public void setTransactionValueBtc(String value) {
        mBinding.transactionAmount.setText(value);
    }

    @Override
    public void setTransactionValueFiat(String fiat) {
        mBinding.transactionValue.setText(fiat);
    }

    @Override
    public void setToAddresses(List<RecipientModel> addresses) {
        if (addresses.size() == 1) {
            mBinding.toAddress.setText(addresses.get(0).getAddress());
        } else {
            mBinding.spinner.setVisibility(View.VISIBLE);
            RecipientAdapter adapter = new RecipientAdapter(addresses);
            mBinding.toAddress.setText(String.format(Locale.getDefault(), "%1s Recipients", addresses.size()));
            mBinding.toAddress.setOnClickListener(v -> mBinding.spinner.performClick());
            mBinding.spinner.setAdapter(adapter);
            mBinding.spinner.setOnItemSelectedListener(null);
        }
    }

    @Override
    public void setDate(String date) {
        mBinding.date.setText(date);
    }

    @Override
    public void setDescription(String description) {
        mBinding.descriptionField.setText(description);
    }

    @Override
    public void setFromAddress(String address) {
        mBinding.fromAddress.setText(address);
    }

    @Override
    public void setStatus(String status, String hash) {
        mBinding.status.setText(status);
        mBinding.statusLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionDetailWebViewActivity.class);
            intent.putExtra(KEY_TRANSACTION_URL, "https://blockchain.info/tx/" + hash);
            startActivity(intent);
        });
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void setFee(String fee) {
        mBinding.transactionFee.setText(String.format(Locale.getDefault(), getString(R.string.transaction_detail_fee), fee));
    }

    @Override
    public void pageFinish() {
        finish();
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

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
