package demo.reactivestreams.part1a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

/**
 * SyncSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs synchronously (on the Publisher's thread) and requests one element
 * at a time and invokes a user-defined method to process each element.
 * <p>
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class AbstractSyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSyncSubscriber.class);

    private Flow.Subscription subscription;
    private boolean done = false;

    @Override
    public void onSubscribe(final Flow.Subscription s) {
        logger.info("subscriber.subscribe: {}", s);
        if (s == null) throw new NullPointerException();

        if (this.subscription != null) { // If someone has made a mistake and added this Subscriber multiple times, let's handle it gracefully
            s.cancel(); // Cancel the additional subscription
        } else {
            // We have to assign it locally before we use it, if we want to be a synchronous `Subscriber`
            // Because according to rule 3.10, the Subscription is allowed to call `onNext` synchronously from within `request`
            this.subscription = s;

            s.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
        }
    }

    @Override
    public void onNext(final T element) {
        logger.info("subscriber.next: {}", element);
        if (element == null) throw new NullPointerException();

        if (!done) { // If we aren't already done
            try {
                if (whenNext(element)) {
                    this.subscription.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
                } else {
                    done();
                }
            } catch (final Throwable t) {
                done();
                try {
                    onError(t);
                } catch (final Throwable t2) {
                    //Subscriber.onError is not allowed to throw an exception, according to rule 2.13
                    (new IllegalStateException(this + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
                }
            }
        }
    }

    // Showcases a convenience method to idempotently marking the Subscriber as "done", so we don't want to process more elements
    // herefor we also need to cancel our `Subscription`.
    private void done() {
        //On this line we could add a guard against `!done`, but since rule 3.7 says that `Subscription.cancel()` is idempotent, we don't need to.
        done = true; // If we `whenNext` throws an exception, let's consider ourselves done (not accepting more elements)

        this.subscription.cancel(); // Cancel the subscription
    }

    // This method is left as an exercise to the reader/extension point
    // Returns whether more elements are desired or not, and if no more elements are desired
    protected abstract boolean whenNext(final T element);

    @Override
    public void onError(final Throwable t) {
        logger.error("subscriber.error", t);
        if (t == null) throw new NullPointerException();
    }

    @Override
    public void onComplete() {
        logger.info("subscriber.complete");
    }
}