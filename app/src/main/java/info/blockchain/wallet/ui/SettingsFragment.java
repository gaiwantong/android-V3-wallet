package info.blockchain.wallet.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mukesh.countrypicker.fragments.CountryPicker;
import com.mukesh.countrypicker.models.Country;

import info.blockchain.api.Settings;
import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.helpers.BackgroundExecutor;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PasswordUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.RootUtil;

import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener
{
    //Profile
    Preference guidPref;
    Preference emailPref;
    Preference smsPref;

    //Preferences
    Preference unitsPref;
    Preference fiatPref;
    SwitchPreference emailNotificationPref;

    //Security
    Preference pinPref;
    SwitchPreference twoStepVerificationPref;
    Preference passwordHint1Pref;
    Preference changePasswordPref;
    SwitchPreference torPref;

    //App
    Preference aboutPref;
    Preference tosPref;
    Preference privacyPref;
    Preference disableRootWarningPref;

    Settings settingsApi;
    int pwStrength = 0;
    PrefsUtil prefsUtil;
    MonetaryUtil monetaryUtil;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        prefsUtil = new PrefsUtil(getActivity());
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));

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
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
                if(settingsApi != null) {
                    refreshList();
                }

                super.onPostExecute(aVoid);
            }

            @Override
            protected Void doInBackground(Void... params) {
                Payload payload = PayloadFactory.getInstance().get();
                settingsApi = new Settings(payload.getGuid(), payload.getSharedKey());
                return null;
            }

        }.execute();
    }

    @UiThread
    private void refreshList(){

        PreferenceScreen prefScreen = getPreferenceScreen();
        if(prefScreen != null)prefScreen.removeAll();
        addPreferencesFromResource(R.xml.settings);

        //Profile
        PreferenceCategory profileCategory = (PreferenceCategory) findPreference("profile");
        guidPref = (Preference) findPreference("guid");
        guidPref.setSummary(PayloadFactory.getInstance().get().getGuid());
        guidPref.setOnPreferenceClickListener(SettingsFragment.this);

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

        //Preferences
        PreferenceCategory preferencesCategory = (PreferenceCategory) findPreference("preferences");
        unitsPref = (Preference) findPreference("units");
        unitsPref.setSummary(getDisplayUnits());
        unitsPref.setOnPreferenceClickListener(SettingsFragment.this);

        fiatPref = (Preference) findPreference("fiat");
        fiatPref.setSummary(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
        fiatPref.setOnPreferenceClickListener(SettingsFragment.this);

        emailNotificationPref = (SwitchPreference) findPreference("email_notifications");
        if(settingsApi.isEmailVerified()){
            emailNotificationPref.setChecked(settingsApi.isNotificationsOn());
            emailNotificationPref.setOnPreferenceClickListener(this);
        }else{
            preferencesCategory.removePreference(emailNotificationPref);
        }

        //Security
        PreferenceCategory securityCategory = (PreferenceCategory) findPreference("security");
        pinPref = (Preference) findPreference("pin");
        pinPref.setOnPreferenceClickListener(this);

        twoStepVerificationPref = (SwitchPreference) findPreference("2fa");
        twoStepVerificationPref.setOnPreferenceClickListener(this);
        twoStepVerificationPref.setChecked(settingsApi.getAuthType() == Settings.AUTH_TYPE_SMS);

        passwordHint1Pref = (Preference) findPreference("pw_hint1");
        if(settingsApi.getPasswordHint1() != null && !settingsApi.getPasswordHint1().isEmpty()){
            passwordHint1Pref.setSummary(settingsApi.getPasswordHint1());
        }else{
            passwordHint1Pref.setSummary("");
        }
        passwordHint1Pref.setOnPreferenceClickListener(this);

        changePasswordPref = (Preference) findPreference("change_pw");
        changePasswordPref.setOnPreferenceClickListener(this);

        torPref = (SwitchPreference) findPreference("tor");
        torPref.setChecked(settingsApi.isTorBlocked());
        torPref.setOnPreferenceClickListener(this);

        //App
        aboutPref = (Preference) findPreference("about");
        aboutPref.setSummary("v"+ BuildConfig.VERSION_NAME);
        aboutPref.setOnPreferenceClickListener(this);

        tosPref = (Preference) findPreference("tos");
        tosPref.setOnPreferenceClickListener(this);

        privacyPref = (Preference) findPreference("privacy");
        privacyPref.setOnPreferenceClickListener(this);

        disableRootWarningPref = (Preference) findPreference("disable_root_warning");
        if (disableRootWarningPref != null &&
                !new RootUtil().isDeviceRooted()) {
            PreferenceCategory appCategory = (PreferenceCategory) findPreference("app");
            appCategory.removePreference(disableRootWarningPref);
        }
    }

    private String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    @UiThread
    private void updateEmail(String email){
        if(email == null || email.isEmpty()) {
            email = getString(R.string.not_specified);
            emailPref.setSummary(email);
        }else{
            final String finalEmail = email;
            Handler handler = new Handler(Looper.getMainLooper());
            new BackgroundExecutor(getActivity(),
                    () -> {
                        settingsApi.setEmail(finalEmail, new Settings.ResultListener() {
                            @Override
                            public void onSuccess() {
                                handler.post(() -> {
                                    updateEmailNotification(false);
                                    refreshList();});
                            }

                            @Override
                            public void onFail() {
                                ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            }

                            @Override
                            public void onBadRequest() {

                            }
                        });
                    }).execute();
        }
    }

    @UiThread
    private void updateSms(String sms){

        if(sms == null || sms.isEmpty()) {
            sms = getString(R.string.not_specified);
            smsPref.setSummary(sms);
        }else {

            final String finalSms = sms;
            Handler handler = new Handler(Looper.getMainLooper());
            new BackgroundExecutor(getActivity(),
                    () -> {
                        settingsApi.setSms(finalSms, new Settings.ResultListener() {
                            @Override
                            public void onSuccess() {
                                handler.post(() -> {
                                    refreshList();
                                    showDialogVerifySms();
                                });
                            }

                            @Override
                            public void onFail() {
                                ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            }

                            @Override
                            public void onBadRequest() {

                            }
                        });
                    }).execute();

        }
    }

    @UiThread
    private void verifySms(final String code){
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> {
                    settingsApi.verifySms(code, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> {
                                refreshList();
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.success)
                                        .setMessage(R.string.sms_verified)
                                        .setPositiveButton(R.string.dialog_continue, null)
                                        .show();
                            });
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.verification_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    });
                }).execute();
    }

    @UiThread
    private void updateTor(final boolean enabled){
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> {
                    settingsApi.setTorBlocked(enabled, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> torPref.setChecked(enabled));
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    });
                }).execute();
    }

    @UiThread
    private void updatePasswordHint1(final String hint){
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> {
                    settingsApi.setPasswordHint1(hint, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> passwordHint1Pref.setSummary(hint));
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    });
                }).execute();
    }

    @UiThread
    private void updatePin(final String pin){

        new AsyncTask<Void, Void, CharSequenceX>() {

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
            protected void onPostExecute(CharSequenceX params) {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    progress = null;
                }
                if(params != null){
                    prefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
                    prefsUtil.removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);

                    Intent intent = new Intent(getActivity(), PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }else{
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_pin), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }

                super.onPostExecute(params);
            }

            @Override
            protected CharSequenceX doInBackground(Void... params) {
                try {
                    return AccessState.getInstance(getActivity()).validatePIN(pin);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }.execute();
    }

    @UiThread
    private void updateEmailNotification(final boolean enabled){
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> {
                    settingsApi.enableNotifications(enabled, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> emailNotificationPref.setChecked(enabled));
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    });
                }).execute();

        if(enabled) {
            new BackgroundExecutor(getActivity(),
                    () -> {
                        settingsApi.setNotificationType(Settings.NOTIFICATION_TYPE_EMAIL, new Settings.ResultListener() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onFail() {
                            }

                            @Override
                            public void onBadRequest() {
                            }
                        });
                    }).execute();
        }
    }

    @UiThread
    private void update2FA(final int type){
        Handler handler = new Handler(Looper.getMainLooper());
        new BackgroundExecutor(getActivity(),
                () -> {
                    settingsApi.setAuthType(type, new Settings.ResultListener() {
                        @Override
                        public void onSuccess() {
                            handler.post(() -> twoStepVerificationPref.setChecked(type == Settings.AUTH_TYPE_SMS));
                        }

                        @Override
                        public void onFail() {
                            ToastCustom.makeText(getActivity(), getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            handler.post(() -> twoStepVerificationPref.setChecked(type == Settings.AUTH_TYPE_SMS));
                        }

                        @Override
                        public void onBadRequest() {

                        }
                    });
                }).execute();
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {

            case "email":
                showDialogEmail();
                break;

            case "email_notifications":
                showDialogEmailNotifications();
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

            case "2fa":
                showDialogTwoFA();
                break;

            case "pin":
                showDialogChangePin();
                break;

            case "pw_hint1":
                showDialogPasswordHint1();
                break;

            case "change_pw":
                showDialogChangePasswordWarning();
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
                .setPositiveButton(R.string.block, null)
                .setNegativeButton(R.string.allow, null)
                .create();
        alertDialogEmail.setOnShowListener(dialog -> {

            Button buttonPositive = alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(view -> {

                updateTor(true);
                alertDialogEmail.dismiss();
            });

            Button buttonNegative = alertDialogEmail.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {

                updateTor(false);
                alertDialogEmail.dismiss();
            });
        });
        alertDialogEmail.show();
    }

    private void showDialogEmail(){

        final EditText etEmail = new EditText(getActivity());
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setPadding(46, 16, 46, 16);
        etEmail.setText(settingsApi.getEmail());
        etEmail.setSelection(etEmail.getText().length());
        final Handler mHandler = new Handler(Looper.getMainLooper());

        final AlertDialog alertDialogEmail = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.email)
                .setMessage(R.string.verify_email2)
                .setView(etEmail)
                .setCancelable(false)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.resend, null)
                .create();
        alertDialogEmail.setOnShowListener(dialog -> {

            Button button = alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String email = etEmail.getText().toString();

                if (!FormatsUtil.getInstance().isValidEmailAddress(email)) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_email), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }else {
                    updateEmail(email);
                    alertDialogEmail.dismiss();
                    showDialogEmailVerification();
                }
            });

            Button buttonNeutral = alertDialogEmail.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(view -> {

                mHandler.post(() -> {
                    //Resend verification code
                    updateEmail(settingsApi.getEmail());
                });

                if(alertDialogEmail != null && alertDialogEmail.isShowing())alertDialogEmail.dismiss();
                showDialogEmailVerification();
            });
        });
        alertDialogEmail.show();
    }

    private void showDialogEmailVerification() {

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.verify)
                .setMessage(R.string.verify_email_notice)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showDialogMobile(){
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View smsPickerView = inflater.inflate(R.layout.include_sms_update, null);
        final EditText etMobile = (EditText)smsPickerView.findViewById(R.id.etSms);
        final TextView tvCountry = (TextView)smsPickerView.findViewById(R.id.tvCountry);
        final TextView tvSms = (TextView)smsPickerView.findViewById(R.id.tvSms);

        final CountryPicker picker = CountryPicker.newInstance(getString(R.string.select_country));
        final Country country = picker.getUserCountryInfo(getActivity());
        if(country.getDialCode().equals("93")){
            setCountryFlag(tvCountry, "+1", R.drawable.flag_us);
        }else{
            setCountryFlag(tvCountry, country.getDialCode(), country.getFlag());
        }
        tvCountry.setOnClickListener(v -> {

            picker.show(SettingsActivity.fragmentManager, "COUNTRY_PICKER");
            picker.setListener((name, code, dialCode, flagDrawableResID) -> {

                setCountryFlag(tvCountry, dialCode, flagDrawableResID);
                picker.dismiss();
            });
        });

        if (!settingsApi.isSmsVerified() && settingsApi.getSms() != null && !settingsApi.getSms().isEmpty()) {
            tvSms.setText(settingsApi.getSms());
            tvSms.setVisibility(View.VISIBLE);
        }else {
            tvSms.setVisibility(View.GONE);
        }

        final AlertDialog.Builder alertDialogSmsBuilder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.mobile)
                .setMessage(getString(R.string.mobile_description))
                .setView(smsPickerView)
                .setCancelable(false)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(R.string.cancel, null);

        if (!settingsApi.isSmsVerified() && settingsApi.getSms() != null && !settingsApi.getSms().isEmpty()) {
            alertDialogSmsBuilder.setNeutralButton(R.string.verify, null);
        }

        AlertDialog alertDialogSms = alertDialogSmsBuilder.create();

        final Handler mHandler = new Handler(Looper.getMainLooper());
        alertDialogSms.setOnShowListener(dialog -> {

            Button button = alertDialogSms.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {

                final String sms = tvCountry.getText().toString()+etMobile.getText().toString();

                if (!FormatsUtil.getInstance().isValidMobileNumber(sms)) {
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_mobile), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }else {
                    mHandler.post(() -> updateSms(sms));

                    alertDialogSms.dismiss();
                }
            });

            alertDialogSms.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setOnClickListener(view -> {
                showDialogVerifySms();
                alertDialogSms.dismiss();
            });
        });
        alertDialogSms.show();
    }

    private void showDialogGUI(){
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_name)
                .setMessage(R.string.guid_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("guid", PayloadFactory.getInstance().get().getGuid());
                    clipboard.setPrimaryClip(clip);
                    ToastCustom.makeText(getActivity(), getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                    ;
                }).show();
    }

    private void showDialogBTCUnits(){
        final CharSequence[] units = monetaryUtil.getBTCUnits();
        final int sel = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, 0);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_units)
                .setSingleChoiceItems(units, sel, (dialog, which) -> {
                            prefsUtil.setValue(PrefsUtil.KEY_BTC_UNITS, which);
                    unitsPref.setSummary(getDisplayUnits());
                    dialog.dismiss();
                }
                ).show();
    }

    private void showDialogFiatUnits(){
        final String[] currencies = ExchangeRateFactory.getInstance(getActivity()).getCurrencyLabels();
        String strCurrency = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        int selected = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].endsWith(strCurrency)) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_currency)
                .setSingleChoiceItems(currencies, selected, (dialog, which) -> {
                            prefsUtil.setValue(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
                    fiatPref.setSummary(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
                    dialog.dismiss();
                }
                ).show();
    }

    private void showDialogVerifySms(){

        final EditText etSms = new EditText(getActivity());
        etSms.setPadding(46, 16, 46, 16);
        etSms.setSingleLine(true);
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
        alertDialog.setOnShowListener(dialog -> {

            Button buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(view -> {
                final String codeS = etSms.getText().toString();
                if(codeS != null && codeS.length()>0){
                    mHandler.post(() -> verifySms(codeS));
                    if(alertDialog != null && alertDialog.isShowing())alertDialog.dismiss();
                }
            });

            Button buttonNeutral = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            buttonNeutral.setOnClickListener(view -> {

                mHandler.post(() -> {
                    //Resend verification code
                    alertDialog.dismiss();
                    updateSms(settingsApi.getSms());
                });
            });
        });
        alertDialog.show();
    }

    private void showDialogPasswordHint1(){
        final EditText etPwHint1 = new EditText(getActivity());
        etPwHint1.setPadding(46, 16, 46, 16);
        etPwHint1.setText(settingsApi.getPasswordHint1());
        etPwHint1.setSelection(etPwHint1.getText().length());
        etPwHint1.setSingleLine(true);

        final AlertDialog alertDialogEmail = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.password_hint)
                .setMessage(R.string.password_hint_summary)
                .setView(etPwHint1)
                .setCancelable(false)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialogEmail.setOnShowListener(dialog -> {

            Button button = alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {

                String hint = etPwHint1.getText().toString();
                if(!hint.equals(PayloadFactory.getInstance().getTempPassword().toString())) {
                    updatePasswordHint1(hint);
                    alertDialogEmail.dismiss();
                }else{
                    ToastCustom.makeText(getActivity(), getString(R.string.hint_reveals_password_error),ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                }
            });
        });
        alertDialogEmail.show();
    }

    private void showDialogChangePin(){

        final EditText etPin = new EditText(getActivity());
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
        etPin.setPadding(46, 16, 46, 16);
        int maxLength = 4;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        etPin.setFilters(fArray);

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_pin_code)
                .setMessage(R.string.enter_current_pin)
                .setView(etPin)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {

            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {

                String pin = etPin.getText().toString();
                updatePin(pin);
                alertDialog.dismiss();

            });
        });
        alertDialog.show();
    }

    private void showDialogEmailNotifications() {

        final AlertDialog alertDialogEmail = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.email_notifications)
                .setMessage(R.string.email_notifications_summary)
                .setCancelable(false)
                .setPositiveButton(R.string.enable, null)
                .setNegativeButton(R.string.disable, null)
                .create();
        alertDialogEmail.setOnShowListener(dialog -> {

            Button buttonPositive = alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(view -> {

                updateEmailNotification(true);
                alertDialogEmail.dismiss();
            });

            Button buttonNegative = alertDialogEmail.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {

                updateEmailNotification(false);
                alertDialogEmail.dismiss();
            });
        });
        alertDialogEmail.show();
    }

    private void showDialogChangePasswordWarning() {

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.warning)
                .setMessage(R.string.change_password_summary)
                .setPositiveButton(R.string.dialog_continue, (dialog, which) -> {
                    showDialogChangePassword();
                })
                .show();

    }

    private void showDialogChangePassword(){

        LayoutInflater inflater = (LayoutInflater) getActivity().getBaseContext().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);
        final LinearLayout pwLayout = (LinearLayout) inflater.inflate(R.layout.modal_change_password2, null);

        EditText etCurrentPw = (EditText) pwLayout.findViewById(R.id.current_password);
        EditText etNewPw = (EditText) pwLayout.findViewById(R.id.new_password);
        EditText etNewConfirmedPw = (EditText) pwLayout.findViewById(R.id.confirm_password);

        LinearLayout entropyMeter = (LinearLayout) pwLayout.findViewById(R.id.entropy_meter);
        ProgressBar passStrengthBar = (ProgressBar) pwLayout.findViewById(R.id.pass_strength_bar);
        passStrengthBar.setMax(100);
        TextView passStrengthVerdict = (TextView) pwLayout.findViewById(R.id.pass_strength_verdict);

        etNewPw.addTextChangedListener(new TextWatcher() {
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
                        getActivity().runOnUiThread(() -> {
                            entropyMeter.setVisibility(View.VISIBLE);
                            setPasswordStrength(passStrengthVerdict, passStrengthBar, editable.toString());
                        });
                    }
                }, 200);
            }
        });

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_password)
                .setCancelable(false)
                .setView(pwLayout)
                .setPositiveButton(R.string.update, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> {

            Button buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(view -> {

                String currentPw = etCurrentPw.getText().toString();
                String newPw = etNewPw.getText().toString();
                String newConfirmedPw = etNewConfirmedPw.getText().toString();
                final CharSequenceX walletPassword = PayloadFactory.getInstance().getTempPassword();

                if(currentPw.equals(walletPassword.toString())) {
                    if (newPw.equals(newConfirmedPw)) {
                        if (newConfirmedPw == null || newConfirmedPw.length() < 9 || newConfirmedPw.length() > 255) {
                            ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                        } else if (pwStrength < 50){
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.weak_password)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.yes, (dialog1, which) -> {
                                        etNewConfirmedPw.setText("");
                                        etNewConfirmedPw.requestFocus();
                                        etNewPw.setText("");
                                        etNewPw.requestFocus();
                                    })
                                    .setNegativeButton(R.string.polite_no, (dialog1, which) -> {
                                        updatePassword(alertDialog, new CharSequenceX(newConfirmedPw), walletPassword);
                                    })
                                    .show();
                        } else{
                            updatePassword(alertDialog, new CharSequenceX(newConfirmedPw), walletPassword);
                        }
                    }else{
                        etNewConfirmedPw.setText("");
                        etNewConfirmedPw.requestFocus();
                        ToastCustom.makeText(getActivity(), getString(R.string.password_mismatch_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    }
                }else{
                    etCurrentPw.setText("");
                    etCurrentPw.requestFocus();
                    ToastCustom.makeText(getActivity(), getString(R.string.invalid_password), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
            });

            Button buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            buttonNegative.setOnClickListener(view -> {

                alertDialog.dismiss();
            });
        });
        alertDialog.show();
    }

    private void showDialogTwoFA() {

        if(!settingsApi.isSmsVerified()){
            twoStepVerificationPref.setChecked(false);
            showDialogMobile();
        }else{
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.two_fa)
                    .setMessage(R.string.two_fa_summary)
                    .setNeutralButton(R.string.cancel, null);

            if(settingsApi.getAuthType() == Settings.AUTH_TYPE_SMS){
                alertDialogBuilder.setNegativeButton(R.string.disable, null);
            }else{
                alertDialogBuilder.setPositiveButton(R.string.enable, null);
            }
            AlertDialog alertDialogEmail = alertDialogBuilder.create();
            alertDialogEmail.setOnShowListener(dialog -> {

                alertDialogEmail.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(view -> {
                            update2FA(Settings.AUTH_TYPE_SMS);
                            alertDialogEmail.dismiss();
                        });

                alertDialogEmail.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setOnClickListener(view -> {
                            update2FA(Settings.AUTH_TYPE_OFF);
                            alertDialogEmail.dismiss();
                        });

                alertDialogEmail.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener(view -> {
                            if(settingsApi.getAuthType() == Settings.AUTH_TYPE_SMS){
                                twoStepVerificationPref.setChecked(true);
                            }else{
                                twoStepVerificationPref.setChecked(false);
                            }

                            alertDialogEmail.dismiss();
                        });
            });
            alertDialogEmail.show();
        }
    }

    private void updatePassword(AlertDialog alertDialog, final CharSequenceX updatedPassword, final CharSequenceX fallbackPassword){
        ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setTitle(R.string.app_name);
        progress.setMessage(getActivity().getResources().getString(R.string.please_wait));
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            Looper.prepare();

            PayloadFactory.getInstance().setTempPassword(updatedPassword);

            if (AccessState.getInstance(getActivity()).createPIN(updatedPassword, AccessState.getInstance(getActivity()).getPIN())
                    && PayloadFactory.getInstance().put()) {

                ToastCustom.makeText(getActivity(), getString(R.string.password_changed), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
            } else {
                //Revert on fail
                PayloadFactory.getInstance().setTempPassword(fallbackPassword);
                ToastCustom.makeText(getActivity(), getString(R.string.remote_save_ko), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                ToastCustom.makeText(getActivity(), getString(R.string.password_unchanged), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            progress.dismiss();
            alertDialog.dismiss();
            Looper.loop();
        }).start();
    }

    @UiThread
    private void setPasswordStrength(TextView passStrengthVerdict, ProgressBar passStrengthBar, String pw){
        int[] strengthVerdicts = {R.string.strength_weak, R.string.strength_medium, R.string.strength_strong, R.string.strength_very_strong};
        int[] strengthColors = {R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_green, R.drawable.progress_green};
        pwStrength = (int) Math.round(PasswordUtil.getInstance().getStrength(pw));

        if(pw.equals(prefsUtil.getValue(PrefsUtil.KEY_EMAIL,"")))pwStrength = 0;

        int pwStrengthLevel = 0;//red
        if (pwStrength >= 75) pwStrengthLevel = 3;//green
        else if (pwStrength >= 50) pwStrengthLevel = 2;//green
        else if (pwStrength >= 25) pwStrengthLevel = 1;//orange

        passStrengthBar.setProgress(pwStrength);
        passStrengthBar.setProgressDrawable(ContextCompat.getDrawable(getActivity(), strengthColors[pwStrengthLevel]));
        passStrengthVerdict.setText(getResources().getString(strengthVerdicts[pwStrengthLevel]));
    }

    @UiThread
    private void setCountryFlag(TextView tvCountry, String dialCode, int flagResourceId){
        tvCountry.setText(dialCode);
        Drawable drawable = getResources().getDrawable(flagResourceId);
        drawable.setAlpha(30);
        tvCountry.setBackgroundDrawable(drawable);
    }
}
