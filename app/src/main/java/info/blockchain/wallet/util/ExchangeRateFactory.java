package info.blockchain.wallet.util;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.inject.Inject;

import piuk.blockchain.android.di.Injector;

/**
 * This class obtains info on the currencies communicated via https://blockchain.info/ticker
 */
public class ExchangeRateFactory {

    private static String strData = null;
    @Inject protected PrefsUtil mPrefsUtil;

    private static HashMap<String, Double> fxRates = null;
    private static HashMap<String, String> fxSymbols = null;

    private static ExchangeRateFactory instance = null;

    /**
     * Currencies handles by https://blockchain.info/ticker
     */
    private static String[] currencies = {
            "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "DKK", "EUR", "GBP", "HKD",
            "ISK", "JPY", "KRW", "NZD", "PLN", "RUB", "SEK", "SGD", "THB", "TWD", "USD"
    };

    private static String[] currencyLabels = {
            "United States Dollar - USD",
            "Euro - EUR",
            "British Pound Sterling - GBP",
            "Australian Dollar - AUD",
            "Brazilian Real - BRL",
            "Canadian Dollar - CAD",
            "Chinese Yuan - CNY",
            "Swiss Franc - CHF",
            "Chilean Peso - CLP",
            "Danish Krone - DKK",
            "Hong Kong Dollar - HKD",
            "Icelandic Krona - ISK",
            "Japanese Yen - JPY",
            "South Korean Won - KRW",
            "New Zealand Dollar - NZD",
            "Polish Zloty - PLN",
            "Russian Rouble - RUB",
            "Swedish Krona - SEK",
            "Singapore Dollar - SGD",
            "Thai Baht - THB",
            "Taiwanese Dollar - TWD",
    };


    private ExchangeRateFactory() {
        Injector.getInstance().getAppComponent().inject(this);
    }

    public static ExchangeRateFactory getInstance() {
        if (instance == null) {
            fxRates = new HashMap<>();
            fxSymbols = new HashMap<>();

            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public double getLastPrice(String currency) {
        if (fxRates.get(currency) != null && fxRates.get(currency) > 0.0) {
            mPrefsUtil.setValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, Double.toString(fxRates.get(currency)));
            return fxRates.get(currency);
        } else {
            return Double.parseDouble(mPrefsUtil.getValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, "0.0"));
        }
    }

    public String getSymbol(String currency) {
        if (fxSymbols.get(currency) != null) {
            return fxSymbols.get(currency);
        } else {
            return null;
        }
    }

    public String[] getCurrencies() {
        return currencies;
    }

    public String[] getCurrencyLabels() {
        return currencyLabels;
    }

    public void setData(String data) {
        strData = data;
    }

    /**
     * Parse the data supplied to this instance.
     */
    public void updateFxPricesForEnabledCurrencies() {
        for (String currency : currencies) {
            setFxPriceForCurrency(currency);
        }
    }

    private void setFxPriceForCurrency(String currency) {
        try {
            JSONObject jsonObject = new JSONObject(strData);
            JSONObject jsonCurr = jsonObject.getJSONObject(currency);
            if (jsonCurr != null) {
                double last_price = jsonCurr.getDouble("last");
                fxRates.put(currency, last_price);
                String symbol = jsonCurr.getString("symbol");
                fxSymbols.put(currency, symbol);
            }
        } catch (JSONException je) {
            fxRates.put(currency, -1.0);
            fxSymbols.put(currency, null);
        }
    }
}
