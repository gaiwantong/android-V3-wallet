package info.blockchain.wallet.send;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;

import info.blockchain.api.PushTx;
import info.blockchain.util.FeeUtil;
import info.blockchain.wallet.callbacks.OpCallback;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.Hash;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONArray;
import org.json.JSONObject;
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
 * SendFactory.java : Oddly named singleton class for spending fromAddresses Blockchain Android HD wallet
 */
public class SendFactory {

    private static PushTx pushTxApi;
    private static SendFactory instance;
    private String[] fromAddresses;
    public HashMap<String, String> fromAddressPathMap;
    private boolean sentChange = false;
    private static PayloadManager payloadManager;

    private SendFactory() {
        // No-op
    }

    public static SendFactory getInstance() {
        if (instance == null) {
            instance = new SendFactory();
            pushTxApi = new PushTx();
            payloadManager = PayloadManager.getInstance();
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
            fromAddressPathMap = new HashMap<>();
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
            fromAddressPathMap = new HashMap<>();
            fromAddresses = new String[1];
            fromAddresses[0] = address;
        }

        UnspentOutputsBundle ret;
        if (isXpub) {
            ret = getUnspentOutputPoints(true, new String[]{address}, amount, feePerKb, unspentsApiResponse);
        } else {
            if (payloadManager.isNotUpgraded()) {
                List<String> addrs = payloadManager.getPayload().getActiveLegacyAddressStrings();
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
     * @param context The current application context
     * @param accountIdx HD account index, -1 if legacy spend
     * @param unspent List of unspent outpoints
     * @param toAddress Receiving public address
     * @param amount Spending amount (not including fee)
     * @param legacyAddress If legacy spend, spend fromAddresses this LegacyAddress, otherwise null
     * @param fee Miner's fee
     * @param note Note to be attached to this tx
     * @param opc
     */
    public void execSend(Context context, boolean isWatchOnlySpend, final int accountIdx, final List<MyTransactionOutPoint> unspent, final String toAddress,
                         final BigInteger amount, final LegacyAddress legacyAddress, final BigInteger fee,
                         final String note, final boolean isQueueSend, final String secondPassword, final OpCallback opc) {

        final boolean isHD = accountIdx != -1;

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
                        changeAddr = payloadManager.getChangeAddress(accountIdx);
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
                                walletKey = payloadManager.getECKey(accountIdx, path);
                            } else {
                                if(!isWatchOnlySpend && payloadManager.getPayload().isDoubleEncrypted()){
                                    walletKey = legacyAddress.getECKey(new CharSequenceX(secondPassword));
                                }else{
                                    walletKey = legacyAddress.getECKey();
                                }
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

                    if (payloadManager.isNotUpgraded()) {
                        wallet = new Wallet(MainNetParams.get());
                        List<LegacyAddress> addrs = payloadManager.getPayload().getActiveLegacyAddresses();
                        for (LegacyAddress addr : addrs) {
                            ECKey ecKey = null;
                            if(!isWatchOnlySpend && payloadManager.getPayload().isDoubleEncrypted()) {
                                ecKey = addr.getECKey(new CharSequenceX(secondPassword));
                            }else{
                                ecKey = addr.getECKey();
                            }
                            if (addr != null && ecKey != null && ecKey.hasPrivKey()) {
                                wallet.addKey(ecKey);
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
                            String response = pushTxApi.submitTransaction(tx);
                            if (response.contains("Transaction Submitted")) {

                                opc.onSuccess(tx.getHashAsString());

                                if (note != null && note.length() > 0) {
                                    Map<String, String> notes = payloadManager.getPayload().getNotes();
                                    notes.put(tx.getHashAsString(), note);
                                    payloadManager.getPayload().setNotes(notes);
                                }

                                if (isHD && sentChange) {
                                    // increment change address counter
                                    payloadManager.getPayload().getHdWallet().getAccounts().get(accountIdx).incChange();
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
                        TxQueue.getInstance().add(context, spendable);
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
     * @param spendAmount Amount including fee
     * @param feePerKb Fee per kb
     * @return UnspentOutputsBundle
     */
    public UnspentOutputsBundle getUnspentOutputPoints(boolean isHD, String[] from, BigInteger spendAmount, BigInteger feePerKb, String unspentsApiResponse) throws Exception {

        UnspentOutputsBundle ret = new UnspentOutputsBundle();

        List<MyTransactionOutPoint> unspentOutputsList = new ArrayList<MyTransactionOutPoint>();

        JSONObject root = new JSONObject(unspentsApiResponse);
        JSONArray unspentsJsonArray = root.getJSONArray("unspent_outputs");
        if (unspentsJsonArray == null || unspentsJsonArray.length() == 0) {
            throw new Exception("Unable to find confirmed funds.");
        }

        if(root.has("notice")) {
            ret.setNotice(root.get("notice").toString());
        }

        long sweepAmount = 0;

        for (int i = 0; i < unspentsJsonArray.length(); i++) {

            JSONObject unspentJson = unspentsJsonArray.getJSONObject(i);

            byte[] hashBytes = Hex.decode(unspentJson.getString("tx_hash"));

            Hash hash = new Hash(hashBytes);
            hash.reverse();
            Sha256Hash txHash = new Sha256Hash(hash.getBytes());

            int txOutputN = unspentJson.getInt("tx_output_n");
            BigInteger value = BigInteger.valueOf(unspentJson.getLong("value"));
            sweepAmount += value.longValue();

            byte[] scriptBytes = Hex.decode(unspentJson.getString("script"));
            int confirmations = unspentJson.getInt("confirmations");

            if (isHD) {
                String address = new BitcoinScript(scriptBytes).getAddress().toString();
                String path = null;
                if (unspentJson.has("xpub")) {
                    JSONObject obj = unspentJson.getJSONObject("xpub");
                    if (obj.has("path")) {
                        path = (String) obj.get("path");
                        fromAddressPathMap.put(address, path);
                    }
                }
            }

            // Construct the output
            MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes);
            outPoint.setConfirmations(confirmations);

            unspentOutputsList.add(outPoint);
        }

        // select the minimum number of outputs necessary
        Collections.sort(unspentOutputsList, new UnspentOutputAmountComparator());
        List<MyTransactionOutPoint> minimumUnspentOutputsList = new ArrayList<MyTransactionOutPoint>();
        BigInteger totalValue = BigInteger.ZERO;
        int outputCount = 2;
        for (MyTransactionOutPoint output : unspentOutputsList) {
            totalValue = totalValue.add(output.getValue());
            minimumUnspentOutputsList.add(output);

            /*
            Dust inclusion removed to match up with js
             */
//            //No change = 1 output
//            BigInteger spendAmountNoChange = spendAmount.add(FeeUtil.estimatedFee(minimumUnspentOutputsList.size(), 1, feePerKb));
//            if (spendAmountNoChange.compareTo(totalValue) <= 0 && spendAmountNoChange.compareTo(totalValue.subtract(SendCoins.bDust)) >= 0) {
//                outputCount = 1;
//                break;
//            }
//
//            //Expect change = 2 outputs
//            BigInteger spendAmountWithChange = spendAmount.add(FeeUtil.estimatedFee(minimumUnspentOutputsList.size(), 2, feePerKb));
//            if (totalValue.subtract(SendCoins.bDust).compareTo(spendAmountWithChange) >= 0) {
//                outputCount = 2;//[multiple inputs, 2 outputs] - assume change
//                break;
//            }

            //No change = 1 output
            BigInteger spendAmountNoChange = spendAmount.add(FeeUtil.estimatedFee(minimumUnspentOutputsList.size(), 1, feePerKb));
            if (spendAmountNoChange.compareTo(totalValue) == 0) {
                outputCount = 1;
                break;
            }

            //Expect change = 2 outputs
            BigInteger spendAmountWithChange = spendAmount.add(FeeUtil.estimatedFee(minimumUnspentOutputsList.size(), 2, feePerKb));
            if (totalValue.compareTo(spendAmountWithChange) >= 0) {
                outputCount = 2;//[multiple inputs, 2 outputs] - assume change
                break;
            }
        }

        ret.setTotalAmount(totalValue);
        ret.setOutputs(minimumUnspentOutputsList);

        ret.setRecommendedFee(FeeUtil.estimatedFee(minimumUnspentOutputsList.size(), outputCount, feePerKb));
        ret.setSweepAmount(BigInteger.valueOf(sweepAmount).subtract(FeeUtil.estimatedFee(unspentOutputsList.size(), outputCount, feePerKb)));

        return ret;
    }

    private interface SendProgress {

        void onStart();

        // Return false to cancel
        boolean onReady(Transaction tx, BigInteger fee, long priority);

        void onSend(Transaction tx, String message);

        // Return true to cancel the transaction or false to continue without it
        ECKey onPrivateKeyMissing(String address);

        void onError(String message);

        void onProgress(String message);
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

                SuggestedFee suggestedFee = null;

                //TODO fix this
//                try {
////                    info.blockchain.wallet.payment.data.SuggestedFee dynamicFeeJson = new DynamicFee().getDynamicFee();
////                    if(dynamicFeeJson != null){
////
////                        suggestedFee = new SuggestedFee();
////                        JSONObject defaultJson = dynamicFeeJson.getJSONObject("default");
////                        suggestedFee.defaultFeePerKb = BigInteger.valueOf(defaultJson.getLong("fee"));
////                        suggestedFee.isSurge = defaultJson.getBoolean("surge");
////
////                        JSONArray estimateArray = dynamicFeeJson.getJSONArray("estimate");
////                        suggestedFee.estimateList = new ArrayList<>();
////                        for(int i = 0; i < estimateArray.length(); i++){
////
////                            JSONObject estimateJson = estimateArray.getJSONObject(i);
////
////                            BigInteger fee = BigInteger.valueOf(estimateJson.getLong("fee"));
////                            boolean surge = estimateJson.getBoolean("surge");
////                            boolean ok = estimateJson.getBoolean("ok");
////
////                            suggestedFee.estimateList.add(new SuggestedFee.Estimates(fee, surge, ok));
////                        }
////                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
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
