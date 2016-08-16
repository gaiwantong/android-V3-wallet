package info.blockchain.wallet.pairing;


import junit.framework.Assert;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PairingTest {

    private Pairing pairing = new Pairing();

    @Test
    public void decode_whenBadString_shouldFail() {

        String qrData_withMissingParts = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";

        try {
            Assert.assertNull(pairing.getQRComponentsFromRawString(qrData_withMissingParts));
            assertThat("QR components from bad string should not pass", false);
        } catch (Exception e) {
            assertThat("QR components from bad string failed as expected", true);
        }
    }

    @Test
    public void decode_whenGoodString_shouldPass() throws Exception {

        String guid = "a09910d9-1906-4ea1-a956-2508c3fe0661";
        String strGood = "1|"+guid+"|TGbFKLZQ+ZxaAyDwdUcMOAtzolqUYMdkjOYautXPNt41AXqjk67P9aDqRPMM4mmbZ0VPDEpr/xYBSBhjxDCye4L9/MwABu6S3NNV8x+Kn/Q=";

        PairingQRComponents components = pairing.getQRComponentsFromRawString(strGood);
        assertThat(components.guid, is(guid));

        //Can't test this - encryptes pw and sharedkey not consistent
//        pairing.getSharedKeyAndPassword(components.encryptedPairingCode, encryptionPassword);
    }
}
