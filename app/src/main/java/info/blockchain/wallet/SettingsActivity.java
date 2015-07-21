package info.blockchain.wallet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import org.apache.commons.codec.DecoderException;

import java.io.IOException;

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.ToastCustom;
import piuk.blockchain.android.R;

//import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);
        addPreferencesFromResource(R.xml.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final String guid = PayloadFactory.getInstance().get().getGuid();
        Preference guidPref = (Preference) findPreference("guid");
        guidPref.setSummary(guid);
        guidPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.guid_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)SettingsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("guid", guid);
                                clipboard.setPrimaryClip(clip);
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

                return true;
            }
        });

        Preference unitsPref = (Preference) findPreference("units");
        unitsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getUnits();
                return true;
            }
        });

        Preference fiatPref = (Preference) findPreference("fiat");
        fiatPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                final String[] currencies = ExchangeRateFactory.getInstance(SettingsActivity.this).getCurrencyLabels();
                String strCurrency = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
                int sel = 0;
                for(int i = 0; i < currencies.length; i++) {
                    if(currencies[i].endsWith(strCurrency)) {
                        sel = i;
                        break;
                    }
                }

                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.select_currency)
//                .setCancelable(false)
                        .setSingleChoiceItems(currencies, sel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
//										PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, which);
                                        PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_SELECTED_FIAT, currencies[which].substring(currencies[which].length() - 3));
                                        dialog.dismiss();
                                    }
                                }
                        ).show();

                return true;
            }
        });

        Preference mnemonicPref = (Preference) findPreference("mnemonic");
        mnemonicPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                // Wallet is not double encrypted
                if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                    displayHDSeedAsMnemonic(true);
                }
                // User has already entered double-encryption password
                else if (DoubleEncryptionFactory.getInstance().isActivated()) {
                    displayMnemonicForDoubleEncryptedWallet();
                }
                // Solicit & set double-encryption password, then display
                else {
                    final EditText double_encrypt_password = new EditText(SettingsActivity.this);
                    double_encrypt_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.enter_double_encryption_pw)
                            .setView(double_encrypt_password)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    PayloadFactory.getInstance().setTempDoubleEncryptPassword(new CharSequenceX(double_encrypt_password.getText().toString()));
                                    displayMnemonicForDoubleEncryptedWallet();
                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ;
                        }
                    }).show();
                }

                return true;
            }
        });
        if(!PayloadFactory.getInstance().get().isUpgraded())    {
            getPreferenceScreen().removePreference(mnemonicPref);
        }

        Preference aboutPref = (Preference) findPreference("about");
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);

                return true;
            }
        });

        Preference tosPref = (Preference) findPreference("tos");
        tosPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, PolicyActivity.class);
                intent.putExtra("uri","https://blockchain.info/Resources/TermsofServicePolicy.pdf");
                startActivity(intent);

                return true;
            }
        });

        Preference privacyPref = (Preference) findPreference("privacy");
        privacyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, PolicyActivity.class);
                intent.putExtra("uri", "https://blockchain.info/Resources/PrivacyPolicy.pdf");
                startActivity(intent);

                return true;
            }
        });

    }

    private void displayMnemonicForDoubleEncryptedWallet() {

        if (!DoubleEncryptionFactory.getInstance().isActivated()) {
            ToastCustom.makeText(SettingsActivity.this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return;
        }

        // Decrypt seedHex (which is double encrypted in this case)
        String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
                PayloadFactory.getInstance().get().getSharedKey(),
                PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
                PayloadFactory.getInstance().get().getIterations());

        String mnemonic = null;

        // Try to create a using the decrypted seed hex
        try {
            HD_Wallet hdw = HD_WalletFactory.getInstance(this).get();
            HD_WalletFactory.getInstance(this).restoreWallet(decrypted_hex, "", 1);

            mnemonic = HD_WalletFactory.getInstance(this).get().getMnemonic();
            HD_WalletFactory.getInstance(this).set(hdw);
        } catch (IOException | DecoderException | AddressFormatException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException e) {
            e.printStackTrace();
        } finally {
            if (mnemonic != null && mnemonic.length() > 0) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(mnemonic)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();

            } else {
                ToastCustom.makeText(this, getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            }
        }
    }

    private void displayHDSeedAsMnemonic(boolean mnemonic) {
        String seed = null;
        try {
            if (mnemonic) {
                seed = HDPayloadBridge.getInstance(SettingsActivity.this).getHDMnemonic();
            } else {
                seed = HDPayloadBridge.getInstance(SettingsActivity.this).getHDSeed();
            }

            if(AppUtil.getInstance(SettingsActivity.this).isDEBUG()) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = null;
                clip = android.content.ClipData.newPlainText(mnemonic ? "Mnemonic" : "Hex seed", seed);
                clipboard.setPrimaryClip(clip);
            }

        } catch (IOException | MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
            ToastCustom.makeText(SettingsActivity.this, getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } finally {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle(R.string.app_name)
                    .setMessage(seed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    private void getUnits() {

        final CharSequence[] units = MonetaryUtil.getInstance().getBTCUnits();
        final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.KEY_BTC_UNITS, 0);

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.select_units)
//                .setCancelable(false)
                .setSingleChoiceItems(units, sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.KEY_BTC_UNITS, which);
                                dialog.dismiss();
                            }
                        }
                ).show();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);

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
            }else{
                height = bar.getHeight();
            }

            content.setPadding(0, height, 0, 0);

            root.addView(content);

            View shadow = LayoutInflater.from(this).inflate(R.layout.include_container_shadow, root, false);
            root.addView(shadow);

            root.addView(bar);
        }

        bar.setTitle(getResources().getString(R.string.action_settings));
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(SettingsActivity.this).updatePinEntryTime();
    }

}
