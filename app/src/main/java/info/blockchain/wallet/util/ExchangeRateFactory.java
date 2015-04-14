package info.blockchain.wallet.util;


import java.util.HashMap;

import android.content.Context;
//import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
* This class obtains info on the currencies communicated via http://blockchain.info/ticker.
* 
*/
public class ExchangeRateFactory	{

	private static Context context = null;

	private static String strData = null;

    private static HashMap<String,Double> fxRates = null;
    private static HashMap<String,String> fxSymbols = null;
    
    private static ExchangeRateFactory instance = null;

    /**
    * Currencies handles by http://blockchain.info/ticker
    *
    */
    private static String[] currencies = {
    	   "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "DKK", "EUR", "GBP", "HKD",
    	   "ISK", "JPY", "KRW", "NZD", "PLN", "RUB", "SEK", "SGD", "THB", "TWD", "USD"
	};

    /*
	private static String[] currencies = {
	    "United States Dollar - USD",
	    "Euro - EUR",
	    "British Pound Sterling - GBP",
	    "Indian Rupee - INR",
	    "Australian Dollar - AUD",
	    "Canadian Dollar - CAD",
	    "Arab Emirates Dirham - AED",
	    "Argentine Peso - ARS",
	    "Aruban Florin - AWG",
	    "Convertible Mark - BAM",
	    "Barbadian Dollar - BBD",
	    "Bangladeshi Taka - BDT",
	    "Bulgarian Lev - BGN",
	    "Bahraini Dinar - BHD",
	    "Bermudian Dollar - BMD",
	    "Bolivian Boliviano - BOB",
	    "Brazilian Real - BRL",
	    "Bahamian Dollar - BSD",
	    "Swiss Franc - CHF",
	    "Chilean Peso - CLP",
	    "Chinese Yuan - CNY",
	    "Colombian Peso - COP",
	    "Czech Koruna - CZK",
	    "Danish Krone - DKK",
	    "Dominican Peso - DOP",
	    "Egyptian Pound - EGP",
	    "Fijian Dollar - FJD",
	    "Ghana Cedi - GHS",
	    "Gambian Dalasi - GMD",
	    "Guatemalan Quetzal - GTQ",
	    "Hong Kong Dollar - HKD",
	    "Croatian Kuna - HRK",
	    "Hungarian Forint - HUF",
	    "Indonesian Rupiah - IDR",
	    "Israeli Sheqel - ILS",
	    "Icelandic Krona - ISK",
	    "Jamaican Dollar - JMD",
	    "Jordanian Dinar - JOD",
	    "Japanese Yen - JPY",
	    "Kenyan Shilling - KES",
	    "Cambodian Riel - KHR",
	    "South Korean Won - KRW",
	    "Kuwaiti Dinar - KWD",
	    "Lao Kip - LAK",
	    "Lebanese Pound - LBP",
	    "Sri Lankan Rupee - LKR",
	    "Lithuanian Litas - LTL",
	    "Moroccan Dirham - MAD",
	    "Moldovan Leu - MDL",
	    "Malagasy Ariary - MGA",
	    "Macedonian Denar - MKD",
	    "Mauritian Rupee - MUR",
	    "Maldivian Rufiyaa - MVR",
	    "Mexican Peso - MXN",
	    "Malaysian Ringgit - MYR",
	    "Namibian Dollar - NAD",
	    "Nigerian Naira - NGN",
	    "Norwegian Krone - NOK",
	    "Nepalese Rupee - NPR",
	    "New Zealand Dollar - NZD",
	    "Omani Rial - OMR",
	    "Panamanian Balboa - PAB",
	    "Peruvian Sol - PEN",
	    "Philippine Peso - PHP",
	    "Pakistani Rupee - PKR",
	    "Polish Zloty - PLN",
	    "Paraguayan Guaraní - PYG",
	    "Qatari Riyal - QAR",
	    "Romanian Leu - RON",
	    "Serbian Dinar - RSD",
	    "Russian Rouble - RUB",
	    "Saudi Riyal - SAR",
	    "Seychellois Rupee - SCR",
	    "Swedish Krona - SEK",
	    "Singapore Dollar - SGD",
	    "Syrian Pound - SYP",
	    "Thai Baht - THB",
	    "Tunisian Dinar - TND",
	    "Turkish Lira - TRY",
	    "Taiwanese Dollar - TWD",
	    "Ukraine Hryvnia - UAH",
	    "Ugandan Shilling - UGX",
	    "Uruguayan Peso - UYU",
	    "Venezuelan Bolívar - VEF",
	    "Vietnamese Dong - VND",
	    "Central African Franc - XAF",
	    "East Caribbean Dollar - XCD",
	    "West African Franc - XOF",
	    "CFP Franc - XPF",
	    "South African Rand - ZAR"
		};
	*/

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

    
    private ExchangeRateFactory()	 { ; }

    public static ExchangeRateFactory getInstance(Context ctx)	 {
    	
    	context = ctx;
    	
    	if(instance == null)	 {
        	fxRates = new HashMap<String,Double>();
        	fxSymbols = new HashMap<String,String>();

    		instance = new ExchangeRateFactory();
    	}

    	return instance;
    }

    public double getLastPrice(String currency)	 {
    	if(fxRates.get(currency) != null && fxRates.get(currency) > 0.0)	 {
            PrefsUtil.getInstance(context).setValue("CANNED_" + currency, Double.toString(fxRates.get(currency)));
    		return fxRates.get(currency);
    	}
    	else	 {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("CANNED_" + currency, "0.0"));
    	}
    }

    public String getSymbol(String currency)	 {
    	if(fxSymbols.get(currency) != null)	 {
    		return fxSymbols.get(currency);
    	}
    	else	 {
    		return null;
    	}
    }

    public String[] getCurrencies()	 {
    	return currencies;
    }

    public String[] getCurrencyLabels()	 {
    	return currencyLabels;
    }

    public void setData(String data)	 {
    	strData = data;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void updateFxPricesForEnabledCurrencies()	 {
    	for(int i = 0; i < currencies.length; i++)	 {
        	setFxPriceForCurrency(currencies[i]);
    	}
    }

    private void setFxPriceForCurrency(String currency)	 {
        try {
    		JSONObject jsonObject = new JSONObject(strData);
    		if(jsonObject != null)	{
    			JSONObject jsonCurr = jsonObject.getJSONObject(currency);
        		if(jsonCurr != null)	{
        			double last_price = jsonCurr.getDouble("last");
        			fxRates.put(currency, Double.valueOf(last_price));
//        			Log.i("ExchangeRateFactory", currency + "  " + Double.valueOf(last_price));
        			String symbol = jsonCurr.getString("symbol");
        			fxSymbols.put(currency, symbol);
        		}
    		}
    	} catch (JSONException je) {
			fxRates.put(currency, Double.valueOf(-1.0));
			fxSymbols.put(currency, null);
    	}
    }
}
