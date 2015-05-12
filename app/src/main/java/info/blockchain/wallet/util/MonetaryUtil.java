package info.blockchain.wallet.util;

import android.content.Context;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;
import java.text.NumberFormat;

public class MonetaryUtil {

    private static Context context = null;

    private static CharSequence[] btcUnits = { "BTC", "mBTC", "bits" };
    public static final int UNIT_BTC = 0;
    public static final int MILLI_BTC = 1;
    public static final int MICRO_BTC = 2;

    private static MonetaryUtil instance = null;
	private static NumberFormat btcFormat = null;
	private static NumberFormat fiatFormat = null;

	private MonetaryUtil() { ; }

	public static MonetaryUtil getInstance() {
		
		if(instance == null) {
        	fiatFormat = NumberFormat.getInstance(Locale.getDefault());
        	fiatFormat.setMaximumFractionDigits(2);
        	fiatFormat.setMinimumFractionDigits(2);

        	btcFormat = NumberFormat.getInstance(Locale.getDefault());
        	btcFormat.setMaximumFractionDigits(8);
        	btcFormat.setMinimumFractionDigits(1);

			instance = new MonetaryUtil();
		}
		
		return instance;
	}

    public static MonetaryUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(2);
            fiatFormat.setMinimumFractionDigits(2);

            btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(8);
            btcFormat.setMinimumFractionDigits(1);

            instance = new MonetaryUtil();
        }

        return instance;
    }

    public NumberFormat getBTCFormat() {
		return btcFormat;
	}

	public NumberFormat getFiatFormat(String fiat) {
    	fiatFormat.setCurrency(Currency.getInstance(fiat));
		return fiatFormat;
	}

    public CharSequence[] getBTCUnits() {
        return btcUnits;
    }

    public String getBTCUnit(int unit) {
        return (String)btcUnits[unit];
    }

    public String getDisplayAmount(long value) {

        String strAmount = null;

        int unit = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = Double.toString((((double) (value * 1000000L)) / 1e8));
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = Double.toString((((double) (value * 1000L)) / 1e8));
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    public BigInteger getUndenominatedAmount(long value) {

        BigInteger amount = BigInteger.ZERO;

        int unit = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = BigInteger.valueOf(value / 1000000L);
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = BigInteger.valueOf(value / 1000L);
                break;
            default:
                amount = BigInteger.valueOf(value);
                break;
        }

        return amount;
    }

    public double getUndenominatedAmount(double value) {

        double amount = 0.0;

        int unit = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = value / 1000000.0;
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = value / 1000.0;
                break;
            default:
                amount = value;
                break;
        }

        return amount;
    }

    public double getDenominatedAmount(double value) {

        double amount = 0.0;

        int unit = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                amount = value * 1000000.0;
                break;
            case MonetaryUtil.MILLI_BTC:
                amount = value * 1000.0;
                break;
            default:
                amount = value;
                break;
        }

        return amount;
    }

    public String getDisplayAmountWithFormatting(long value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format(((double)(value * 1000000L)) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format(((double)(value * 1000L)) / 1e8);
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

    public String getDisplayAmountWithFormatting(double value) {

        String strAmount = null;
        DecimalFormat df = new DecimalFormat("#");
        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(8);

        int unit = PrefsUtil.getInstance(context).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        switch(unit) {
            case MonetaryUtil.MICRO_BTC:
                strAmount = df.format((value * 1000000.0) / 1e8);
                break;
            case MonetaryUtil.MILLI_BTC:
                strAmount = df.format((value * 1000.0) / 1e8);
                break;
            default:
                strAmount = MonetaryUtil.getInstance().getBTCFormat().format(value / 1e8);
                break;
        }

        return strAmount;
    }

}
