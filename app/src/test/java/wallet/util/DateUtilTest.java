package wallet.util;

import android.content.Context;

import info.blockchain.wallet.util.DateUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.SimpleDateFormat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DateUtilTest {

    @Mock
    Context mMockContext;

    @Test
    public void dateFormatTest() {
        DateUtil dateUtil = new DateUtil(mMockContext);

        //unit test for 'Today' and 'Yesterday' uses android framework (code unchanged)

        assertThat(dateUtil.formatted(parseDateTime("2016-01-01 00:00:00")), is("January 1"));
        assertThat(dateUtil.formatted(parseDateTime("2015-12-31 23:59:59")), is("December 31, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-01-01 00:00:00")), is("January 1, 2015"));

        assertThat(dateUtil.formatted(parseDateTime("2016-04-15 00:00:00")), is("April 15"));
        assertThat(dateUtil.formatted(parseDateTime("2016-04-15 12:00:00")), is("April 15"));
        assertThat(dateUtil.formatted(parseDateTime("2016-04-15 23:00:00")), is("April 15"));
        assertThat(dateUtil.formatted(parseDateTime("2016-04-15 23:59:59")), is("April 15"));

        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 00:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 12:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 23:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 23:59:59")), is("April 15, 2015"));
    }

    private long parseDateTime(String time) {
        try { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime() / 1000; } catch (Exception e){}; return 0;
    }
}