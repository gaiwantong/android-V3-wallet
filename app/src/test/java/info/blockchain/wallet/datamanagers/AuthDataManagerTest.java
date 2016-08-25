package info.blockchain.wallet.datamanagers;

import android.app.Application;

import info.blockchain.api.Access;
import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AESUtilWrapper;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.PrefsUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.di.ApiModule;
import piuk.blockchain.android.di.ApplicationModule;
import piuk.blockchain.android.di.DataManagerModule;
import piuk.blockchain.android.di.Injector;
import piuk.blockchain.android.di.InjectorTestUtils;
import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by adambennett on 15/08/2016.
 */
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricGradleTestRunner.class)
public class AuthDataManagerTest extends RxTest {

    private static final String STRING_TO_RETURN = "string_to_return";

    private AuthDataManager mSubject;
    @Mock private PayloadManager mPayloadManager;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private Access mAccess;
    @Mock private AppUtil mAppUtil;
    @Mock private AESUtilWrapper mAesUtils;
    @Mock private AccessState mAccessState;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new DataManagerModule());

        mSubject = new AuthDataManager();
    }

    @Test
    public void getEncryptedPayload() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        mSubject.getEncryptedPayload("1234567890", "1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        mSubject.getSessionId("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getSessionId(anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    @Test
    public void validatePin() throws Exception {
        // Arrange
        TestSubscriber<CharSequenceX> subscriber = new TestSubscriber<>();
        CharSequenceX charSequenceX = new CharSequenceX("1234567890");
        when(mAccessState.validatePIN(anyString())).thenReturn(charSequenceX);
        // Act
        mSubject.validatePin(anyString()).toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccessState).validatePIN(anyString());
        subscriber.assertCompleted();
        subscriber.onNext(charSequenceX);
        subscriber.assertNoErrors();
    }

    @Test
    public void createPin() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(mAccessState.createPIN(any(CharSequenceX.class), anyString())).thenReturn(true);
        // Act
        mSubject.createPin(any(CharSequenceX.class), anyString()).toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccessState).createPIN(any(CharSequenceX.class), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(true);
        subscriber.assertNoErrors();
    }

    @Test
    public void createHdWallet() throws Exception {
        // Arrange
        TestSubscriber<Payload> subscriber = new TestSubscriber<>();
        Payload payload = new Payload();
        when(mPayloadManager.createHDWallet(anyString(), anyString())).thenReturn(payload);
        // Act
        mSubject.createHdWallet("", "").toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).createHDWallet(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mAppUtil).setNewlyCreated(true);
        verify(mPrefsUtil).setValue(eq(PrefsUtil.KEY_GUID), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(payload);
        subscriber.assertNoErrors();
    }

    @Test
    public void restoreHdWallet() throws Exception {
        // Arrange
        TestSubscriber<Payload> subscriber = new TestSubscriber<>();
        Payload payload = new Payload();
        when(mPayloadManager.restoreHDWallet(anyString(), anyString(), anyString())).thenReturn(payload);
        // Act
        mSubject.restoreHdWallet("", "", "").toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).restoreHDWallet(anyString(), anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mPrefsUtil).setValue(eq(PrefsUtil.KEY_GUID), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(payload);
        subscriber.assertNoErrors();
    }

    /**
     * Payload returns null, which indicates save failure. Should throw an Exception
     */
    @Test
    public void restoreHdWalletNullPayload() throws Exception {
        // Arrange
        TestSubscriber<Payload> subscriber = new TestSubscriber<>();
        when(mPayloadManager.restoreHDWallet(anyString(), anyString(), anyString())).thenReturn(null);
        // Act
        mSubject.restoreHdWallet("", "", "").toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).restoreHDWallet(anyString(), anyString(), anyString());
        verifyZeroInteractions(mAppUtil);
        verifyZeroInteractions(mPrefsUtil);
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    /**
     * Access returns a valid payload, Observable should complete successfully
     */
    @Test
    public void startPollingAuthStatusSuccess() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mAccess.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        mSubject.startPollingAuthStatus("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getSessionId(anyString());
        verify(mAccess).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(STRING_TO_RETURN);
        subscriber.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns error, should be caught by Observable and transformed into
     * {@link Access#KEY_AUTH_REQUIRED}
     */
    @Test
    public void startPollingAuthStatusError() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mAccess.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenThrow(mock(RuntimeException.class));
        // Act
        mSubject.startPollingAuthStatus("1234567890").toBlocking().subscribe(subscriber);
        // Assert
        verify(mAccess).getSessionId(anyString());
        verify(mAccess).getEncryptedPayload(anyString(), anyString());
        subscriber.assertCompleted();
        subscriber.onNext(Access.KEY_AUTH_REQUIRED);
        subscriber.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns Auth Required, should be filtered out and emit no values.
     */
    @Test
    public void startPollingAuthStatusAccessRequired() throws Exception {
        // Arrange
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        when(mAccess.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        when(mAccess.getEncryptedPayload(anyString(), anyString())).thenReturn(Access.KEY_AUTH_REQUIRED);
        // Act
        mSubject.startPollingAuthStatus("1234567890").take(1, TimeUnit.SECONDS).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoValues();
        subscriber.assertNoErrors();
    }

    /**
     * Update payload completes successfully, should set temp password and complete with no errors
     */
    @Test
    public void initiatePayloadSuccess() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        verify(mPayloadManager).setTempPassword(any(CharSequenceX.class));
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    /**
     * Update payload returns a pairing failure, Observable should throw {@link AuthDataManager.PairFailThrowable}
     */
    @Test
    public void initiatePayloadPairFail() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitPairFail();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(AuthDataManager.PairFailThrowable.class);
    }

    /**
     * Update payload returns a create failure, Observable should throw {@link AuthDataManager.CreateFailThrowable}
     */
    @Test
    public void initiatePayloadCreateFail() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitCreateFail("1234567890");
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(AuthDataManager.CreateFailThrowable.class);
    }

    /**
     * PayloadManager throws exception, should trigger onError
     */
    @Test
    public void initiatePayloadException() throws Exception {
        // Arrange
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        doThrow(new RuntimeException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNotCompleted();
        subscriber.assertError(Throwable.class);
    }

    @Test
    public void createCheckEmailTimer() throws Exception {
        // Arrange
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        // Act
        mSubject.createCheckEmailTimer().take(1).toBlocking().subscribe(subscriber);
        mSubject.timer = 1;
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void attemptDecryptPayloadInvalidPayload() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                "1234567890",
                listener);
        // Assert
        verify(listener).onFatalError();
    }

    @Test
    public void attemptDecryptPayloadSuccessful() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onSuccess();
    }

    @Test
    public void attemptDecryptPayloadInitPairFail() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitPairFail();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onPairFail();
    }

    @Test
    public void attemptDecryptPayloadFatalError() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new Exception()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onFatalError();
    }

    @Test
    public void attemptDecryptPayloadCreateFail() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onInitCreateFail("1234567890");
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onCreateFail();
    }

    @Test
    public void attemptDecryptPayloadDecryptionFailed() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(null);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onAuthFail();
    }

    @Test
    public void attemptDecryptPayloadDecryptionThrowsException() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenThrow(mock(RuntimeException.class));
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onFatalError();
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected AESUtilWrapper provideAesUtils() {
            return mAesUtils;
        }

        @Override
        protected AccessState provideAccessState() {
            return mAccessState;
        }
    }

    private class MockApiModule extends ApiModule {

        @Override
        protected Access provideAccess() {
            return mAccess;
        }

        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }
    }

    private static final String TEST_PAYLOAD = "{\n" +
            "  \"payload\": \"test payload\",\n" +
            "  \"pbkdf2_iterations\": 2000\n" +
            "}";

    private static final String DECRYPTED_PAYLOAD = "{\n" +
            "\t\"sharedKey\": \"1234567890\"\n" +
            "}";



}