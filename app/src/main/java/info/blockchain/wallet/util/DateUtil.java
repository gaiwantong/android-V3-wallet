package info.blockchain.wallet.util;

import java.util.Date;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.text.format.DateUtils;

import info.blockchain.wallet.R;

public class DateUtil {

    private static DateUtil instance = null;
    private static Date now = null;
    private static Context context = null;

    private DateUtil() { ; }

    public static DateUtil getInstance(Context ctx) {

        now = new Date();
        context = ctx;

        if(instance == null) {
            instance = new DateUtil();
        }

        return instance;
    }

    public String formatted(long date) {
        String ret = null;

        date *= 1000L;
        long hours24 = 60L * 60L * 24L * 1000L;
        long now = System.currentTimeMillis();

        Calendar calNow = Calendar.getInstance();
        calNow.setTime(new Date(now));
        int nowDay = calNow.get(Calendar.DAY_OF_MONTH);

        Calendar calThen = Calendar.getInstance();
        calThen.setTime(new Date(date));
        int thenYear = calThen.get(Calendar.YEAR);
        int thenDay = calThen.get(Calendar.DAY_OF_MONTH);
        int thenMonth = calThen.get(Calendar.MONTH);

        // within 24h
        if(now - date < hours24) {
            if(thenDay < nowDay) {
                ret = context.getString(R.string.YESTERDAY);
            }
            else {
                ret = (String)DateUtils.getRelativeTimeSpanString (date, now, DateUtils.SECOND_IN_MILLIS, 0);
            }
        }
        // within 48h
        else if(now - date < (hours24 * 2)) {
            ret = context.getString(R.string.YESTERDAY);
        }
        else {
            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            String day = calThen.getDisplayName(Calendar.DAY_OF_MONTH, Calendar.LONG, Locale.getDefault());
            ret = month + " " + thenDay;
        }

        return ret;
    }

}
