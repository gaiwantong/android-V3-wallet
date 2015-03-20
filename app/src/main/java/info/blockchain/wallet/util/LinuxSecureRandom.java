package info.blockchain.wallet.util;

import java.io.*;
import java.security.Provider;
import java.security.SecureRandomSpi;
import java.security.Security;

/**
 * A SecureRandom implementation that is able to override the standard JVM provided implementation, and which simply
 * serves random numbers by reading /dev/urandom. That is, it delegates to the kernel on UNIX systems and is unusable
 * on other platforms. Attempts to manually set the seed are ignored. There is no difference between seed bytes and
 * non-seed bytes, they are all from the same source.
 */
public class LinuxSecureRandom extends SecureRandomSpi {
    private static FileInputStream urandom;
    public byte[] seed;

    private static class LinuxSecureRandomProvider extends Provider {
        public LinuxSecureRandomProvider() {
            super("LinuxSecureRandom", 1.0, "A Linux specific random number provider that uses /dev/urandom");
            put("SecureRandom.LinuxSecureRandom", LinuxSecureRandom.class.getName());
        }
    }

    public static void init() {
        if (urandom == null) {
            try {
                File file = new File("/dev/urandom");
                if (file.exists()) {

                    System.out.println("Opened /dev/urandom");

                    // This stream is deliberately leaked.
                    urandom = new FileInputStream(file);
                    // Now override the default SecureRandom implementation with this one.
                    Security.insertProviderAt(new LinuxSecureRandomProvider(), 1);
                } else {
                    urandom =  null;
                }
            } catch (FileNotFoundException e) {
                // Should never happen.
                throw new RuntimeException(e);
            }
        }
    }

    static {
        init();
    }

    private final DataInputStream dis;

    public LinuxSecureRandom() {
        // DataInputStream is not thread safe, so each random object has its own.
        dis = new DataInputStream(urandom);
    }

    @Override
    protected void engineSetSeed(byte[] bytes) {
        byte[] clone = bytes.clone();

        if (seed != null) {
            xor(clone, seed);
        }

        seed = clone;
    }

    private static void xor(byte[] b1, byte[] b2) {
        for (int ii = 0; ii < b1.length && ii < b2.length; ++ii)
            b1[ii] = (byte) (b1[ii] ^ b2[ii]);
    }

    @Override
    public void engineNextBytes(byte[] bytes) {
        try {
            dis.readFully(bytes);  // This will block until all the bytes can be read.

            if (seed != null) {
                xor(bytes, seed);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);  // Fatal error. Do not attempt to recover from this.
        }
    }

    @Override
    protected byte[] engineGenerateSeed(int i) {
        byte[] bits = new byte[i];
        engineNextBytes(bits);
        return bits;
    }
}
