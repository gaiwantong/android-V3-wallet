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

import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.send.MyTransactionOutPoint;
import info.blockchain.wallet.send.SendFactory;

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

        SendFactory sf = getFactoryInstance(context);
/*
        String xpub = "xpub6D881wkrgprMT6xkS8oNUFeWdwvXZcMAfeLQQSeB3i5d6CTtEKAX9hMAeJYhnj8vCLs42VYo26FXMEUmFMJrhaJCMtjFuuDRrMPhMzcmzzh";
        List<MyTransactionOutPoint> unspents = unspentOutputsHD(sf, new String[]{ xpub }, Utils.toNanoCoins("0.0002").add(Utils.toNanoCoins("0.0001")));

        String changeAddress = "1KEBxsYMy79jKYu7PJzsF2GvFAw54BJJ2";
        HashMap<String, BigInteger> receivingAddresses = new HashMap<String, BigInteger>();
        receivingAddresses.put("1KEBxsYMy79jKYu7PJzsF2GvFAw54BJJ2", Utils.toNanoCoins("0.0002"));
        Pair<Transaction, Long> pair = makeTransactionHD(sf, unspents, receivingAddresses,  Utils.toNanoCoins("0.0002"), changeAddress);
*/
    }

    public SendFactory getFactoryInstance(Context ctx) {
        SendFactory factory = SendFactory.getInstance(context);
        AssertUtil.getInstance().assert_true(this, "SendFactory instance returned", factory != null);
        return factory;
    }

    public List<MyTransactionOutPoint> unspentOutputsHD(SendFactory sf, String[] from, BigInteger totalAmount) {
        List<MyTransactionOutPoint> ret = null;
        try {
            ret = sf._getUnspentOutputPoints(true, from, totalAmount);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        AssertUtil.getInstance().assert_true(this, "Has unspent outputs", (ret != null && ret.size() > 0));
        return ret;
    }

    public Pair<Transaction, Long> makeTransactionHD(SendFactory sf, List<MyTransactionOutPoint> unspent, HashMap<String, BigInteger> receivingAddresses, BigInteger fee, final String changeAddress) {
        Pair<Transaction, Long> ret = null;
        try {
            ret = sf._makeTransaction(true, unspent, receivingAddresses, fee, changeAddress);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        AssertUtil.getInstance().assert_true(this, "Makes tx", (ret != null && ret.first.getHash() != null));
        return ret;
    }

}
