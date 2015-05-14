package info.blockchain.wallet;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.blockchain.wallet.util.BackupWalletUtil;

public class BackupWalletFragment2 extends Fragment {

	private TextView tvStart = null;
	private TextView tvNextWord = null;

	private LinearLayout cardLayout = null;
	private TextView tvPressReveal = null;
	private TextView tvCurrentWord = null;

	private Toolbar toolbar = null;

	private int currentWordIndex = 0;
	private String[] mnemonic = null;

	private String word = null;
	private String of = null;

	private Animation animExitToLeft = null;
	private Animation animEnterFromRight = null;

	private Animation animExitToRight = null;
	private Animation animEnterFromLeft = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_backup_wallet_2, container, false);

		toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_general);

		tvStart = (TextView)rootView.findViewById(R.id.start_action);
		tvStart.setVisibility(View.VISIBLE);

		tvNextWord = (TextView)rootView.findViewById(R.id.next_word_action);
		tvNextWord.setVisibility(View.INVISIBLE);

		cardLayout = (LinearLayout)rootView.findViewById(R.id.card_layout);
		cardLayout.setVisibility(View.INVISIBLE);

		tvPressReveal = (TextView)rootView.findViewById(R.id.tv_press_reveal);
		tvPressReveal.setVisibility(View.INVISIBLE);

		tvCurrentWord = (TextView)rootView.findViewById(R.id.tv_current_word);
		tvCurrentWord.setVisibility(View.INVISIBLE);

		tvStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tvStart.setVisibility(View.INVISIBLE);
				tvNextWord.setVisibility(View.VISIBLE);
				cardLayout.setVisibility(View.VISIBLE);
				tvPressReveal.setVisibility(View.VISIBLE);
				tvCurrentWord.setVisibility(View.VISIBLE);
			}
		});

		word = getResources().getString(R.string.Word);
		of = getResources().getString(R.string.of);

		animExitToLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_left);
		animEnterFromRight = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_right);

		animExitToRight = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_right);
		animEnterFromLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_left);

		mnemonic = BackupWalletUtil.getInstance(getActivity()).getMnemonic();
		showWordAtIndex(currentWordIndex);

		tvNextWord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (currentWordIndex < mnemonic.length){

					animExitToLeft.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

						@Override
						public void onAnimationEnd(Animation animation) {
							cardLayout.startAnimation(animEnterFromRight);
						}
					});

					cardLayout.startAnimation(animExitToLeft);

					currentWordIndex++;
				}

				if (currentWordIndex == mnemonic.length) {

					getFragmentManager().beginTransaction()
							.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
							.replace(R.id.content_frame, new BackupWalletFragment3())
							.addToBackStack(null)
							.commit();
				} else {

					if(currentWordIndex == mnemonic.length-1)
						tvNextWord.setText(getResources().getString(R.string.VERIFY));
					else
						tvNextWord.setText(getResources().getString(R.string.NEXT_WORD));

					showWordAtIndex(currentWordIndex);

					toolbar.setNavigationOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {

							tvNextWord.setText(getResources().getString(R.string.NEXT_WORD));

							if (currentWordIndex == 0){
								getActivity().onBackPressed();
							}else {

								animExitToRight.setAnimationListener(new Animation.AnimationListener() {
									@Override
									public void onAnimationStart(Animation animation) {
									}

									@Override
									public void onAnimationRepeat(Animation animation) {
									}

									@Override
									public void onAnimationEnd(Animation animation) {
										cardLayout.startAnimation(animEnterFromLeft);
									}
								});

								cardLayout.startAnimation(animExitToRight);

								currentWordIndex--;
								showWordAtIndex(currentWordIndex);
							}
						}
					});
				}
			}
		});

		return rootView;
	}

	private void showWordAtIndex(final int index){

		tvCurrentWord.setText(word+" "+(index+1)+" "+of+" 12");

		cardLayout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				revealText(index, event, v);

				return true;
			}
		});
		tvPressReveal.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				revealText(index, event, v);

				return true;
			}
		});


	}

	private void revealText(int index, MotionEvent event, View v){

		switch (event.getAction() & MotionEvent.ACTION_MASK) {

			case MotionEvent.ACTION_DOWN:

				v.setPressed(true);
				cardLayout.setBackgroundColor(getResources().getColor(R.color.white));
				tvPressReveal.setText(mnemonic[index]);
				tvPressReveal.setTextSize(24);
				tvPressReveal.setTextColor(getResources().getColor(R.color.backup_card_text));
				tvPressReveal.setAlpha(1.0f);

				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_OUTSIDE:
			case MotionEvent.ACTION_CANCEL:
				v.setPressed(false);

				tvPressReveal.setTextSize(14);
				tvPressReveal.setTextColor(getResources().getColor(R.color.white));
				cardLayout.setBackgroundColor(getResources().getColor(R.color.backup_card_blue));
				tvPressReveal.setText(getResources().getString(R.string.press_to_reveal));
				tvPressReveal.setAlpha(0.45f);

				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				break;
			case MotionEvent.ACTION_POINTER_UP:
				break;
			case MotionEvent.ACTION_MOVE:
				break;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		currentWordIndex = 0;

		Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_general);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onBackPressed();
			}
		});
	}
}