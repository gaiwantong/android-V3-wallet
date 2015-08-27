package info.blockchain.wallet.util;

import android.content.Context;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;

import org.bitcoinj.core.bip44.WalletFactory;
import org.bitcoinj.core.bip44.Wallet;
import org.bitcoinj.core.bip44.Address;

import java.io.IOException;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;

import piuk.blockchain.android.R;

public class AddressFactory {

    public static final int LOOKAHEAD_GAP = 20;

    public static final int RECEIVE_CHAIN = 0;
    public static final int CHANGE_CHAIN = 1;

    private static Wallet double_encryption_wallet = null;

    private static Context context = null;
    private static AddressFactory instance = null;

    private AddressFactory() { ; }

    public static AddressFactory getInstance(Context ctx, String[] xpub) throws AddressFormatException {

        context = ctx;

        if(instance == null) {

            if(xpub != null) {
                double_encryption_wallet = WalletFactory.getInstance(xpub).getWatchOnlyWallet();
            }

            instance = new AddressFactory();
        }

        return instance;
    }

    public static AddressFactory getInstance() {

        if(instance == null) {
            instance = new AddressFactory();
        }

        return instance;
    }

    public ReceiveAddress getReceiveAddress(int accountIdx)	{

        int idx = 0;
        Address addr = null;

        try	{
            idx = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getIdxReceiveAddresses();
            if(!PayloadFactory.getInstance().get().isDoubleEncrypted())	{
                addr = WalletFactory.getInstance().get().getAccount(accountIdx).getChain(AddressFactory.RECEIVE_CHAIN).getAddressAt(idx);
            }
            else	{
                addr = double_encryption_wallet.getAccount(accountIdx).getChain(AddressFactory.RECEIVE_CHAIN).getAddressAt(idx);
            }
            if(((idx - MultiAddrFactory.getInstance().getHighestTxReceiveIdx(PayloadFactory.getInstance().get().getAccount2Xpub().get(accountIdx))) < (LOOKAHEAD_GAP - 1)))	{
//                PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).incReceive();
                PayloadBridge.getInstance(context).remoteSaveThread();
            }

        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

        ReceiveAddress ret = new ReceiveAddress(addr.getAddressString(), idx);

        return ret;

    }

    public Address get(int accountIdx, int chain, int idx)	{

        Address addr = null;

        try	{
            if(!PayloadFactory.getInstance().get().isDoubleEncrypted())	{
                addr = WalletFactory.getInstance().get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            }
            else	{
                addr = double_encryption_wallet.getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            }
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            ToastCustom.makeText(context, context.getString(R.string.hd_error), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
        }

        return addr;

    }

}
