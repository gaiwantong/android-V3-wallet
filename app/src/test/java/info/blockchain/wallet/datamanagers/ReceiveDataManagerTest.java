package info.blockchain.wallet.datamanagers;

import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricGradleTestRunner.class)
public class ReceiveDataManagerTest extends RxTest {

    private ReceiveDataManager mSubject;
    private static final String TEST_URI = "bitcoin://1234567890";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mSubject = new ReceiveDataManager();
    }

    @Test
    public void generateQrCode() throws Exception {
        // Arrange
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        // Act
        mSubject.generateQrCode(TEST_URI, 100).toBlocking().subscribe(subscriber);
        // Assert
        Bitmap bitmap = subscriber.getOnNextEvents().get(0);
        assertNotNull(bitmap);
        assertEquals(100, bitmap.getWidth());
        assertEquals(100, bitmap.getHeight());
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void generateQrCodeNullUri() throws Exception {
        // Arrange
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        // Act
        mSubject.generateQrCode(null, 100).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(Throwable.class);
    }

    @Test
    public void generateQrCodeInvalidDimensions() throws Exception {
        // Arrange
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        // Act
        mSubject.generateQrCode(TEST_URI, -1).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(Throwable.class);
    }

}