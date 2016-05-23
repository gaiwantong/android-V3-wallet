package info.blockchain.wallet.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.UiThread;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mukesh.countrypicker.fragments.CountryPicker;
import com.mukesh.countrypicker.interfaces.CountryPickerListener;
import com.mukesh.countrypicker.models.Country;

import info.blockchain.api.Settings;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.RootUtil;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener
{
    //Profile
    Preference emailPref;
    Preference smsPref;
    Preference verifySmsPref;

    //Wallet
    Preference guidPref;
    Preference unitsPref;
    Preference fiatPref;

    //Security
    CheckBoxPreference torPref;

    //App
    Preference aboutPref;
    Preference tosPref;
    Preference privacyPref;
    Preference disableRootWarningPref;

    Settings settingsApi;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(getActivity());
                progress.setTitle(R.string.app_name);
                progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                Payload payload = PayloadFactory.getInstance().get();
                settingsApi = new Settings(payload.getGuid(), payload.getSharedKey());
                if(settingsApi != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshList();
                        }
                    });
                }
                return null;
            }

        }.execute();
    }

    @UiThread
    private void refreshList(){

        addPreferencesFromResource(R.xml.settings);

        emailPref = (Preference) findPreference("email");
        String emailAndStatus = settingsApi.getEmail();
        if(emailAndStatus == null || emailAndStatus.isEmpty()) {
            emailAndStatus = getString(R.string.not_specified);
        }else if(settingsApi.isEmailVerified()){
            emailAndStatus += "  ("+getString(R.string.verified)+")";
        }else{
            emailAndStatus += "  ("+getString(R.string.unverified)+")";
        }
        emailPref.setSummary(emailAndStatus);
        emailPref.setOnPreferenceClickListener(SettingsFragment.this);

        smsPref = (Preference) findPreference("mobile");
        String smsAndStatus = settingsApi.getSms();
        if(smsAndStatus == null || smsAndStatus.isEmpty()) {
            smsAndStatus = getString(R.string.not_specified);
        }else if(settingsApi.isSmsVerified()){
            smsAndStatus += "  ("+getString(R.string.verified)+")";
        }else{
            smsAndStatus += "  ("+getString(R.string.unverified)+")";
        }
        smsPref.setSummary(smsAndStatus);
        smsPref.setOnPreferenceClickListener(SettingsFragment.this);

        verifySmsPref  = (Preference) findPreference("verify_mobile");
        if(verifySmsPref != null) {
            verifySmsPref.setOnPreferenceClickListener(SettingsFragment.this);
            PreferenceCategory profileCategory = (PreferenceCategory) findPreference("profile");
            if (settingsApi.isSmsVerified() || settingsApi.getSms() == null || settingsApi.getSms().isEmpty()) {
                profileCategory.removePreference(verifySmsPref);
            } else {
                profileCategory.addPreference(verifySmsPref);
            }
        }

        unitsPref = (Preference) findPreference("units");
        unitsPref.setSummary(getDisplayUnits());
        unitsPref.setOnPreferenceClickListener(SettingsFragment.this);

        fiatPref = (Preference) findPreference("fiat");
        fiatPref.setSummary(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
        fiatPref.setOnPreferenceClickListener(SettingsFragment.this);

        guidPref = (Preference) findPreference("guid");
        guidPref.setSummary(PayloadFactory.getInstance().get().getGuid());
        guidPref.setOnPreferenceClickListener(this);

        torPref = (CheckBoxPreference) findPreference("tor");
        torPref.setChecked(settingsApi.isTorBlocked());
        torPref.setOnPreferenceClickListener(this);

        aboutPref = (Preference) findPreference("about");
        aboutPref.setSummary("v"+ BuildConfig.VERSION_NAME);
        aboutPref.setOnPreferenceClickListener(this);

        tosPref = (Preference) findPreference("tos");
        tosPref.setOnPreferenceClickListener(this);

        privacyPref = (Preference) findPreference("privacy");
        privacyPref.setOnPreferenceClickListener(this);

        disableRootWarningPref = (Preference) findPreference("disable_root_warning");
        if (disableRootWarningPref != null &&
                !RootUtil.getInstance().isDeviceRooted()) {
            PreferenceCategory appCategory = (PreferenceCategory) findPreference("app");
            appCategory.removePreference(disableRootWarningPref);
        }
    }

    private String getDisplayUnits() {
        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    private void updateEmail(String email){
        if(email == null || email.isEmpty()) {
            email = getString(R.string.not_specified);
            emailPref.setSummary(email);
        }else{

            final String finalEmail = email;
            new AsyncTask<Void, Void, Void>() {

                ProgressDialog progress;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progress = new ProgressDialog(getActivity());
                    progress.setTitle(R.string.app_name);
                    progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                    progress.show();
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }
                }

                @Override
                protected Void doInBackground(final Void... params) {
                    final boolean success = settingsApi.setEmail(finalEmail);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!success){
                                ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            }else{
                                emailPref.setSummary(finalEmail+"  ("+getString(R.string.unverified)+")");
                            }
                        }
                    });

                    return null;
                }
            }.execute();
        }
    }

    private void updateSms(String sms){

        if(sms == null || sms.isEmpty()) {
            sms = getString(R.string.not_specified);
            smsPref.setSummary(sms);
        }else{

            final Handler mHandler = new Handler(Looper.getMainLooper());

            final String finalSms = sms;
            new AsyncTask<Void, Void, Void>() {

                ProgressDialog progress;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progress = new ProgressDialog(getActivity());
                    progress.setTitle(R.string.app_name);
                    progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                    progress.show();
                }

                @Override
                protected Void doInBackground(final Void... params) {
                    final boolean success = settingsApi.setSms(finalSms);

                    if (progress != null && progress.isShowing()) {
                        progress.dismiss();
                        progress = null;
                    }

                    if (success) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshList();
                            }
                        });
                    } else {
                        ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    }

                    return null;
                }
            }.execute();
        }
    }

    private void verifySms(final String code){

        final Handler mHandler = new Handler(Looper.getMainLooper());

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(getActivity());
                progress.setTitle(R.string.app_name);
                progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected Void doInBackground(final Void... params) {
                final boolean success = settingsApi.verifySms((String)code);
                if(success) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshList();
                        }
                    });
                }else{
                    ToastCustom.makeText(getActivity(), getString(R.string.verification_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                }
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
                return null;
            }
        }.execute();
    }

    private void updateTor(final boolean enabled){

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(getActivity());
                progress.setTitle(R.string.app_name);
                progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
                progress.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
            }

            @Override
            protected Void doInBackground(final Void... params) {
                final boolean success = settingsApi.setTorBlocked(enabled);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!success){
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }else{
                            torPref.setChecked(enabled);
                        }
                    }
                });

                return null;
            }
        }.execute();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {

            case "email":
                showDialogEmail();
                break;

            case "mobile":
                showDialogMobile();
                break;

            case "verify_mobile":
                showDialogVerifySms();
                break;

            case "guid":
                showDialogGUI();
                break;

            case "units":
                showDialogBTCUnits();
                break;

            case "fiat":
                showDialogFiatUnits();
                break;

            case "tor":
                showDialogTorEnable();
                break;

            case "about":
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                break;

            case "tos":
                intent = new Intent(getActivity(), PolicyActivity.class);
                intent.putExtra("uri", "https://blockchain.com/terms");//plain text/html
                startActivity(intent);
                break;

            case "privacy":
                intent = new Intent(getActivity(), PolicyActivity.class);
                intent.putExtra("uri", "https://blockchain.com/privacy");//plain text/html
                startActivity(intent);
                break;

            case "disable_root_warning":
                break;
        }

        return true;
    }

    private void showDialogTorEnable() {

        final AlertDialog alertDialogEmail = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tor_requests)
                .setMessage(R.string.tor_summary)
                .setCancelable(false)
                .setPositiveButton(R.string.allow, null)
                .setNegativeButton(R.string.block, null)
                .create();
        alertDialogEmail.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button buttonPositive = alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        updateTor(true);
                        alertDialogEmail.dismiss();
                    }
                });

                Button buttonNegative = alertDialogEmail.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        updateTor(false);
                        alertDialogEmail.dismiss();
                    }
                });
            }
        });
        alertDialogEmail.show();
    }

    private void showDialogEmail(){
        final EditText etEmail = new EditText(getActivity());
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setPadding(46, 16, 46, 16);

        final AlertDialog alertDialogEmail = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.email)
                .setMessage(R.string.verify_email2)
                .setView(etEmail)
                .setCancelable(false)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialogEmail.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button button = alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        String email = etEmail.getText().toString();

                        if (!FormatsUtil.getInstance().isValidEmailAddress(email)) {
                            ToastCustom.makeText(getActivity(), getString(R.string.invalid_email), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }else {
                            updateEmail(email);
                            alertDialogEmail.dismiss();
                        }
                    }
                });
            }
        });
        alertDialogEmail.show();
    }

    private void showDialogMobile(){
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View smsPickerView = inflater.inflate(R.layout.include_sms_update, null);
        final EditText etMobile = (EditText)smsPickerView.findViewById(R.id.etSms);
        final TextView tvCountry = (TextView)smsPickerView.findViewById(R.id.tvCountry);

        final CountryPicker picker = CountryPicker.newInstance(getString(R.string.select_country));
        final Country country = picker.getUserCountryInfo(getActivity());
        if(country.getDialCode().equals("93")){
            setCountryFlag(tvCountry, "+1", R.drawable.flag_us);
        }else{
            setCountryFlag(tvCountry, country.getDialCode(), country.getFlag());
        }
        tvCountry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                picker.show(SettingsActivity.fragmentManager, "COUNTRY_PICKER");
                picker.setListener(new CountryPickerListener() {
                    @Override
                    public void onSelectCountry(String name, String code, String dialCode, int flagDrawableResID) {

                        setCountryFlag(tvCountry, dialCode, flagDrawableResID);
                        picker.dismiss();
                    }
                });
            }
        });

        final AlertDialog alertDialogSms = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.mobile)
                .setMessage(getString(R.string.mobile_description))
                .setView(smsPickerView)
                .setCancelable(false)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        final Handler mHandler = new Handler(Looper.getMainLooper());
        alertDialogSms.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button button = alertDialogSms.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        final String sms = tvCountry.getText().toString()+etMobile.getText().toString();

                        if (!FormatsUtil.getInstance().isValidMobileNumber(sms)) {
                            ToastCustom.makeText(getActivity(), getString(R.string.invalid_mobile), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        }else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    updateSms(sms);
                                }
                            });

                            alertDialogSms.dismiss();
                        }
                    }
                });
            }
        });
        alertDialogSms.show();
    }

    private void showDialogGUI(){
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_name)
                .setMessage(R.string.guid_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = null;
                        clip = android.content.ClipData.newPlainText("guid", PayloadFactory.getInstance().get().getGuid());
                        clipboard.setPrimaryClip(clip);
                        ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                    }

                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                ;
            }
        }).show();
    }

    private void showDialogBTCUnits(){
        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        final int sel = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_BTC_UNITS, 0);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_units)
                .setSingleChoiceItems(units, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_BTC_UNITS, which);
                                unitsPref.setSummary(getDisplayUnits());
                                dialog.dismiss();
                            }
                        }
                ).show();
    }

    private void showDialogFiatUnits(){
        final String[] currencies = ExchangeRateFactory.getInstance(getActivity()).getCurrencyLabels();
        String strCurrency = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        int selected = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].endsWith(strCurrency)) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_currency)
                .setSingleChoiceItems(currencies, selected, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(getActivity()).setValue(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
                                fiatPref.setSummary(PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
                                dialog.dismiss();
                            }
                        }
                ).show();
    }

    private void showDialogVerifySms(){

        final EditText etSms = new EditText(getActivity());
        etSms.setPadding(46, 16, 46, 16);
        final Handler mHandler = new Handler(Looper.getMainLooper());

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.verify_mobile)
                .setMessage(R.string.verify_sms_summary)
                .setView(etSms)
                .setCancelable(false)
                .setPositiveButton(R.string.verify, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.resend, null)
                .create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        final String codeS = etSms.getText().toString();
                        if(codeS != null && codeS.length()>0){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    verifySms(codeS);
                                }
                            });
                            if(alertDialog != null && alertDialog.isShowing())alertDialog.dismiss();
                        }
                    }
                });

                Button buttonNeutral = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                buttonNeutral.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //Resend verification code
                                updateSms(settingsApi.getSms());
                            }
                        });

                        if(alertDialog != null && alertDialog.isShowing())alertDialog.dismiss();
                    }
                });
            }
        });
        alertDialog.show();
    }

    private void setCountryFlag(TextView tvCountry, String dialCode, int flagResourceId){
        tvCountry.setText(dialCode);
        Drawable drawable = getResources().getDrawable(flagResourceId);
        drawable.setAlpha(30);
        tvCountry.setBackgroundDrawable(drawable);
    }
}
