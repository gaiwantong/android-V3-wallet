package info.blockchain.wallet.viewModel;

import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import piuk.blockchain.android.di.Injector;
import rx.subscriptions.CompositeSubscription;

public class ReceiveViewModel implements ViewModel {

    private DataListener mDataListener;
    @Inject protected PayloadManager mPayloadManager;
    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    public interface DataListener {



    }

    public ReceiveViewModel(DataListener listener) {
        Injector.getInstance().getAppComponent().inject(this);
        mDataListener = listener;
    }

    public void onViewReady(){
        // No-op
    }

    @Override
    public void destroy() {
        // No-op
    }
}
