package info.blockchain.wallet.rxjava;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by adambennett on 12/08/2016.
 *
 * Most of the time in RxJava, we're only concerned about the objects emitted by the Observable
 * and any errors - onCompleted isn't important. Use this class to reduce some of the boilerplate
 * associated with standard Subscriber implementations. Override onCompleted if you so wish.
 */

public abstract class BasicSubscriber<T> extends Subscriber<T> implements Observer<T>, Subscription {

    @Override
    public void onCompleted() {
        // Override if necessary or just use Subscriber
    }
}
