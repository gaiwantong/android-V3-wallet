package info.blockchain.wallet.account_manager;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.view.AccountActivity;

import java.util.ArrayList;

import piuk.blockchain.android.R;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private static final int TYPE_IMPORTED_HEADER = -1;
    private ArrayList<AccountItem> items;
    private Context context;

    public AccountAdapter(ArrayList<AccountItem> myAccountItems, Context context) {
        this.items = myAccountItems;
        this.context = context;
    }

    @Override
    public AccountAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row, parent, false);

        if (viewType == TYPE_IMPORTED_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_header, parent, false);
            TextView header = (TextView) v.findViewById(R.id.my_account_row_header);
            header.setText(AccountActivity.IMPORTED_HEADER);
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        if (holder.getItemViewType() == TYPE_IMPORTED_HEADER)
            return;

        TextView title = (TextView) holder.itemView.findViewById(R.id.my_account_row_label);
        TextView address = (TextView) holder.itemView.findViewById(R.id.my_account_row_address);
        ImageView icon = (ImageView) holder.itemView.findViewById(R.id.my_account_row_icon);
        TextView amount = (TextView) holder.itemView.findViewById(R.id.my_account_row_amount);
        TextView tag = (TextView) holder.itemView.findViewById(R.id.my_account_row_tag);

        title.setText(items.get(position).getLabel());

        if(items.get(position).getAddress() != null) {
            address.setText(items.get(position).getAddress());
        }else {
            address.setVisibility(View.GONE);
        }

        if(items.get(position).isArchived()){
            amount.setText(R.string.archived_label);
            amount.setTextColor(ContextCompat.getColor(context, R.color.blockchain_transfer_blue));
        }else{
            amount.setText(items.get(position).getAmount());
            amount.setTextColor(ContextCompat.getColor(context, R.color.blockchain_receive_green));
        }

        if(items.get(position).isWatchOnly()){
            tag.setText(context.getString(R.string.watch_only));
            tag.setTextColor(ContextCompat.getColor(context, R.color.blockchain_send_red));
        }

        if(items.get(position).isDefault()){
            tag.setText(context.getString(R.string.default_label));
            tag.setTextColor(ContextCompat.getColor(context, R.color.blockchain_grey));
        }

        if(!items.get(position).isWatchOnly() && !items.get(position).isDefault()){
            tag.setVisibility(View.INVISIBLE);
        }

        Drawable drawable = items.get(position).getIcon();
        if (drawable != null)
            icon.setImageDrawable(drawable);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {

        String title = items.get(position).getLabel();
        if (title.equals(AccountActivity.IMPORTED_HEADER))
            return TYPE_IMPORTED_HEADER;

        else
            return position;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View view) {
            super(view);
        }
    }
}