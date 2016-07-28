package info.blockchain.wallet.util;

import org.bitcoinj.uri.BitcoinURI;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by adambennett on 28/07/2016.
 */
public class BitcoinLinkGeneratorTest {

    private static final String TEST_ADDRESS = "test_address";
    private static final Double TEST_AMOUNT = 1.2345D;
    private static final Double TEST_AMOUNT_EMPTY = 0D;
    private static final String VALID_ADDRESS = "13ceUWo48GyCcSHwKpCKZtTkkJfr7jyxbT";
    private static final String VALID_AMOUNT = "1.2345";
    private static final String VALID_URI_NO_AMOUNT = "bitcoin:" + VALID_ADDRESS;
    private static final String VALID_URI_WITH_AMOUNT = "bitcoin:" + VALID_ADDRESS + "?amount=" + VALID_AMOUNT;

    @Test
    public void getLinkNoAmount() throws Exception {
        // Arrange

        // Act
        String uri = BitcoinLinkGenerator.getLink(TEST_ADDRESS, null);
        // Assert
        assertEquals("bitcoin://test_address", uri);
    }

    @Test
    public void getLinkWithEmptyAmount() throws Exception {
        // Arrange

        // Act
        String uri = BitcoinLinkGenerator.getLink(TEST_ADDRESS, TEST_AMOUNT_EMPTY);
        // Assert
        assertEquals("bitcoin://test_address", uri);
    }

    @Test
    public void getLinkWithAmount() throws Exception {
        // Arrange

        // Act
        String uri = BitcoinLinkGenerator.getLink(TEST_ADDRESS, TEST_AMOUNT);
        // Assert
        assertEquals("bitcoin://test_address?amount=1.2345", uri);
    }

    @Test
    public void getLinkWithBitcoinUriNoAmount() throws Exception {
        // Arrange
        BitcoinURI bitcoinURI = new BitcoinURI(VALID_URI_NO_AMOUNT);
        // Act
        String uri = BitcoinLinkGenerator.getLink(bitcoinURI);
        // Assert
        assertEquals("bitcoin://13ceUWo48GyCcSHwKpCKZtTkkJfr7jyxbT", uri);
    }

    @Test
    public void getLinkWithBitcoinUriZeroAmount() throws Exception {
        // Arrange
        BitcoinURI bitcoinURI = new BitcoinURI(VALID_URI_WITH_AMOUNT);
        // Act
        String uri = BitcoinLinkGenerator.getLink(bitcoinURI);
        // Assert
        assertEquals("bitcoin://13ceUWo48GyCcSHwKpCKZtTkkJfr7jyxbT?amount=1.2345", uri);
    }

}