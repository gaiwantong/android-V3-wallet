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

        String payload = "{ \"guid\" : \"524b5e9f-72ea-4690-b28c-8c1cfce65ca0\",    \"sharedKey\" : \"6088a51a-26f2-47e0-9d16-934dd2a6131a\",    \"options\" : {\"fee_policy\":0,\"logout_time\":600000,\"additional_seeds\":[],\"enable_multiple_accounts\":true,\"pbkdf2_iterations\":5000},    \"keys\" : [    {\"label\":\"base64\",\"addr\":\"1KCgKnEnWLdgpEdwPDKoxwmcnTdopSYx3c\",\"priv\":\"FNnZN8Ex9seemoCrPEA4nY7LK1fRYKBwSqeCAkYoaeCF\",\"created_device_version\":\"\",\"created_time\":1431022026,\"tag\":0,\"created_device_name\":\"android\"},    {\"label\":\"\",\"addr\":\"15c1BY46cYB7mnrmG87ERovrwogCC7NU8Q\",\"priv\":\"EBYGkPSb3NDgBj782xj6Ta5Pe55DtVtJ3o27yjG8nboU\",\"created_device_version\":\"\",\"created_time\":1431681152,\"tag\":0,\"created_device_name\":\"android\"}    ],    \"tag_names\" : [],    \"hd_wallets\" : [    {    \"seed_hex\" : \"8ca4adead3af684908ead430953d609d\",    \"passphrase\" : \"\",    \"mnemonic_verified\" : false,    \"default_account_idx\" : 0,    \"paidTo\" : {},    \"accounts\" : [    {\"label\":\"Spending\",\"archived\":false,\"xpriv\":\"xprv9z8mcSDxrTJ4EctHL7GN77hn5v63A9dKJRQoc4EZVNYeDQ8jgmrGbu2go2g7ngbJUsUF2kbB5F3NeFWj4Yga4FvzRcnAMUXEdsq3yXuvRDr\",\"xpub\":\"xpub6D881wkrgprMT6xkS8oNUFeWdwvXZcMAfeLQQSeB3i5d6CTtEKAX9hMAeJYhnj8vCLs42VYo26FXMEUmFMJrhaJCMtjFuuDRrMPhMzcmzzh\",\"address_labels\":[],\"cache\":{\"changeAccount\":\"xpub6FHh7Qg8AhyLHC9BCe9tdLXNFYF1yTJWwFG97pPsG3UuNqLNhJ3Y2WMYNnNfxUqyd6ZD1ZWwTXpps5LgKbNZTyJpC1oRz2F7RegFcJmnyAR\",\"receiveAccount\":\"xpub6FHh7Qg8AhyLDUjEsRbw9Mmd7SdvMoLua6J3cpCY94yomEYFBDp5xKSN4dgUwi4rb8XA4ZUFbkZp7FnfnN9oBus7c2kR41n3GCpWLhiKgxD\"}},    {\"label\":\"Savings\",\"archived\":false,\"xpriv\":\"xprv9z8mcSDxrTJ4GFXVEVA6f5smnHTjZCeCkGQs13fE4QU6hPYY3pL4Gt9H9Q8BAK2NxgQyQrnoJuBM8bgf15exitmmoxMKULBJ3SCiNdEzQfd\",\"xpub\":\"xpub6D881wkrgprMUjbxLWh72DpWLKJDxfN47VLToS4qck15aBsgbMeJpgTkzfq7zcuk2xXCPKsTdgdcYv5EEUEzM6rkQrx2ZP3XfQubdTTNqme\",\"address_labels\":[],\"cache\":{\"changeAccount\":\"xpub6EC8yCQzq2NvZ845HKwZBbRNFoNwtgd8GcmMyhCgPytnBMwJ89cTywwGrhYNd5jGpoJinWL2sK3wjRLHo6cvywuxkNQSsLUfMgFK3ACzaHe\",\"receiveAccount\":\"xpub6EC8yCQzq2NvWjVdLMbDKVnV9PL7TdY423Tqrsd5dXsPK8nt5QcuBessBzdd9VGoWrNydWo7wXfuMemb5qXMwu55zg2J4hFmH6TVMBibFjb\"}} ] } ] }";
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

    }

    public SendFactory getFactoryInstance(Context ctx) {
        SendFactory factory = SendFactory.getInstance(context);
        AssertUtil.getInstance().assert_true(this, "SendFactory instance returned", factory != null);
        return factory;
    }

    public UnspentOutputsBundle unspentOutputsHD(SendFactory sf) {

        UnspentOutputsBundle unspents = SendFactory.getInstance(context).send1(0, "1Km7dzNLr6Li4M1vJzKwFYELzC2PRizzWd", Utils.toNanoCoins("0.0001"), null, Utils.toNanoCoins("0.0001"), "");

        AssertUtil.getInstance().assert_true(this, "Has unspent outputs", (unspents != null && unspents.getOutputs().size() > 0));

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
            ;
        }

//        System.out.println(pair.first.toString());
//        System.out.println("" + pair.second);

        AssertUtil.getInstance().assert_true(this, "Produces transaction", pair.first != null);
        AssertUtil.getInstance().assert_true(this, "Produces priority", pair.second != null);
    }

/*
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
*/
}
