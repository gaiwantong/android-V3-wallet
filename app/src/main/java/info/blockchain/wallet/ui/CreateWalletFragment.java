package info.blockchain.wallet.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.PasswordUtil;
import info.blockchain.wallet.ui.helpers.ToastCustom;

import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.R;

public class CreateWalletFragment extends Fragment {

    LinearLayout entropyMeter;
    ProgressBar passStrengthBar;
    TextView passStrengthVerdict;
    TextView next;
    TextView tos;
    int pwStrength;
    int[] strengthVerdicts = {R.string.strength_weak, R.string.strength_medium, R.string.strength_strong, R.string.strength_very_strong};
    int[] strengthColors = {R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_green, R.drawable.progress_green};
    private EditText edEmail = null;
    private EditText edPassword1 = null;
    private EditText edPassword2 = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_create_wallet, container, false);

        getActivity().setTitle(getResources().getString(R.string.new_wallet));

        ((TextView) rootView.findViewById(R.id.tos)).setMovementMethod(LinkMovementMethod.getInstance());//make link clickable

        edEmail = (EditText) rootView.findViewById(R.id.email_address);
        edPassword1 = (EditText) rootView.findViewById(R.id.wallet_pass);
        edPassword2 = (EditText) rootView.findViewById(R.id.wallet_pass_confrirm);
        next = (TextView) rootView.findViewById(R.id.command_next);
        next.setClickable(false);

        passStrengthBar = (ProgressBar) rootView.findViewById(R.id.pass_strength_bar);
        passStrengthBar.setMax(100);
        passStrengthVerdict = (TextView) rootView.findViewById(R.id.pass_strength_verdict);
        entropyMeter = (LinearLayout) rootView.findViewById(R.id.entropy_meter);

        edEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) setEntropyMeterVisible(View.GONE);
            }
        });

        edPassword2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) setEntropyMeterVisible(View.GONE);
            }
        });

        edPassword1.addTextChangedListener(new TextWatcher() {
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

                final String em = edEmail.getText().toString().trim();
                final String pw1 = edPassword1.getText().toString();
                final String pw2 = edPassword2.getText().toString();
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
                                    edPassword1.setText("");
                                    edPassword2.setText("");
                                    edPassword1.requestFocus();
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

        tos = (TextView) rootView.findViewById(R.id.tos);

        String text = getString(R.string.agree_terms_of_service) + " ";
        String text2 = getString(R.string.blockchain_tos);

        Spannable spannable = new SpannableString(text + text2);
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blockchain_blue)),
                text.length(), text.length() + text2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tos.setText(spannable, TextView.BufferType.SPANNABLE);

        tos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PolicyActivity.class);
//                intent.putExtra("uri","https://blockchain.info/Resources/TermsofServicePolicy.pdf");//pdf
                intent.putExtra("uri", "https://blockchain.com/terms");//plain text/html
                startActivity(intent);
            }
        });

        return rootView;
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
                entropyMeter.setVisibility(visible);
            }
        });
    }
}
