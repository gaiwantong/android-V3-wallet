package info.blockchain.wallet.view.helpers;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TransactionHelper {

    private PayloadManager mPayloadManager;

    public TransactionHelper(PayloadManager payloadManager) {
        mPayloadManager = payloadManager;
    }

    /**
     * Searches the list of Accounts to find if the address is labeled and returns that label
     *
     * @param address   A bitcoin address
     * @return          Either the label associated with the address, or the original address
     */
    @NonNull
    public String addressToLabel(String address) {
        HDWallet hdWallet = mPayloadManager.getPayload().getHdWallet();
        List<Account> accountList = new ArrayList<>();
        if (hdWallet != null && hdWallet.getAccounts() != null) {
            accountList = hdWallet.getAccounts();
        }

        // If address belongs to owned xpub
        if (MultiAddrFactory.getInstance().isOwnHDAddress(address)) {
            HashMap<String, String> addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();
            String xpub = addressToXpubMap.get(address);
            if (xpub != null) {
                // Even though it looks like this shouldn't happen, it sometimes happens with
                // transfers if user clicks to view details immediately.
                // TODO - see if isOwnHDAddress could be updated to solve this
                int accIndex = mPayloadManager.getPayload().getXpub2Account().get(xpub);
                String label = accountList.get(accIndex).getLabel();
                if (label != null && !label.isEmpty())
                    return label;
            }
            // If address one of owned legacy addresses
        } else if (mPayloadManager.getPayload().getLegacyAddressStrings().contains(address)
                || mPayloadManager.getPayload().getWatchOnlyAddressStrings().contains(address)) {

            Payload payload = mPayloadManager.getPayload();

            String label = payload.getLegacyAddresses().get(payload.getLegacyAddressStrings().indexOf(address)).getLabel();
            if (label != null && !label.isEmpty()) {
                return label;
            }
        }

        return address;
    }

    /**
     * Return a Pair of maps that correspond to the inputs and outputs of a transaction, whilst
     * filtering out Change addresses.
     *
     * @param transactionDetails    A {@link Transaction} object
     * @param transaction           An associated {@link Tx}
     * @return                      A Pair of Maps representing the input and output of the transaction
     */
    @NonNull
    public Pair<HashMap<String, Long>, HashMap<String, Long>> filterNonChangeAddresses(Transaction transactionDetails, Tx transaction) {

        HashMap<String, String> addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();

        HashMap<String, Long> inputMap = new HashMap<>();
        HashMap<String, Long> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        // Inputs / From field
        if (transaction.getDirection().equals(MultiAddrFactory.RECEIVED) && transactionDetails.getInputs().size() > 0) {
            // Only 1 addr for receive
            inputMap.put(transactionDetails.getInputs().get(0).addr, transactionDetails.getInputs().get(0).value);
        } else {
            for (Transaction.xPut input : transactionDetails.getInputs()) {
                // Move or Send
                // The address belongs to us
                String xpub = addressToXpubMap.get(input.addr);

                // Address belongs to xpub we own
                if (xpub != null) {
                    // Only add xpub once
                    if (!inputXpubList.contains(xpub)) {
                        inputMap.put(input.addr, input.value);
                        inputXpubList.add(xpub);
                    }
                } else {
                    // Legacy Address we own
                    inputMap.put(input.addr, input.value);
                }
            }
        }

        // Outputs / To field
        for (Transaction.xPut output : transactionDetails.getOutputs()) {

            if (MultiAddrFactory.getInstance().isOwnHDAddress(output.addr)) {
                // If output address belongs to an xpub we own - we have to check if it's change
                String xpub = addressToXpubMap.get(output.addr);
                if (inputXpubList.contains(xpub)) {
                    continue;// change back to same xpub
                }

                // Receiving to same address multiple times?
                if (outputMap.containsKey(output.addr)) {
                    long prevAmount = outputMap.get(output.addr) + output.value;
                    outputMap.put(output.addr, prevAmount);
                } else {
                    outputMap.put(output.addr, output.value);
                }

            } else if (mPayloadManager.getPayload().getLegacyAddressStrings().contains(output.addr)
                    || mPayloadManager.getPayload().getWatchOnlyAddressStrings().contains(output.addr)) {
                // If output address belongs to a legacy address we own - we have to check if it's change
                // If it goes back to same address AND if it's not the total amount sent (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if (inputMap.containsKey(output.addr) && output.value != Math.abs(transaction.getAmount())) {
                    continue;// change back to same input address
                }

                // Output more than tx amount - change
                if (output.value > Math.abs(transaction.getAmount())) {
                    continue;
                }

                outputMap.put(output.addr, output.value);
            } else {
                if (!transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {
                    outputMap.put(output.addr, output.value);
                }
            }
        }

        return new Pair<>(inputMap, outputMap);
    }
}
