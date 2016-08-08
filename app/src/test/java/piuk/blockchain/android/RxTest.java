package piuk.blockchain.android;

import android.support.annotation.CallSuper;

import org.junit.After;
import org.junit.Before;

import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

/**
 * Created by adambennett on 08/08/2016.
 *
 * Class that forces all Rx observables to be subscribed and observed in the same thread
 * through the same Scheduler that runs immediately.
 */
public class RxTest {

    @Before
    @CallSuper
    public void setUp() throws Exception {
        RxAndroidPlugins.getInstance().reset();
        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });

        RxJavaHooks.setOnIOScheduler(scheduler -> Schedulers.immediate());
        RxJavaHooks.setOnComputationScheduler(scheduler -> Schedulers.immediate());
        RxJavaHooks.setOnNewThreadScheduler(scheduler -> Schedulers.immediate());
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        RxAndroidPlugins.getInstance().reset();
        RxJavaHooks.reset();
    }
}
