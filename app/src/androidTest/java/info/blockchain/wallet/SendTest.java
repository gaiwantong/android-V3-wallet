package info.blockchain.wallet;

import android.content.Context;
import android.util.Pair;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.MnemonicException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import info.blockchain.credentials.WalletUtil;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.send.MyTransactionOutPoint;
import info.blockchain.wallet.send.SendFactory;
import info.blockchain.wallet.send.UnspentOutputsBundle;

public class SendTest extends BlockchainTest {

    /**
     * @param String name
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
        PayloadFactory.getInstance(context).set(new Payload(payload));
        try {
            PayloadFactory.getInstance(context).get().parseJSON();
        }
        catch(Exception e) {
            ;
        }
        MultiAddrFactory.getInstance().getXPUB(new String[] { PayloadFactory.getInstance(context).get().getHdWallet().getAccounts().get(0).getXpub() } );

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

        UnspentOutputsBundle unspents = SendFactory.getInstance(context).send1(0, "1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd", Utils.toNanoCoins("0.0001"), null, Utils.toNanoCoins("0.0001"), "");

        AssertUtil.getInstance().assert_true(this, "HD has unspent outputs", (unspents != null && unspents.getOutputs().size() > 0));

        return unspents;
    }

    public void makeTransactionHD(SendFactory sf, UnspentOutputsBundle unspents) {

        HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put("1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd", Utils.toNanoCoins("0.0001"));
        Pair<Transaction, Long> pair = null;
        try {
            pair = SendFactory.getInstance(context).makeTransaction(true, unspents.getOutputs(), receivers, Utils.toNanoCoins("0.0001"), "1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd");
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        AssertUtil.getInstance().assert_true(this, "HD produces transaction", pair.first != null);
        AssertUtil.getInstance().assert_true(this, "HD produces priority", pair.second != null);
    }

    public UnspentOutputsBundle unspentOutputsLegacy(SendFactory sf) {

        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("1KCgKnEnWLdgpEdwPDKoxwmcnTdopSYx3c");

        UnspentOutputsBundle unspents = SendFactory.getInstance(context).send1(-1, "1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd", Utils.toNanoCoins("0.0001"), legacyAddress, Utils.toNanoCoins("0.0001"), "");

        AssertUtil.getInstance().assert_true(this, "Legacy has unspent outputs", (unspents != null && unspents.getOutputs().size() > 0));

        return unspents;
    }

    public void makeTransactionLegacy(SendFactory sf, UnspentOutputsBundle unspents) {

        HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put("1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd", Utils.toNanoCoins("0.0001"));
        Pair<Transaction, Long> pair = null;
        try {
            pair = SendFactory.getInstance(context).makeTransaction(true, unspents.getOutputs(), receivers, Utils.toNanoCoins("0.0001"), "1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd");
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        AssertUtil.getInstance().assert_true(this, "Legacy produces transaction", pair.first != null);
        AssertUtil.getInstance().assert_true(this, "Legacy produces priority", pair.second != null);
    }

}
