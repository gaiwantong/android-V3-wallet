package info.blockchain.wallet;

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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.DateUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FloatingActionButton;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;
import info.blockchain.wallet.util.WebUtil;

public class BalanceFragment extends Fragment {

	private Locale locale = null;

	//
	// main balance display
	//
	private TextView tvBalance1 = null;

	private double btc_balance = 0.0;
	private double fiat_balance = 0.0;
	private double btc_fx = 319.13;

	private Spannable span1 = null;
	private final String strBTC = "BTC";
	private String strFiat = null;
	private boolean isBTC = true;

	//
	// accounts list
	//
	private Spinner accountSpinner = null;
	ArrayAdapter<String> accountsAdapter = null;
	private static int spinnerIndex = 0;
	private static int selectedAccount = 0;
	public int balanceBarHeight;

	//
	// tx list
	//
	private HashMap<String,List<Tx>> txMap = null;
	private List<Tx> txs = new ArrayList<Tx>();
	private RecyclerView txList = null;
	private TxAdapter txAdapter = null;
	LinearLayoutManager layoutManager;
	HashMap<View, Boolean> rowViewState = null;
	private LinearLayout noTxMessage = null;

	public static final String ACTION_INTENT = "info.blockchain.wallet.BalanceFragment.REFRESH";

	protected BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (ACTION_INTENT.equals(intent.getAction())) {

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity(), R.string.refresh_balance, Toast.LENGTH_SHORT).show();

						displayBalance();
						accountsAdapter.notifyDataSetChanged();
						updateTx();
						txAdapter.notifyDataSetChanged();
					}
				});

			}
		}
	};

	private SlidingUpPanelLayout mLayout;
	private LinearLayout bottomSel1 = null;
	private LinearLayout bottomSel2 = null;
	private LinearLayout mainContentShadow;
    private static boolean isBottomSheetOpen = false;

    private Activity thisActivity = null;

	public BalanceFragment() { ; }

	Communicator comm;
	ImageButton fab;

	ValueAnimator movingFabUp;
	ValueAnimator movingFabDown;
	float fabTopY;
	float fabBottomY;

	private int originalHeight = 0;
	private int newHeight = 0;
	private int expandDuration = 200;
	private boolean mIsViewExpanded = false;
	private View rootView = null;
	private View prevRowClicked = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(getResources().getLayout(R.layout.balance_layout_oriented), container, false);

		locale = Locale.getDefault();
		thisActivity = getActivity();
		setHasOptionsMenu(true);

		balanceBarHeight = (int)getResources().getDimension(R.dimen.action_bar_height)+35;

		setupViews(rootView);

        return rootView;
	}

	private ArrayList<String> setAccountSpinner() {

		//Account names
		ArrayList<String> accountList = new ArrayList<String>();
		for (Account item : MainActivity.visibleAccountList.values()){
			accountList.add(item.getLabel());
		}

		if(MainActivity.visibleAccountList.size()>1){
			//Multiple accounts - Show spinner
			((ActionBarActivity) thisActivity).getSupportActionBar().setDisplayShowTitleEnabled(false);
			accountSpinner = (Spinner) thisActivity.findViewById(R.id.account_spinner);
			accountSpinner.setVisibility(View.VISIBLE);
		}else{
			//Single account - no spinner
			((ActionBarActivity)thisActivity).getSupportActionBar().setDisplayShowTitleEnabled(true);
			((ActionBarActivity)thisActivity).getSupportActionBar().setTitle(MainActivity.visibleAccountList.get(0).getLabel());
			accountSpinner = (Spinner)thisActivity.findViewById(R.id.account_spinner);
			accountSpinner.setVisibility(View.GONE);
		}

		return accountList;
	}

	@Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            isBottomSheetOpen = false;
        	displayBalance();
        	accountsAdapter.notifyDataSetChanged();
        	txAdapter.notifyDataSetChanged();
        	updateTx();
        }
        else {
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
        
		if(!OSUtil.getInstance(thisActivity).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
			thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.service.WebSocketService.class));
		}
		else {
			thisActivity.stopService(new Intent(thisActivity, info.blockchain.wallet.service.WebSocketService.class));
			thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.service.WebSocketService.class));
		}

    	displayBalance();
    	accountsAdapter.notifyDataSetChanged();
    	txAdapter.notifyDataSetChanged();
    	updateTx();
		setAccountSpinner();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
        LocalBroadcastManager.getInstance(thisActivity).unregisterReceiver(receiver);
    }

	private class TxAdapter extends RecyclerView.Adapter<TxAdapter.ViewHolder> {

		public class ViewHolder extends RecyclerView.ViewHolder  {

			public ViewHolder(View view) {
				super(view);
			}
		}

		@Override
		public TxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			View v = null;

			boolean isTwoPane = getResources().getBoolean(R.bool.isDualPane);
			if(!isTwoPane)v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_expandable, parent, false);
			else v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_simple, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {

			if(txs != null) {
				final Tx tx = txs.get(position);
				double _btc_balance = tx.getAmount() / 1e8;
				double _fiat_balance = btc_fx * _btc_balance;

				TextView tvResult = (TextView)holder.itemView.findViewById(R.id.result);
				tvResult.setTypeface(TypefaceUtil.getInstance(thisActivity).getRobotoTypeface());
				tvResult.setTextColor(Color.WHITE);

				TextView tvTS = (TextView)holder.itemView.findViewById(R.id.ts);
				tvTS.setTypeface(TypefaceUtil.getInstance(thisActivity).getRobotoTypeface());
				tvTS.setText(DateUtil.getInstance(thisActivity).formatted(tx.getTS()));

				TextView tvDirection = (TextView)holder.itemView.findViewById(R.id.direction);
				tvDirection.setTypeface(TypefaceUtil.getInstance(thisActivity).getRobotoTypeface());
				String dirText = tx.getDirection();
				if(dirText.equals(MultiAddrFactory.MOVED))tvDirection.setText(getResources().getString(R.string.MOVED));
				if(dirText.equals(MultiAddrFactory.RECEIVED))tvDirection.setText(getResources().getString(R.string.RECEIVED));
				if(dirText.equals(MultiAddrFactory.SENT))tvDirection.setText(getResources().getString(R.string.SENT));

				if(isBTC) {
					span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(tx.getAmount()) + " " + getDisplayUnits());
					span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - getDisplayUnits().length(), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else	{
					span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(_fiat_balance) + " " + strFiat);
					span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if(tx.isMove()) {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);
					tvDirection.setTextColor(thisActivity.getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue));
				}
				else if(_btc_balance < 0.0) {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);
					tvDirection.setTextColor(thisActivity.getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_red_50 : R.color.blockchain_send_red));
				}
				else {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);
					tvDirection.setTextColor(thisActivity.getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_green_50 : R.color.blockchain_receive_green));
				}

				tvResult.setText(span1);

				tvResult.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {

						FrameLayout parent = (FrameLayout) v.getParent();
						event.setLocation(v.getX()+(v.getWidth()/2), v.getY()+(v.getHeight()/2));
						parent.onTouchEvent(event);

						if (event.getAction() == MotionEvent.ACTION_UP) {
							isBTC = (isBTC) ? false : true;
							displayBalance();
							accountsAdapter.notifyDataSetChanged();
							txAdapter.notifyDataSetChanged();
						}
						return true;
					}
				});

				tvTS.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {

						FrameLayout parent = (FrameLayout) v.getParent();
						event.setLocation(event.getX(), v.getY()+(v.getHeight()/2));
						parent.onTouchEvent(event);

						if (event.getAction() == MotionEvent.ACTION_UP) {
							onRowClick(holder.itemView, position);
						}
						return true;
					}
				});

				tvDirection.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {

						FrameLayout parent = (FrameLayout) v.getParent();
						event.setLocation(event.getX(), v.getY()+(v.getHeight()/2));
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
			if(txs==null)return 0;
			return txs.size();
		}
	}

	private void displayBalance() {

		if(txs!=null && txs.size()>0) {
			txList.setVisibility(View.VISIBLE);
			noTxMessage.setVisibility(View.GONE);
		}else{
			txList.setVisibility(View.GONE);
			noTxMessage.setVisibility(View.VISIBLE);
		}

        strFiat = PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(thisActivity).getLastPrice(strFiat);

        Account hda = null;
        if(spinnerIndex == 0) {
			//All accounts
            btc_balance = ((double)MultiAddrFactory.getInstance().getXpubBalance() / 1e8);
        }
        else {
            hda = MainActivity.visibleAccountList.get(selectedAccount);
            if(hda instanceof ImportedAccount) {
                btc_balance = ((double)MultiAddrFactory.getInstance().getLegacyBalance() / 1e8);
            }
            else {
                btc_balance = ((double)(MultiAddrFactory.getInstance().getXpubAmounts().get(account2Xpub(selectedAccount))) / 1e8);
            }
        }

        fiat_balance = btc_fx * btc_balance;

        if(hda != null && hda instanceof ImportedAccount) {
            span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(MultiAddrFactory.getInstance().getLegacyBalance()) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
        }
        else if(spinnerIndex == 0) {
            span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(MultiAddrFactory.getInstance().getXpubBalance()) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
        }
        else {
            span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(MultiAddrFactory.getInstance().getXpubAmounts().get(account2Xpub(selectedAccount))) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
        }
		span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - (isBTC ? getDisplayUnits().length() : 3), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		tvBalance1.setText(span1);
	}

    private String getDisplayUnits() {

        return (String)MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

    private void updateTx() {

        txMap = MultiAddrFactory.getInstance().getXpubTxs();
		
    	if(MainActivity.visibleAccountList == null || MainActivity.visibleAccountList.size() < 1) {
    		return;
    	}

        if(spinnerIndex == 0) {
            txs = MultiAddrFactory.getInstance().getAllXpubTxs();
        }
        else {
            String xpub = account2Xpub(selectedAccount);

            if(xpub != null) {
                if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    txs = txMap.get(xpub);
                }
            }
            else {
                Account hda = MainActivity.visibleAccountList.get(selectedAccount);
                if(hda instanceof ImportedAccount) {
                    txs = MultiAddrFactory.getInstance().getLegacyTxs();
                }
            }

        }

	}

	private String account2Xpub(int accountIndex) {

		Account hda = MainActivity.visibleAccountList.get(accountIndex);
		String xpub = null;
	    if(hda instanceof ImportedAccount) {
	    	xpub = null;
	    }
	    else {
			xpub = HDPayloadBridge.getInstance(thisActivity).account2Xpub(accountIndex);
	    }
	    
	    return xpub;
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.action_merchant_directory).setVisible(true);
		menu.findItem(R.id.action_qr).setVisible(true);
		menu.findItem(R.id.action_send).setVisible(false);
		menu.findItem(R.id.action_share_receive).setVisible(false);
	}

	private void onAddClicked(){

		fab.bringToFront();

		if(mLayout != null) {
			if (mLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN) {

				//Bottom sheet down
				movingFabDown.start();

				mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
				mainContentShadow.setVisibility(View.GONE);
                isBottomSheetOpen = false;

				comm.setNavigationDrawerToggleEnabled(true);
			} else {

				//Bottom sheet up
				movingFabUp.start();

				mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
				mainContentShadow.setVisibility(View.VISIBLE);
                isBottomSheetOpen = true;

				comm.setNavigationDrawerToggleEnabled(false);
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		comm=(Communicator)activity;
	}

	interface Communicator{

		public void setNavigationDrawerToggleEnabled(boolean enabled);
		public void resetNavigationDrawer();
	}

	public abstract class CollapseActionbarScrollListener extends RecyclerView.OnScrollListener {

		private int mToolbarOffset = 0;

		public CollapseActionbarScrollListener() {
		}

		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

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
			if(mToolbarOffset > balanceBarHeight) {
				mToolbarOffset = balanceBarHeight;
			} else if(mToolbarOffset < 0) {
				mToolbarOffset = 0;
			}
		}

		public abstract void onMoved(int distance);
	}

	private void initFab(final View rootView){

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			fab = (ImageButton) rootView.findViewById(R.id.btActivateBottomSheet);
		else
			fab = (FloatingActionButton) rootView.findViewById(R.id.btActivateBottomSheet);

		rootView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					public void onGlobalLayout() {
						//Remove the listener before proceeding
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
						} else {
							rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						}

						DisplayMetrics displayMetrics = thisActivity.getResources().getDisplayMetrics();

						fabBottomY = fab.getY();
						//56 = fab height
						//48 = row height
						//16 = padding
						int padding = 26;
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
							padding = 18;//shadow 4dp top and bottom - so 8dp here

						fabTopY = fabBottomY + (((56 / 2) + padding) * displayMetrics.density) - ((48 + 48 + 16) * displayMetrics.density);

						//Move up
						movingFabUp = ValueAnimator.ofFloat(fabBottomY, fabTopY);
						movingFabUp.setInterpolator(new AccelerateDecelerateInterpolator());
						movingFabUp.setDuration(200);
						movingFabUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							public void onAnimationUpdate(ValueAnimator animation) {
								Float value = (Float) animation.getAnimatedValue();
								fab.setY(value.floatValue());
								fab.setRotation(45f);
							}
						});

						//move down
						movingFabDown = ValueAnimator.ofFloat(fabTopY, fabBottomY);
						movingFabDown.setInterpolator(new BounceInterpolator());
						movingFabDown.setDuration(500);
						movingFabDown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							public void onAnimationUpdate(ValueAnimator animation) {
								Float value = (Float) animation.getAnimatedValue();
								fab.setY(value.floatValue());
								fab.setRotation(0f);
							}
						});
					}
				});

		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				onAddClicked();
			}
		});
	}

	private class AccountAdapter extends ArrayAdapter<String> {

		Context context;
		int layoutResourceId;
		String data[] = null;

		public AccountAdapter(Context context, int layoutResourceId, String[] data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			if (null == view) {
				view = LayoutInflater.from(this.getContext()).inflate(R.layout.spinner_title_bar, null);
				((TextView) view).setText(getItem(position));
			}
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
			view.setLayoutParams(params);
			return view;
		}
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

	private void setupViews(View rootView){

		rootView.setFilterTouchesWhenObscured(true);

		initFab(rootView);

		noTxMessage = (LinearLayout)rootView.findViewById(R.id.no_tx_message);
		noTxMessage.setVisibility(View.GONE);

		tvBalance1 = (TextView)rootView.findViewById(R.id.balance1);
		tvBalance1.setTypeface(TypefaceUtil.getInstance(thisActivity).getRobotoTypeface());

		tvBalance1.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				isBTC = (isBTC) ? false : true;
				displayBalance();
				accountsAdapter.notifyDataSetChanged();
				txAdapter.notifyDataSetChanged();
				return false;
			}
		});

		ArrayList<String> accountList = setAccountSpinner();
		accountsAdapter = new AccountAdapter(thisActivity, R.layout.spinner_title_bar, accountList.toArray(new String[0]));
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
						int position = accountSpinner.getSelectedItemPosition();

						spinnerIndex = position;
						if(spinnerIndex>0)
							selectedAccount = MainActivity.accountIndexResolver.get(spinnerIndex-1);
						else
							selectedAccount = -1;

						if (MainActivity.visibleAccountList == null || MainActivity.visibleAccountList.size() < 1) {
							return;
						}

						if (spinnerIndex == 0) {
							//All accounts
							txs = MultiAddrFactory.getInstance().getAllXpubTxs();
						} else {
							String xpub = account2Xpub(selectedAccount);

							if (xpub != null) {
								if (MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
									txs = txMap.get(xpub);
								}
							} else {
								Account hda = MainActivity.visibleAccountList.get(selectedAccount);
								if (hda instanceof ImportedAccount) {
									txs = MultiAddrFactory.getInstance().getLegacyTxs();
								}
							}

						}

						displayBalance();

						txAdapter.notifyDataSetChanged();
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						;
					}
				});
			}
		});
		accountSpinner.setSelection(spinnerIndex);

		txList = (RecyclerView)rootView.findViewById(R.id.txList2);
		txAdapter = new TxAdapter();
		layoutManager = new LinearLayoutManager(thisActivity);
		txList.setLayoutManager(layoutManager);
		txList.setAdapter(txAdapter);

		if(!getResources().getBoolean(R.bool.isDualPane))
			txList.setOnScrollListener(new CollapseActionbarScrollListener() {
				@Override
				public void onMoved(int distance) {

					tvBalance1.setTranslationY(-distance);
				}
			});

		displayBalance();
		updateTx();

		// drawerTitle account now that wallet has been created
		if(PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, "").length() > 0) {
			PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(PrefsUtil.getInstance(thisActivity).getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, ""));
			PrefsUtil.getInstance(thisActivity).removeValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME);
			PayloadFactory.getInstance(thisActivity).remoteSaveThread();
			accountsAdapter.notifyDataSetChanged();
		}

		if(!OSUtil.getInstance(thisActivity).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
			thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.service.WebSocketService.class));
		}
		else {
			thisActivity.stopService(new Intent(thisActivity, info.blockchain.wallet.service.WebSocketService.class));
			thisActivity.startService(new Intent(thisActivity, info.blockchain.wallet.service.WebSocketService.class));
		}

		mLayout = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
		mLayout.setTouchEnabled(false);
		mLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
		mLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
			@Override
			public void onPanelSlide(View panel, float slideOffset) {
			}

			@Override
			public void onPanelExpanded(View panel) {

			}

			@Override
			public void onPanelCollapsed(View panel) {

			}

			@Override
			public void onPanelAnchored(View panel) {
			}

			@Override
			public void onPanelHidden(View panel) {
			}
		});
		bottomSel1 = ((LinearLayout)rootView.findViewById(R.id.bottom_sel1));
		bottomSel1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = new SendFragment();
//				Bundle args = new Bundle();
//				args.putInt("selected_account", spinnerIndex == 0 ? 0 : selectedAccount);
//				fragment.setArguments(args);
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
				comm.setNavigationDrawerToggleEnabled(true);

			}
		});
		bottomSel2 = ((LinearLayout)rootView.findViewById(R.id.bottom_sel2));
		bottomSel2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = new ReceiveFragment();
//				Bundle args = new Bundle();
//				args.putInt("selected_account", spinnerIndex == 0 ? 0 : selectedAccount);
//				fragment.setArguments(args);
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
				comm.setNavigationDrawerToggleEnabled(true);
			}
		});

		mainContentShadow = (LinearLayout)rootView.findViewById(R.id.balance_main_content_shadow);
		mainContentShadow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)){
					onAddClicked();
				}
			}
		});

		rowViewState = new HashMap<View, Boolean>();

		noTxMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation bounce = AnimationUtils.loadAnimation(getActivity(), R.anim.jump);
				fab.startAnimation(bounce);
			}
		});
	}

	private void onRowClick(final View view, final int position){
		if(txs != null) {
			final Tx tx = txs.get(position);
			final String strTx = tx.getHash();
			final String strConfirmations = Long.toString(tx.getConfirmations());

			try {
				mIsViewExpanded = rowViewState.get(view);
			} catch (Exception e) {
				mIsViewExpanded = false;
			}

			//Set views
			View detailsView = view;
			if(getResources().getBoolean(R.bool.isDualPane))
				detailsView = rootView;

			final LinearLayout txsDetails = (LinearLayout) detailsView.findViewById(R.id.txs_details);
			final TextView tvOutAddr = (TextView) detailsView.findViewById(R.id.tx_from_addr);
			final TextView tvToAddr = (TextView) detailsView.findViewById(R.id.tx_to_addr);
			final TextView tvConfirmations = (TextView) detailsView.findViewById(R.id.tx_confirmations);
			final TextView tvFee = (TextView) detailsView.findViewById(R.id.tx_fee_value);
			final TextView tvTxHash = (TextView) detailsView.findViewById(R.id.tx_hash);
			final ProgressBar progressView = (ProgressBar)detailsView.findViewById(R.id.progress_view);

			if(getResources().getBoolean(R.bool.isDualPane) || (!getResources().getBoolean(R.bool.isDualPane) && !mIsViewExpanded)) {
				if(prevRowClicked!=null && prevRowClicked==txList.getLayoutManager().getChildAt(position)){
					txsDetails.setVisibility(View.INVISIBLE);
					prevRowClicked.findViewById(R.id.tx_row).setBackgroundResource(R.drawable.selector_pearl_white_tx);
					prevRowClicked = null;
					return;
				}

				txsDetails.setVisibility(View.VISIBLE);
				progressView.setVisibility(View.VISIBLE);
				tvOutAddr.setVisibility(View.INVISIBLE);
				tvToAddr.setVisibility(View.INVISIBLE);

				tvTxHash.setText(strTx);
				tvTxHash.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {

						if (event.getAction() == MotionEvent.ACTION_UP) {
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + strTx));
							startActivity(browserIntent);
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
							displayBalance();
							accountsAdapter.notifyDataSetChanged();
							txAdapter.notifyDataSetChanged();
						}
						return true;
					}
				});

				if(!getResources().getBoolean(R.bool.isDualPane))
					txsDetails.setOnTouchListener(new OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {

							if (event.getAction() == MotionEvent.ACTION_UP) {
								onRowClick(view, position);
							}
							return true;
						}
					});

				//Get Details
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
								transaction = new Transaction(new JSONObject(stringResult));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							progressView.setVisibility(View.GONE);

							String fee = (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(btc_fx * (transaction.getFee() / 1e8)) + " " + strFiat);
							if (isBTC)
								fee = (MonetaryUtil.getInstance(thisActivity).getDisplayAmountWithFormatting(transaction.getFee()) + " " + getDisplayUnits());
							tvFee.setText(fee);

							//Receive Address
							StringBuilder fromAddress = new StringBuilder("");
							if (tx.getDirection().equals(MultiAddrFactory.RECEIVED))//only 1 addr for receive
								fromAddress.append(transaction.getInputs().get(0).addr);
							else
								for (Transaction.xPut ip : transaction.getInputs()) {
									if (MultiAddrFactory.getInstance().isOwnHDAddress(ip.addr)) {

										HashMap<String, String> xpub = MultiAddrFactory.getInstance().getAddress2Xpub();
										Map<String, Integer> xpubAcc = PayloadFactory.getInstance().get().getXpub2Account();
										int accIndex = xpubAcc.get(xpub.get(ip.addr));

										List<Account> acc = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
										if (acc != null) {
											if(fromAddress.toString().contains(acc.get(accIndex).getLabel()))
												continue;

											if (!fromAddress.toString().isEmpty()) fromAddress.append("\n");

											fromAddress.append(acc.get(accIndex).getLabel());
										}else
											fromAddress.append(ip.addr);
									} else
										fromAddress.append(ip.addr);
								}
							tvOutAddr.setText(fromAddress);

							//To Address
							StringBuilder toAddress = new StringBuilder("");
							for (Transaction.xPut ip : transaction.getOutputs()) {
								if (MultiAddrFactory.getInstance().isOwnHDAddress(ip.addr)) {
									if (tx.getDirection().equals(MultiAddrFactory.SENT))
										continue;//change addr
									if (tx.getDirection().equals(MultiAddrFactory.MOVED) && tx.getAmount() != (double) ip.value)
										continue;//change addr



									HashMap<String, String> xpub = MultiAddrFactory.getInstance().getAddress2Xpub();
									Map<String, Integer> xpubAcc = PayloadFactory.getInstance().get().getXpub2Account();
									int accIndex = xpubAcc.get(xpub.get(ip.addr));
									List<Account> acc = PayloadFactory.getInstance().get().getHdWallet().getAccounts();

									if (acc != null) {
										if (!toAddress.toString().isEmpty()){
											if(toAddress.toString().contains(acc.get(accIndex).getLabel()))
												continue;
											toAddress.append("\n");
										}

										toAddress.append(acc.get(accIndex).getLabel());
									}else {
										toAddress.append("\n");
										toAddress.append(ip.addr);
									}

								} else {
									if (tx.getDirection().equals(MultiAddrFactory.RECEIVED))
										continue;

									if (!toAddress.toString().isEmpty()) toAddress.append("\n");
									toAddress.append(ip.addr);
								}
							}
							tvToAddr.setText(toAddress);

							tvConfirmations.setText(strConfirmations);

							tvOutAddr.setVisibility(View.VISIBLE);
							tvToAddr.setVisibility(View.VISIBLE);
						}
					}
				}.execute();
			}

			//Single Pane View - Expand and collapse details
			if(!getResources().getBoolean(R.bool.isDualPane)) {
				if (originalHeight == 0) {
					originalHeight = view.getHeight();
				}

				newHeight = originalHeight + txsDetails.getHeight();

				ValueAnimator resizeAnimator;
				if (!mIsViewExpanded) {
					//Expanding
					view.setBackgroundColor(getResources().getColor(R.color.white));

					//Fade Details in - expansion of row will create slide down effect
					txsDetails.setVisibility(View.VISIBLE);
					txsDetails.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in));
					txsDetails.setEnabled(true);

					mIsViewExpanded = !mIsViewExpanded;
					resizeAnimator = ValueAnimator.ofInt(originalHeight, newHeight);

				} else {
					//Collapsing
					TypedValue outValue = new TypedValue();
					getActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
					view.setBackgroundResource(outValue.resourceId);

					mIsViewExpanded = !mIsViewExpanded;
					resizeAnimator = ValueAnimator.ofInt(newHeight, originalHeight);

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
				}

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

				rowViewState.put(view, mIsViewExpanded);
			}else
			{
				//Dual Pane View
				view.findViewById(R.id.tx_row).setBackgroundResource(R.color.blockchain_light_grey);

				if(prevRowClicked!=null)
					prevRowClicked.findViewById(R.id.tx_row).setBackgroundResource(R.drawable.selector_pearl_white_tx);

				prevRowClicked = view;
			}
		}
	}
}