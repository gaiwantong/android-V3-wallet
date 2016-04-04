package wallet.util;

import android.content.Context;

import info.blockchain.wallet.util.DateUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(MockitoJUnitRunner.class)
public class DateUtilTest {

    @Mock
    Context mMockContext;

    @Test
    public void simpleTest() {
        DateUtil myObjectUnderTest = DateUtil.getInstance(mMockContext);
        assertThat(myObjectUnderTest.formatted(1453202658), is("January 19"));
    }
}