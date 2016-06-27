package wallet.pairing;


import android.content.Context;

import info.blockchain.wallet.pairing.Pairing;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PairingTest {

    @Mock
    Context mMockContext;

    Pairing pairing = new Pairing(mMockContext);

    @Test
    public void decode_whenBadString_shouldFail() {

        String strBad1 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // missing 2 parts
        String strBad2 = "1524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
        // won't decrypt
        String strBad3 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0|BOGUS_PJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";

        Assert.assertNull(pairing.handleQRCode(strBad1));
        Assert.assertNull(pairing.handleQRCode(strBad2));
        Assert.assertNull(pairing.handleQRCode(strBad3));
    }

    @Test
    public void decode_whenGoodString_shouldPass() {

        //TODO - fix unit test
//        String strGood = "1|70c46c4c-6fb2-4790-a4d9-9160ed942263|V5H7lH+FixpBPTuU2Uv+LLVn0FPs+Kv9VKsHc/TY3PRYBmuVQAWOStJh/6/35+FlIMuybutRLJcfjdycGg26WhwKNsqlzM/im0nOzc7PmjJS8UXcTuQE7NgGRkIi3Xcb20M2/x5gZ7dnvAHYue1P/A==";
//        Assert.assertNotNull(pairingFactory.handleQRCode(strGood));
    }
}
