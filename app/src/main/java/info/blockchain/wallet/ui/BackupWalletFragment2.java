package info.blockchain.wallet.ui;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.blockchain.wallet.util.BackupWalletUtil;

import piuk.blockchain.android.R;

public class BackupWalletFragment2 extends Fragment {

    private TextView tvStart = null;
    private TextView tvPreviousWord = null;
    private TextView tvNextWord = null;
    private TextView tvInstructions = null;

    private LinearLayout cardLayout = null;
    private TextView tvPressReveal = null;
    private TextView tvCurrentWord = null;

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

        tvStart = (TextView) rootView.findViewById(R.id.start_action);
        tvStart.setVisibility(View.VISIBLE);

        tvPreviousWord = (TextView) rootView.findViewById(R.id.previous_word_action);
        tvPreviousWord.setVisibility(View.INVISIBLE);

        tvNextWord = (TextView) rootView.findViewById(R.id.next_word_action);
        tvNextWord.setVisibility(View.INVISIBLE);

        cardLayout = (LinearLayout) rootView.findViewById(R.id.card_layout);
        cardLayout.setVisibility(View.INVISIBLE);

        tvPressReveal = (TextView) rootView.findViewById(R.id.tv_press_reveal);
        tvPressReveal.setVisibility(View.INVISIBLE);

        tvCurrentWord = (TextView) rootView.findViewById(R.id.tv_current_word);
        tvCurrentWord.setVisibility(View.INVISIBLE);

        tvInstructions = (TextView) rootView.findViewById(R.id.tvInstructions);

        tvStart.setOnClickListener(v -> {
            tvStart.setVisibility(View.INVISIBLE);
            tvInstructions.setText(getString(R.string.backup_write_down_words));
            tvPreviousWord.setVisibility(View.GONE);
            tvNextWord.setVisibility(View.VISIBLE);
            cardLayout.setVisibility(View.VISIBLE);
            tvPressReveal.setVisibility(View.VISIBLE);
            tvCurrentWord.setVisibility(View.VISIBLE);
        });

        word = getResources().getString(R.string.Word);
        of = getResources().getString(R.string.of);

        animExitToLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_left);
        animEnterFromRight = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_right);

        animExitToRight = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_right);
        animEnterFromLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_left);

        mnemonic = BackupWalletUtil.getInstance(getActivity()).getMnemonic();
        if (currentWordIndex == mnemonic.length) {
            currentWordIndex = 0;
        }
        tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
        tvPressReveal.setText(mnemonic[currentWordIndex]);

        tvNextWord.setOnClickListener(v -> {

            if(currentWordIndex >= 0){
                tvPreviousWord.setVisibility(View.VISIBLE);
            }

            if (currentWordIndex < mnemonic.length) {

                animExitToLeft.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        tvPressReveal.setText("");
                        tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        cardLayout.startAnimation(animEnterFromRight);
                        tvPressReveal.setText(mnemonic[currentWordIndex]);
                    }
                });

                cardLayout.startAnimation(animExitToLeft);

                currentWordIndex++;
            }

            if (currentWordIndex == mnemonic.length) {

                currentWordIndex = 0;
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.content_frame, new BackupWalletFragment3())
                        .addToBackStack(null)
                        .commit();
            } else {

                if (currentWordIndex == mnemonic.length - 1)
                    tvNextWord.setText(getResources().getString(R.string.DONE));
                else
                    tvNextWord.setText(getResources().getString(R.string.NEXT_WORD));
            }
        });

        tvPreviousWord.setOnClickListener(v1 -> {

            tvNextWord.setText(getResources().getString(R.string.NEXT_WORD));

            if (currentWordIndex == 1) {
                tvPreviousWord.setVisibility(View.GONE);
            }

            animExitToRight.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    tvPressReveal.setText("");
                    tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    cardLayout.startAnimation(animEnterFromLeft);
                    tvPressReveal.setText(mnemonic[currentWordIndex]);
                }
            });

            cardLayout.startAnimation(animExitToRight);

            currentWordIndex--;
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        currentWordIndex = 0;

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_general);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}