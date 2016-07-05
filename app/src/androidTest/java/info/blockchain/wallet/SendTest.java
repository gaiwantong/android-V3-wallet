package info.blockchain.wallet;

import android.content.Context;

import info.blockchain.credentials.WalletUtil;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;

import org.bitcoinj.core.Transaction;

import java.math.BigInteger;
import java.util.HashMap;

public class SendTest extends BlockchainTest {

    long lamount = (long) (Double.parseDouble("0.0001") * 1e8);
    PrefsUtil prefsUtil;
    MonetaryUtil monetaryUtil;

    /**
     * @param String  name
     * @param Context ctx
     */
    public SendTest(String name, Context ctx) {
        super(name, ctx);
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        prefsUtil = new PrefsUtil(context);
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test something
     */
    public void test() {

        String payload = WalletUtil.getInstance(context).getPayload();
        PayloadFactory.getInstance().set(new Payload(payload));
        try {
            PayloadFactory.getInstance().get().parseJSON();
        } catch (Exception e) {
            ;
        }
        try {
            MultiAddrFactory.getInstance().refreshXPUBData(new String[]{PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(0).getXpub()});
        } catch (Exception e) {
            e.printStackTrace();
        }

        SendFactory sf = getFactoryInstance(context);

        UnspentOutputsBundle unspents = unspentOutputsHD(sf);
        makeTransactionHD(sf, unspents);

        unspents = unspentOutputsLegacy(sf);
        makeTransactionLegacy(sf, unspents);

    }

    public SendFactory getFactoryInstance(Context ctx) {
        SendFactory factory = SendFactory.getInstance(context);
        AssertUtil.getInstance().assert_true(this, "SendFactory instance returned", factory != null);
        return factory;
    }

    public UnspentOutputsBundle unspentOutputsHD(SendFactory sf) {

        //final String address, final BigInteger amount, final BigInteger feePerKb, String unspentsApiResponse
//        UnspentOutputsBundle unspents = SendFactory.getInstance(context).prepareSend(WalletUtil.getInstance(context).getHdSpendAddress(), MonetaryUtil.getInstance().getUndenominatedAmount(lamount), null, MonetaryUtil.getInstance().getUndenominatedAmount(lamount), Unspent);
//
//        AssertUtil.getInstance().assert_true(this, "HD has unspent outputs", (unspents != null && unspents.getOutputs().size() > 0));

//        return unspents;
        return null;
    }

    /*
        final float amount = Float.parseFloat(humanFriendlyString.toString());
        return Utils.toNanoCoins((int) amount, (int) ((amount % 1) * 100));
     */

    public void makeTransactionHD(SendFactory sf, UnspentOutputsBundle unspents) {

        HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put(WalletUtil.getInstance(context).getHdReceiveAddress(), monetaryUtil.getUndenominatedAmount(lamount));
        org.apache.commons.lang3.tuple.Pair<Transaction, Long> pair = null;
        try {
            pair = SendCoins.getInstance().makeTransaction(true, unspents.getOutputs(), receivers, monetaryUtil.getUndenominatedAmount(lamount), WalletUtil.getInstance(context).getHdSpendAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        AssertUtil.getInstance().assert_true(this, "HD produces transaction", pair.getLeft() != null);
        AssertUtil.getInstance().assert_true(this, "HD produces priority", pair.getRight() != null);
    }

    public UnspentOutputsBundle unspentOutputsLegacy(SendFactory sf) {

//        LegacyAddress legacyAddress = new LegacyAddress();
//        legacyAddress.setAddress(WalletUtil.getInstance(context).getLegacySpendAddress());
//
//        UnspentOutputsBundle unspents = SendFactory.getInstance(context).prepareSend(-1, WalletUtil.getInstance(context).getHdReceiveAddress(), MonetaryUtil.getInstance().getUndenominatedAmount(lamount), legacyAddress, MonetaryUtil.getInstance().getUndenominatedAmount(lamount), "");
//
//        AssertUtil.getInstance().assert_true(this, "Legacy has unspent outputs", (unspents != null && unspents.getOutputs().size() > 0));
//
//        return unspents;
        return null;
    }

    public void makeTransactionLegacy(SendFactory sf, UnspentOutputsBundle unspents) {

        HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put(WalletUtil.getInstance(context).getHdReceiveAddress(), monetaryUtil.getUndenominatedAmount(lamount));
        org.apache.commons.lang3.tuple.Pair<Transaction, Long> pair = null;
        try {
            pair = SendCoins.getInstance().makeTransaction(true, unspents.getOutputs(), receivers, monetaryUtil.getUndenominatedAmount(lamount), WalletUtil.getInstance(context).getHdSpendAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        AssertUtil.getInstance().assert_true(this, "Legacy produces transaction", pair.getLeft() != null);
        AssertUtil.getInstance().assert_true(this, "Legacy produces priority", pair.getRight() != null);
    }

}
