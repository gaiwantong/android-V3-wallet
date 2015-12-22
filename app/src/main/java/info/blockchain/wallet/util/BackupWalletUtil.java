package info.blockchain.wallet.util;

import android.content.Context;
import android.util.Pair;

import info.blockchain.wallet.HDPayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import piuk.blockchain.android.R;

//import android.util.Log;

public class BackupWalletUtil {

    private static BackupWalletUtil instance = null;
    private static Context context = null;

    private BackupWalletUtil() {
        ;
    }

    /**
     * Return instance for BackupWalletUtil.
     *
     * @param Context ctx app context
     * @return BackupWalletUtil
     */
    public static BackupWalletUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new BackupWalletUtil();
        }

        return instance;
    }

    /**
     * Return ordered list of integer, string pairs which can be used to confirm mnemonic.
     *
     * @return List<Pair<Integer,String>>
     */
    public List<Pair<Integer, String>> getConfirmSequence() {

        List<Pair<Integer, String>> toBeConfirmed = new ArrayList<Pair<Integer, String>>();
        String[] s = getMnemonic();
        SecureRandom random = new SecureRandom();
        List<Integer> seen = new ArrayList<Integer>();

        int sel = 0;
        int i = 0;
        while (i < 3) {
            if (i == 3) {
                break;
            }
            sel = random.nextInt(s.length);
            if (seen.contains(sel)) {
                continue;
            } else {
                seen.add(sel);
                i++;
            }
        }

        Collections.sort(seen);

        for (int ii = 0; ii < 3; ii++) {
            toBeConfirmed.add(new Pair<Integer, String>(seen.get(ii), s[seen.get(ii)]));
        }

        return toBeConfirmed;
    }

    /**
     * Return mnemonic in the form of a string array. Make sure double encryption access is activated before calling.
     *
     * @return String[]
     */
    public String[] getMnemonic() {
        // Wallet is not double encrypted
        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            return getHDSeedAsMnemonic(true);
        }
        // User has already entered double-encryption password
        else if (PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString().length() > 0) {
            return getMnemonicForDoubleEncryptedWallet();
        }
        // access must be established before calling this function
        else {
            return null;
        }

    }

    private String[] getMnemonicForDoubleEncryptedWallet() {

        if (PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString().length() == 0) {
            ToastCustom.makeText(context, context.getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            return null;
        }

        // Decrypt seedHex (which is double encrypted in this case)
        String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
                PayloadFactory.getInstance().get().getSharedKey(),
                PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
                PayloadFactory.getInstance().get().getDoubleEncryptionPbkdf2Iterations());

        String mnemonic = null;

        // Try to create a using the decrypted seed hex
        try {
            Wallet hdw = WalletFactory.getInstance().get();
            WalletFactory.getInstance().restoreWallet(decrypted_hex, "", 1);

            mnemonic = WalletFactory.getInstance().get().getMnemonic();

            WalletFactory.getInstance().set(hdw);

        } catch (IOException | DecoderException | AddressFormatException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException e) {
            e.printStackTrace();
        } finally {
            if (mnemonic != null && mnemonic.length() > 0) {

                return mnemonic.split("\\s+");

            } else {
                ToastCustom.makeText(context, context.getString(R.string.double_encryption_password_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
        }

        return null;
    }

    private String[] getHDSeedAsMnemonic(boolean mnemonic) {

        String seed = null;

        try {

            seed = HDPayloadBridge.getInstance(context).getHDMnemonic();

        } catch (IOException | MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
        } finally {

            return seed.split("\\s+");

        }
    }
}
