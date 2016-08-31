package info.blockchain.wallet.view.helpers;

import android.app.Application;

import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import piuk.blockchain.android.di.ApiModule;
import piuk.blockchain.android.di.ApplicationModule;
import piuk.blockchain.android.di.DataManagerModule;
import piuk.blockchain.android.di.Injector;
import piuk.blockchain.android.di.InjectorTestUtils;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReceiveCurrencyHelperTest {

    private ReceiveCurrencyHelper mSubject;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private ExchangeRateFactory mExchangeRateFactory;
    @Mock private MonetaryUtil mMonetaryUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new DataManagerModule()
        );

        mSubject = new ReceiveCurrencyHelper(mMonetaryUtil, Locale.UK);
    }

    @Test
    public void getBtcUnit() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(1);
        when(mMonetaryUtil.getBTCUnit(1)).thenReturn("mBTC");
        // Act
        String value = mSubject.getBtcUnit();
        // Assert
        assertEquals("mBTC", value);
    }

    @Test
    public void getFiatUnit() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)).thenReturn("GBP");
        // Act
        String value = mSubject.getFiatUnit();
        // Assert
        assertEquals("GBP", value);
    }

    @Test
    public void getLastPrice() throws Exception {
        // Arrange
        when(mExchangeRateFactory.getLastPrice(anyString())).thenReturn(1000D);
        // Act
        double value = mSubject.getLastPrice();
        // Assert
        assertEquals(1000D, value);
    }

    @Test
    public void getFormattedBtcString() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0);
        NumberFormat format = DecimalFormat.getInstance(Locale.US);
        when(mMonetaryUtil.getBTCFormat()).thenReturn(format);
        when(mMonetaryUtil.getDenominatedAmount(anyDouble())).thenReturn(13.37);
        // Act
        String value = mSubject.getFormattedBtcString(13.37D);
        // Assert
        assertEquals("13.37", value);
    }

    @Test
    public void getFormattedFiatString() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)).thenReturn("USD");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        when(mMonetaryUtil.getFiatFormat(anyString())).thenReturn(format);
        /// Act
        String value = mSubject.getFormattedFiatString(13.37D);
        // Assert
        assertEquals("$13.37", value);
    }

    @Test
    public void getUndenominatedAmountLong() throws Exception {
        // Arrange
        BigInteger mockBigInt = mock(BigInteger.class);
        when(mMonetaryUtil.getUndenominatedAmount(anyLong())).thenReturn(mockBigInt);
        // Act
        BigInteger value = mSubject.getUndenominatedAmount(1337);
        // Assert
        assertEquals(mockBigInt, value);
    }

    @Test
    public void getUndenominatedAmountDouble() throws Exception {
        // Arrange
        when(mMonetaryUtil.getUndenominatedAmount(anyDouble())).thenReturn(13.37D);
        // Act
        Double value = mSubject.getUndenominatedAmount(1337D);
        // Assert
        assertEquals(13.37D, value);
    }

    @Test
    public void getDenominatedBtcAmount() throws Exception {
        // Arrange
        when(mMonetaryUtil.getDenominatedAmount(anyDouble())).thenReturn(1337D);
        // Act
        Double value = mSubject.getDenominatedBtcAmount(13.37D);
        // Assert
        assertEquals(1337D, value);
    }

    @Test
    public void getMaxBtcDecimalLengthMillibtc() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(MonetaryUtil.MILLI_BTC);
        // Act
        Integer value = mSubject.getMaxBtcDecimalLength();
        // Assert
        assertEquals(Integer.valueOf(4), value);
    }

    @Test
    public void getMaxBtcDecimalLengthBtc() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(MonetaryUtil.UNIT_BTC);
        // Act
        Integer value = mSubject.getMaxBtcDecimalLength();
        // Assert
        assertEquals(Integer.valueOf(8), value);
    }

    @Test
    public void getMaxBtcDecimalLengthMicroBtc() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(MonetaryUtil.MICRO_BTC);
        // Act
        Integer value = mSubject.getMaxBtcDecimalLength();
        // Assert
        assertEquals(Integer.valueOf(2), value);
    }

    @Test
    public void getLongAmount() throws Exception {
        // Arrange

        // Act
        Long value = mSubject.getLongAmount("13.37");
        // Assert
        assertEquals(Long.valueOf(1337000000), value);
    }

    @Test
    public void getLongAmountInvalidString() throws Exception {
        // Arrange

        // Act
        Long value = mSubject.getLongAmount("leet");
        // Assert
        assertEquals(Long.valueOf(0), value);
    }

    @Test
    public void getDoubleAmount() throws Exception {
        // Arrange

        // Act
        Double value = mSubject.getDoubleAmount("13.37");
        // Assert
        assertEquals(Double.valueOf(13.37), value);
    }

    @Test
    public void getDoubleAmountInvalidString() throws Exception {
        // Arrange

        // Act
        Double value = mSubject.getDoubleAmount("leet");
        // Assert
        assertEquals(Double.valueOf(0), value);
    }

    @Test
    public void getIfAmountInvalid() throws Exception {
        // Arrange

        // Act
        Boolean value = mSubject.getIfAmountInvalid(new BigInteger("2100000000000001"));
        // Assert
        assertTrue(value);
    }

    private class MockApplicationModule extends ApplicationModule {
        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected ExchangeRateFactory provideExchangeRateFactory() {
            return mExchangeRateFactory;
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }
    }
}