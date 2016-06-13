package info.blockchain.wallet.viewModel;

import android.content.Context;

public class BalanceViewModel implements ViewModel{

    private Context context;
    private DataListener dataListener;

    public interface DataListener {
    }

    public BalanceViewModel(Context context, DataListener dataListener) {
        this.context = context;
        this.dataListener = dataListener;
    }

    @Override
    public void destroy() {
        context = null;
        dataListener = null;
    }
}
