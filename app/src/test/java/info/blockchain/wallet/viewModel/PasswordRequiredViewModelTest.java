package info.blockchain.wallet.viewModel;

import android.app.Application;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.PasswordRequiredActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.di.ApiModule;
import piuk.blockchain.android.di.ApplicationModule;
import piuk.blockchain.android.di.Injector;
import piuk.blockchain.android.di.InjectorTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by adambennett on 10/08/2016.
 */
public class PasswordRequiredViewModelTest extends RxTest {

    private PasswordRequiredViewModel mSubject;

    @Mock private PasswordRequiredActivity mActivity;
    @Mock protected AppUtil mAppUtil;
    @Mock protected PrefsUtil mPrefsUtil;
    @Mock protected PayloadManager mPayloadManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(), new MockApplicationModule(new BlockchainTestApplication()), new MockApiModule());

        mSubject = new PasswordRequiredViewModel(mActivity);
    }

    /**
     * Password is missing, should trigger {@link PasswordRequiredActivity#restartApp()} ()}
     */
    @Test
    public void onContinueClickedNoPassword() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("");
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity, times(1)).showToast(anyInt(), anyString());
        verify(mActivity, times(1)).restartApp();
        verify(mActivity, times(1)).resetPasswordField();
    }

    /**
     * Password is correct, should trigger {@link PasswordRequiredActivity#goToPinPage()}
     */
    @Ignore
    @Test
    public void onContinueClickedCorrectPassword() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(),
                anyString(),
                any(CharSequenceX.class),
                any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity, times(1)).showToast(anyInt(), anyString());
        verify(mActivity, times(1)).goToPinPage();
    }

    /**
     * PayloadManager returns a pairing failure, should trigger {@link PasswordRequiredActivity#restartApp()}
     */
    @Ignore
    @Test
    public void onContinueClickedIncorrectPassword() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitPairFail();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(),
                anyString(),
                any(CharSequenceX.class),
                any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity, times(1)).showToast(anyInt(), anyString());
        verify(mActivity, times(1)).restartApp();
    }

    /**
     * PayloadManager returns wallet creation failure, should trigger {@link PasswordRequiredActivity#restartApp()}
     */
    @Ignore
    @Test
    public void onContinueClickedCreateFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitCreateFail("error string");
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(),
                anyString(),
                any(CharSequenceX.class),
                any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity, times(1)).showToast(anyInt(), anyString());
        verify(mActivity, times(1)).restartApp();
    }

    /**
     * PayloadManager throws exception, should trigger {@link PasswordRequiredActivity#restartApp()}
     */
    @Ignore
    @Test
    public void onContinueClickedThrowException() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        doAnswer(invocation -> {
            throw new Exception("Test exception");
        }).when(mPayloadManager).initiatePayload(
                anyString(),
                anyString(),
                any(CharSequenceX.class),
                any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity, times(1)).showToast(anyInt(), anyString());
        verify(mActivity, times(1)).restartApp();
    }

    @Test
    public void onForgetWalletClicked() throws Exception {
        // Arrange

        // Act
        mSubject.onForgetWalletClicked();
        // Assert
        verify(mAppUtil, times(1)).clearCredentialsAndRestart();
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        assertEquals(util, mAppUtil);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        mSubject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @Test
    public void destroy() throws Exception {
        // Arrange

        // Act
        mSubject.destroy();
        // Assert
        assertFalse(mSubject.mCompositeSubscription.hasSubscriptions());
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }
    }

    private class MockApiModule extends ApiModule {
        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }
    }
}