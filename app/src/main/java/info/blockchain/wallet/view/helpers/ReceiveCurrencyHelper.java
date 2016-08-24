package info.blockchain.wallet.view.helpers;

import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class ReceiveCurrencyHelper {

    private MonetaryUtil mMonetaryUtil;
    private PrefsUtil mPrefsUtil;
    private Locale mLocale;
    private ExchangeRateFactory mExchangeRateFactory;

    public ReceiveCurrencyHelper(MonetaryUtil monetaryUtil, PrefsUtil prefsUtil, Locale locale, ExchangeRateFactory exchangeRateFactory) {
        mMonetaryUtil = monetaryUtil;
        mPrefsUtil = prefsUtil;
        mLocale = locale;
        mExchangeRateFactory = exchangeRateFactory;
    }

    /**
     * Get saved BTC unit - BTC, mBits or bits
     * @return  The saved BTC unit
     */
    public String getBtcUnit() {
        return mMonetaryUtil.getBTCUnit(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    /**
     * Get save Fiat currency unit
     * @return  The saved Fiat unit
     */
    public String getFiatUnit() {
        return mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    /**
     * Get last price for saved currency
     * @return  A double exchange rate
     */
    public double getLastPrice() {
        return mExchangeRateFactory.getLastPrice(getFiatUnit());
    }

    /**
     * Get correctly formatted BTC currency String, ie with region specific separator
     * @param amount    The amount of Bitcoin in either BTC, mBits or bits
     * @return          A region formatted BTC string for the saved unit
     */
    public String getFormattedBtcString(double amount) {
        return mMonetaryUtil.getBTCFormat().format(getDenominatedBtcAmount(amount));
    }

    /**
     * Get correctly formatted Fiat currency String, ie with region specific separator
     * @param amount    The amount of currency as a double
     * @return          A region formatted string
     */
    public String getFormattedFiatString(double amount) {
        return mMonetaryUtil.getFiatFormat(getFiatUnit()).format(amount);
    }

    /**
     * Get the amount of Bitcoin in BTC
     * @param amount    The amount to be converted as a long
     * @return          The amount of Bitcoin as a {@link BigInteger} value
     */
    public BigInteger getUndenominatedAmount(long amount) {
        return mMonetaryUtil.getUndenominatedAmount(amount);
    }

    /**
     * Get the amount of Bitcoin in the saved BTC unit format
     * @param amount    The amount to be converted as a long
     * @return          The amount of BTC/mBits/bits as a double
     */
    public double getUndenominatedAmount(double amount) {
        return mMonetaryUtil.getUndenominatedAmount(amount);
    }

    /**
     * Get the amount of Bitcoin in BTC from BTC, mBits or bits
     * @param amount    An amount of bitcoin in any denomination
     * @return          The amount of bitcoin in BTC
     */
    public Double getDenominatedBtcAmount(double amount) {
        return mMonetaryUtil.getDenominatedAmount(amount);
    }

    /**
     * Get the maximum number of decimal points for the saved BTC unit
     * @return  The max number of allowed decimal points
     */
    public int getMaxBtcDecimalLength() {
        int unit = mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        int maxLength;
        switch (unit) {
            case MonetaryUtil.MICRO_BTC:
                maxLength = 2;
                break;
            case MonetaryUtil.MILLI_BTC:
                maxLength = 4;
                break;
            default:
                maxLength = 8;
                break;
        }
        return maxLength;
    }

    /**
     * Parse String value to region formatted long
     * @param amount    A string to be parsed
     * @return          The amount as a long, formatted for the current region
     */
    public long getLongAmount(String amount) {
        try {
            return Math.round(NumberFormat.getInstance(mLocale).parse(amount).doubleValue() * 1e8);
        } catch (ParseException e) {
            return 0L;
        }
    }

    /**
     * Parse String value to region formatted double
     * @param amount    A string to be parsed
     * @return          The amount as a double, formatted for the current region
     */
    public double getDoubleAmount(String amount) {
        try {
            return NumberFormat.getInstance(mLocale).parse(amount).doubleValue();
        } catch (ParseException e) {
            return 0D;
        }
    }

    /**
     * Return false if value is higher than the sum of all Bitcoin in future existence
     * @param amount    A {@link BigInteger} amount of Bitcoin in BTC
     * @return          True if amount higher than 21 Million
     */
    public boolean getIfAmountInvalid(BigInteger amount) {
        return amount.compareTo(BigInteger.valueOf(2100000000000000L)) == 1;
    }

}
