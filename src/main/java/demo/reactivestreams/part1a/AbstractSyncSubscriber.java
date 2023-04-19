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
    public void onSubscribe(Flow.Subscription s) {
        logger.info("subscriber.subscribe: {}", s);
        if (s == null) {
          throw new NullPointerException();
        }

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
    public void onNext(T element) {
        logger.info("subscriber.next: {}", element);
        if (element == null) {
          throw new NullPointerException();
        }

        if (!done) {
                if (whenNext(element)) {
                    this.subscription.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
                } else {
                    done();
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
    public void onError(Throwable throwable) {
        logger.error("subscriber.error", throwable);
        if (throwable == null) {
          throw new NullPointerException();
        }
    }

    @Override
    public void onComplete() {
        logger.info("subscriber.complete");
    }
}
