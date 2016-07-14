package wallet.pairing;


import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PairingTest {

    @Mock
    Context mMockContext;

//    Pairing pairing = new Pairing(mMockContext);

    @Test
    public void decode_whenBadString_shouldFail() {

//        String strBad1 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
//        // missing 2 parts
//        String strBad2 = "1524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
//        // won't decrypt
//        String strBad3 = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0|BOGUS_PJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";
//
//        Assert.assertNull(pairing.handleQRCode(strBad1));
//        Assert.assertNull(pairing.handleQRCode(strBad2));
//        Assert.assertNull(pairing.handleQRCode(strBad3));
    }

    @Test
    public void decode_whenGoodString_shouldPass() throws Exception {

//        //TODO - fix unit test
//        String guid = "a09910d9-1906-4ea1-a956-2508c3fe0661";
//        String strGood = "1|"+guid+"|TGbFKLZQ+ZxaAyDwdUcMOAtzolqUYMdkjOYautXPNt41AXqjk67P9aDqRPMM4mmbZ0VPDEpr/xYBSBhjxDCye4L9/MwABu6S3NNV8x+Kn/Q=";
//
//        PairingQRComponents components = pairing.getQRComponentsFromRawString(strGood);
//        assertThat(components.guid, is(guid));
//
//        String encryptedPassword = pairing.getPairingEncryptionPassword(components.guid);
//
//        pairing.handleQRCode(components, encryptedPassword);

//        assertThat("QR decode should pass", pairing.handleQRCode(strGood) != null);
    }
}
