package info.blockchain.wallet.view;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;

import info.blockchain.wallet.util.BackupWalletUtil;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentBackupWallet2Binding;

public class BackupWalletFragment2 extends Fragment {

    private int currentWordIndex = 0;
    private String[] mnemonic = null;

    private String word = null;
    private String of = null;

    private Animation animExitToLeft = null;
    private Animation animEnterFromRight = null;

    private Animation animExitToRight = null;
    private Animation animEnterFromLeft = null;

    private FragmentBackupWallet2Binding binding;
    private String secondPassword = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_wallet_2, container, false);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            secondPassword = bundle.getString("second_password");
        }

        binding.startAction.setVisibility(View.VISIBLE);
        binding.previousWordAction.setVisibility(View.INVISIBLE);
        binding.nextWordAction.setVisibility(View.INVISIBLE);
        binding.cardLayout.setVisibility(View.INVISIBLE);
        binding.tvPressReveal.setVisibility(View.INVISIBLE);
        binding.tvCurrentWord.setVisibility(View.INVISIBLE);

        binding.startAction.setOnClickListener(v -> {
            binding.startAction.setVisibility(View.INVISIBLE);
            binding.tvInstructions.setText(getString(R.string.backup_write_down_words));
            binding.previousWordAction.setVisibility(View.GONE);
            binding.nextWordAction.setVisibility(View.VISIBLE);
            binding.cardLayout.setVisibility(View.VISIBLE);
            binding.tvPressReveal.setVisibility(View.VISIBLE);
            binding.tvCurrentWord.setVisibility(View.VISIBLE);
        });

        word = getResources().getString(R.string.Word);
        of = getResources().getString(R.string.of);

        animExitToLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_left);
        animEnterFromRight = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_right);

        animExitToRight = AnimationUtils.loadAnimation(getActivity(), R.anim.exit_to_right);
        animEnterFromLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.enter_from_left);

        mnemonic = new BackupWalletUtil(getActivity()).getMnemonic(secondPassword);
        if (currentWordIndex == mnemonic.length) {
            currentWordIndex = 0;
        }
        binding.tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
        binding.tvPressReveal.setText(mnemonic[currentWordIndex]);

        binding.nextWordAction.setOnClickListener(v -> {

            if(currentWordIndex >= 0){
                binding.previousWordAction.setVisibility(View.VISIBLE);
            }

            if (currentWordIndex < mnemonic.length) {

                animExitToLeft.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        binding.tvPressReveal.setText("");
                        binding.tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        binding.cardLayout.startAnimation(animEnterFromRight);
                        binding.tvPressReveal.setText(mnemonic[currentWordIndex]);
                    }
                });

                binding.cardLayout.startAnimation(animExitToLeft);

                currentWordIndex++;
            }

            if (currentWordIndex == mnemonic.length) {

                currentWordIndex = 0;

                Fragment fragment = new BackupWalletFragment3();
                if(secondPassword!=null) {
                    Bundle args = new Bundle();
                    args.putString("second_password", secondPassword);
                    fragment.setArguments(args);
                }

                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.content_frame, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {

                if (currentWordIndex == mnemonic.length - 1)
                    binding.nextWordAction.setText(getResources().getString(R.string.DONE));
                else
                    binding.nextWordAction.setText(getResources().getString(R.string.NEXT_WORD));
            }
        });

        binding.previousWordAction.setOnClickListener(v1 -> {

            binding.nextWordAction.setText(getResources().getString(R.string.NEXT_WORD));

            if (currentWordIndex == 1) {
                binding.previousWordAction.setVisibility(View.GONE);
            }

            animExitToRight.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    binding.tvPressReveal.setText("");
                    binding.tvCurrentWord.setText(word + " " + (currentWordIndex + 1) + " " + of + " 12");
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    binding.cardLayout.startAnimation(animEnterFromLeft);
                    binding.tvPressReveal.setText(mnemonic[currentWordIndex]);
                }
            });

            binding.cardLayout.startAnimation(animExitToRight);

            currentWordIndex--;
        });

        return binding.getRoot();
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