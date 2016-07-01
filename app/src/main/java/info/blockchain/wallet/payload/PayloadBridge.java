package info.blockchain.wallet.payload;

import android.os.Looper;

import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PayloadBridge.java : singleton class for remote save of payload and wallet creation
 */
public class PayloadBridge {

    private static PayloadBridge instance;

    private PayloadBridge() {
    }

    public static PayloadBridge getInstance(){

        if(instance == null){
            instance = new PayloadBridge();
        }

        return instance;
    }

    public interface PayloadSaveListener{
        void onSaveSuccess();
        void onSaveFail();
    }

    /**
     * Thread for remote save of payload to server.
     */
    public void remoteSaveThread(PayloadSaveListener listener) {

        new Thread(() -> {
            Looper.prepare();

            if (PayloadFactory.getInstance().put()) {
                listener.onSaveSuccess();
            } else {
                listener.onSaveFail();
            }

            Looper.loop();

        }).start();
    }

    public Payload createBlockchainWallet(String defaultAccountName) throws IOException, MnemonicException.MnemonicLengthException {

        org.bitcoinj.core.bip44.Wallet hdw = WalletFactory.getInstance().get();

        String guid = UUID.randomUUID().toString();
        String sharedKey = UUID.randomUUID().toString();

        Payload payload = new Payload();
        payload.setGuid(guid);
        payload.setSharedKey(sharedKey);

        HDWallet payloadHDWallet = new HDWallet();
        payloadHDWallet.setSeedHex(hdw.getSeedHex());

        List<org.bitcoinj.core.bip44.Account> hdAccounts = hdw.getAccounts();
        List<info.blockchain.wallet.payload.Account> payloadAccounts = new ArrayList<Account>();
        for (int i = 0; i < hdAccounts.size(); i++) {
            info.blockchain.wallet.payload.Account account = new info.blockchain.wallet.payload.Account(defaultAccountName);

            String xpub = WalletFactory.getInstance().get().getAccounts().get(i).xpubstr();
            account.setXpub(xpub);
            String xpriv = WalletFactory.getInstance().get().getAccounts().get(i).xprvstr();
            account.setXpriv(xpriv);

            payloadAccounts.add(account);
        }
        payloadHDWallet.setAccounts(payloadAccounts);

        payload.setHdWallets(payloadHDWallet);

        PayloadFactory.getInstance().set(payload);
        PayloadFactory.getInstance().setNew(true);

        return payload;
    }
}