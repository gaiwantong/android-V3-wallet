package piuk.blockchain.android;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LauncherActivityTest {

    @Rule
    public ActivityTestRule<LauncherActivity> mActivityRule = new ActivityTestRule(LauncherActivity.class);

    @Test
    public void isLaunched() throws Exception {
        assertTrue(mActivityRule.getActivity() != null);
    }

}