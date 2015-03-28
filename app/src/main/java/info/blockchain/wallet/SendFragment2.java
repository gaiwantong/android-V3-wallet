package info.blockchain.wallet;

import java.math.BigInteger;
import java.util.Locale;

import android.os.Bundle;
import android.os.Handler;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.support.v7.app.ActionBarActivity;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.TypefaceUtil;

public class SendFragment2 extends Fragment {
	
	private Locale locale = null;
	
	private Button btConfirm = null;
	private TextView tvDirection = null;
	private TextView tvOutgoing = null;
	private TextView tvDestination = null;
	private TextView tvBTC = null;
	private TextView tvFiat = null;

//	private TextView tvFeeLabel = null;
//	private TextView tvFee = null;
	private TextView tvTotalLabel = null;
	private TextView tvTotal = null;
//	private TextView tvNoteLabel = null;
	private TextView tvNote = null;
	private TextView tvAddNote = null;
	private TextView tvAddNote2 = null;

//	private ImageView ivBack = null;
	private TextView tvBack = null;

//	private LinearLayout layoutFee = null;
	private LinearLayout layoutNote = null;

	private boolean isHD = true;
	private BigInteger bamount = BigInteger.ZERO;
	private BigInteger bfee = BigInteger.ZERO;
	private String destination = null;
	private String btc_amount = null;
	private String fiat_amount = null;
	private String sending_from = null;
	private int account = 0;
	private String legacy_addr = null;
	private String legacy_priv = null;
	private LegacyAddress legacyAddress = null;
	private String seed = null;

	private String strBTC = null;
	
	private String strNote = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_send2, container, false);
		
		locale = Locale.getDefault();

    	hideActionBar();

        Bundle bundle = this.getArguments();
        if(bundle != null) {
            isHD = bundle.getBoolean("hd", true);
            bamount = new BigInteger(bundle.getString("bamount", ""));
            bfee = new BigInteger(bundle.getString("bfee", ""));
            destination = bundle.getString("destination", "");
            account = bundle.getInt("account", 0);
            sending_from = bundle.getString("sending_from", "");
            btc_amount = bundle.getString("btc_amount", "");
            fiat_amount = bundle.getString("fiat_amount", "");
            legacy_addr = bundle.getString("legacy_addr", null);
            legacy_priv = bundle.getString("legacy_priv", null);
            if(legacy_addr != null && legacy_priv != null && account == -1 && !isHD) {
            	legacyAddress = new LegacyAddress(legacy_priv, legacy_addr);
            }
            seed = bundle.getString("seed", null);
            strBTC = bundle.getString("btc_units", null);
        }

        tvDirection = ((TextView)rootView.findViewById(R.id.direction));
        tvDirection.setTypeface(TypefaceUtil.getInstance(getActivity()).getAwesomeTypeface());
        tvDirection.setText(Character.toString((char)TypefaceUtil.awesome_arrow_down));

        tvOutgoing = ((TextView)rootView.findViewById(R.id.outgoing));
        tvOutgoing.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
        tvOutgoing.setText(sending_from);

        tvDestination = ((TextView)rootView.findViewById(R.id.destination));
        tvDestination.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        tvDestination.setText(destination);

        tvBTC = ((TextView)rootView.findViewById(R.id.btc));
        tvBTC.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
        tvBTC.setText(btc_amount + " " + strBTC);

        tvFiat = ((TextView)rootView.findViewById(R.id.fiat));
        tvFiat.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        tvFiat.setText(fiat_amount + " " + PrefsUtil.getInstance(getActivity()).getValue("ccurrency", "USD"));

        /*
        tvFeeLabel = ((TextView)rootView.findViewById(R.id.fee_label));
        tvFeeLabel.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
        
        tvFee = ((TextView)rootView.findViewById(R.id.fee));
        tvFee.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        tvFee.setText(MonetaryUtil.getInstance().getBTCFormat().format((double)bfee.longValue() / 1e8) + " " + strBTC);

        tvTotalLabel = ((TextView)rootView.findViewById(R.id.total_label));
        tvTotalLabel.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
        
        tvTotal = ((TextView)rootView.findViewById(R.id.total));
        tvTotal.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        tvTotal.setText(MonetaryUtil.getInstance().getBTCFormat().format((bamount.doubleValue() + bfee.doubleValue()) / 1e8) + " " + strBTC);
        */

        tvNote = ((TextView)rootView.findViewById(R.id.note));
        tvNote.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
//        tvNote.setText();
        
//        layoutFee = ((LinearLayout)rootView.findViewById(R.id.fee));
        /*
        layoutFee.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
            	int checkedItem = 1;
            	if(bfee.compareTo(Utils.toNanoCoins("0.0001")) == -1) {
            		checkedItem = 0;
            	}
            	else if(bfee.compareTo(Utils.toNanoCoins("0.0001")) == 1) {
            		checkedItem = 2;
            	}
            	else {
            		checkedItem = 1;
            	}

            	final AlertDialog feeDialog;
            	CharSequence[] items = new CharSequence[3];
            	items[0] = getActivity().getResources().getText(R.string.frugal);
            	items[1] = getActivity().getResources().getText(R.string.standard);
            	items[2] = getActivity().getResources().getText(R.string.generous);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.choose_fee);
                builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                   
                    switch(item)	{
                    case 0:
                    	bfee = BigInteger.ZERO;
                        break;
                    case 1:
                    	bfee = Utils.toNanoCoins("0.0001");
                        break;
                    case 2:
                    	bfee = Utils.toNanoCoins("0.0005");
                        break;
                    default:
                    	break;
                    
                    }
                    
                    dialog.dismiss();    
                }
                });

                feeDialog = builder.create();
                feeDialog.show();            	

            	return false;
            }
        });
        */

        tvAddNote = ((TextView)rootView.findViewById(R.id.add_note));
        tvAddNote.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	editNote();
            	return false;
            }
        });

        tvAddNote2 = ((TextView)rootView.findViewById(R.id.add_note2));
        tvAddNote2.setTypeface(TypefaceUtil.getInstance(getActivity()).getAwesomeTypeface());
        tvAddNote2.setText(Character.toString((char)TypefaceUtil.awesome_pencil_square));
        tvAddNote2.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	editNote();
            	return false;
            }
        });

        layoutNote = ((LinearLayout)rootView.findViewById(R.id.edit_note));
        layoutNote.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	editNote();
            	return false;
            }
        });

        /*
        ivBack = ((ImageView)rootView.findViewById(R.id.back));
        ivBack.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
        		Fragment fragment = new BalanceFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });
        */
        tvBack = ((TextView)rootView.findViewById(R.id.back));
        tvBack.setTypeface(TypefaceUtil.getInstance(getActivity()).getAwesomeTypeface());
        tvBack.setText(Character.toString((char)TypefaceUtil.awesome_angle_double_left));
        tvBack.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
        		Fragment fragment = new BalanceFragment();
        		FragmentManager fragmentManager = getFragmentManager();
        		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            	return false;
            }
        });

        btConfirm = ((Button)rootView.findViewById(R.id.send));
        btConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
                final Animation animation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setRepeatCount(3);
                animation.setDuration(1000);
                tvDirection.startAnimation(animation);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                      animation.cancel();
                      animation.reset();
                  }
                }, 3000);

        		if(isHD) {
        		    SendFactory.getInstance(getActivity()).send(account, destination, bamount, null, bfee, strNote, new OpCallback() {

						public void onSuccess() {
							getActivity().runOnUiThread(new Runnable() {
					            @Override
					            public void run() {
					        		Toast.makeText(getActivity(), "Transaction submitted", Toast.LENGTH_SHORT).show();
			                        tvDirection.setText(Character.toString((char)TypefaceUtil.awesome_checkmark));
						    		PayloadFactory.getInstance(getActivity()).remoteSaveThread();
						    		
						    		MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (bamount.longValue() + bfee.longValue()));
						    		MultiAddrFactory.getInstance().setXpubAmount(HDPayloadBridge.getInstance(getActivity()).account2Xpub(account), MultiAddrFactory.getInstance().getXpubAmounts().get(HDPayloadBridge.getInstance(getActivity()).account2Xpub(account)) - (bamount.longValue() + bfee.longValue()));
					            }
					        });
						}

						public void onFail() {			
							getActivity().runOnUiThread(new Runnable() {
					            @Override
					            public void run() {
					        		Toast.makeText(getActivity(), "Transaction failed", Toast.LENGTH_SHORT).show();
			                        tvDirection.setText("X");
					            }
					        });
						}
						
						});
        		}
        		else {
    			    SendFactory.getInstance(getActivity()).send(-1, destination, bamount, legacyAddress, bfee, strNote, new OpCallback() {

						public void onSuccess() {
							getActivity().runOnUiThread(new Runnable() {
					            @Override
					            public void run() {
					        		Toast.makeText(getActivity(), "Transaction submitted", Toast.LENGTH_SHORT).show();
			                        tvDirection.setText(Character.toString((char)TypefaceUtil.awesome_checkmark));
			                        if(strNote != null) {
							    		PayloadFactory.getInstance(getActivity()).remoteSaveThread();
			                        }
						    		MultiAddrFactory.getInstance().setXpubBalance(MultiAddrFactory.getInstance().getXpubBalance() - (bamount.longValue() + bfee.longValue()));
						    		MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (bamount.longValue() + bfee.longValue()));
						    		MultiAddrFactory.getInstance().setLegacyBalance(destination, MultiAddrFactory.getInstance().getLegacyBalance(destination) - (bamount.longValue() + bfee.longValue()));
					            }
					        });
						}

						public void onFail() {			
							getActivity().runOnUiThread(new Runnable() {
					            @Override
					            public void run() {
					        		Toast.makeText(getActivity(), "Transaction failed", Toast.LENGTH_SHORT).show();
			                        tvDirection.setText("X");
					            }
					        });
						}
						
						});
            	}

        		btConfirm.setVisibility(View.INVISIBLE);

            }
        });

		return rootView;
	}
	
    @Override
    public void onResume() {
    	super.onResume();
    	
    	hideActionBar();

    }

	@Override
	public void onDestroy() {
		
    	showActionBar();

		super.onDestroy();
	}

    public void hideActionBar() {
    	android.support.v7.app.ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
    	if(actionBar != null) {
    		actionBar.hide();
    	}
    }

    public void showActionBar() {
    	android.support.v7.app.ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
    	if(actionBar != null) {
    		actionBar.show();
    	}
    }

    public void editNote() {
		final EditText note = new EditText(getActivity());
		
		if(strNote != null) {
			note.setText(strNote);
		}
		
		new AlertDialog.Builder(getActivity())
	    .setTitle(R.string.app_name)
	    .setMessage("Enter your note")
	    .setView(note)
	    .setCancelable(false)
	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {

	        	strNote = note.getText().toString();
	        	tvNote.setText(strNote);

	        }
	    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	;
	        }
	    }).show();
    }

}
