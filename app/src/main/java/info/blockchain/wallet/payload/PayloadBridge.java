package info.blockchain.wallet.payload;

import android.content.Context;
import android.os.Looper;

import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.PrefsUtil;

import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import piuk.blockchain.android.R;

//import android.util.Log;

/**
 * PayloadBridge.java : singleton class for remote save of payload and wallet creation
 */
public class PayloadBridge {


    private static Context context = null;

    private static PayloadBridge instance = null;
    private static AppUtil appUtil;

    private PayloadBridge() {
        ;
    }

    /**
     * Return instance for a PayloadBridge
     *
     * @param ctx app context
     * @return PayloadBridge
     */
    public static PayloadBridge getInstance(Context ctx) {

        context = ctx;
        appUtil = new AppUtil(context);

        if (instance == null) {
            instance = new PayloadBridge();
        }

        return instance;
    }

    /**
     * Create a Blockchain wallet and include the HD_Wallet passed as an argument and write it to this instance's
     * payload.
     *
     * @param hdw HD wallet to include in the payload
     * @return boolean
     */
    public boolean createBlockchainWallet(org.bitcoinj.core.bip44.Wallet hdw) {

        String guid = UUID.randomUUID().toString();
        String sharedKey = UUID.randomUUID().toString();

        Payload payload = new Payload();
        payload.setGuid(guid);
        payload.setSharedKey(sharedKey);

        new PrefsUtil(context).setValue(PrefsUtil.KEY_GUID, guid);
        appUtil.setSharedKey(sharedKey);

        HDWallet payloadHDWallet = new HDWallet();
        payloadHDWallet.setSeedHex(hdw.getSeedHex());

        List<org.bitcoinj.core.bip44.Account> hdAccounts = hdw.getAccounts();
        List<info.blockchain.wallet.payload.Account> payloadAccounts = new ArrayList<info.blockchain.wallet.payload.Account>();
        for (int i = 0; i < hdAccounts.size(); i++) {
            info.blockchain.wallet.payload.Account account = new info.blockchain.wallet.payload.Account(context.getString(R.string.default_wallet_name));
            try {
                String xpub = WalletFactory.getInstance().get().getAccounts().get(i).xpubstr();
                account.setXpub(xpub);
                String xpriv = WalletFactory.getInstance().get().getAccounts().get(i).xprvstr();
                account.setXpriv(xpriv);
            } catch (IOException | MnemonicException.MnemonicLengthException e) {
                e.printStackTrace();
            }

            payloadAccounts.add(account);
        }
        payloadHDWallet.setAccounts(payloadAccounts);

        payload.setHdWallets(payloadHDWallet);

        PayloadFactory.getInstance().set(payload);
        PayloadFactory.getInstance().setNew(true);

        return true;
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
}