package info.blockchain.wallet.util;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import piuk.blockchain.android.R;

public class DateUtil {

    private static DateUtil instance = null;
    private static Context context = null;

    private DateUtil() {
        ;
    }

    public static DateUtil getInstance(Context ctx) {
        context = ctx;

        if (instance == null) {
            instance = new DateUtil();
        }

        return instance;
    }

    public String formatted(long ts) {
        String ret = null;

        Date localTime = new Date(ts);
        long date = localTime.getTime();

        date *= 1000L;
        long hours24 = 60L * 60L * 24L * 1000L;
        long now = System.currentTimeMillis();

        Calendar calNow = Calendar.getInstance();
        calNow.setTime(new Date(now));

        Calendar calThen = Calendar.getInstance();
        calThen.setTime(new Date(date));
        int thenDay = calThen.get(Calendar.DAY_OF_MONTH);

        long yesterdayEnd = getDayEnd(now - hours24);
        long yesterdayStart = getDayStart(now - hours24);
//        long yearStart = getYearStart(now);//TODO

        // today
        if (date > yesterdayEnd) {
            ret = (String) DateUtils.getRelativeTimeSpanString(date, now, DateUtils.SECOND_IN_MILLIS, 0);
        }
        // yesterday
        else if (date < yesterdayEnd && date > yesterdayStart) {
            ret = context.getString(R.string.YESTERDAY);
        }

        //TODO - add year if in previous year - Below to be tested
//        else if(date < yearStart){
//            String year = calThen.getDisplayName(Calendar.YEAR, Calendar.LONG, Locale.getDefault());
//            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
//            String day = calThen.getDisplayName(Calendar.DAY_OF_MONTH, Calendar.LONG, Locale.getDefault());
//            ret = month + " " + thenDay+", "+year;
//        }

        // within current year
        else{
            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            String day = calThen.getDisplayName(Calendar.DAY_OF_MONTH, Calendar.LONG, Locale.getDefault());
            ret = month + " " + thenDay;
        }

        return ret;
    }

    private long parseDateTime(String time) {
        try { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime(); } catch (Exception e){}; return 0;
    }

    private long getDayStart(long time)
    {
        return parseDateTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date(time))+" 00:00:00");
    }

    private long getDayEnd(long time)
    {
        return parseDateTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date(time))+" 23:59:59");
    }

    private long getYearStart(long time)
    {
        return parseDateTime(new SimpleDateFormat("yyyy").format(new Date(time))+"-01-01 00:00:00");
    }
}
