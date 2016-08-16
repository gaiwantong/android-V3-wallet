package info.blockchain.wallet.util;

import info.blockchain.wallet.crypto.AESUtil;

public class AESUtilWrapper {

    public String decrypt(String ciphertext, CharSequenceX password, int iterations) {
        return AESUtil.decrypt(ciphertext, password, iterations);
    }

}
