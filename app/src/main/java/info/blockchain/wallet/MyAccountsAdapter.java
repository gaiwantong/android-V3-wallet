package info.blockchain.wallet;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class MyAccountsAdapter extends RecyclerView.Adapter<MyAccountsAdapter.ViewHolder> {

	private ArrayList<MyAccountItem> myAccountItems;

	public class ViewHolder extends RecyclerView.ViewHolder  {

		public ViewHolder(View view) {
			super(view);
		}
	}

	public MyAccountsAdapter(ArrayList<MyAccountItem> myAccountItems) {
		this.myAccountItems = myAccountItems;
	}

	@Override
	public MyAccountsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_my_accounts_row, parent, false);
		return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		TextView title = (TextView) holder.itemView.findViewById(R.id.my_account_row_label);
		//ImageView icon = (ImageView) holder.itemView.findViewById(R.id.my_account_row_icon);
		TextView amount = (TextView) holder.itemView.findViewById(R.id.my_account_row_amount);

		title.setText(myAccountItems.get(position).getTitle());
		//icon.setImageDrawable(myAccountItems.get(position).getIcon());
		amount.setText(myAccountItems.get(position).getAmount());
	}

	@Override
	public int getItemCount() {
		return myAccountItems.size();
	}
}