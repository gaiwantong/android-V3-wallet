package info.blockchain.wallet;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AssertUtil {

    private static AssertUtil instance = null;
    private static boolean EXIT_ON_FAIL = false;

    private AssertUtil() { ; }

    public static AssertUtil getInstance() {

        if(instance == null) {
            instance = new AssertUtil();
        }

        return instance;
    }

    public boolean exitOnFail() {
        return EXIT_ON_FAIL;
    }

    public void exitOnFail(boolean exit) {
        EXIT_ON_FAIL = exit;
    }

    public void assert_true(BlockchainTest obj, String msg, boolean test) {

        if(EXIT_ON_FAIL) {
            TestCase.assertTrue(test);
        }
        else {
            try {
                TestCase.assertTrue(test);
            } catch (AssertionError ae) {
                LogUtil.getInstance().log(obj.getName(), "assert true failed: " + msg);
            }
        }
    }

}
