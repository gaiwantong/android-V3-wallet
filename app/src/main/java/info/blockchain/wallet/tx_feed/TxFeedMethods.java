package info.blockchain.wallet.tx_feed;

import android.support.v4.util.Pair;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.Transaction;
import info.blockchain.wallet.payload.Tx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxFeedMethods {

    List<LegacyAddress> legacyAddressList;
    List<String> legacyAddressLabelList;
    List<Account> accountList;
    HashMap<String, String> addressToXpubMap;
    Map<String, Integer> xpubToAccountMap;

    public TxFeedMethods() {

        Payload payload = PayloadFactory.getInstance().get();
        legacyAddressList = payload.getLegacyAddresses();
        legacyAddressLabelList = payload.getLegacyAddressStrings();
        HDWallet hdWallet = PayloadFactory.getInstance().get().getHdWallet();
        if(hdWallet != null)
            accountList = hdWallet.getAccounts();

        addressToXpubMap = MultiAddrFactory.getInstance().getAddress2Xpub();
        xpubToAccountMap = PayloadFactory.getInstance().get().getXpub2Account();
    }

    public Pair<HashMap<String,Long>, HashMap<String,Long>> filterNonChangeAddresses(Transaction transactionDetails, Tx transaction){

        HashMap<String,Long> inputMap = new HashMap<>();
        HashMap<String,Long> outputMap = new HashMap<>();

        ArrayList<String> inputXpubList = new ArrayList<>();

        //Inputs / From field
        if (transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {//only 1 addr for receive
            inputMap.put(transactionDetails.getInputs().get(0).addr, transactionDetails.getInputs().get(0).value);

        } else {

            for (Transaction.xPut input : transactionDetails.getInputs()) {

                if (!transaction.getDirection().equals(MultiAddrFactory.RECEIVED)){

                    //Move or Send
                    //The address belongs to us
                    String xpub = addressToXpubMap.get(input.addr);

                    //Address belongs to xpub we own
                    if(xpub != null) {
                        //Only add xpub once
                        if (!inputXpubList.contains(xpub)) {
                            inputMap.put(input.addr, input.value);
                            inputXpubList.add(xpub);
                        }
                    }else{
                        //Legacy Address we own
                        inputMap.put(input.addr, input.value);
                    }

                }else{
                    //Receive
                    inputMap.put(input.addr, input.value);
                }
            }
        }

        //Outputs / To field
        for (Transaction.xPut output : transactionDetails.getOutputs()) {

            if (MultiAddrFactory.getInstance().isOwnHDAddress(output.addr)) {

                //If output address belongs to an xpub we own - we have to check if it's change
                String xpub = addressToXpubMap.get(output.addr);
                if(inputXpubList.contains(xpub)){
                    continue;//change back to same xpub
                }

                //Receiving to same address multiple times?
                if (outputMap.containsKey(output.addr)) {
                    long prevAmount = outputMap.get(output.addr) + output.value;
                    outputMap.put(output.addr, prevAmount);
                } else {
                    outputMap.put(output.addr, output.value);
                }

            } else if(PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(output.addr) ||
                    PayloadFactory.getInstance().get().getWatchOnlyAddressStrings().contains(output.addr)){

                //If output address belongs to a legacy address we own - we have to check if it's change

                //If it goes back to same address AND if it's not the total amount sent (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
                if(inputMap.containsKey(output.addr) && output.value != transaction.getAmount()){
                    continue;//change back to same input address
                }

                //Output more than tx amount - change
                if(output.value > transaction.getAmount()){
                    continue;
                }

                outputMap.put(output.addr, output.value);

            } else {

                //Address does not belong to us
                if (transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {
                    continue;//ignore other person's change
                }else{
                    outputMap.put(output.addr, output.value);
                }
            }
        }

        return new Pair<>(inputMap,outputMap);
    }

    public String addressToLabel(String address){

        //If address belongs to owned xpub
        if (MultiAddrFactory.getInstance().isOwnHDAddress(address)){

            if (accountList != null) {
                String xpub = addressToXpubMap.get(address);
                if(xpub != null) {
                    //eventhough it looks like this shouldn't happen,
                    //it sometimes happens with transfers if user clicks to view details immediately.
                    //TODO - see if isOwnHDAddress could be updated to solve this
                    int accIndex = xpubToAccountMap.get(xpub);
                    String label = accountList.get(accIndex).getLabel();
                    if (label != null && !label.isEmpty())
                        return label;
                }
            }

        //If address one of owned legacy addresses
        }else if (PayloadFactory.getInstance().get().getLegacyAddressStrings().contains(address) ||
                PayloadFactory.getInstance().get().getWatchOnlyAddressStrings().contains(address)){

            String label = legacyAddressList.get(legacyAddressLabelList.indexOf(address)).getLabel();
            if (label != null && !label.isEmpty())
                return label;
        }

        return address;
    }
}
