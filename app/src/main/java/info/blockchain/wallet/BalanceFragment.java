package info.blockchain.wallet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.DateUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FloatingActionButton;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;

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
	private List<Account> accounts = null;
	private Spinner accountSpinner = null;
	ArrayAdapter<String> accountsAdapter = null;
	private static int selectedAccount = 0;
	public int toolbarHeight;

	//
	// tx list
	//
	private HashMap<String,List<Tx>> txMap = null;
	private List<Tx> txs = new ArrayList<Tx>();
	private RecyclerView txList = null;
	private TxAdapter txAdapter = null;
	LinearLayoutManager layoutManager;

	public static final String ACTION_INTENT = "info.blockchain.wallet.BalanceFragment.REFRESH";

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        	if(ACTION_INTENT.equals(intent.getAction())) {

    	    	getActivity().runOnUiThread(new Runnable() {
   	    	     @Override
   	    	     public void run() {
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
	private FrameLayout mainContent;
	private LinearLayout mainContentShadow;
    private static boolean isBottomSheetOpen = false;

	public BalanceFragment() { ; }

	Communicator comm;
	ImageButton fab;

	ValueAnimator movingFabUp;
	ValueAnimator movingFabDown;
	float fabTopY;
	float fabBottomY;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_balance, container, false);

		locale = Locale.getDefault();

		setHasOptionsMenu(true);
		((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
		accountSpinner = (Spinner)getActivity().findViewById(R.id.account_spinner);
		accountSpinner.setVisibility(View.VISIBLE);

		initFab(rootView);

		toolbarHeight = (int)getResources().getDimension(R.dimen.action_bar_height)+35;

		tvBalance1 = (TextView)rootView.findViewById(R.id.balance1);
		tvBalance1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());

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

        accounts = PayloadFactory.getInstance().get().getHdWallet().getAccounts();
        if(accounts != null && accounts.size() > 0 && !(accounts.get(accounts.size() - 1) instanceof ImportedAccount) && (PayloadFactory.getInstance().get().getLegacyAddresses().size() > 0)) {
        	ImportedAccount iAccount = new ImportedAccount(getString(R.string.imported_addresses), PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        	accounts.add(iAccount);
        }

		ArrayList<String> accountList = new ArrayList<String>();
        accountList.add(getActivity().getResources().getString(R.string.all_accounts));
		for(Account item : accounts)accountList.add(item.getLabel());

		accountsAdapter = new ArrayAdapter<String>(getActivity(),R.layout.spinner_title_bar, accountList.toArray(new String[0]));
		accountsAdapter.setDropDownViewResource(R.layout.spinner_title_bar_dropdown);
		accountSpinner.setAdapter(accountsAdapter);
        accountSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP && MainActivity.drawerIsOpen) {
                    return true;
                }
                else if(isBottomSheetOpen) {
                    return true;
                }
                else {
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

						selectedAccount = position;

						if(accounts == null || accounts.size() < 1) {
							return;
						}

                        if(selectedAccount == 0) {
                            txs = MultiAddrFactory.getInstance().getAllXpubTxs();
                        }
                        else {
                            String xpub = account2Xpub(selectedAccount - 1);

                            if(xpub != null) {
                                if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                                    txs = txMap.get(xpub);
                                }
                            }
                            else {
                                Account hda = accounts.get(selectedAccount - 1);
                                if(hda instanceof ImportedAccount) {
                                    txs = MultiAddrFactory.getInstance().getLegacyTxs();
                                }
                            }

                        }

                        displayBalance();

//						txAdapter.notifyDataSetInvalidated();
						txAdapter.notifyDataSetChanged();
					}
					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						;
					}
				});
			}
		});
		accountSpinner.setSelection(selectedAccount);

		txList = (RecyclerView)rootView.findViewById(R.id.txList2);
		txAdapter = new TxAdapter();
		layoutManager = new LinearLayoutManager(getActivity());
		txList.setLayoutManager(layoutManager);
		txList.setAdapter(txAdapter);

//		txList.addOnItemTouchListener(
//				new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
//
//					@Override
//					public void onItemClick(final View view, int position) {
//					//TODO add tx row onclicks in here
//					}
//				})
//		);

		txList.setOnScrollListener(new CollapseActionbarScrollListener() {
			@Override
			public void onMoved(int distance) {

				tvBalance1.setTranslationY(-distance);
			}
		});

        displayBalance();
        updateTx();

        // drawerTitle account now that wallet has been created
        if(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, "").length() > 0) {
    		PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME, ""));
    		PrefsUtil.getInstance(getActivity()).removeValue(PrefsUtil.KEY_INITIAL_ACCOUNT_NAME);
    		PayloadFactory.getInstance(getActivity()).remoteSaveThread();
        	accountsAdapter.notifyDataSetChanged();
        }
        
		if(!OSUtil.getInstance(getActivity()).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
			getActivity().startService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
		}
		else {
			getActivity().stopService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
			getActivity().startService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
		}

		mLayout = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
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
				Bundle args = new Bundle();
				args.putInt("selected_account", selectedAccount == 0 ? 0 : selectedAccount - 1);
				fragment.setArguments(args);
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();

			}
		});
		bottomSel2 = ((LinearLayout)rootView.findViewById(R.id.bottom_sel2));
		bottomSel2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = new ReceiveFragment();
				Bundle args = new Bundle();
				args.putInt("selected_account", selectedAccount == 0 ? 0 : selectedAccount - 1);
				fragment.setArguments(args);
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit();
			}
		});

		mainContent = (FrameLayout)rootView.findViewById(R.id.balance_main_content);
		mainContentShadow = (LinearLayout)rootView.findViewById(R.id.balance_main_content_shadow);
		mainContentShadow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.COLLAPSED)){
					onAddClicked();
				}
			}
		});

        return rootView;
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

		setNavigationDrawer();

        isBottomSheetOpen = false;

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
        
		if(!OSUtil.getInstance(getActivity()).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
			getActivity().startService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
		}
		else {
			getActivity().stopService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
			getActivity().startService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
		}

    	displayBalance();
    	accountsAdapter.notifyDataSetChanged();
    	txAdapter.notifyDataSetChanged();
    	updateTx();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

	private class TxAdapter extends RecyclerView.Adapter<TxAdapter.ViewHolder> {

		public class ViewHolder extends RecyclerView.ViewHolder  {

			public ViewHolder(View view) {
				super(view);
			}
		}

		@Override
		public TxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.txs_layout_simple2, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {

			if(txs != null) {
				final Tx tx = txs.get(position);
				double _btc_balance = tx.getAmount() / 1e8;
				double _fiat_balance = btc_fx * _btc_balance;

				TextView tvResult = (TextView)holder.itemView.findViewById(R.id.result);
				tvResult.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				tvResult.setTextColor(Color.WHITE);

				TextView tvTS = (TextView)holder.itemView.findViewById(R.id.ts);
				tvTS.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				tvTS.setText(DateUtil.getInstance(getActivity()).formatted(tx.getTS()));

				TextView tvDirection = (TextView)holder.itemView.findViewById(R.id.direction);
				tvDirection.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				tvDirection.setText(tx.getDirection());

				if(isBTC) {
					span1 = Spannable.Factory.getInstance().newSpannable(getDisplayAmount(tx.getAmount()) + " " + getDisplayUnits());
					span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - getDisplayUnits().length(), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				else	{
					span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(_fiat_balance) + " " + strFiat);
					span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if(tx.isMove()) {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);
					tvDirection.setTextColor(getActivity().getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_transfer_blue_50 : R.color.blockchain_transfer_blue));
				}
				else if(_btc_balance < 0.0) {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);
					tvDirection.setTextColor(getActivity().getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_red_50 : R.color.blockchain_send_red));
				}
				else {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);
					tvDirection.setTextColor(getActivity().getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_green_50 : R.color.blockchain_receive_green));
				}

				tvResult.setText(span1);

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

				tvTS.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {

						if (event.getAction() == MotionEvent.ACTION_UP) {
							String strTx = tx.getHash();
							if (strTx != null) {
								Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + strTx));
								startActivity(browserIntent);
							}
						}
						return true;
					}
				});

				tvDirection.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {

						if (event.getAction() == MotionEvent.ACTION_UP) {
							String strTx = tx.getHash();
							if (strTx != null) {
								Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + strTx));
								startActivity(browserIntent);
							}
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
        strFiat = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

        Account hda = null;
        if(selectedAccount == 0) {
            btc_balance = ((double)MultiAddrFactory.getInstance().getXpubBalance() / 1e8);
        }
        else {
            hda = accounts.get(selectedAccount - 1);
            if(hda instanceof ImportedAccount) {
                btc_balance = ((double)MultiAddrFactory.getInstance().getLegacyBalance() / 1e8);
            }
            else {
                btc_balance = ((double)(MultiAddrFactory.getInstance().getXpubAmounts().get(account2Xpub(selectedAccount - 1))) / 1e8);
            }
        }

        fiat_balance = btc_fx * btc_balance;

        if(hda != null && hda instanceof ImportedAccount) {
            span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (getDisplayAmount(MultiAddrFactory.getInstance().getLegacyBalance()) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
        }
        else if(selectedAccount == 0) {
            span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (getDisplayAmount(MultiAddrFactory.getInstance().getXpubBalance()) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
        }
        else {
            span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (getDisplayAmount(MultiAddrFactory.getInstance().getXpubAmounts().get(account2Xpub(selectedAccount - 1))) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
        }
		span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - (isBTC ? getDisplayUnits().length() : 3), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		tvBalance1.setText(span1);
	}

    private String getDisplayAmount(long value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double)(value * 1000000L)) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double)(value * 1000L)) / 1e8);
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    private String getDisplayAmount(double value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format((value * 1000000.0) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format((value * 1000.0) / 1e8);
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    private String getDisplayUnits() {

        return (String)MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

    private void updateTx() {

        txMap = MultiAddrFactory.getInstance().getXpubTxs();
		
    	if(accounts == null || accounts.size() < 1) {
    		return;
    	}

        if(selectedAccount == 0) {
            txs = MultiAddrFactory.getInstance().getAllXpubTxs();
        }
        else {
            String xpub = account2Xpub(selectedAccount - 1);

            if(xpub != null) {
                if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
                    txs = txMap.get(xpub);
                }
            }
            else {
                Account hda = accounts.get(selectedAccount - 1);
                if(hda instanceof ImportedAccount) {
                    txs = MultiAddrFactory.getInstance().getLegacyTxs();
                }
            }

        }

	}

	private String account2Xpub(int accountIndex) {

		Account hda = accounts.get(accountIndex);
		String xpub = null;
	    if(hda instanceof ImportedAccount) {
	    	xpub = null;
	    }
	    else {
			xpub = HDPayloadBridge.getInstance(getActivity()).account2Xpub(accountIndex);
	    }
	    
	    return xpub;
	}

	private String getAccountLabel() {
		String ret = null;
        Account hda = accounts.get(selectedAccount);
        if(hda instanceof ImportedAccount) {
        	ret = getString(R.string.imported_addresses);
        }
        if(hda.getLabel() != null && hda.getLabel().length() > 0) {
        	ret = hda.getLabel();
        }
        else {
        	ret = getString(R.string.account_colon) + selectedAccount;
        }
        
        return ret;
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
			} else {

				//Bottom sheet up
				movingFabUp.start();

				mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
				mainContentShadow.setVisibility(View.VISIBLE);
                isBottomSheetOpen = true;
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		comm=(Communicator)activity;
	}

	interface Communicator{

		public void setNavigationDrawer();
	}

	private void setNavigationDrawer(){
		comm.setNavigationDrawer();
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

				if ((mToolbarOffset < toolbarHeight && dy > 0) || (mToolbarOffset > 0 && dy < 0)) {
					mToolbarOffset += dy;
				}

				clipToolbarOffset();
				onMoved(mToolbarOffset);
			}
		}

		private void clipToolbarOffset() {
			if(mToolbarOffset > toolbarHeight) {
				mToolbarOffset = toolbarHeight;
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

						DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();

						fabBottomY = fab.getY();
						//56 = fab height
						//48 = row height
						//16 = padding
						int padding = 16;
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)padding = 8;//shadow 4dp top and bottom - so 8dp here

						fabTopY = fabBottomY + (((56/2)+padding)*displayMetrics.density) - ((48+48+16)*displayMetrics.density);

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
}