package info.blockchain.wallet.view.helpers;

import android.support.v4.util.Pair;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionHelperTest {

    @Mock PayloadManager mPayloadManager;
    private TransactionHelper mSubject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mSubject = new TransactionHelper(mPayloadManager);
    }

    /**
     * Can't test certain parts of this method as {@link MultiAddrFactory#isOwnHDAddress(String)}
     * can't easily be set or overridden.
     * TODO: Fix this for testing
     */
    @Test
    public void addressToLabelLegacyAddresses() throws Exception {
        // Arrange
        HDWallet hdWallet = new HDWallet();
        hdWallet.getAccounts().add(new Account());
        hdWallet.getAccounts().add(new Account());
        Payload payload = new Payload();
        payload.setHdWallets(hdWallet);
        LegacyAddress address = new LegacyAddress();
        address.setAddress("addr");
        address.setLabel("label");
        ArrayList<LegacyAddress> legacyAddresses = new ArrayList<LegacyAddress>() {{
            add(address);
        }};
        payload.setLegacyAddresses(legacyAddresses);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        // Act
        String value = mSubject.addressToLabel("addr");
        // Assert
        assertEquals("label", value);
    }

    @Test
    public void addressToLabelNotFound() throws Exception {
        // Arrange
        HDWallet hdWallet = new HDWallet();
        hdWallet.getAccounts().add(new Account());
        hdWallet.getAccounts().add(new Account());
        Payload payload = new Payload();
        payload.setHdWallets(hdWallet);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        // Act
        String value = mSubject.addressToLabel("addr");
        // Assert
        assertEquals("addr", value);
    }

    /**
     * Can't test certain parts of this method as {@link MultiAddrFactory#isOwnHDAddress(String)}
     * can't easily be set or overridden.
     * TODO: Fix this for testing
     */
    @Test
    public void filterNonChangeAddressesSingleInput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("RECEIVED");
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(mock(Transaction.xPut.class));
        }};
        when(transaction.getInputs()).thenReturn(inputs);

        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(0, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesMultipleInput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        Transaction.xPut xPut0 = mock(Transaction.xPut.class);
        xPut0.addr = "addr0";
        Transaction.xPut xPut1 = mock(Transaction.xPut.class);
        Transaction.xPut xPut2 = mock(Transaction.xPut.class);
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
            add(xPut1);
            add(xPut2);
        }};
        MultiAddrFactory.getInstance().getAddress2Xpub().put("addr0", "xpub0");
        when(transaction.getInputs()).thenReturn(inputs);

        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(2, value.first.size());
        assertEquals(0, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputSingleOutput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(mock(Transaction.xPut.class));
        }};
        when(transaction.getInputs()).thenReturn(inputs);
        when(transaction.getOutputs()).thenReturn(inputs);

        Payload payload = new Payload();
        ArrayList<LegacyAddress> legacyAddresses = new ArrayList<>();
        payload.setLegacyAddresses(legacyAddresses);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(1, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputMultipleOutput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        Transaction.xPut xPut0 = mock(Transaction.xPut.class);
        xPut0.addr = "addr0";
        xPut0.value = 1L;
        Transaction.xPut xPut1 = mock(Transaction.xPut.class);
        xPut1.addr = "addr1";
        xPut1.value = 1L;
        Transaction.xPut xPut2 = mock(Transaction.xPut.class);
        xPut2.addr = "addr2";
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
        }};
        ArrayList<Transaction.xPut> outputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
            add(xPut1);
            add(xPut2);
        }};
        when(transaction.getInputs()).thenReturn(inputs);
        when(transaction.getOutputs()).thenReturn(outputs);

        Payload mockPayload = mock(Payload.class);
        List<String> legacyStrings = new ArrayList<String>() {{
            add("addr0");
            add("addr1");
        }};
        List<String> watchOnlyStrings = new ArrayList<String>() {{
            add("addr2");
        }};
        when(mockPayload.getLegacyAddressStrings()).thenReturn(legacyStrings);
        when(mockPayload.getWatchOnlyAddressStrings()).thenReturn(watchOnlyStrings);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(1, value.second.size());
    }

}