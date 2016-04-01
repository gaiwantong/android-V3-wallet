package info.blockchain.wallet.send;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;

import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ConnectivityStatus;
import info.blockchain.wallet.util.FeeUtil;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.Hash;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.bip44.Address;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import piuk.blockchain.android.R;

/**
 * SendFactory.java : singleton class for spending fromAddresses Blockchain Android HD wallet
 */
public class SendFactory {

    private static SendFactory instance = null;
    private static Context context = null;
    private String[] fromAddresses = null;
    private HashMap<String, String> fromAddressPathMap = null;
    private boolean sentChange = false;

    private SendFactory() {
        ;
    }

    public static SendFactory getInstance(Context ctx) {

        context = ctx.getApplicationContext();

        if (instance == null) {
            instance = new SendFactory();
        }

        return instance;
    }

    /**
     * Initial preparation for sending coins fromAddresses this wallet. <p> Collects sending addresses for HD or legacy spend
     * Collects unspent outputs fromAddresses sending addresses <p> After calling this method alternate fee amounts may be
     * calculated based on the number of inputs.
     *
     * @param address xpub or legacyAddress
     * @param amount Spending amount (not including fee)
     * @param feePerKb Miner's fee
     * @return UnspentOutputsBundle
     */
    public UnspentOutputsBundle prepareSend(final String address, final BigInteger amount, final BigInteger feePerKb, String unspentsApiResponse) throws Exception{

        boolean isXpub = FormatsUtil.getInstance().isValidXpub(address);

        if (isXpub) {

            HashMap<String, List<String>> unspentOutputs = MultiAddrFactory.getInstance().getUnspentOuts();
            List<String> data = unspentOutputs.get(address);
            fromAddressPathMap = new HashMap<String, String>();
            if (data == null) {
                return null;
            }
            for (String f : data) {
                if (f != null) {
                    String[] s = f.split(",");
                    // get path info which will be used to calculate private key
                    fromAddressPathMap.put(s[1], s[0]);
                }
            }

            fromAddresses = fromAddressPathMap.keySet().toArray(new String[fromAddressPathMap.keySet().size()]);
        } else {
            fromAddressPathMap = new HashMap<String, String>();
            fromAddresses = new String[1];
            fromAddresses[0] = address;
        }

        UnspentOutputsBundle ret;
        if (isXpub) {
            ret = getUnspentOutputPoints(true, new String[]{address}, amount, feePerKb, unspentsApiResponse);
        } else {
            if (AppUtil.getInstance(context).isNotUpgraded()) {
                List<String> addrs = PayloadFactory.getInstance().get().getActiveLegacyAddressStrings();
                fromAddresses = addrs.toArray(new String[addrs.size()]);
            }

            ret = getUnspentOutputPoints(false, fromAddresses, amount, feePerKb, unspentsApiResponse);
        }

        if (ret == null || ret.getOutputs() == null) {
            return null;
        }

        return ret;
    }

    /**
     * Send coins fromAddresses this wallet. <p> Creates transaction Assigns change address Signs tx
     *
     * @param accountIdx HD account index, -1 if legacy spend
     * @param unspent List of unspent outpoints
     * @param toAddress Receiving public address
     * @param amount Spending amount (not including fee)
     * @param legacyAddress If legacy spend, spend fromAddresses this LegacyAddress, otherwise null
     * @param fee Miner's fee
     * @param note Note to be attached to this tx
     * @param opc
     */
    public void execSend(final int accountIdx, final List<MyTransactionOutPoint> unspent, final String toAddress, final BigInteger amount, final LegacyAddress legacyAddress, final BigInteger fee, final String note, final boolean isQueueSend, final OpCallback opc) {

        final boolean isHD = accountIdx == -1 ? false : true;

        final HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
        receivers.put(toAddress, amount);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();

                    Pair<Transaction, Long> pair = null;
                    String changeAddr = null;
                    if (isHD) {
                        int changeIdx = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getIdxChangeAddresses();
                        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                            changeAddr = WalletFactory.getInstance().get().getAccount(accountIdx).getChange().getAddressAt(changeIdx).getAddressString();
                        } else {
                            changeAddr = WalletFactory.getInstance().getWatchOnlyWallet().getAccount(accountIdx).getChange().getAddressAt(changeIdx).getAddressString();
                        }
                    } else {
                        changeAddr = legacyAddress.getAddress();
                    }
                    pair = SendCoins.getInstance().makeTransaction(true, unspent, receivers, fee, changeAddr);

                    // Transaction cancelled
                    if (pair == null) {
                        opc.onFail("Transaction cancelled");
                        return;
                    }
                    Transaction tx = pair.getLeft();
                    Long priority = pair.getRight();

                    Wallet wallet = new Wallet(MainNetParams.get());
                    for (TransactionInput input : tx.getInputs()) {
                        byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
                        String address = new BitcoinScript(scriptBytes).getAddress().toString();
                        ECKey walletKey = null;
                        try {
                            String privStr = null;
                            if (isHD) {
                                String path = fromAddressPathMap.get(address);
                                String[] s = path.split("/");
                                Address hd_address = null;
                                if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                                    hd_address = WalletFactory.getInstance().get().getAccount(accountIdx).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                                } else {
                                    hd_address = WalletFactory.getInstance().getWatchOnlyWallet().getAccount(accountIdx).getChain(Integer.parseInt(s[1])).getAddressAt(Integer.parseInt(s[2]));
                                }
                                privStr = hd_address.getPrivateKeyString();
                                walletKey = PrivateKeyFactory.getInstance().getKey(PrivateKeyFactory.WIF_COMPRESSED, privStr);
                            } else {
                                walletKey = legacyAddress.getECKey();
                            }
                        } catch (AddressFormatException afe) {
                            // skip add Watch Only Bitcoin Address key because already accounted for later with tempKeys
                            afe.printStackTrace();
                            continue;
                        }

                        if (walletKey != null) {
                            wallet.addKey(walletKey);
                        } else {
                            opc.onFail("walletKey null");
                        }

                    }

                    if (AppUtil.getInstance(context).isNotUpgraded()) {
                        wallet = new Wallet(MainNetParams.get());
                        List<LegacyAddress> addrs = PayloadFactory.getInstance().get().getActiveLegacyAddresses();
                        for (LegacyAddress addr : addrs) {
                            if (addr != null && addr.getECKey() != null && addr.getECKey().hasPrivKey()) {
                                wallet.addKey(addr.getECKey());
                            }
                        }
                    }

                    SendCoins.getInstance().signTx(tx, wallet);
                    String hexString = SendCoins.getInstance().encodeHex(tx);
                    if (hexString.length() > (100 * 1024)) {
                        opc.onFail(context.getString(R.string.tx_length_error));
                        throw new Exception(context.getString(R.string.tx_length_error));
                    }

                    if (!isQueueSend) {
                        if (ConnectivityStatus.hasConnectivity(context)) {
                            String response = SendCoins.getInstance().pushTx(tx);
                            if (response.contains("Transaction Submitted")) {

                                opc.onSuccess(tx.getHashAsString());

                                if (note != null && note.length() > 0) {
                                    Map<String, String> notes = PayloadFactory.getInstance().get().getNotes();
                                    notes.put(tx.getHashAsString(), note);
                                    PayloadFactory.getInstance().get().setNotes(notes);
                                }

                                if (isHD && sentChange) {
                                    // increment change address counter
                                    PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).incChange();
                                }

                            } else {
                                opc.onFail(response);
                            }
                        } else {
                            opc.onFailPermanently(context.getString(R.string.check_connectivity_exit));
                        }
                    } else {
                        // Queue tx
                        Spendable spendable = new Spendable(tx, opc, note, isHD, sentChange, accountIdx);
                        TxQueue.getInstance(context).add(spendable);
                    }

//					progress.onSend(tx, response);

                    Looper.loop();

                } catch (Exception e) {
                    e.printStackTrace();
                    opc.onFailPermanently(e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Collect unspent outputs for this spend. <p> Collects all unspent outputs for spending addresses, randomizes them,
     * and then selects outputs until amount of selected outputs >= totalAmount
     *
     * @param isHD true == HD account spend, false == legacy address spend
     * @param from (contains 1 XPUB if HD spend, public address(es) if legacy spend
     * @param totalAmount Amount including fee
     * @param feePerKb Fee per kb
     * @return UnspentOutputsBundle
     */
    private UnspentOutputsBundle getUnspentOutputPoints(boolean isHD, String[] from, BigInteger totalAmount, BigInteger feePerKb, String unspentsApiResponse) throws Exception {

        UnspentOutputsBundle ret = new UnspentOutputsBundle();

        String args = null;
        if (isHD) {
            args = from[0];
        } else {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < from.length; i++) {
                buffer.append(from[i]);
                if (i != (from.length - 1)) {
                    buffer.append("|");
                }
            }

            args = buffer.toString();
        }

        List<MyTransactionOutPoint> outputs = new ArrayList<MyTransactionOutPoint>();

        Map<String, Object> root = (Map<String, Object>) JSONValue.parse(unspentsApiResponse);
        List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>) root.get("unspent_outputs");
        if (outputsRoot == null || outputsRoot.size() == 0) {
            throw new Exception("Unable to find confirmed funds.");
        }

        if(root.containsKey("notice")) {
            ret.setNotice(root.get("notice").toString());
        }

        long sweepAmount = 0;
        ArrayList<BigInteger> coins = new ArrayList<>();
        for (Map<String, Object> outDict : outputsRoot) {
            sweepAmount += ((Number) outDict.get("value")).longValue();
            coins.add(BigInteger.valueOf(((Number) outDict.get("value")).longValue()));//Debug
        }
//        Log.v("vos","Coins: "+coins.toString());
//        Log.v("vos","Coins (sweepAmount): "+sweepAmount);
        ret.setSweepAmount(BigInteger.valueOf(sweepAmount));

        BigInteger totalAvailableBalance = 	BigInteger.ZERO;
        for (Map<String, Object> outDict : outputsRoot) {

            byte[] hashBytes = Hex.decode((String) outDict.get("tx_hash"));

            Hash hash = new Hash(hashBytes);
            hash.reverse();
            Sha256Hash txHash = new Sha256Hash(hash.getBytes());

            int txOutputN = ((Number) outDict.get("tx_output_n")).intValue();
            BigInteger value = BigInteger.valueOf(((Number) outDict.get("value")).longValue());
            totalAvailableBalance = totalAvailableBalance.add(value);
            byte[] scriptBytes = Hex.decode((String) outDict.get("script"));
            int confirmations = ((Number) outDict.get("confirmations")).intValue();

            if (isHD) {
                String address = new BitcoinScript(scriptBytes).getAddress().toString();
                String path = null;
                if (outDict.containsKey("xpub")) {
                    JSONObject obj = (JSONObject) outDict.get("xpub");
                    if (obj.containsKey("path")) {
                        path = (String) obj.get("path");
                        fromAddressPathMap.put(address, path);
                    }
                }
            }

            // Construct the output
            MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes);
            outPoint.setConfirmations(confirmations);

            // Single output has been found that is exactly the amount we want to spend - no need to continue looking - return this coin
            //estimate fee with no change (outputs = 1)
            if (outPoint.getValue().compareTo(totalAmount.add(FeeUtil.estimatedFee(1, 1, feePerKb))) == 0) {
                outputs.clear();
                outputs.add(outPoint);
                ret.setTotalAmount(outPoint.getValue());
                ret.setOutputs(outputs);
                ret.setRecommendedFee(FeeUtil.estimatedFee(outputs.size(), 1, feePerKb));
                return ret;

            }
            // Single output has been found that is higher than the amount we want to spend - no need to continue looking - return this coin
            //estimate fee with change (outputs = 2)
            else if (outPoint.getValue().compareTo(totalAmount.add(FeeUtil.estimatedFee(1, 2, feePerKb))) >= 0) {
                outputs.clear();
                outputs.add(outPoint);
                ret.setTotalAmount(outPoint.getValue());
                ret.setOutputs(outputs);
                ret.setRecommendedFee(FeeUtil.estimatedFee(outputs.size(), 2, feePerKb));
                return ret;

            } else {
                outputs.add(outPoint);
            }

        }

        // select the minimum number of outputs necessary
        Collections.sort(outputs, new UnspentOutputAmountComparator());
        List<MyTransactionOutPoint> _outputs = new ArrayList<MyTransactionOutPoint>();
        BigInteger totalValue = BigInteger.ZERO;
        for (MyTransactionOutPoint output : outputs) {
            totalValue = totalValue.add(output.getValue());
            _outputs.add(output);
            if (totalValue.compareTo(totalAmount.add(FeeUtil.estimatedFee(_outputs.size(), 1, feePerKb))) == 0) {
                break;
            } else if (totalValue.compareTo(totalAmount.add(SendCoins.bDust).add(FeeUtil.estimatedFee(_outputs.size(), 2, feePerKb))) >= 0) {
                break;
            }
        }

        ret.setTotalAmount(totalValue);
        ret.setOutputs(_outputs);
        if (totalValue.compareTo(totalAmount.add(FeeUtil.estimatedFee(_outputs.size(), 1, feePerKb))) == 0) {
            ret.setRecommendedFee(FeeUtil.estimatedFee(_outputs.size(), 1, feePerKb));
        } else if (totalValue.compareTo(totalAmount.add(FeeUtil.estimatedFee(_outputs.size(), 2, feePerKb))) >= 0) {
            ret.setRecommendedFee(FeeUtil.estimatedFee(_outputs.size(), 2, feePerKb));
        }else{
            ret.setRecommendedFee(FeeUtil.estimatedFee(_outputs.size(), from.length, feePerKb));
        }

        return ret;
    }

    private interface SendProgress {

        public void onStart();

        // Return false to cancel
        public boolean onReady(Transaction tx, BigInteger fee, long priority);

        public void onSend(Transaction tx, String message);

        // Return true to cancel the transaction or false to continue without it
        public ECKey onPrivateKeyMissing(String address);

        public void onError(String message);

        public void onProgress(String message);
    }

    /**
     * Sort unspent outputs by amount in descending order.
     */
    private class UnspentOutputAmountComparator implements Comparator<MyTransactionOutPoint> {

        public int compare(MyTransactionOutPoint o1, MyTransactionOutPoint o2) {

            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            int ret = 0;

            if (o1.getValue().compareTo(o2.getValue()) > 0) {
                ret = BEFORE;
            } else if (o1.getValue().compareTo(o2.getValue()) < 0) {
                ret = AFTER;
            } else {
                ret = EQUAL;
            }

            return ret;
        }

    }

    public interface OnFeeSuggestListener{
        void onFeeSuggested(SuggestedFee suggestedFee);
    }

    public void getSuggestedFee(final OnFeeSuggestListener onFeeSuggestListener){

        new AsyncTask<Void, Void, SuggestedFee>() {

            @Override
            protected SuggestedFee doInBackground(Void... params) {

                SuggestedFee suggestedFee = new SuggestedFee();

                try {
                    org.json.JSONObject dynamicFeeJson = FeeUtil.getDynamicFee();
                    if(dynamicFeeJson != null){

                        suggestedFee.feePerKb = BigInteger.valueOf(dynamicFeeJson.getLong("fee"));
                        suggestedFee.isSuggestionAbnormallyHigh = dynamicFeeJson.getBoolean("surge");//We'll use this later to warn the user if the dynamic fee is too high
                        suggestedFee.suggestSuccess = true;
                    }else{
                        suggestedFee.feePerKb = FeeUtil.AVERAGE_FEE;
                        suggestedFee.suggestSuccess = false;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    suggestedFee.feePerKb = FeeUtil.AVERAGE_FEE;
                    suggestedFee.suggestSuccess = false;
                }
                return suggestedFee;
            }

            @Override
            protected void onPostExecute(SuggestedFee suggestedFee) {

                onFeeSuggestListener.onFeeSuggested(suggestedFee);
                super.onPostExecute(suggestedFee);
            }

        }.execute();
    }
}
