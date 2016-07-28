package info.blockchain.wallet.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by adambennett on 28/07/2016.
 *
 * A simple class to ease the creation of consumable bitcoin links. Should probably be expanded
 * in the future to allow the encoding of messages, labels and other extras.
 */

public class BitcoinLinkGenerator {

    private static final String LINK_START = "bitcoin://";
    private static final String LINK_AMOUNT = "?amount=";

    /**
     * Returns a correctly formatted URL in String format
     *
     * @param address   The wallet address as a String
     * @param amount    A Double amount, nullable
     * @return          The correctly formatted URL as a String
     */
    public static String getLink(@NonNull String address, @Nullable Double amount) {
        String link = LINK_START + address;

        if (amount != null && amount != 0) {
            link += LINK_AMOUNT + amount;
        }

        return link;
    }

    /**
     * Returns a correctly formatted URL in String format
     *
     * @param uri   A {@link BitcoinURI}
     * @return      The correctly formatted URL as a String
     */
    public static String getLink(@NonNull BitcoinURI uri) {
        String link = LINK_START + uri.getAddress();

        if (uri.getAmount() != null && !uri.getAmount().isZero()) {
            link += LINK_AMOUNT + uri.getAmount().toPlainString();
        }

        return link;
    }

}
