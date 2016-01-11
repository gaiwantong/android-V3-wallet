package info.blockchain.wallet;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import piuk.blockchain.android.R;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private static final int TYPE_ACCOUNT_HEADER = -1;
    private static final int TYPE_IMPORTED_HEADER = -2;
    private ArrayList<AccountItem> items;

    public AccountAdapter(ArrayList<AccountItem> myAccountItems) {
        this.items = myAccountItems;
    }

    @Override
    public AccountAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row, parent, false);

        if (viewType == TYPE_ACCOUNT_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_header, parent, false);
            TextView header = (TextView) v.findViewById(R.id.my_account_row_header);
            header.setText(AccountActivity.ACCOUNT_HEADER);

        } else if (viewType == TYPE_IMPORTED_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_accounts_row_header, parent, false);
            TextView header = (TextView) v.findViewById(R.id.my_account_row_header);
            header.setText(AccountActivity.IMPORTED_HEADER);
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        if (holder.getItemViewType() == TYPE_ACCOUNT_HEADER || holder.getItemViewType() == TYPE_IMPORTED_HEADER)
            return;

        TextView title = (TextView) holder.itemView.findViewById(R.id.my_account_row_label);
        ImageView icon = (ImageView) holder.itemView.findViewById(R.id.my_account_row_icon);
        TextView amount = (TextView) holder.itemView.findViewById(R.id.my_account_row_amount);

        title.setText(items.get(position).getTitle());
        amount.setText(items.get(position).getAmount());

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

        String title = items.get(position).getTitle();
        if (title.equals(AccountActivity.ACCOUNT_HEADER))
            return TYPE_ACCOUNT_HEADER;

        else if (title.equals(AccountActivity.IMPORTED_HEADER))
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