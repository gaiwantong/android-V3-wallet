package info.blockchain.wallet.datamanagers;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Tx;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class TransactionListDataManagerTest extends RxTest {

    @Mock PayloadManager mPayloadManager;
    @InjectMocks TransactionListDataManager mSubject;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void generateTransactionListInvalidObject() throws Exception {
        // Arrange
        exception.expect(IllegalArgumentException.class);
        // Act
        mSubject.generateTransactionList(new Object());
        // Assert

    }

    @Test
    public void generateTransactionListAccountTagAllPayloadUpgraded() throws Exception {
        // Arrange
        Account account = new Account();
        account.setTags(new ArrayList<String>(){{
            add("TAG_ALL");
        }});
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.generateTransactionList(account);
        // Assert

    }

    @Test
    public void generateTransactionListAccountTagAllPayloadNotUpgraded() throws Exception {
        // Arrange
        Account account = new Account();
        account.setTags(new ArrayList<String>(){{
            add("TAG_ALL");
        }});
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(false);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.generateTransactionList(account);
        // Assert

    }

    @Test
    public void generateTransactionListAccountImportedAddresses() throws Exception {
        // Arrange
        Account account = new Account();
        account.setTags(new ArrayList<String>(){{
            add("TAG_IMPORTED_ADDRESSES");
        }});
        // Act
        mSubject.generateTransactionList(account);
        // Assert

    }

    @Test
    public void generateTransactionAccountListNoTags() throws Exception {
        // Arrange
        Account account = new Account();
        account.setXpub("test");
        MultiAddrFactory.getInstance().setXpubAmount("test", 0L);
        // Act
        mSubject.generateTransactionList(account);
        // Assert

    }

    @Test
    public void generateTransactionLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("addr");
        // Act
        mSubject.generateTransactionList(legacyAddress);
        // Assert

    }

    @Test
    public void getTransactionList() throws Exception {
        // Arrange

        // Act
        List<Tx> value = mSubject.getTransactionList();
        // Assert
        assertEquals(mSubject.mTransactionList, value);
        assertEquals(Collections.emptyList(), value);
    }

    @Test
    public void clearTransactionList() throws Exception {
        // Arrange
        mSubject.mTransactionList.add(new Tx("", "", "", 0D, 0L, new HashMap<>()));
        // Act
        mSubject.clearTransactionList();
        // Assert
        assertEquals(Collections.emptyList(), mSubject.getTransactionList());
    }

    @Test
    public void insertTransactionIntoListAndReturnSorted() throws Exception {
        // Arrange
        Tx tx0 = new Tx("", "", "", 0D, 0L, new HashMap<>());
        Tx tx1 = new Tx("", "", "", 0D, 500L, new HashMap<>());
        Tx tx2 = new Tx("", "", "", 0D, 1000L, new HashMap<>());

        mSubject.mTransactionList.addAll(Arrays.asList(tx1, tx0));
        // Act
        List<Tx> value = mSubject.insertTransactionIntoListAndReturnSorted(tx2);
        // Assert
        assertNotNull(value);
        assertEquals(tx2, value.get(0));
        assertEquals(tx1, value.get(1));
        assertEquals(tx0, value.get(2));
    }

    @Test
    public void getAllXpubAndLegacyTxs() {
        // Arrange
        Tx tx0 = new Tx("", "", "", 0D, 0L, new HashMap<>());
        Tx tx1 = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx1.setHash("hash");
        MultiAddrFactory.getInstance().getXpubTxs().put("test", new ArrayList<Tx>() {{
            add(tx0);
            add(tx0);
        }});
        MultiAddrFactory.getInstance().getLegacyTxs().add(tx0);
        MultiAddrFactory.getInstance().getLegacyTxs().add(tx1);
        // Act
        List<Tx> value = mSubject.getAllXpubAndLegacyTxs();
        // Assert
        assertNotNull(value);
        assertEquals(2, value.size());
    }

    @Test()
    public void getBtcBalanceInvalidObject() throws Exception {
        // Arrange
        exception.expect(IllegalArgumentException.class);
        // Act
        double value = mSubject.getBtcBalance(new Object());
        // Assert
        assertEquals(0D, value, 0D);
    }

    @Test()
    public void getBtcBalanceAccountTagAllUpgraded() throws Exception {
        // Arrange
        Account account = new Account();
        account.setTags(new ArrayList<String>() {{
            add("TAG_ALL");
        }});
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        double value = mSubject.getBtcBalance(account);
        // Assert
        assertEquals(0D, value, 0D);
    }

    @Test()
    public void getBtcBalanceAccountTagAllNotUpgraded() throws Exception {
        // Arrange
        Account account = new Account();
        account.setTags(new ArrayList<String>() {{
            add("TAG_ALL");
        }});
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(false);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        double value = mSubject.getBtcBalance(account);
        // Assert
        assertEquals(0D, value, 0D);
    }

    @Test()
    public void getBtcBalanceAccountTagImported() throws Exception {
        // Arrange
        Account account = new Account();
        account.setTags(new ArrayList<String>() {{
            add("TAG_IMPORTED_ADDRESSES");
        }});
        // Act
        double value = mSubject.getBtcBalance(account);
        // Assert
        assertEquals(0D, value, 0D);
    }

    @Test()
    public void getBtcBalanceAccountV3Individual() throws Exception {
        // Arrange
        Account account = new Account();
        account.setXpub("test");
        MultiAddrFactory.getInstance().getXpubAmounts().put("test", 0L);
        // Act
        double value = mSubject.getBtcBalance(account);
        // Assert
        assertEquals(0D, value, 0D);
    }

    @Test()
    public void getBtcBalanceLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        // Act
        double value = mSubject.getBtcBalance(legacyAddress);
        // Assert
        assertEquals(0D, value, 0D);
    }

    @Test
    public void getTransactionFromHash() throws Exception {
        // Arrange
        TestSubscriber subscriber = new TestSubscriber();
        // Act
        mSubject.getTransactionFromHash("1c12443203a48f42cdf7b1acee5b4b1c1fedc144cb909a3bf5edbffafb0cd204").toBlocking().subscribe(subscriber);
        // Assert
        assertNotNull(subscriber.getOnNextEvents().get(0));
        subscriber.onCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void updateTransactionNotes() throws Exception {
        // Arrange
        TestSubscriber subscriber = new TestSubscriber();
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getNotes()).thenReturn(new HashMap<>());
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mPayloadManager.savePayloadToServer()).thenReturn(true);
        // Act
        mSubject.updateTransactionNotes("hash", "notes").toBlocking().subscribe(subscriber);
        // Assert
        assertEquals(subscriber.getOnNextEvents().get(0), true);
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

}