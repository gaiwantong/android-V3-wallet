package info.blockchain.wallet.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import info.blockchain.api.Settings;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.RootUtil;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener{

    //Profile
    Preference emailPref;
    Preference smsPref;

    //Wallet
    Preference guidPref;
    Preference unitsPref;
    Preference fiatPref;

    //App
    Preference aboutPref;
    Preference tosPref;
    Preference privacyPref;
    Preference disableRootWarningPref;

    Settings settingsApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setTitle(R.string.app_name);
        addPreferencesFromResource(R.xml.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                settingsApi = new Settings(PayloadFactory.getInstance().get());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                if(settingsApi != null) {
                    emailPref = (Preference) findPreference("email");
                    displayEmail();
                    emailPref.setOnPreferenceClickListener(SettingsActivity.this);

                    smsPref = (Preference) findPreference("mobile");
                    displaySms();
                    smsPref.setOnPreferenceClickListener(SettingsActivity.this);

                    unitsPref = (Preference) findPreference("units");
                    unitsPref.setSummary(getDisplayUnits());
                    unitsPref.setOnPreferenceClickListener(SettingsActivity.this);

                    fiatPref = (Preference) findPreference("fiat");
                    fiatPref.setSummary(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
                    fiatPref.setOnPreferenceClickListener(SettingsActivity.this);
                }

                super.onPostExecute(aVoid);
            }
        }.execute();

        guidPref = (Preference) findPreference("guid");
        guidPref.setSummary(PayloadFactory.getInstance().get().getGuid());
        guidPref.setOnPreferenceClickListener(this);

        aboutPref = (Preference) findPreference("about");
        aboutPref.setSummary("v"+BuildConfig.VERSION_NAME);
        aboutPref.setOnPreferenceClickListener(this);

        tosPref = (Preference) findPreference("tos");
        tosPref.setOnPreferenceClickListener(this);

        privacyPref = (Preference) findPreference("privacy");
        privacyPref.setOnPreferenceClickListener(this);

        disableRootWarningPref = (Preference) findPreference("disable_root_warning");
        if (disableRootWarningPref != null &&
                !RootUtil.getInstance().isDeviceRooted()) {
            PreferenceCategory notificationsCategory = (PreferenceCategory) findPreference("app");
            notificationsCategory.removePreference(disableRootWarningPref);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtil.getInstance(this).stopLogoutTimer();
    }

    @Override
    protected void onPause() {
        AppUtil.getInstance(this).startLogoutTimer();
        super.onPause();
    }

    private void setupToolbar(){
        Toolbar bar;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_general, root, false);

            View shadow = LayoutInflater.from(this).inflate(R.layout.include_container_shadow, root, false);
            root.addView(shadow, 0);
            root.addView(bar, 0);
        } else {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);

            root.removeAllViews();

            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_general, root, false);

            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            } else {
                height = bar.getHeight();
            }

            content.setPadding(0, height, 0, 0);

            root.addView(content);

            View shadow = LayoutInflater.from(this).inflate(R.layout.include_container_shadow, root, false);
            root.addView(shadow);

            root.addView(bar);
        }

        //TODO - don't use NoActionBar in styles.xml (affects BalanceFragment, so don't just edit styles.xml)
        bar.setTitle(getResources().getString(R.string.action_settings));
        bar.setTitleTextColor(getResources().getColor(R.color.white));
        bar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String getDisplayUnits() {
        return (String) MonetaryUtil.getInstance().getBTCUnits()[PrefsUtil.getInstance(this).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
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
                    progress = new ProgressDialog(SettingsActivity.this);
                    progress.setTitle(R.string.app_name);
                    progress.setMessage(SettingsActivity.this.getResources().getString(R.string.please_wait));
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!success){
                                ToastCustom.makeText(SettingsActivity.this, getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
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

    private void displayEmail(){
        new AsyncTask<Void, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(SettingsActivity.this);
                progress.setTitle(R.string.app_name);
                progress.setMessage(SettingsActivity.this.getResources().getString(R.string.please_wait));
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String emailAndStatus = settingsApi.getEmail();
                        if(settingsApi.isEmailVerified()){
                            emailAndStatus += "  ("+getString(R.string.verified)+")";
                        }else{
                            emailAndStatus += "  ("+getString(R.string.unverified)+")";
                        }
                        emailPref.setSummary(emailAndStatus);
                    }
                });

                return null;
            }
        }.execute();
    }

    private void displaySms(){

        new AsyncTask<Void, Void, Void>() {

            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress = new ProgressDialog(SettingsActivity.this);
                progress.setTitle(R.string.app_name);
                progress.setMessage(SettingsActivity.this.getResources().getString(R.string.please_wait));
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String smsAndStatus = settingsApi.getSms();
                        if(settingsApi.isSmsVerified()){
                            smsAndStatus += "  ("+getString(R.string.verified)+")";
                        }else{
                            smsAndStatus += "  ("+getString(R.string.unverified)+")";
                        }
                        smsPref.setSummary(smsAndStatus);
                    }
                });
                return null;
            }
        }.execute();
    }

    private void updateSms(String sms){
        if(sms == null || sms.isEmpty()) {
            sms = getString(R.string.not_specified);
            smsPref.setSummary(sms);
        }else{

            final String finalSms = sms;
            new AsyncTask<Void, Void, Void>() {

                ProgressDialog progress;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progress = new ProgressDialog(SettingsActivity.this);
                    progress.setTitle(R.string.app_name);
                    progress.setMessage(SettingsActivity.this.getResources().getString(R.string.please_wait));
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
                    final boolean success = settingsApi.setSms(finalSms);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!success) {
                                ToastCustom.makeText(SettingsActivity.this, getString(R.string.update_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                            }else{
                                smsPref.setSummary(finalSms+"  ("+getString(R.string.unverified)+")");
                            }
                        }
                    });
                    return null;
                }
            }.execute();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {

            case "email":
                final EditText etEmail = new EditText(this);
                etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                etEmail.setPadding(46, 16, 46, 16);

                final AlertDialog alertDialogEmail = new AlertDialog.Builder(this)
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
                                    ToastCustom.makeText(SettingsActivity.this, getString(R.string.invalid_email), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                }else {
                                    updateEmail(email);
                                    alertDialogEmail.dismiss();
                                }
                            }
                        });
                    }
                });
                alertDialogEmail.show();

                break;

            case "mobile":

                final EditText etMobile = new EditText(this);
                etMobile.setInputType(InputType.TYPE_CLASS_NUMBER);
                etMobile.setPadding(46, 16, 46, 16);

                final AlertDialog alertDialogSms = new AlertDialog.Builder(this)
                        .setTitle(R.string.mobile)
                        .setMessage(R.string.mobile_description)
                        .setView(etMobile)
                        .setCancelable(false)
                        .setPositiveButton(R.string.update, null)
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                alertDialogSms.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialog) {

                        Button button = alertDialogSms.getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                String sms = etMobile.getText().toString();

                                if (!FormatsUtil.getInstance().isValidMobileNumber(sms)) {
                                    ToastCustom.makeText(SettingsActivity.this, getString(R.string.invalid_mobile), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                }else {
                                    updateSms(sms);
                                    alertDialogSms.dismiss();
                                }
                            }
                        });
                    }
                });
                alertDialogSms.show();
                break;

            case "guid":
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.guid_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) SettingsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("guid", PayloadFactory.getInstance().get().getGuid());
                                clipboard.setPrimaryClip(clip);
                                ToastCustom.makeText(SettingsActivity.this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();
                break;

            case "units":
                final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
                final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_BTC_UNITS, 0);

                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.select_units)
                        .setSingleChoiceItems(units, sel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_BTC_UNITS, which);
                                        unitsPref.setSummary(getDisplayUnits());
                                        dialog.dismiss();
                                    }
                                }
                        ).show();
                break;

            case "fiat":
                final String[] currencies = ExchangeRateFactory.getInstance(SettingsActivity.this).getCurrencyLabels();
                String strCurrency = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
                int selected = 0;
                for (int i = 0; i < currencies.length; i++) {
                    if (currencies[i].endsWith(strCurrency)) {
                        selected = i;
                        break;
                    }
                }

                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.select_currency)
                        .setSingleChoiceItems(currencies, selected, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
                                        fiatPref.setSummary(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY));
                                        dialog.dismiss();
                                    }
                                }
                        ).show();
                break;

            case "about":
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;

            case "tos":
                intent = new Intent(this, PolicyActivity.class);
                intent.putExtra("uri", "https://blockchain.com/terms");//plain text/html
                startActivity(intent);
                break;

            case "privacy":
                intent = new Intent(this, PolicyActivity.class);
                intent.putExtra("uri", "https://blockchain.com/privacy");//plain text/html
                startActivity(intent);
                break;

            case "disable_root_warning":
                break;
        }

        return true;
    }
}
