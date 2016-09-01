package info.blockchain.wallet.view.helpers;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.util.MonetaryUtil;

public class AddressBalanceHelper {

    private MonetaryUtil mMonetaryUtil;

    public AddressBalanceHelper(MonetaryUtil monetaryUtil) {
        mMonetaryUtil = monetaryUtil;
    }

    public String getAccountBalance(Account account, boolean isBTC, double btcExchange, String fiatUnit, String btcUnit) {

        long btcBalance = MultiAddrFactory.getInstance().getXpubAmounts().get(account.getXpub());

        if (!isBTC) {
            double fiatBalance = btcExchange * (btcBalance / 1e8);
            return "(" + mMonetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance) + " " + fiatUnit + ")";
        } else {
            return "(" + mMonetaryUtil.getDisplayAmount(btcBalance) + " " + btcUnit + ")";
        }
    }

    public String getAddressBalance(LegacyAddress legacyAddress, boolean isBTC, double btcExchange, String fiatUnit, String btcUnit) {

        long btcBalance = MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress());

        if (!isBTC) {
            double fiatBalance = btcExchange * (btcBalance / 1e8);
            return "(" + mMonetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance) + " " + fiatUnit + ")";
        } else {
            return "(" + mMonetaryUtil.getDisplayAmount(btcBalance) + " " + btcUnit + ")";
        }
    }
}
