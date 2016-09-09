package info.blockchain.wallet.view.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import info.blockchain.wallet.model.RecipientModel;

import java.util.List;

import piuk.blockchain.android.R;

public class RecipientAdapter extends BaseAdapter implements SpinnerAdapter {

    private List<RecipientModel> mRecipients;

    public RecipientAdapter(List<RecipientModel> recipients) {
        mRecipients = recipients;
    }

    @Override
    public int getCount() {
        return mRecipients != null ? mRecipients.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mRecipients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mRecipients.hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    // Specifying parent not allowed in AdapterView
    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewholder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.spinner_item_transaction_detail, null);
            viewholder = new ViewHolder();
            viewholder.address = (TextView) convertView.findViewById(R.id.address);
            viewholder.amount = (TextView) convertView.findViewById(R.id.amount);

            convertView.setTag(viewholder);
        } else {
            viewholder = (ViewHolder) convertView.getTag();
        }

        RecipientModel recipient = (RecipientModel) getItem(position);
        viewholder.address.setText(recipient.getAddress());
        viewholder.amount.setText(
                (recipient.getValue()
                        + " "
                        + recipient.getDisplayUnits()));

        return convertView;
    }

    private static class ViewHolder {

        TextView address;
        TextView amount;

        ViewHolder() {
            // Empty constructor
        }
    }

}
