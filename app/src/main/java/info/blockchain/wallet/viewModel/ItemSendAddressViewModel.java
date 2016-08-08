package info.blockchain.wallet.viewModel;

import android.content.Context;
import android.databinding.BaseObservable;

import info.blockchain.wallet.model.ItemAccount;

/**
 * Created by riaanvos on 01/08/16.
 */
public class ItemSendAddressViewModel extends BaseObservable implements ViewModel{

    private ItemAccount addressItem;
    private Context context;

    public ItemSendAddressViewModel(Context context, ItemAccount address) {
        this.addressItem = address;
        this.context = context;
    }

    public String getLabel(){
        return addressItem.label;
    }

    public String getBalance(){
        return addressItem.balance;
    }

    public void setAddress(ItemAccount address) {
        this.addressItem = address;
        notifyChange();
    }

    @Override
    public void destroy() {
        context = null;
        addressItem = null;
    }

}
