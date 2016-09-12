package info.blockchain.wallet.view;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.DateUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;
import info.blockchain.wallet.viewModel.BalanceViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBalanceBinding;

public class BalanceFragment extends Fragment implements BalanceViewModel.DataListener{

    public static final String ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH";
    public static final String KEY_TRANSACTION_LIST_POSITION = "key_transaction_list_position";
    private final static int SHOW_BTC = 1;
    private final static int SHOW_FIAT = 2;
    private final static int SHOW_HIDE = 3;
    private static int BALANCE_DISPLAY_STATE = SHOW_BTC;
    public int balanceBarHeight;
    ArrayAdapter<String> accountsAdapter = null;
    LinearLayoutManager layoutManager;
    HashMap<View, Boolean> rowViewState = null;
    Communicator comm;
    //
    // main balance display
    //
    private double btc_fx = 319.13;//TODO remove hard coded when refactoring
    private boolean isBTC = true;
    //
    // accounts list
    //
    private AppCompatSpinner accountSpinner = null;//TODO - move to drawer header
    //
    // tx list
    //
    private TxAdapter transactionAdapter = null;
    private Activity context = null;
    private PrefsUtil prefsUtil;
    private DateUtil dateUtil;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {
                binding.swipeContainer.setRefreshing(true);
                viewModel.updateAccountList();
                viewModel.updateBalanceAndTransactionList(intent, accountSpinner.getSelectedItemPosition(), isBTC);
                binding.swipeContainer.setRefreshing(false);
            }
        }
    };

    FragmentBalanceBinding binding;
    BalanceViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        context = getActivity();
        prefsUtil = new PrefsUtil(context);
        dateUtil = new DateUtil(context);

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_balance, container, false);
        viewModel = new BalanceViewModel(context, this);
        binding.setViewModel(viewModel);

        setHasOptionsMenu(true);

        balanceBarHeight = (int) getResources().getDimension(R.dimen.action_bar_height) + 35;

        BALANCE_DISPLAY_STATE = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        if (BALANCE_DISPLAY_STATE == SHOW_FIAT) {
            isBTC = false;
        }

        setupViews();

        return binding.getRoot();
    }

    private void setAccountSpinner(){

        Toolbar toolbar = (Toolbar) context.findViewById(R.id.toolbar);
        ((AppCompatActivity) context).setSupportActionBar(toolbar);

        if(viewModel.getActiveAccountAndAddressList().size() > 1){
            ((AppCompatActivity) context).getSupportActionBar().setDisplayShowTitleEnabled(false);
            accountSpinner.setVisibility(View.VISIBLE);
        }else if(viewModel.getActiveAccountAndAddressList().size() > 0){
            ((AppCompatActivity) context).getSupportActionBar().setDisplayShowTitleEnabled(true);
            accountSpinner.setSelection(0);
            ((AppCompatActivity) context).getSupportActionBar().setTitle(viewModel.getActiveAccountAndAddressList().get(0));
            accountSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        comm.resetNavigationDrawer();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        viewModel.startWebSocketService();
        viewModel.updateAccountList();
        viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_qr).setVisible(true);
        menu.findItem(R.id.action_send).setVisible(false);
    }

    /**
     * Deprecated, but necessary to prevent casting issues on <API21
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        comm = (Communicator) activity;
    }

    private void initFab(){

        //First icon when fab expands
        com.getbase.floatingactionbutton.FloatingActionButton actionA = new com.getbase.floatingactionbutton.FloatingActionButton(context);
        actionA.setColorNormal(getResources().getColor(R.color.blockchain_send_red));
        actionA.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
        Drawable sendIcon = context.getResources().getDrawable(R.drawable.icon_send);
        sendIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        actionA.setIconDrawable(sendIcon);
        actionA.setColorPressed(getResources().getColor(R.color.blockchain_red_50));
        actionA.setTitle(getResources().getString(R.string.send_bitcoin));
        actionA.setOnClickListener(v -> sendClicked());

        //Second icon when fab expands
        com.getbase.floatingactionbutton.FloatingActionButton actionB = new com.getbase.floatingactionbutton.FloatingActionButton(context);
        actionB.setColorNormal(getResources().getColor(R.color.blockchain_receive_green));
        actionB.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
        Drawable receiveIcon = context.getResources().getDrawable(R.drawable.icon_receive);
        receiveIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        actionB.setIconDrawable(receiveIcon);
        actionB.setColorPressed(getResources().getColor(R.color.blockchain_green_50));
        actionB.setTitle(getResources().getString(R.string.receive_bitcoin));
        actionB.setOnClickListener(v -> receiveClicked());

        //Add buttons to expanding fab
        binding.fab.addButton(actionA);
        binding.fab.addButton(actionB);

        binding.fab.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                binding.balanceMainContentShadow.setVisibility(View.VISIBLE);
                comm.setNavigationDrawerToggleEnabled(false);
            }

            @Override
            public void onMenuCollapsed() {
                binding.fab.collapse();
                binding.balanceMainContentShadow.setVisibility(View.GONE);
                comm.setNavigationDrawerToggleEnabled(true);
            }
        });
    }

    /**
     * Only available for Dogfood/Debug build
     */
    private void initDebugFab() {

        if(BuildConfig.DOGFOOD || BuildConfig.DEBUG) {
            binding.fabDebug.setVisibility(View.VISIBLE);

            com.getbase.floatingactionbutton.FloatingActionButton actionA = new com.getbase.floatingactionbutton.FloatingActionButton(context);
            actionA.setColorNormal(getResources().getColor(R.color.blockchain_receive_green));
            actionA.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            Drawable debugIcon = context.getResources().getDrawable(R.drawable.icon_news);
            debugIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            actionA.setIconDrawable(debugIcon);
            actionA.setColorPressed(getResources().getColor(R.color.blockchain_green_50));
            actionA.setTitle("Show Payload");
            actionA.setOnClickListener(v -> {
                AlertDialog dialog = null;
                try {
                    dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                            .setTitle("Payload")
                            .setMessage(new JSONObject(viewModel.getPayloadManager().getBciWallet().getPayload().getDecryptedPayload()).toString(4))
                            .show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(9);
            });

            com.getbase.floatingactionbutton.FloatingActionButton actionB = new com.getbase.floatingactionbutton.FloatingActionButton(context);
            actionB.setColorNormal(getResources().getColor(R.color.blockchain_receive_green));
            actionB.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            debugIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            actionB.setIconDrawable(debugIcon);
            actionB.setColorPressed(getResources().getColor(R.color.blockchain_green_50));
            actionB.setTitle("Show unparsed wallet data");
            actionB.setOnClickListener(v -> {
                AlertDialog dialog = null;
                try {
                    dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                            .setTitle("Unparsed wallet data")
                            .setMessage(new JSONObject(viewModel.getPayloadManager().getBciWallet().getUnparsedWalletData()).toString(4))
                            .show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(9);
            });

            com.getbase.floatingactionbutton.FloatingActionButton actionC = new com.getbase.floatingactionbutton.FloatingActionButton(context);
            actionC.setColorNormal(getResources().getColor(R.color.blockchain_receive_green));
            actionC.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
            debugIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            actionC.setIconDrawable(debugIcon);
            actionC.setColorPressed(getResources().getColor(R.color.blockchain_green_50));
            actionC.setTitle("Show parsed wallet data");
            actionC.setOnClickListener(v -> {
                AlertDialog dialog = null;
                dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle)
                        .setTitle("Parsed wallet data")
                        .setMessage(viewModel.getPayloadManager().getBciWallet().toString())
                        .show();
                TextView textView = (TextView) dialog.findViewById(android.R.id.message);
                textView.setTextSize(9);
            });

            binding.fabDebug.addButton(actionA);
            binding.fabDebug.addButton(actionB);
            binding.fabDebug.addButton(actionC);
        }else{
            binding.fabDebug.setVisibility(View.GONE);
        }
    }

    public boolean isFabExpanded() {
        return isAdded() && binding.fab != null && binding.fab.isExpanded();
    }

    public void collapseFab() {
        if (binding.fab != null) binding.fab.collapse();
    }

    private void sendClicked() {
        startActivity(new Intent(getActivity(), SendActivity.class));
        binding.fab.collapse();
    }

    private void receiveClicked() {
        startActivity(new Intent(getActivity(), ReceiveActivity.class));
        binding.fab.collapse();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void setupViews() {
        initFab();
        initDebugFab();

        binding.noTransactionMessage.noTxMessage.setVisibility(View.GONE);

        //Elevation compat
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            binding.balance1.setBackgroundResource(R.drawable.container_blue_shadow);
        }

        binding.balance1.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //TODO this BALANCE_DISPLAY_STATE could be improved
                if (BALANCE_DISPLAY_STATE == SHOW_BTC) {
                    BALANCE_DISPLAY_STATE = SHOW_FIAT;
                    isBTC = false;
                    viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);

                } else if (BALANCE_DISPLAY_STATE == SHOW_FIAT) {
                    BALANCE_DISPLAY_STATE = SHOW_HIDE;
                    isBTC = true;
                    viewModel.setBalance(context.getString(R.string.show_balance));

                } else {
                    BALANCE_DISPLAY_STATE = SHOW_BTC;
                    isBTC = true;
                    viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
                }
                prefsUtil.setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, BALANCE_DISPLAY_STATE);

                return false;
            }
        });

        accountSpinner = (AppCompatSpinner) context.findViewById(R.id.account_spinner);
        viewModel.updateAccountList();
        accountsAdapter = new AccountAdapter(context, R.layout.spinner_title_bar, viewModel.getActiveAccountAndAddressList());
        accountsAdapter.setDropDownViewResource(R.layout.spinner_title_bar_dropdown);
        accountSpinner.setAdapter(accountsAdapter);
        accountSpinner.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_UP && ((MainActivity) getActivity()).getDrawerOpen();
            }
        });
        accountSpinner.post(new Runnable() {
            public void run() {
                accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        //Refresh balance header and tx list
                        viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                });
            }
        });

        transactionAdapter = new TxAdapter();
        layoutManager = new LinearLayoutManager(context);
        binding.rvTransactions.setLayoutManager(layoutManager);
        binding.rvTransactions.setAdapter(transactionAdapter);

        binding.rvTransactions.setOnScrollListener(new CollapseActionbarScrollListener() {
            @Override
            public void onMoved(int distance) {

                binding.balance1.setTranslationY(-distance);
            }
        });

        // drawerTitle account now that wallet has been created
        if (prefsUtil.getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, "").length() > 0) {
            viewModel.getPayloadManager().getPayload().getHdWallet().getAccounts().get(0).setLabel(prefsUtil.getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, ""));
            prefsUtil.removeValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME);
            PayloadBridge.getInstance().remoteSaveThread(new PayloadBridge.PayloadSaveListener() {
                @Override
                public void onSaveSuccess() {

                }

                @Override
                public void onSaveFail() {
                    ToastCustom.makeText(getActivity(), getActivity().getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
            });
            accountsAdapter.notifyDataSetChanged();
        }

        binding.balanceMainContentShadow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.fab.collapse();
            }
        });

        rowViewState = new HashMap<View, Boolean>();

        binding.noTransactionMessage.noTxMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(context, R.anim.jump);
                binding.fab.startAnimation(bounce);
            }
        });

        binding.swipeContainer.setProgressViewEndTarget(false, (int) (getResources().getDisplayMetrics().density * (72 + 20)));
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        binding.swipeContainer.setRefreshing(true);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            viewModel.getPayloadManager().updateBalancesAndTransactions();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        viewModel.updateAccountList();
                        viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
                        binding.swipeContainer.setRefreshing(false);
                    }

                }.execute();
            }
        });
        binding.swipeContainer.setColorSchemeResources(R.color.blockchain_receive_green,
                R.color.blockchain_blue,
                R.color.blockchain_send_red);
    }

    private void onRowClick(int position) {
        Intent intent = new Intent(getActivity(), TransactionDetailActivity.class);
        intent.putExtra(KEY_TRANSACTION_LIST_POSITION, position);
        startActivity(intent);
    }

    @Override
    public void onRefreshAccounts() {
        //TODO revise
        if (accountSpinner != null)
            setAccountSpinner();

        context.runOnUiThread(() -> {
            if (accountsAdapter != null) accountsAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onAccountSizeChange() {
        if(accountSpinner != null)
            accountSpinner.setSelection(0);
    }

    @Override
    public void onRefreshBalanceAndTransactions() {

        String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance().getLastPrice(strFiat);

        //Notify adapters of change
        accountsAdapter.notifyDataSetChanged();
        transactionAdapter.notifyDataSetChanged();

        //Display help text to user if no transactionList on selected account/address
        if (viewModel.getTransactionList().size() > 0) {
            binding.rvTransactions.setVisibility(View.VISIBLE);
            binding.noTransactionMessage.noTxMessage.setVisibility(View.GONE);
        } else {
            binding.rvTransactions.setVisibility(View.GONE);
            binding.noTransactionMessage.noTxMessage.setVisibility(View.VISIBLE);
        }

        if (isAdded() && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //Fix for padding bug related to Android 4.1
            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());
            binding.balance1.setPadding((int) px, 0, 0, 0);
        }
    }

    interface Communicator {

        void setNavigationDrawerToggleEnabled(boolean enabled);

        void resetNavigationDrawer();
    }

    private class TxAdapter extends RecyclerView.Adapter<TxAdapter.ViewHolder> {

        @Override
        public TxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_expandable, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);

            if (viewModel.getTransactionList() != null) {
                final Tx tx = viewModel.getTransactionList().get(position);
                double _btc_balance = tx.getAmount() / 1e8;
                double _fiat_balance = btc_fx * _btc_balance;

                View txTouchView = holder.itemView.findViewById(R.id.tx_touch_view);

                TextView tvResult = (TextView) holder.itemView.findViewById(R.id.result);
                tvResult.setTextColor(Color.WHITE);

                TextView tvTS = (TextView) holder.itemView.findViewById(R.id.ts);
                tvTS.setText(dateUtil.formatted(tx.getTS()));

                TextView tvDirection = (TextView) holder.itemView.findViewById(R.id.direction);
                String dirText = tx.getDirection();
                if (dirText.equals(MultiAddrFactory.MOVED))
                    tvDirection.setText(getResources().getString(R.string.MOVED));
                if (dirText.equals(MultiAddrFactory.RECEIVED))
                    tvDirection.setText(getResources().getString(R.string.RECEIVED));
                if (dirText.equals(MultiAddrFactory.SENT))
                    tvDirection.setText(getResources().getString(R.string.SENT));

                Spannable span1;
                if (isBTC) {
                    span1 = Spannable.Factory.getInstance().newSpannable(viewModel.getMonetaryUtil().getDisplayAmountWithFormatting(Math.abs(tx.getAmount())) + " " + viewModel.getDisplayUnits());
                    span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - viewModel.getDisplayUnits().length(), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    span1 = Spannable.Factory.getInstance().newSpannable(viewModel.getMonetaryUtil().getFiatFormat(strFiat).format(Math.abs(_fiat_balance)) + " " + strFiat);
                    span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                int nbConfirmations = 3;
                if (tx.isMove()) {
                    tvResult.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);
                    tvDirection.setTextColor(context.getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue));
                } else if (_btc_balance < 0.0) {
                    tvResult.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);
                    tvDirection.setTextColor(context.getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_red_50 : R.color.blockchain_send_red));
                } else {
                    tvResult.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);
                    tvDirection.setTextColor(context.getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_green_50 : R.color.blockchain_receive_green));
                }

                TextView tvWatchOnly = (TextView) holder.itemView.findViewById(R.id.watch_only);
                if(tx.isWatchOnly()){
                    tvWatchOnly.setVisibility(View.VISIBLE);
                }else{
                    tvWatchOnly.setVisibility(View.GONE);
                }

                tvResult.setText(span1);

                tvResult.setOnTouchListener((v, event) -> {

                    View parent = (View) v.getParent();
                    event.setLocation(v.getX() + (v.getWidth() / 2), v.getY() + (v.getHeight() / 2));
                    parent.onTouchEvent(event);

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        isBTC = !isBTC;
                        viewModel.updateBalanceAndTransactionList(null, accountSpinner.getSelectedItemPosition(), isBTC);
                    }
                    return true;
                });

                txTouchView.setOnTouchListener((v, event) -> {

                    View parent = (View) v.getParent();
                    event.setLocation(event.getX(), v.getY() + (v.getHeight() / 2));
                    parent.onTouchEvent(event);

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        onRowClick(position);
                    }
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            if (viewModel.getTransactionList() == null) return 0;
            return viewModel.getTransactionList().size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            ViewHolder(View view) {
                super(view);
            }
        }
    }

    public abstract class CollapseActionbarScrollListener extends RecyclerView.OnScrollListener {

        private int mToolbarOffset = 0;

        CollapseActionbarScrollListener() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            binding.swipeContainer.setEnabled(layoutManager.findFirstCompletelyVisibleItemPosition() == 0);

            //Only bring heading back down after 2nd item visible (0 = heading)
            if (layoutManager.findFirstCompletelyVisibleItemPosition() <= 2) {

                if ((mToolbarOffset < balanceBarHeight && dy > 0) || (mToolbarOffset > 0 && dy < 0)) {
                    mToolbarOffset += dy;
                }

                clipToolbarOffset();
                onMoved(mToolbarOffset);
            }
        }

        private void clipToolbarOffset() {
            if (mToolbarOffset > balanceBarHeight) {
                mToolbarOffset = balanceBarHeight;
            } else if (mToolbarOffset < 0) {
                mToolbarOffset = 0;
            }
        }

        public abstract void onMoved(int distance);
    }

    private class AccountAdapter extends ArrayAdapter<String> {

        Context context;
        int layoutResourceId;

        AccountAdapter(Context context, int layoutResourceId, ArrayList<String> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;
            if (null == view) {
                view = LayoutInflater.from(this.getContext()).inflate(R.layout.spinner_title_bar, null);
                ((TextView) view).setText(getItem(position));
            }
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
            return view;
        }
    }
}