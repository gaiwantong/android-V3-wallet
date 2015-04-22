package info.blockchain.wallet.util;

import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.widget.Toast;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.crypto.MnemonicException;

import info.blockchain.wallet.hd.HD_Address;
import info.blockchain.wallet.hd.HD_Wallet;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;

public class AddressFactory {

    public static final int LOOKAHEAD_GAP = 20;

    public static final int RECEIVE_CHAIN = 0;
    public static final int CHANGE_CHAIN = 1;

    private static HD_Wallet double_encryption_wallet = null;

    private static Context context = null;
    private static AddressFactory instance = null;

    private static HashMap<Integer,ReceiveAddress> receiveAddresses = null;

    private AddressFactory() { ; }

    public static AddressFactory getInstance(Context ctx, String[] xpub) throws AddressFormatException {

        context = ctx;

        if(instance == null) {
            receiveAddresses = new HashMap<Integer,ReceiveAddress>();

            if(xpub != null) {
                double_encryption_wallet = HD_WalletFactory.getInstance(context, xpub).getWatchOnlyWallet();
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

    public ReceiveAddress get(int accountIdx, int chain)	{

        int idx = 0;
        HD_Address addr = null;

        try	{
            if(chain == RECEIVE_CHAIN)	{
                idx = PayloadFactory.getInstance().getPayloadObject().getHdWallet().getAccounts().get(accountIdx).getNbReceiveAddresses();
                PayloadFactory.getInstance().getPayloadObject().getHdWallet().getAccounts().get(accountIdx).incReceive();
            }
            else	{
                idx = PayloadFactory.getInstance().getPayloadObject().getHdWallet().getAccounts().get(accountIdx).getNbChangeAddresses();
                PayloadFactory.getInstance().getPayloadObject().getHdWallet().getAccounts().get(accountIdx).incChange();
            }
            if(!PayloadFactory.getInstance().getPayloadObject().isDoubleEncrypted())	{
                addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            }
            else	{
                addr = double_encryption_wallet.getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            }
            if(chain == RECEIVE_CHAIN && ((idx - PayloadFactory.getInstance().getPayloadObject().getHdWallet().getAccounts().get(accountIdx).getNbReceiveAddresses()) < (LOOKAHEAD_GAP - 1)))	{
//                HD_WalletFactory.getInstance(context).getPayloadObject().getAccount(0).getChain(chain).incAddrIdx();
                PayloadFactory.getInstance().getPayloadObject().getHdWallet().getAccounts().get(chain).incReceive();
            }
            PayloadFactory.getInstance(context).remoteSaveThread();

        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        ReceiveAddress ret = new ReceiveAddress(addr.getAddressString(), idx);
        if(receiveAddresses.get(accountIdx) != null && ((ReceiveAddress)receiveAddresses.get(accountIdx)).getAddress().equals(ret.getAddress()))	{
            ret = new ReceiveAddress(addr.getAddressString(), idx);
        }
        receiveAddresses.put(accountIdx, ret);

        return ret;

    }

    public HD_Address get(int accountIdx, int chain, int idx)	{

        HD_Address addr = null;

        try	{
            if(!PayloadFactory.getInstance().getPayloadObject().isDoubleEncrypted())	{
                addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            }
            else	{
                addr = double_encryption_wallet.getAccount(accountIdx).getChain(chain).getAddressAt(idx);
            }
        }
        catch(IOException ioe)	{
            ioe.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }
        catch(MnemonicException.MnemonicLengthException mle)	{
            mle.printStackTrace();
            Toast.makeText(context, "HD wallet error", Toast.LENGTH_SHORT).show();
        }

        return addr;

    }

}
