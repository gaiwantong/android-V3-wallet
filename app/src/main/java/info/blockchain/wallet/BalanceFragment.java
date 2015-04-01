package info.blockchain.wallet;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
//import android.widget.ImageView;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
//import android.text.TextUtils;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
//import android.view.animation.RotateAnimation;
//import android.view.animation.TranslateAnimation;
import android.net.Uri;
import android.util.Log;

import com.google.bitcoin.crypto.MnemonicException;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.Tx;
import info.blockchain.wallet.util.DateUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class BalanceFragment extends Fragment {

	private Locale locale = null;

	//
	// main balance display
	//
	private TextView tvBalance0 = null;				// header, not used in main display
	private TextView tvBalance1 = null;
	private TextView tvBalance2 = null;

    private LinearLayout layoutBalance = null;
    private LinearLayout layoutAccounts = null;
//    private LinearLayout layoutIcons = null;
    
	private Animation slideUp = null;
	private Animation slideDown = null;

	private ImageView ivReceive = null;
	private ImageView ivHome = null;
	private ImageView ivSend = null;

	private TextView tvSwipe = null;

    private LinearLayout layoutReceive = null;
    private LinearLayout layoutHome = null;
    private LinearLayout layoutSend = null;
    
    private LinearLayout layoutReceiveIcon = null;
    private LinearLayout layoutHomeIcon = null;
    private LinearLayout layoutSendIcon = null;
    
    private LinearLayout layoutAnchor = null;

	private double btc_balance = 0.0;
	private double fiat_balance = 0.0;
	private double btc_fx = 319.13;

	private Spannable span1 = null;
	private Spannable span2 = null;
	private final String strBTC = "BTC";
	private String strFiat = null;
	private boolean isBTC = true;

	//
	// accounts list
	//
	private List<Account> accounts = null;
	private ListView accountsList = null;
	private AccountAdapter accountsAdapter = null;
	private static int selectedAccount = 0;

	//
	// tx list
	//
	private HashMap<String,List<Tx>> txMap = null;
	private List<Tx> txs = new ArrayList<Tx>();
	private ListView txList = null;
	private TransactionAdapter txAdapter = null;

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

	public BalanceFragment() { ; }
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_balance, container, false);

		locale = Locale.getDefault();

		((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.blockchain);

        layoutAnchor = (LinearLayout)rootView.findViewById(R.id.anchor);

        layoutReceive = (LinearLayout)rootView.findViewById(R.id.iconsReceive2);
        layoutHome = (LinearLayout)rootView.findViewById(R.id.iconsHome2);
        layoutSend = (LinearLayout)rootView.findViewById(R.id.iconsSend2);

        ivReceive = (ImageView)rootView.findViewById(R.id.view_receive);
        ivHome = (ImageView)rootView.findViewById(R.id.view_home);
        ivSend = (ImageView)rootView.findViewById(R.id.view_send);

        tvSwipe = (TextView)rootView.findViewById(R.id.swipe);
        tvSwipe.setTypeface(TypefaceUtil.getInstance(getActivity()).getAwesomeTypeface());
        tvSwipe.setText(Character.toString((char)TypefaceUtil.awesome_angle_double_up) + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));

        layoutReceiveIcon = (LinearLayout)rootView.findViewById(R.id.view_receive1);
        layoutHomeIcon = (LinearLayout)rootView.findViewById(R.id.view_home1);
        layoutSendIcon = (LinearLayout)rootView.findViewById(R.id.view_send1);

        layoutReceiveIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));

        		Fragment fragment = new ReceiveFragment2();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        layoutHomeIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	return false;
            }
        });

        layoutSendIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	
        		Fragment fragment = new SendFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        ivReceive.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));

            	return false;
            }
        });

        ivHome.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

            	return false;
            }
        });

        ivSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	layoutReceive.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutHome.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_blue));
            	layoutSend.setBackgroundColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));

                return false;
            }
        });

        layoutBalance = (LinearLayout)rootView.findViewById(R.id.balanceLayout);
        layoutAccounts = (LinearLayout)rootView.findViewById(R.id.accountsLayout);
//        layoutIcons = (LinearLayout)rootView.findViewById(R.id.iconsLayout);

        layoutAccounts.setVisibility(View.GONE);

        slideUp = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up2);
        slideDown = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down1);
        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Called when the Animation starts
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Called when the Animation ended
                // Since we are fading a View out we set the visibility
                // to GONE once the Animation is finished
//                view.setVisibility(View.GONE);
		        layoutAccounts.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // This is called each time the Animation repeats
            }
        });

        layoutAnchor.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
		    public void onSwipeTop() {
		        if(layoutAccounts.getVisibility() == View.VISIBLE) {
		        	layoutAccounts.setVisibility(View.GONE);
		        	//layoutAnchor.startAnimation(slideUp);
			        tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_up) + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));
			        PrefsUtil.getInstance(getActivity()).setValue("BalanceDropDownState", 1);
		        }
		        else if(layoutBalance.getVisibility() == View.VISIBLE) {
		        	layoutBalance.setVisibility(View.GONE);
		            tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));
			        PrefsUtil.getInstance(getActivity()).setValue("BalanceDropDownState", 3);
		        }
		        else {
		        	;
		        }
		    }

		    public void onSwipeBottom() {
		        if(layoutBalance.getVisibility() == View.GONE) {
			        layoutBalance.setVisibility(View.VISIBLE);
			        tvSwipe.setText(getAccountLabel() + "\n" +  Character.toString((char)TypefaceUtil.awesome_angle_double_up) + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));
			        PrefsUtil.getInstance(getActivity()).setValue("BalanceDropDownState", 1);
		        }
		        else if(layoutAccounts.getVisibility() == View.GONE) {
			        layoutAccounts.setVisibility(View.VISIBLE);
			        tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_up));
			        PrefsUtil.getInstance(getActivity()).setValue("BalanceDropDownState", 2);
		        }
		        else {
		        	;
		        }
		    }
		});

		tvBalance0 = (TextView)rootView.findViewById(R.id.header);
		tvBalance0.setVisibility(View.GONE);
		tvBalance1 = (TextView)rootView.findViewById(R.id.balance1);
		tvBalance1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
		tvBalance2 = (TextView)rootView.findViewById(R.id.balance2);
		tvBalance2.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());

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

		tvBalance2.setOnTouchListener(new OnTouchListener() {
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
        	ImportedAccount iAccount = new ImportedAccount("Imported addresses", PayloadFactory.getInstance().get().getLegacyAddresses(), new ArrayList<String>(), MultiAddrFactory.getInstance().getLegacyBalance());
        	accounts.add(iAccount);
        }
        accountsList = (ListView)rootView.findViewById(R.id.accountsList);
        accountsList.getLayoutParams().height = 600;
        accountsAdapter = new AccountAdapter();
        accountsList.setAdapter(accountsAdapter);
        accountsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

        	        Account hda = accounts.get(position);
        	        if(hda instanceof ImportedAccount) {
    		        	Toast.makeText(getActivity(), getString(R.string.cannot_edit_imported_label), Toast.LENGTH_SHORT).show();
    		        	return false;
        	        }

            		final EditText label = new EditText(getActivity());
            		if(hda.getLabel() != null && hda.getLabel().length() > 0) {
            			label.setText(hda.getFullLabel());
            		}

            		new AlertDialog.Builder(getActivity())
            	    .setTitle(R.string.app_name)
            	    .setMessage(R.string.edit_label)
            	    .setView(label)
            	    .setCancelable(false)
            	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int whichButton) {

            	        	final String strLabel = label.getText().toString();
            	        	PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(position).setLabel(strLabel);
            	        	PayloadFactory.getInstance(getActivity()).remoteSaveThread();
                        	accountsAdapter.notifyDataSetChanged();
                        	updateDropDownAnchor();
            	        }
            	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int whichButton) {
            	        	;
            	        }
            	    }).show();

                return true;       
            }
        });
        accountsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

            	selectedAccount = position;
//		        Log.i("account2Xpub", "position:" + selectedAccount);
            	
            	if(accounts == null || accounts.size() < 1) {
            		return;
            	}

                String xpub = account2Xpub(selectedAccount);
//		        Log.i("account2Xpub", xpub);

                if(xpub != null) {
        			if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
        		        txs = txMap.get(xpub);
//        		        Log.i("account2Xpub", "M:" + txs.size());
        			}
                }
                else {
//    		        Log.i("account2Xpub", "xpub is null");
        	        Account hda = accounts.get(selectedAccount);
        	        if(hda instanceof ImportedAccount) {
        	            txs = MultiAddrFactory.getInstance().getLegacyTxs();
//        		        Log.i("account2Xpub", "I:" + txs.size());
        	        }
                }

		        tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_up) + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));

                txAdapter.notifyDataSetInvalidated();
            }

        });

        txList = (ListView)rootView.findViewById(R.id.txList);
        txAdapter = new TransactionAdapter();
        txList.setAdapter(txAdapter);
        /*
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            	Tx tx = txs.get(position);
            	String strTx = tx.getHash();
            	if(strTx != null) {
            		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + strTx));
            		startActivity(browserIntent);            		
            	}
            }
        });
        */

		displayBalance();
        updateTx();

        if(PrefsUtil.getInstance(getActivity()).getValue("_1ST_ACCOUNT_NAME", "").length() > 0) {
    		PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).setLabel(PrefsUtil.getInstance(getActivity()).getValue("_1ST_ACCOUNT_NAME", ""));
    		PrefsUtil.getInstance(getActivity()).removeValue("_1ST_ACCOUNT_NAME");
    		PayloadFactory.getInstance(getActivity()).remoteSaveThread();
        	accountsAdapter.notifyDataSetChanged();
        }
        
        updateDropDownAnchor();
        
		if(!OSUtil.getInstance(getActivity()).isServiceRunning(info.blockchain.wallet.service.WebSocketService.class)) {
			getActivity().startService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
		}
		else {
			getActivity().stopService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
			getActivity().startService(new Intent(getActivity(), info.blockchain.wallet.service.WebSocketService.class));
		}

        /*
         *
         *
             Bottom sheet implementation: make temporarily invisible until FAB is worked into balance screen
         *
         *
         */
        ((LinearLayout)rootView.findViewById(R.id.panel)).setVisibility(View.INVISIBLE);

        return rootView;
	}

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
        	displayBalance();
        	accountsAdapter.notifyDataSetChanged();
//        	txAdapter.notifyDataSetChanged();
        	updateTx();
        }
        else {
        	;
        }
    }

    @Override
    public void onResume() {
    	super.onResume();

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

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

    private class TransactionAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    TransactionAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if(txs == null) {
				txs = new ArrayList<Tx>();
			}
			return txs.size();
		}

		@Override
		public String getItem(int position) {
			if(txs == null) {
				txs = new ArrayList<Tx>();
			}
			return txs.get(position).toString();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = null;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.txs_layout_simple, parent, false);
	        } else {
	            view = convertView;
	        }
	        
	        if(txs != null) {
		        final Tx tx = txs.get(position);
		        double _btc_balance = tx.getAmount() / 1e8;
		    	double _fiat_balance = btc_fx * _btc_balance;
		    	
		    	TextView tvResult = (TextView)view.findViewById(R.id.result);
				tvResult.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				tvResult.setTextColor(Color.WHITE);

		    	TextView tvTS = (TextView)view.findViewById(R.id.ts);
				tvTS.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				tvTS.setTextColor(0xffadc0c9);
				tvTS.setText(DateUtil.getInstance(getActivity()).formatted(tx.getTS()));
				
		    	TextView tvDirection = (TextView)view.findViewById(R.id.direction);
				tvDirection.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
//				tvDirection.setTextColor(0xff828181);
				tvDirection.setText(tx.getDirection());

				/*
	    		Spannable msg = Spannable.Factory.getInstance().newSpannable(tx.getDirection());
	    		msg.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    		msg.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), msg.length() - 15, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tvDirection.setText(msg);
				*/

		    	TextView tvNote = (TextView)view.findViewById(R.id.note);
				tvNote.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				if(PayloadFactory.getInstance().get().getNotes().get(tx.getHash()) != null) {
					tvNote.setVisibility(View.VISIBLE);
					tvNote.setText(PayloadFactory.getInstance().get().getNotes().get(tx.getHash()));
				}
				else {
					tvNote.setVisibility(View.INVISIBLE);
				}

		        if(isBTC) {
//                    span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance().getBTCFormat().format(btc_balance) + " " + strBTC);
                    span1 = Spannable.Factory.getInstance().newSpannable(getDisplayAmount(tx.getAmount()) + " " + getDisplayUnits());
                    span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - getDisplayUnits().length(), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }
		        else	{
					span1 = Spannable.Factory.getInstance().newSpannable(MonetaryUtil.getInstance().getFiatFormat(strFiat).format(_fiat_balance) + " " + strFiat);
                    span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        }
				if(tx.isMove()) {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_lighter_blue_50 : R.drawable.rounded_view_lighter_blue);
					tvDirection.setTextColor(getActivity().getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_lighter_blue_50 : R.color.blockchain_lighter_blue));
				}
				else if(_btc_balance < 0.0) {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_red_50 : R.drawable.rounded_view_red);
					tvDirection.setTextColor(getActivity().getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_red_50 : R.color.blockchain_red));
				}
				else {
					tvResult.setBackgroundResource(tx.getConfirmations() < 3 ? R.drawable.rounded_view_green_50 : R.drawable.rounded_view_green);
					tvDirection.setTextColor(getActivity().getResources().getColor(tx.getConfirmations() < 3 ? R.color.blockchain_green_50 : R.color.blockchain_green));
				}
				tvResult.setText(span1);
                tvResult.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        isBTC = (isBTC) ? false : true;
                        displayBalance();
                        accountsAdapter.notifyDataSetChanged();
                        txAdapter.notifyDataSetChanged();
                        return false;
                    }
                });

                LinearLayout layoutTxInfo = (LinearLayout)view.findViewById(R.id.tx_info);
                layoutTxInfo.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        String strTx = tx.getHash();
                        if(strTx != null) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + strTx));
                            startActivity(browserIntent);
                        }
                        return false;
                    }
                });

				/*
		    	TextView tvTags = (TextView)view.findViewById(R.id.tags);
				tvTags.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
				int bullet = 0x25cf;	// other bullet == 0x2022
				SpannableStringBuilder strTags = new SpannableStringBuilder();
				Spannable span = null;
				int spanColor = 0;
				Map<Integer,String> tags = tx.getTags();
				List<Integer> keys = new ArrayList<Integer>();
				keys.addAll(tags.keySet());
				for(int i = 0; i < keys.size(); i++) {
					SpannableStringBuilder strTag = new SpannableStringBuilder();
					span = Spannable.Factory.getInstance().newSpannable(Character.toString((char)bullet) + " ");
					switch(keys.get(i)) {
					case 0:
						spanColor = Color.BLUE;
						break;
					case 1: 
						spanColor = Color.CYAN;
						break;
					case 2: 
						spanColor = Color.GREEN;
						break;
					case 3:
						spanColor = Color.RED;
						break;
					default:
						spanColor = Color.GRAY;
						break;
					}
					span.setSpan(new ForegroundColorSpan(spanColor), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					strTag.append(span);
					strTag.append(tags.get(keys.get(i)));
					if(i == (keys.size() - 1)) {
						strTag.append(" ");
					}
					strTags.append(strTag);
				}
				tvTags.setText(strTags);
				*/
	        }

	        return view;
		}

    }

    private class AccountAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    AccountAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return accounts.size();
		}

		@Override
		public String getItem(int position) {
			return accounts.get(position).getLabel();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = null;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.balance_layout, parent, false);
	        } else {
	            view = convertView;
	        }

			long amount = 0L;
	        Account hda = accounts.get(position);
	        String xpub = account2Xpub(position);
	        if(xpub != null) {
				if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
					amount = MultiAddrFactory.getInstance().getXpubAmounts().get(xpub);
				}
	        }
	        else {
		        if(hda instanceof ImportedAccount) {
		        	amount = hda.getAmount();
		        }
	        }

	    	double btc_balance = (((double)amount) / 1e8);
	    	double fiat_balance = btc_fx * btc_balance;

	        TextView tvBalance0 = (TextView)view.findViewById(R.id.header);
			tvBalance0.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
			tvBalance0.setTextColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
			TextView tvBalance1 = (TextView)view.findViewById(R.id.balance1);
			tvBalance1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
			tvBalance1.setTextSize(28.0f);
			tvBalance1.setTextColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));
			TextView tvBalance2 = (TextView)view.findViewById(R.id.balance2);
			tvBalance2.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
			tvBalance2.setTextColor(getActivity().getResources().getColor(R.color.blockchain_light_blue));

			tvBalance0.setText(hda.getLabel());
			span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (MonetaryUtil.getInstance().getBTCFormat().format(btc_balance) + " " + strBTC) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
			span2 = Spannable.Factory.getInstance().newSpannable(isBTC ? (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat) : (MonetaryUtil.getInstance().getBTCFormat().format(btc_balance) + " " + strBTC));
			span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - 3, span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span2.setSpan(new RelativeSizeSpan(0.67f), span2.length() - 3, span2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tvBalance1.setText(span1);
			tvBalance2.setText(span2);

	        return view;
		}

    }

	private void displayBalance() {
        strFiat = PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD");
        btc_fx = ExchangeRateFactory.getInstance(getActivity()).getLastPrice(strFiat);

    	btc_balance = ((double)(MultiAddrFactory.getInstance().getTotalBalance()) / 1e8);
    	fiat_balance = btc_fx * btc_balance;

		span1 = Spannable.Factory.getInstance().newSpannable(isBTC ? (getDisplayAmount(MultiAddrFactory.getInstance().getTotalBalance()) + " " + getDisplayUnits()) : (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat));
		span2 = Spannable.Factory.getInstance().newSpannable(isBTC ? (MonetaryUtil.getInstance().getFiatFormat(strFiat).format(fiat_balance) + " " + strFiat) : (getDisplayAmount(MultiAddrFactory.getInstance().getTotalBalance()) + " " + getDisplayUnits()));
		span1.setSpan(new RelativeSizeSpan(0.67f), span1.length() - (isBTC ? getDisplayUnits().length() : 3), span1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		span2.setSpan(new RelativeSizeSpan(0.67f), span2.length() - (isBTC ? 3 : getDisplayUnits().length()), span2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		tvBalance1.setText(span1);
		tvBalance2.setText(span2);
	}

    private String getDisplayAmount(long value) {

        String strAmount = null;

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = Double.toString((((double)(value * 1000000L)) / 1e8));
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = Double.toString((((double)(value * 1000L)) / 1e8));
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    private String getDisplayAmount(double value) {

        String strAmount = null;

        int unit = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = Double.toString((value * 1000000.0) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = Double.toString((value * 1000.0) / 1e8);
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    private String getDisplayUnits() {

        return (String)MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.BTC_UNITS, MonetaryUtil.UNIT_BTC)];

    }

    private void updateTx() {
		
    	if(accounts == null || accounts.size() < 1) {
    		return;
    	}

        txMap = MultiAddrFactory.getInstance().getXpubTxs();
        String xpub = account2Xpub(selectedAccount);
        if(xpub != null) {
			if(MultiAddrFactory.getInstance().getXpubAmounts().containsKey(xpub)) {
		        txs = txMap.get(xpub); 
			}
			else {
				txs = new ArrayList<Tx>();
			}
        }
        else {
	        Account hda = accounts.get(selectedAccount);
	        if(hda instanceof ImportedAccount) {
	            txs = MultiAddrFactory.getInstance().getLegacyTxs();
	        }
			else {
				txs = new ArrayList<Tx>();
			}
        }

	}

	/*
	private void slideToBottom(View view)	{
		TranslateAnimation animate = new TranslateAnimation(0, 0, 0, view.getHeight());
		animate.setDuration(1500);
		animate.setFillAfter(true);
		view.startAnimation(animate);
		view.setVisibility(View.GONE);
	}
	 
	private void slideToTop(View view)	{
		TranslateAnimation animate = new TranslateAnimation(0, 0, 0, -view.getHeight());
		animate.setDuration(1500);
		animate.setFillAfter(true);
		view.startAnimation(animate);
		view.setVisibility(View.GONE);
	}
	*/

	private String account2Xpub(int sel) {

		Account hda = accounts.get(sel);
		String xpub = null;
	    if(hda instanceof ImportedAccount) {
	    	xpub = null;
	    }
	    else {
			xpub = HDPayloadBridge.getInstance(getActivity()).account2Xpub(sel);
	    }
	    
	    return xpub;
	}

	private String getAccountLabel() {
		String ret = null;
        Account hda = accounts.get(selectedAccount);
        if(hda instanceof ImportedAccount) {
        	ret = "Imported addresses";
        }
        if(hda.getLabel() != null && hda.getLabel().length() > 0) {
        	ret = hda.getLabel();
        }
        else {
        	ret = "Account:" + selectedAccount;
        }
        
        return ret;
	}

	private void updateDropDownAnchor() {
        int dropDownState = PrefsUtil.getInstance(getActivity()).getValue("BalanceDropDownState", 1);
        switch(dropDownState) {
        case 2:
	        layoutAccounts.setVisibility(View.VISIBLE);
            tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_up));
            break;
        case 3:
        	layoutBalance.setVisibility(View.GONE);
            tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));
            break;
        default:
            tvSwipe.setText(getAccountLabel() + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_up) + "\n" + Character.toString((char)TypefaceUtil.awesome_angle_double_down));
            break;
        }
	}

}
