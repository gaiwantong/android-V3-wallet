package info.blockchain.wallet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PasswordUtil;

/**
 * Created by riaanvos on 25/03/15.
 */
public class CreateWalletFragment extends Fragment {

	private EditText edEmail = null;
	private EditText edPassword1 = null;
	private EditText edPassword2 = null;

	LinearLayout entropyMeter;
	ProgressBar passStrengthBar;
	TextView passStrengthVerdict;
	TextView next;

	int pwStrength;
	int[] strengthVerdicts = {R.string.strength_weak,R.string.strength_medium,R.string.strength_strong,R.string.strength_very_strong};
	int[] strengthColors = {R.drawable.progress_red,R.drawable.progress_orange,R.drawable.progress_green,R.drawable.progress_green};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_create_wallet, container, false);

        rootView.setFilterTouchesWhenObscured(true);

        getActivity().setTitle(getResources().getString(R.string.create_wallet));

		((TextView) rootView.findViewById(R.id.tos)).setMovementMethod(LinkMovementMethod.getInstance());//make link clickable

		edEmail = (EditText)rootView.findViewById(R.id.email_address);
		edPassword1 = (EditText)rootView.findViewById(R.id.wallet_pass);
		edPassword2 = (EditText)rootView.findViewById(R.id.wallet_pass_confrirm);
		next = (TextView)rootView.findViewById(R.id.command_next);
		next.setClickable(false);

		passStrengthBar = (ProgressBar)rootView.findViewById(R.id.pass_strength_bar);
		passStrengthBar.setMax(100);
		passStrengthVerdict = (TextView)rootView.findViewById(R.id.pass_strength_verdict);
		entropyMeter = (LinearLayout)rootView.findViewById(R.id.entropy_meter);

		edEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)setEntropyMeterVisible(View.GONE);
			}
		});

		edPassword2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus)setEntropyMeterVisible(View.GONE);
			}
		});

		edPassword1.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			private Timer timer = new Timer();
			private final long DELAY = 200; // small delay before pass entropy calc - increases performance when user types fast.

			@Override
			public void afterTextChanged(final Editable editable) {
				timer.cancel();
				timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {

						setEntropyMeterVisible(View.VISIBLE);

						final String pw = editable.toString();

						if (pw.equals(edEmail.getText().

										toString()

						))//Email and password can't be the same
							pwStrength = 0;
						else
							pwStrength = (int) Math.round(PasswordUtil.getInstance().

											getStrength(pw)

							);

						int pwStrengthLevel = 0;//red
						if (pwStrength >= 75) pwStrengthLevel = 3;//green
						else if (pwStrength >= 50) pwStrengthLevel = 2;//green
						else if (pwStrength >= 25) pwStrengthLevel = 1;//orange

						setProgress(pwStrengthLevel, pwStrength);
					}

					private void setProgress(final int pwStrengthLevel, final int scorePerc) {

						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								passStrengthBar.setProgress(scorePerc);
								passStrengthBar.setProgressDrawable(ContextCompat.getDrawable(getActivity(), strengthColors[pwStrengthLevel]));
								passStrengthVerdict.setText(getResources().getString(strengthVerdicts[pwStrengthLevel]));
							}
						});
					}

				}, DELAY);
			}
		});

		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				final String em = edEmail.getText().toString();
				final String pw1 = edPassword1.getText().toString();
				final String pw2 = edPassword2.getText().toString();

				if(em == null || !FormatsUtil.getInstance().isValidEmailAddress(em)) {
					Toast.makeText(getActivity(), getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
				}
				else if(pw1==null || pw2==null || pw1.length() < 9 || pw1.length() > 255) {
					Toast.makeText(getActivity(), getString(R.string.invalid_password), Toast.LENGTH_SHORT).show();
				}
				else if(!pw1.equals(pw2)) {
					Toast.makeText(getActivity(), getString(R.string.password_mismatch_error), Toast.LENGTH_SHORT).show();
				}
				else if(pwStrength < 50){

					new AlertDialog.Builder(getActivity())
    				    	    .setTitle(R.string.app_name)
    							.setMessage(R.string.weak_password)
    				    	    .setCancelable(false)
    				    	    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    				    	        public void onClick(DialogInterface dialog, int whichButton) {
                                        edPassword1.setText("");
                                        edPassword2.setText("");
                                        edPassword1.requestFocus();
    				    	        }
    				    	    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
    				    	        public void onClick(DialogInterface dialog, int whichButton) {
    		        		        	Intent intent = new Intent(getActivity(), PinEntryActivity.class);
    		        		        	intent.putExtra("_email", em);
    		        		        	intent.putExtra("_pw", pw1);
    		        		    		getActivity().startActivity(intent);
    				    	        }
    				    	    }).show();
				}
				else {
					Intent intent = new Intent(getActivity(), PinEntryActivity.class);
					intent.putExtra("_email", em);
					intent.putExtra("_pw", pw1);
					getActivity().startActivity(intent);
				}
			}
		});

		return rootView;
	}

	private void setEntropyMeterVisible(final int visible){

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				entropyMeter.setVisibility(visible);
			}
		});
	}
}
