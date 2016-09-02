package piuk.blockchain.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.io.File;

public class BaseEspressoTest {

    /**
     * Clears application state completely for use between tests. Use alongside <code>new
     * ActivityTestRule<>(Activity.class, false, false)</code> and launch activity manually on setup
     * to avoid Espresso starting your activity automatically.
     */
    protected void clearState() {
        File root = InstrumentationRegistry.getTargetContext().getFilesDir().getParentFile();
        String[] sharedPreferencesFileNames = new File(root, "shared_prefs").list();
        for (String fileName : sharedPreferencesFileNames) {
            InstrumentationRegistry.getTargetContext().getSharedPreferences(fileName.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

}
