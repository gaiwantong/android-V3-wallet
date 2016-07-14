package info.blockchain.wallet.viewModel;

import android.content.Context;

public class SendViewModel {

    private DataListener dataListener;
    private Context context;

    public SendViewModel(Context context, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;
    }

    public interface DataListener {
    }
}
