package info.blockchain.wallet.util;

import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;

import com.google.bitcoin.crypto.MnemonicException;

import org.json.JSONException;

import info.blockchain.wallet.MainActivity;
import info.blockchain.wallet.hd.HD_Address;
import info.blockchain.wallet.hd.HD_WalletFactory;
import info.blockchain.wallet.payload.PayloadFactory;
import info.blockchain.wallet.payload.ReceiveAddress;

public class AddressFactory {

    public static final int RECEIVE_CHAIN = 0;
    public static final int CHANGE_CHAIN = 1;

    private static Context context = null;
    private static AddressFactory instance = null;
    
    private static HashMap<Integer,ReceiveAddress> receiveAddresses = null;

    private AddressFactory() { ; }
    
    public static AddressFactory getInstance(Context ctx) {
        
        context = ctx;

        if(instance == null) {
        	receiveAddresses = new HashMap<Integer,ReceiveAddress>();
        	
            instance = new AddressFactory();
        }

        return instance;
    }

    public ReceiveAddress get(int accountIdx, int chain)	{

        int idx = 0;
        HD_Address addr = null;

        try	{
        	if(chain == 0)	{
        		idx = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getNbReceiveAddresses();
                PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).incReceive();
        	}
        	else	{
                idx = PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).getNbChangeAddresses();
                PayloadFactory.getInstance().get().getHdWallet().getAccounts().get(accountIdx).incChange();
        	}
        	addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
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
            addr = HD_WalletFactory.getInstance(context).get().getAccount(accountIdx).getChain(chain).getAddressAt(idx);
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
