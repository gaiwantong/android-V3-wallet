package info.blockchain.wallet.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PasswordUtil;
import info.blockchain.wallet.view.helpers.ToastCustom;

import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentCreateWalletBinding;

public class CreateWalletFragment extends Fragment {

    int pwStrength;
    int[] strengthVerdicts = {R.string.strength_weak, R.string.strength_medium, R.string.strength_normal, R.string.strength_strong};
    int[] strengthColors = {R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_blue, R.drawable.progress_green};

    private FragmentCreateWalletBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_wallet, container, false);

        getActivity().setTitle(getResources().getString(R.string.new_wallet));

        binding.tos.setMovementMethod(LinkMovementMethod.getInstance());//make link clickable
        binding.commandNext.setClickable(false);
        binding.entropyContainer.passStrengthBar.setMax(100);

        binding.emailAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) setEntropyMeterVisible(View.GONE);
            }
        });

        binding.walletPassConfrirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) setEntropyMeterVisible(View.GONE);
            }
        });

        binding.walletPass.addTextChangedListener(new TextWatcher() {
            private final long DELAY = 200; // small delay before pass entropy calc - increases performance when user types fast.
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {

                        setEntropyMeterVisible(View.VISIBLE);

                        final String pw = editable.toString();

                        if (pw.equals(binding.emailAddress.getText().

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
                                binding.entropyContainer.passStrengthBar.setProgress(scorePerc);
                                binding.entropyContainer.passStrengthBar.setProgressDrawable(ContextCompat.getDrawable(getActivity(), strengthColors[pwStrengthLevel]));
                                binding.entropyContainer.passStrengthVerdict.setText(getResources().getString(strengthVerdicts[pwStrengthLevel]));
                            }
                        });
                    }

                }, DELAY);
            }
        });

        binding.commandNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String em = binding.emailAddress.getText().toString().trim();
                final String pw1 = binding.walletPass.getText().toString();
                final String pw2 = binding.walletPassConfrirm.getText().toString();
                AppUtil appUtil = new AppUtil(getActivity());

                if (em == null || !FormatsUtil.getInstance().isValidEmailAddress(em)) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_email), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else if (pw1 == null || pw2 == null || pw1.length() < 9 || pw1.length() > 255) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else if (!pw1.equals(pw2)) {
                    ToastCustom.makeText(getActivity(), getString(R.string.password_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                } else if (pwStrength < 50) {

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.weak_password)
                            .setCancelable(false)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    binding.walletPass.setText("");
                                    binding.walletPassConfrirm.setText("");
                                    binding.walletPass.requestFocus();
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            appUtil.setUpgradeReminder(1L);

                            hideKeyboard();
                            Intent intent = new Intent(getActivity(), PinEntryActivity.class);
                            intent.putExtra("_email", em);
                            intent.putExtra("_pw", pw1);
                            getActivity().startActivity(intent);
                        }
                    }).show();
                } else {
                    appUtil.setUpgradeReminder(1L);

                    hideKeyboard();
                    Intent intent = new Intent(getActivity(), PinEntryActivity.class);
                    intent.putExtra("_email", em);
                    intent.putExtra("_pw", pw1);
                    getActivity().startActivity(intent);
                }
            }
        });

        String text = getString(R.string.agree_terms_of_service) + " ";
        String text2 = getString(R.string.blockchain_tos);

        Spannable spannable = new SpannableString(text + text2);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blockchain_blue)),
                text.length(), text.length() + text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.tos.setText(spannable, TextView.BufferType.SPANNABLE);

        binding.tos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PolicyActivity.class);
//                intent.putExtra("uri","https://blockchain.info/Resources/TermsofServicePolicy.pdf");//pdf
                intent.putExtra("uri", "https://blockchain.com/terms");//plain text/html
                startActivity(intent);
            }
        });

        return binding.getRoot();
    }

    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setEntropyMeterVisible(final int visible) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.entropyContainer.entropyMeter.setVisibility(visible);
            }
        });
    }
}
