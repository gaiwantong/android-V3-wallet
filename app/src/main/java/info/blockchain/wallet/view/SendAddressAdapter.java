package info.blockchain.wallet.view;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import info.blockchain.wallet.model.ItemAccount;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ItemAddressBinding;
import piuk.blockchain.android.databinding.SpinnerItemBinding;

/**
 * Created by riaanvos on 01/08/16.
 */
public class SendAddressAdapter extends ArrayAdapter<ItemAccount> {

    public SendAddressAdapter(Context context, int textViewResourceId, List<ItemAccount> accountList) {
        super(context, textViewResourceId, accountList);
    }

    public void updateData(List<ItemAccount> accountList){
        clear();
        addAll(accountList);
        notifyDataSetChanged();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, true);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, false);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent, boolean isDropdownView) {

        if(isDropdownView) {
            ItemAddressBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.getContext()),
                    R.layout.item_address,
                    parent,
                    false);

            ItemAccount item = getItem(position);
            if(item.tag == null || item.tag.isEmpty()){
                binding.tvTag.setVisibility(View.GONE);
            }else{
                binding.tvTag.setText(item.tag);
            }
            binding.tvLabel.setText(item.label);
            binding.tvBalance.setText(item.balance);
            return binding.getRoot();

        }else{
            SpinnerItemBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.getContext()),
                    R.layout.spinner_item,
                    parent,
                    false);
            //No op
            return binding.getRoot();
        }
    }
}
