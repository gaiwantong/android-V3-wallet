package info.blockchain.wallet.util;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.core.AddressFormatException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Pair;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.codec.DecoderException;

import info.blockchain.wallet.HDPayloadBridge;
import info.blockchain.wallet.R;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
//import android.util.Log;

public class BackupWalletUtil {

    private static BackupWalletUtil instance = null;
    private static Context context = null;

    private BackupWalletUtil() { ; }

    /**
     * Return instance for BackupWalletUtil.
     *
     * @param  Context ctx app context
     *
     * @return BackupWalletUtil
     *
     */
    public static BackupWalletUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new BackupWalletUtil();
        }

        return instance;
    }

    /**
     * Return ordered list of integer, string pairs which can be used to confirm mnemonic.
     *
     * @return List<Pair<Integer,String>>
     *
     */
    public List<Pair<Integer,String>> getConfirmSequence() {

        List<Pair<Integer,String>> toBeConfirmed = new ArrayList<Pair<Integer,String>>();
        String[] s = getMnemonic();
        SecureRandom random = new SecureRandom();
        List<Integer> seen = new ArrayList<Integer>();

        int sel = 0;
        int i = 0;
        while(i < 3) {
            if(i == 3) {
                break;
            }
            sel = random.nextInt(s.length);
            if(seen.contains(sel)) {
                continue;
            }
            else {
                seen.add(sel);
                i++;
            }
        }

        Collections.sort(seen);

        for(int ii = 0; ii < 3; ii++) {
            toBeConfirmed.add(new Pair<Integer, String>(seen.get(ii), s[seen.get(ii)]));
        }

        return toBeConfirmed;
    }

    /**
     * Return mnemonic in the form of a string array. Make sure double encryption access is activated before calling.
     *
     * @return String[]
     *
     */
    public String[] getMnemonic() {
        // Wallet is not double encrypted
        if (!PayloadFactory.getInstance().get().isDoubleEncrypted()) {
            return getHDSeedAsMnemonic(true);
        }
        // User has already entered double-encryption password
        else if (DoubleEncryptionFactory.getInstance().isActivated()) {
            return getMnemonicForDoubleEncryptedWallet();
        }
        // access must be established before calling this function
        else {
            return null;
        }

    }

    private String[] getMnemonicForDoubleEncryptedWallet() {

        if (PayloadFactory.getInstance().getTempDoubleEncryptPassword() == null || PayloadFactory.getInstance().getTempDoubleEncryptPassword().length() == 0) {
            Toast.makeText(context, R.string.double_encryption_password_error, Toast.LENGTH_SHORT).show();
            return null;
        }

        // Decrypt seedHex (which is double encrypted in this case)
        String decrypted_hex = DoubleEncryptionFactory.getInstance().decrypt(
                PayloadFactory.getInstance().get().getHdWallet().getSeedHex(),
                PayloadFactory.getInstance().get().getSharedKey(),
                PayloadFactory.getInstance().getTempDoubleEncryptPassword().toString(),
                PayloadFactory.getInstance().get().getIterations());

        String mnemonic = null;

        // Try to create a using the decrypted seed hex
        try {
            HD_Wallet hdw = HD_WalletFactory.getInstance(context).get();
            HD_WalletFactory.getInstance(context).restoreWallet(decrypted_hex, "", 1);

            mnemonic = HD_WalletFactory.getInstance(context).get().getMnemonic();

            HD_WalletFactory.getInstance(context).set(hdw);

        } catch (IOException | DecoderException | AddressFormatException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException | MnemonicException.MnemonicChecksumException e) {
            e.printStackTrace();
        } finally {
            if (mnemonic != null && mnemonic.length() > 0) {

                return mnemonic.split("\\s+");

            } else {
                Toast.makeText(context, R.string.double_encryption_password_error, Toast.LENGTH_LONG).show();
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
            Toast.makeText(context, R.string.hd_error, Toast.LENGTH_SHORT).show();
        } finally {

            return seed.split("\\s+");

        }
    }
}
