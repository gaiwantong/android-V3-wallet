package info.blockchain.wallet.rxjava;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by adambennett on 12/08/2016.
 *
 * A class for basic RxJava utilities, ie Transformer classes
 */

public class RxUtil {

    /**
     * Applies standard Schedulers to an Observable, ie IO for subscription, Main Thread for
     * onNext/onComplete/onError
     */
    public static <T> Observable.Transformer<T, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
