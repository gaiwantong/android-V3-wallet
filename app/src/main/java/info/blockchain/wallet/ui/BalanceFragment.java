package info.blockchain.wallet.ui;

import com.google.common.collect.HashBiMap;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDPayloadBridge;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.DateUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.SSLVerifierThreadUtil;
import info.blockchain.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import piuk.blockchain.android.R;

public class BalanceFragment extends Fragment {

    public static final String ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH";
    private final static int SHOW_BTC = 1;
    private final static int SHOW_FIAT = 2;
    private final static int SHOW_HIDE = 3;
    private static int nbConfirmations = 3;
    private static int BALANCE_DISPLAY_STATE = SHOW_BTC;
    private static boolean isBottomSheetOpen = false;
    public int balanceBarHeight;
    ArrayAdapter<String> accountsAdapter = null;
    LinearLayoutManager layoutManager;
    HashMap<View, Boolean> rowViewState = null;
    Communicator comm;
    private FloatingActionsMenu menuMultipleActions = null;
    //
    // main balance display
    //
    private TextView tvBalance1 = null;
    private double btc_fx = 319.13;
    private Spannable span1 = null;
    private String strFiat = null;
    private boolean isBTC = true;
    //
    // accounts list
    //
    private Spinner accountSpinner = null;
    private ArrayList<String> activeAccountAndAddressList = null;
    private HashBiMap<Object, Integer> activeAccountAndAddressBiMap = null;
    //
    // tx list
    //
    private final String TAG_ALL = "TAG_ALL";
    private final String TAG_IMPORTED_ADDRESSES = "TAG_IMPORTED_ADDRESSES";
    private List<Tx> transactionList = new ArrayList<Tx>();
    private RecyclerView transactionRecyclerView = null;
    private TxAdapter transactionAdapter = null;
    private LinearLayout noTxMessage = null;
    private LinearLayout mainContentShadow;
    private Activity thisActivity = null;
    private int originalHeight = 0;
    private int newHeight = 0;
    private int expandDuration = 200;
    private boolean mIsViewExpanded = false;
    private View rootView = null;
    private View prevRowClicked = null;
    private SwipeRefreshLayout swipeLayout = null;
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                swipeLayout.setRefreshing(true);
                            }
                        });
                    }

                    @Override
                    protected Void doInBackground(Void... params) {

                        // Update internal balance and transaction data
                        try {
                            HDPayloadBridge.getInstance(getActivity()).updateBalancesAndTransactions();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // Update balance and transactions in the UI
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateAccountList();
                                updateBalanceAndTransactionList(intent);
                            }
                        });

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                swipeLayout.setRefreshing(false);
                            }
                        });
                    }

                }.execute();
            }
        }
    };

    public BalanceFragment() {
        ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(getResources().getLayout(R.layout.balance_layout_oriented), container, false);

        thisActivity = getActivity();
        setHasOptionsMenu(true);

        balanceBarHeight = (int) getResources().getDimension(R.dimen.action_bar_height) + 35;

        BALANCE_DISPLAY_STATE = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        if (BALANCE_DISPLAY_STATE == SHOW_FIAT) {
            isBTC = false;
        }

        activeAccountAndAddressList = new ArrayList<>();
        activeAccountAndAddressBiMap = HashBiMap.create();
        transactionList = new ArrayList<>();

        setupViews(rootView);

        SSLVerifierThreadUtil.getInstance(getActivity()).validateSSLThread();

        return rootView;
    }

    private void updateAccountList(){

        //activeAccountAndAddressList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        activeAccountAndAddressList.clear();
        activeAccountAndAddressBiMap.clear();

        int spinnerIndex = 0;

        //All accounts/addresses
        List<Account> allAccounts = null;
        List<LegacyAddress> allLegacyAddresses = PayloadFactory.getInstance().get().getLegacyAddresses();

        //Only active accounts/addresses (exclude archived)
        List<Account> activeAccounts = new ArrayList<>();
        if (PayloadFactory.getInstance().get().isUpgraded()) {

            allAccounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();//V3

            for (Account item : allAccounts) {
                if (!item.isArchived()) {
                    activeAccounts.add(item);
                }
            }
        }
        List<LegacyAddress> activeLegacyAddresses = new ArrayList<>();
        for (LegacyAddress item : allLegacyAddresses) {
            if (item.getTag() != PayloadFactory.ARCHIVED_ADDRESS){
                activeLegacyAddresses.add(item);
            }
        }

        //"All" - total balance
        if (activeAccounts != null && activeAccounts.size() > 1 || activeLegacyAddresses.size() > 0) {

            if (PayloadFactory.getInstance().get().isUpgraded()) {

                //Only V3 will display "All"
                Account all = new Account();
                all.setLabel(getActivity().getResources().getString(R.string.all_accounts));
                all.setTags(Arrays.asList(TAG_ALL));
                activeAccountAndAddressList.add(all.getLabel());
                activeAccountAndAddressBiMap.put(all, spinnerIndex);
                spinnerIndex++;

            } else if (activeLegacyAddresses.size() > 1) {

                //V2 "All" at top of accounts spinner if wallet contains multiple legacy addresses
                ImportedAccount iAccount = new ImportedAccount(getActivity().getString(R.string.total_funds), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
                iAccount.setTags(Arrays.asList(TAG_ALL));
                activeAccountAndAddressList.add(iAccount.getLabel());
                activeAccountAndAddressBiMap.put(iAccount, spinnerIndex);
                spinnerIndex++;
            }
        }

        //Add accounts to map
        int accountIndex = 0;
        for (Account item : activeAccounts) {

            if (item.getLabel().trim().length() == 0)item.setLabel("Account: " + accountIndex);//Give unlabeled account a label

            activeAccountAndAddressList.add(item.getLabel());
            activeAccountAndAddressBiMap.put(item, spinnerIndex);
            spinnerIndex++;
            accountIndex++;
        }

        //Add "Imported Addresses" or "Total Funds" to map
        if (PayloadFactory.getInstance().get().isUpgraded() && activeLegacyAddresses.size() > 0) {

            //Only V3 - Consolidate and add Legacy addresses to "Imported Addresses" at bottom of accounts spinner
            ImportedAccount iAccount = new ImportedAccount(getActivity().getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
            iAccount.setTags(Arrays.asList(TAG_IMPORTED_ADDRESSES));
            activeAccountAndAddressList.add(iAccount.getLabel());
            activeAccountAndAddressBiMap.put(iAccount, spinnerIndex);
            spinnerIndex++;

        }else{
            for (LegacyAddress legacyAddress : activeLegacyAddresses) {

                //If address has no label, we'll display address
                String labelOrAddress = legacyAddress.getLabel() == null || legacyAddress.getLabel().trim().length() == 0 ? legacyAddress.getAddress() : legacyAddress.getLabel();

                //Prefix "watch-only"
                if (legacyAddress.isWatchOnly()) {
                    labelOrAddress = getActivity().getString(R.string.watch_only_label) + " " + labelOrAddress;
                }

                activeAccountAndAddressList.add(labelOrAddress);
                activeAccountAndAddressBiMap.put(legacyAddress, spinnerIndex);
                spinnerIndex++;
            }
        }

        //If we have multiple accounts/addresses we will show dropdown in toolbar, otherwise we will only display a static text
        if(accountSpinner != null)
            setAccountSpinner();

        //Notify adapter of list update
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (accountsAdapter != null) accountsAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setAccountSpinner(){
        if(activeAccountAndAddressList.size() > 1){
            ((ActionBarActivity) thisActivity).getSupportActionBar().setDisplayShowTitleEnabled(false);
            accountSpinner.setVisibility(View.VISIBLE);
        }else{
            ((ActionBarActivity) thisActivity).getSupportActionBar().setDisplayShowTitleEnabled(true);
            accountSpinner.setSelection(0);
            ((ActionBarActivity) thisActivity).getSupportActionBar().setTitle(activeAccountAndAddressList.get(0));
            accountSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            isBottomSheetOpen = false;
            updateBalanceAndTransactionList(null);
        } else {
            ;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity.currentFragment = this;

        comm.resetNavigationDrawer();

        isBottomSheetOpen = false;

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(thisActivity).registerReceiver(receiver, filter);

        if (!OSUtil.getInstance(thisActivity).isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.websocket.WebSocketService.class));
        } else {
            thisActivity.stopService(new Intent(thisActivity, info.blockchain.wallet.websocket.WebSocketService.class));
            thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.websocket.WebSocketService.class));
        }

        updateBalanceAndTransactionList(null);
        updateAccountList();
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(thisActivity).unregisterReceiver(receiver);
    }

    private String getDisplayUnits() {

        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

    /*
    TODO - this should be removed when doing jar refactor
    Quick fix to remove duplicate txs from 'All' list:
    Remove any duplicate transactions when the wallet requests a consolidated transaction list for the "All" account
    This would be caused by any transfers from HD to Legacy or visa versa
     */
    public List<Tx> getAllXpubAndLegacyTxs(){

        //Remove duplicate txs
        HashMap<String, Tx> consolidatedTxsList = new HashMap<String, Tx>();

        List<Tx> allXpubTransactions = MultiAddrFactory.getInstance().getAllXpubTxs();
        for(Tx tx : allXpubTransactions){
            if(!consolidatedTxsList.containsKey(tx.getHash()))
                consolidatedTxsList.put(tx.getHash(), tx);
        }

        List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
        for(Tx tx : allLegacyTransactions){
            if(!consolidatedTxsList.containsKey(tx.getHash()))
                consolidatedTxsList.put(tx.getHash(), tx);
        }

        return new ArrayList(consolidatedTxsList.values());
    }

    private void updateBalanceAndTransactionList(Intent intent) {

        ArrayList<Tx> unsortedTransactionList = new ArrayList<>();//We will sort this list by date shortly
        double btc_balance = 0.0;
        double fiat_balance = 0.0;

        Object object = activeAccountAndAddressBiMap.inverse().get(accountSpinner.getSelectedItemPosition());//the current selected item in dropdown (Account or Legacy Address)

        //If current selected item gets edited by another platform object might become null
        if(object == null){
            accountSpinner.setSelection(0);
            object = activeAccountAndAddressBiMap.inverse().get(accountSpinner.getSelectedItemPosition());
        }
        
        if(object instanceof Account){
            //V3
            Account account = ((Account) object);

            //V3 - All
            if(account.getTags().contains(TAG_ALL)){
                if (PayloadFactory.getInstance().get().isUpgraded()) {
                    //Total for accounts
                    List<Tx> allTransactions = getAllXpubAndLegacyTxs();
                    if(allTransactions != null)unsortedTransactionList.addAll(allTransactions);

                    //Balance = all xpubs + all legacy address balances
                    btc_balance = ((double) MultiAddrFactory.getInstance().getXpubBalance()) + ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());

                }else{
                    //Total for legacyAddresses
                    List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
                    if(allLegacyTransactions != null)unsortedTransactionList.addAll(allLegacyTransactions);
                    //Balance = all legacy address balances
                    btc_balance = ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());
                }
            }else if(account.getTags().contains(TAG_IMPORTED_ADDRESSES)){
                //V3 - Imported Addresses
                List<Tx> allLegacyTransactions = MultiAddrFactory.getInstance().getLegacyTxs();
                if(allLegacyTransactions != null)unsortedTransactionList.addAll(allLegacyTransactions);
                btc_balance = ((double) MultiAddrFactory.getInstance().getLegacyActiveBalance());

            }else{
                //V3 - Individual
                String xpub = account.getXpub();
                if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    List<Tx> xpubTransactions = MultiAddrFactory.getInstance().getXpubTxs().get(xpub);
                    if(xpubTransactions != null)unsortedTransactionList.addAll(xpubTransactions);
                    HashMap<String, Long> xpubAmounts = MultiAddrFactory.getInstance().getXpubAmounts();
                    Long bal = (xpubAmounts.get(xpub) == null ? 0l : xpubAmounts.get(xpub));
                    btc_balance = ((double) (bal));
                }
            }

        }else{
            //V2
            LegacyAddress legacyAddress = ((LegacyAddress) object);
            List<Tx> legacyTransactions = MultiAddrFactory.getInstance().getAddressLegacyTxs(legacyAddress.getAddress());
            if(legacyTransactions != null)unsortedTransactionList.addAll(legacyTransactions);//V2 get single address' transactionList
            btc_balance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());

            //if (legacyAddress.isWatchOnly()TODO- exclude?
        }

        //Returning from SendFragment the following will happen
        //After sending btc we create a "placeholder" tx until websocket handler refreshes list
        if (intent != null && intent.getExtras() != null) {
            long amount = intent.getLongExtra("queued_bamount", 0);
            String strNote = intent.getStringExtra("queued_strNote");
            String direction = intent.getStringExtra("queued_direction");
            long time = intent.getLongExtra("queued_time", System.currentTimeMillis() / 1000);

            Tx tx = new Tx("", strNote, direction, amount, time, new HashMap<Integer, String>());
            unsortedTransactionList.add(0, tx);
        } else if (unsortedTransactionList != null && unsortedTransactionList.size() > 0) {
            if (unsortedTransactionList.get(0).getHash().isEmpty()) unsortedTransactionList.remove(0);
        }

        //Sort transactionList as server does not return sorted transactionList
        transactionList.clear();
        Collections.sort(unsortedTransactionList, new TxDateComparator());
        transactionList.addAll(unsortedTransactionList);

        //Update Balance
        strFiat = PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(thisActivity).getLastPrice(strFiat);
        fiat_balance = btc_fx * (btc_balance / 1e8);

        String balanceTotal = "";
        if (isBTC) {
            balanceTotal = (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(btc_balance) + " " + getDisplayUnits());
        } else {
            balanceTotal = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat);
        }

        span1 = Spannable.Factory.getInstance().newSpannable(balanceTotal);
        span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - (isBTC ? getDisplayUnits().length() : 3), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (BALANCE_DISPLAY_STATE != SHOW_HIDE) {
            tvBalance1.setText(span1);
        } else {
            span1 = Spannable.Factory.getInstance().newSpannable(thisActivity.getText(R.string.show_balance));
            span1.setSpan(new RelativeSizeSpan(0.67f), 0, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvBalance1.setText(span1);
        }

        //Notify adapters of change
        accountsAdapter.notifyDataSetChanged();
        transactionAdapter.notifyDataSetChanged();

        //Display help text to user if no transactionList on selected account/address
        if (transactionList.size() > 0) {
            transactionRecyclerView.setVisibility(View.VISIBLE);
            noTxMessage.setVisibility(View.GONE);
        } else {
            transactionRecyclerView.setVisibility(View.GONE);
            noTxMessage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_merchant_directory).setVisible(true);
        menu.findItem(R.id.action_qr).setVisible(true);
        menu.findItem(R.id.action_send).setVisible(false);
        menu.findItem(R.id.action_share_receive).setVisible(false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        comm = (Communicator) activity;
    }

    private void initFab(final View rootView){

        //First icon when fab expands
        com.getbase.floatingactionbutton.FloatingActionButton actionA = new com.getbase.floatingactionbutton.FloatingActionButton(getActivity());
        actionA.setColorNormal(getResources().getColor(R.color.blockchain_send_red));
        actionA.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
        Drawable sendIcon = getActivity().getResources().getDrawable(R.drawable.icon_send);
        sendIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        actionA.setIconDrawable(sendIcon);
        actionA.setColorPressed(getResources().getColor(R.color.blockchain_red_50));
        actionA.setTitle(getResources().getString(R.string.send_bitcoin));
        actionA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendClicked();
            }
        });

        //Second icon when fab expands
        com.getbase.floatingactionbutton.FloatingActionButton actionB = new com.getbase.floatingactionbutton.FloatingActionButton(getActivity());
        actionB.setColorNormal(getResources().getColor(R.color.blockchain_receive_green));
        actionB.setSize(com.getbase.floatingactionbutton.FloatingActionButton.SIZE_MINI);
        Drawable receiveIcon = getActivity().getResources().getDrawable(R.drawable.icon_receive);
        receiveIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        actionB.setIconDrawable(receiveIcon);
        actionB.setColorPressed(getResources().getColor(R.color.blockchain_green_50));
        actionB.setTitle(getResources().getString(R.string.receive_bitcoin));
        actionB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveClicked();
            }
        });

        //Add buttons to expanding fab
        menuMultipleActions = (FloatingActionsMenu) rootView.findViewById(R.id.multiple_actions);
        menuMultipleActions.addButton(actionA);
        menuMultipleActions.addButton(actionB);

        menuMultipleActions.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                mainContentShadow.setVisibility(View.VISIBLE);
                isBottomSheetOpen = true;
                comm.setNavigationDrawerToggleEnabled(false);
            }

            @Override
            public void onMenuCollapsed() {
                menuMultipleActions.collapse();
                mainContentShadow.setVisibility(View.GONE);
                isBottomSheetOpen = false;
                comm.setNavigationDrawerToggleEnabled(true);
            }
        });
    }

    private void sendClicked(){
        SSLVerifierThreadUtil.getInstance(getActivity()).validateSSLThread();

        Fragment fragment = new SendFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
        comm.setNavigationDrawerToggleEnabled(true);
    }

    private void receiveClicked(){
        Fragment fragment = new ReceiveFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
        comm.setNavigationDrawerToggleEnabled(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        populateViewForOrientation(inflater, (ViewGroup) getView());
    }

    private void populateViewForOrientation(LayoutInflater inflater, ViewGroup viewGroup) {
        viewGroup.removeAllViewsInLayout();
        View subview = inflater.inflate(getResources().getLayout(R.layout.balance_layout_oriented), viewGroup);
        setupViews(subview);
    }

    private void setupViews(View rootView) {

        initFab(rootView);

        noTxMessage = (LinearLayout) rootView.findViewById(R.id.no_tx_message);
        noTxMessage.setVisibility(View.GONE);

        tvBalance1 = (TextView) rootView.findViewById(R.id.balance1);

        //Elevation compat
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //reapply layout attributes after setBackgroundResource
            int bottom = tvBalance1.getPaddingBottom();
            int top = tvBalance1.getPaddingTop();
            int right = tvBalance1.getPaddingRight();
            int left = tvBalance1.getPaddingLeft();
            tvBalance1.setBackgroundResource(R.drawable.container_blue_shadow);
            tvBalance1.setPadding(left, top, right, bottom);
            tvBalance1.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }

        tvBalance1.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (BALANCE_DISPLAY_STATE == SHOW_BTC) {
                    BALANCE_DISPLAY_STATE = SHOW_FIAT;
                    isBTC = false;
                } else if (BALANCE_DISPLAY_STATE == SHOW_FIAT) {
                    BALANCE_DISPLAY_STATE = SHOW_HIDE;
                    isBTC = true;
                } else {
                    BALANCE_DISPLAY_STATE = SHOW_BTC;
                    isBTC = true;
                }
                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, BALANCE_DISPLAY_STATE);

                updateBalanceAndTransactionList(null);
                return false;
            }
        });

        accountSpinner = (Spinner) thisActivity.findViewById(R.id.account_spinner);
        updateAccountList();
        accountsAdapter = new AccountAdapter(thisActivity, R.layout.spinner_title_bar, activeAccountAndAddressList);
        accountsAdapter.setDropDownViewResource(R.layout.spinner_title_bar_dropdown);
        accountSpinner.setAdapter(accountsAdapter);
        accountSpinner.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && MainActivity.drawerIsOpen) {
                    return true;
                } else if (isBottomSheetOpen) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        accountSpinner.post(new Runnable() {
            public void run() {
                accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        //Refresh balance header and tx list
                        updateBalanceAndTransactionList(null);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        ;
                    }
                });
            }
        });

        transactionRecyclerView = (RecyclerView) rootView.findViewById(R.id.txList2);
        transactionAdapter = new TxAdapter();
        layoutManager = new LinearLayoutManager(thisActivity);
        transactionRecyclerView.setLayoutManager(layoutManager);
        transactionRecyclerView.setAdapter(transactionAdapter);

        if (!getResources().getBoolean(R.bool.isDualPane))
            transactionRecyclerView.setOnScrollListener(new CollapseActionbarScrollListener() {
                @Override
                public void onMoved(int distance) {

                    tvBalance1.setTranslationY(-distance);
                }
            });
        else
            transactionRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    swipeLayout.setEnabled(layoutManager.findFirstCompletelyVisibleItemPosition() == 0);
                }
            });

        // drawerTitle account now that wallet has been created
        if (PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, "").length() > 0) {
            PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, ""));
            PrefsUtil.getInstance(thisActivity).removeValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME);
            PayloadBridge.getInstance(thisActivity).remoteSaveThread();
            accountsAdapter.notifyDataSetChanged();
        }

        if (!OSUtil.getInstance(thisActivity).isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.websocket.WebSocketService.class));
        } else {
            thisActivity.stopService(new Intent(thisActivity, info.blockchain.wallet.websocket.WebSocketService.class));
            thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.websocket.WebSocketService.class));
        }

        mainContentShadow = (LinearLayout) rootView.findViewById(R.id.balance_main_content_shadow);
        mainContentShadow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuMultipleActions.collapse();
            }
        });

        rowViewState = new HashMap<View, Boolean>();

        noTxMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(getActivity(), R.anim.jump);
                menuMultipleActions.startAnimation(bounce);
            }
        });

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setProgressViewEndTarget(false, (int) (getResources().getDisplayMetrics().density * (72 + 20)));
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        });
        swipeLayout.setColorScheme(R.color.blockchain_receive_green,
                R.color.blockchain_blue,
                R.color.blockchain_send_red);
    }

    private void onRowClick(final View view, final int position) {

        if (transactionList != null) {
            final Tx tx = transactionList.get(position);
            final String strTx = tx.getHash();
            final String strConfirmations = Long.toString(tx.getConfirmations());

            try {
                mIsViewExpanded = rowViewState.get(view);
            } catch (Exception e) {
                mIsViewExpanded = false;
            }

            //Set views
            View detailsView = view;
            if (getResources().getBoolean(R.bool.isDualPane))
                detailsView = rootView;

            final ScrollView txsDetails = (ScrollView) detailsView.findViewById(R.id.txs_details);
            final TextView tvOutAddr = (TextView) detailsView.findViewById(R.id.tx_from_addr);

            final TextView tvFee = (TextView) detailsView.findViewById(R.id.tx_fee_value);
            final TextView tvTxHash = (TextView) detailsView.findViewById(R.id.tx_hash);
            final ProgressBar progressView = (ProgressBar) detailsView.findViewById(R.id.progress_view);
            final TextView tvStatus = (TextView) detailsView.findViewById(R.id.transaction_status);
            final ImageView ivStatus = (ImageView) detailsView.findViewById(R.id.transaction_status_icon);
            final LinearLayout toAddressContainer = (LinearLayout) detailsView.findViewById(R.id.tx_details_to_include_container);

            final LinearLayout feeContainer = (LinearLayout) detailsView.findViewById(R.id.tx_fee_container);
            final View feeSeparator = detailsView.findViewById(R.id.tx_fee_separator);

            if (getResources().getBoolean(R.bool.isDualPane) || (!getResources().getBoolean(R.bool.isDualPane) && !mIsViewExpanded)) {
                if (prevRowClicked != null && prevRowClicked == transactionRecyclerView.getLayoutManager().getChildAt(position)) {
                    txsDetails.setVisibility(View.INVISIBLE);
                    prevRowClicked.findViewById(R.id.tx_row).setBackgroundResource(R.drawable.selector_pearl_white_tx);
                    prevRowClicked = null;
                    return;
                }

                txsDetails.setVisibility(View.VISIBLE);
                progressView.setVisibility(View.VISIBLE);
                tvOutAddr.setVisibility(View.INVISIBLE);
                toAddressContainer.setVisibility(View.INVISIBLE);
                tvStatus.setVisibility(View.INVISIBLE);
                ivStatus.setVisibility(View.INVISIBLE);

                if (tx.getDirection().equals(MultiAddrFactory.RECEIVED)) {
                    feeContainer.setVisibility(View.GONE);
                    feeSeparator.setVisibility(View.GONE);
                }

                tvTxHash.setText(strTx);
                tvTxHash.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        if (event.getAction() == MotionEvent.ACTION_UP && !strTx.isEmpty()) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + strTx));
                            startActivity(browserIntent);
                        }
                        return true;
                    }
                });
                tvStatus.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (tvStatus.getTag() != null) {
                                String tag = tvStatus.getTag().toString();
                                String text = tvStatus.getText().toString();
                                tvStatus.setText(tag);
                                tvStatus.setTag(text);
                            }
                        }
                        return true;
                    }
                });

                TextView tvResult = (TextView) view.findViewById(R.id.result);
                tvResult.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            isBTC = (isBTC) ? false : true;
                            updateBalanceAndTransactionList(null);
                        }
                        return true;
                    }
                });

                if (!getResources().getBoolean(R.bool.isDualPane))
                    txsDetails.setOnTouchListener(new OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {

                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                onRowClick(view, position);
                            }
                            return true;

                            //To be used with advance send tx display
                            // Disallow the touch request for parent scroll on touch of child view
                            //v.getParent().requestDisallowInterceptTouchEvent(true);
                            //return false;
                        }
                    });

                //Get Details
                if (tx.getHash().isEmpty()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText(thisActivity.getString(R.string.WAITING));

                            tvOutAddr.setText("");
                            tvTxHash.setText("");

                            tvOutAddr.setVisibility(View.VISIBLE);
                            tvStatus.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    new AsyncTask<Void, Void, String>() {

                        @Override
                        protected String doInBackground(Void... params) {

                            String stringResult = null;
                            try {
                                stringResult = WebUtil.getInstance().getURL(WebUtil.TRANSACTION + strTx + "?format=json");

                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            return stringResult;
                        }

                        @Override
                        protected void onPostExecute(String stringResult) {
                            super.onPostExecute(stringResult);

                            if (stringResult != null) {
                                Transaction transaction = null;
                                try {
//                                    Log.v("", "stringResult: " + stringResult);
                                    transaction = new Transaction(new JSONObject(stringResult));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                progressView.setVisibility(View.GONE);

                                String fee = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (transaction.getFee() / 1e8)) + " " + strFiat);
                                if (isBTC)
                                    fee = (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(transaction.getFee()) + " " + getDisplayUnits());
                                tvFee.setText(fee);

                                //From Address
                                HashMap<String, Long> fromAddressValuePair = transaction.getFromLabelValuePair(tx.getDirection());

                                StringBuilder fromBuilder = new StringBuilder("");
                                for (Map.Entry<String, Long> item : fromAddressValuePair.entrySet()) {
                                    String label = item.getKey();
                                    fromBuilder.append(label + "\n");
                                }

                                String fromString = fromBuilder.toString();
                                if (fromString.length() > 0)
                                    fromString = fromString.substring(0, fromBuilder.toString().length() - 1);
                                tvOutAddr.setText(fromString);

                                //To Address
                                HashMap<String, Long> toddressValuePair = transaction.getToLabelValuePair(tx.getDirection(), tx.getAmount());

                                toAddressContainer.removeAllViews();

                                List<String> ownLegacyAddresses = PayloadFactory.getInstance().get().getLegacyAddressStrings();

                                for (Map.Entry<String, Long> item : toddressValuePair.entrySet()) {

                                    if ((getActivity() == null) || (AppUtil.getInstance(getActivity()).isNotUpgraded() && tx.getDirection().equals(MultiAddrFactory.RECEIVED) && !ownLegacyAddresses.contains(item.getKey()))) {
                                        continue;
                                    }

                                    View v = LayoutInflater.from(getActivity()).inflate(R.layout.include_tx_details_to, toAddressContainer, false);
                                    TextView tvToAddr = (TextView) v.findViewById(R.id.tx_to_addr);

                                    TextView tvToAddrTotal = (TextView) v.findViewById(R.id.tx_to_addr_total);
                                    toAddressContainer.addView(v);

                                    tvToAddr.setText(item.getKey());
                                    long amount = item.getValue();
                                    String amountS = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (amount / 1e8)) + " " + strFiat);
                                    if (isBTC)
                                        amountS = (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(amount) + " " + getDisplayUnits());

                                    tvFee.setText(fee);
                                    tvToAddrTotal.setText(amountS);
                                }

                                tvStatus.setTag(strConfirmations);

                                if (tx.getConfirmations() >= nbConfirmations) {
                                    ivStatus.setImageResource(R.drawable.ic_done_grey600_24dp);
                                    tvStatus.setText(getString(R.string.COMPLETE));
                                } else {
                                    ivStatus.setImageResource(R.drawable.ic_schedule_grey600_24dp);
                                    tvStatus.setText(getString(R.string.PENDING));
                                }
                                tvOutAddr.setVisibility(View.VISIBLE);
                                toAddressContainer.setVisibility(View.VISIBLE);
                                tvStatus.setVisibility(View.VISIBLE);
                                ivStatus.setVisibility(View.VISIBLE);

                                if (fromAddressValuePair.size() >= 2 || toddressValuePair.size() >= 2)//details view needs to be scrollable now
                                    if (!getResources().getBoolean(R.bool.isDualPane))
                                        txsDetails.setOnTouchListener(new OnTouchListener() {
                                            @Override
                                            public boolean onTouch(View v, MotionEvent event) {
                                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                                return false;
                                            }
                                        });
                            }
                        }
                    }.execute();
                }
            }

            //Single Pane View - Expand and collapse details
            if (!getResources().getBoolean(R.bool.isDualPane)) {
                if (originalHeight == 0) {
                    originalHeight = view.getHeight();
                }

                newHeight = originalHeight + txsDetails.getHeight();

                if (!mIsViewExpanded) {
                    expandView(view, txsDetails);

                } else {
                    collapseView(view, txsDetails);
                }

                rowViewState.put(view, mIsViewExpanded);
            } else {
                //Dual Pane View
                view.findViewById(R.id.tx_row).setBackgroundResource(R.color.blockchain_light_grey);

                if (prevRowClicked != null)
                    prevRowClicked.findViewById(R.id.tx_row).setBackgroundResource(R.drawable.selector_pearl_white_tx);

                prevRowClicked = view;
            }
        }
    }

    private void expandView(View view, ScrollView txsDetails) {

        view.setBackgroundColor(getResources().getColor(R.color.white));

        //Fade Details in - expansion of row will create slide down effect
        txsDetails.setVisibility(View.VISIBLE);
        txsDetails.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in));
        txsDetails.setEnabled(true);

        mIsViewExpanded = !mIsViewExpanded;
        ValueAnimator resizeAnimator = ValueAnimator.ofInt(originalHeight, newHeight);
        startAnim(view, resizeAnimator);
    }

    private void collapseView(View view, final ScrollView txsDetails) {

        TypedValue outValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        view.setBackgroundResource(outValue.resourceId);

        mIsViewExpanded = !mIsViewExpanded;
        ValueAnimator resizeAnimator = ValueAnimator.ofInt(newHeight, originalHeight);

        txsDetails.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down));

        Animation anim = new AlphaAnimation(1.00f, 0.00f);
        anim.setDuration(expandDuration / 2);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                txsDetails.setVisibility(View.INVISIBLE);
                txsDetails.setEnabled(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        txsDetails.startAnimation(anim);

        startAnim(view, resizeAnimator);
    }

    private void startAnim(final View view, ValueAnimator resizeAnimator) {

        //Set and start row collapse/expand
        resizeAnimator.setDuration(expandDuration);
        resizeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        resizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                view.getLayoutParams().height = value.intValue();
                view.requestLayout();
            }
        });


        resizeAnimator.start();
    }

    interface Communicator {

        public void setNavigationDrawerToggleEnabled(boolean enabled);

        public void resetNavigationDrawer();
    }

    private class TxAdapter extends RecyclerView.Adapter<TxAdapter.ViewHolder> {

        @Override
        public TxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = null;

            boolean isTwoPane = getResources().getBoolean(R.bool.isDualPane);
            if (!isTwoPane)
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_expandable, parent, false);
            else
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_simple, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            if (transactionList != null) {
                final Tx tx = transactionList.get(position);
                double _btc_balance = tx.getAmount() / 1e8;
                double _fiat_balance = btc_fx * _btc_balance;

                View txTouchView = holder.itemView.findViewById(R.id.tx_touch_view);

                TextView tvResult = (TextView) holder.itemView.findViewById(R.id.result);
                tvResult.setTextColor(Color.WHITE);

                TextView tvTS = (TextView) holder.itemView.findViewById(R.id.ts);
                tvTS.setText(DateUtil.getInstance(thisActivity).formatted(tx.getTS()));

                TextView tvDirection = (TextView) holder.itemView.findViewById(R.id.direction);
                String dirText = tx.getDirection();
                if (dirText.equals(MultiAddrFactory.MOVED))
                    tvDirection.setText(getResources().getString(R.string.MOVED));
                if (dirText.equals(MultiAddrFactory.RECEIVED))
                    tvDirection.setText(getResources().getString(R.string.RECEIVED));
                if (dirText.equals(MultiAddrFactory.SENT))
                    tvDirection.setText(getResources().getString(R.string.SENT));

                if (isBTC) {
                    span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(Math.abs(tx.getAmount())) + " " + getDisplayUnits());
                    span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - getDisplayUnits().length(), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(Math.abs(_fiat_balance)) + " " + strFiat);
                    span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (tx.isMove()) {
                    tvResult.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);
                    tvDirection.setTextColor(thisActivity.getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue));
                } else if (_btc_balance < 0.0) {
                    tvResult.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);
                    tvDirection.setTextColor(thisActivity.getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_red_50 : R.color.blockchain_send_red));
                } else {
                    tvResult.setBackgroundResource(tx.getConfirmations() < nbConfirmations ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);
                    tvDirection.setTextColor(thisActivity.getResources().getColor(tx.getConfirmations() < nbConfirmations ? R.color.blockchain_green_50 : R.color.blockchain_receive_green));
                }

                TextView tvWatchOnly = (TextView) holder.itemView.findViewById(R.id.watch_only);
                if(tx.isWatchOnly()){
                    tvWatchOnly.setVisibility(View.VISIBLE);
                }else{
                    tvWatchOnly.setVisibility(View.GONE);
                }

                tvResult.setText(span1);

                tvResult.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        FrameLayout parent = (FrameLayout) v.getParent();
                        event.setLocation(v.getX() + (v.getWidth() / 2), v.getY() + (v.getHeight() / 2));
                        parent.onTouchEvent(event);

                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            isBTC = (isBTC) ? false : true;
                            updateBalanceAndTransactionList(null);
                        }
                        return true;
                    }
                });

                txTouchView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        FrameLayout parent = (FrameLayout) v.getParent();
                        event.setLocation(event.getX(), v.getY() + (v.getHeight() / 2));
                        parent.onTouchEvent(event);

                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            onRowClick(holder.itemView, position);
                        }
                        return true;
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            if (transactionList == null) return 0;
            return transactionList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public ViewHolder(View view) {
                super(view);
            }
        }
    }

    public abstract class CollapseActionbarScrollListener extends RecyclerView.OnScrollListener {

        private int mToolbarOffset = 0;

        public CollapseActionbarScrollListener() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            swipeLayout.setEnabled(layoutManager.findFirstCompletelyVisibleItemPosition() == 0);

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

        public AccountAdapter(Context context, int layoutResourceId, ArrayList<String> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
        }

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

    private class TxDateComparator implements Comparator<Tx> {

        public int compare(Tx t1, Tx t2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (t2.getTS() < t1.getTS()) {
                return BEFORE;
            } else if (t2.getTS() > t1.getTS()) {
                return AFTER;
            } else {
                return EQUAL;
            }

        }

    }

}