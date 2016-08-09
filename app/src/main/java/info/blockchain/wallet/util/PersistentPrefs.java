package info.blockchain.wallet.util;

interface PersistentPrefs {

    String DEFAULT_CURRENCY = "USD";

    String KEY_PIN_IDENTIFIER       = "pin_kookup_key";
    String KEY_ENCRYPTED_PASSWORD   = "encrypted_password";
    String KEY_GUID                 = "guid";
    String KEY_SHARED_KEY           = "sharedKey";
    String KEY_PIN_FAILS            = "pin_fails";
    String KEY_BTC_UNITS            = "btcUnits";
    String KEY_SELECTED_FIAT        = "ccurrency";
    String KEY_INITIAL_ACCOUNT_NAME = "_1ST_ACCOUNT_NAME";
    String KEY_EMAIL           		= "email";
    String KEY_EMAIL_VERIFIED 		= "code_verified";
    String KEY_HD_UPGRADE_LAST_REMINDER = "hd_upgraded_last_reminder";
    String KEY_HD_UPGRADE_ASK_LATER = "ask_later";
    String KEY_EMAIL_VERIFY_ASK_LATER = "email_verify_ask_later";
    String KEY_BALANCE_DISPLAY_STATE = "balance_display_state";
    String KEY_SCHEME_URL = "scheme_url";
    String KEY_CURRENT_APP_VERSION = "KEY_CURRENT_APP_VERSION";
    String KEY_NEWLY_CREATED_WALLET = "newly_created_wallet";
    String LOGGED_OUT = "logged_out";

    String getValue(String name, String value);
    boolean setValue(String name, String value);
    int getValue(String name, int value);
    boolean setValue(String name, int value);
    boolean setValue(String name, long value);
    long getValue(String name, long value);
    boolean getValue(String name, boolean value);
    boolean setValue(String name, boolean value);
    boolean has(String name);
    boolean removeValue(String name);
    boolean clear();
    boolean logOut();
    boolean logIn();

}
