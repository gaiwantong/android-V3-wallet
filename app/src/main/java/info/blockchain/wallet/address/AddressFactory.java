package info.blockchain.wallet.address;

import android.content.Context;

import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;
import info.blockchain.wallet.ui.helpers.ToastCustom;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.Util;
import info.blockchain.wallet.util.WebUtil;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.bip44.Address;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SecureRandom;

import piuk.blockchain.android.R;

public class AddressFactory {

    public static final int RECEIVE_CHAIN = 0;

    private static Wallet double_encryption_wallet = null;

    private static Context context = null;
    private static AddressFactory instance = null;

    private AddressFactory() {
        ;
    }

    public static AddressFactory getInstance(Context ctx, String[] xpub) throws AddressFormatException {
        context = ctx;

        if (instance == null) {

            if (xpub != null) {
                double_encryption_wallet = WalletFactory.getInstance(xpub).getWatchOnlyWallet();
            }

            instance = new AddressFactory();
        }

        return instance;
    }

    public void wipe(){
        instance = null;
        double_encryption_wallet = null;
    }

    public void updateDoubleEncryptionWallet(){
        double_encryption_wallet = WalletFactory.getInstance().getWatchOnlyWallet();
    }

    public ReceiveAddress getReceiveAddress(int accountIdx) {
        int idx = 0;
        Address addr = null;

        try {
            idx = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getIdxReceiveAddresses();
            if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
                addr = WalletFactory.getInstance().get().getAccount(accountIdx).getChain(AddressFactory.RECEIVE_CHAIN).getAddressAt(idx);
            } else {
                addr = double_encryption_wallet.getAccount(accountIdx).getChain(AddressFactory.RECEIVE_CHAIN).getAddressAt(idx);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        } catch (MnemonicException.MnemonicLengthException mle) {
            mle.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

        ReceiveAddress ret = new ReceiveAddress(addr.getAddressString(), idx);

        return ret;
    }

    /*
    Generate V2 legacy address
     */
    public ECKey newLegacyAddress() {

        new AppUtil(context).applyPRNGFixes();

        String result = null;
        byte[] data = null;
        try {
            result = WebUtil.getInstance().getURL(WebUtil.EXTERNAL_ENTROPY_URL);
            if (!result.matches("^[A-Fa-f0-9]{64}$")) {
                return null;
            }
            data = Hex.decode(result);
        } catch (Exception e) {
            return null;
        }

        ECKey ecKey = null;
        if (data != null) {
            byte[] rdata = new byte[32];
            SecureRandom random = new SecureRandom();
            random.nextBytes(rdata);
            byte[] privbytes = Util.getInstance().xor(data, rdata);
            if (privbytes == null) {
                return null;
            }
            ecKey = ECKey.fromPrivate(privbytes, true);
            // erase all byte arrays:
            random.nextBytes(privbytes);
            random.nextBytes(rdata);
            random.nextBytes(data);
        } else {
            return null;
        }

        return ecKey;
    }
}
