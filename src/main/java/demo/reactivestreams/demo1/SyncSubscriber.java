package demo.reactivestreams.demo1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(SyncSubscriber.class);

    private final int id;
    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;
    private boolean terminated = false;

    public SyncSubscriber(int id) {
        this.id = id;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        // by rule 2.13, a `Subscriber` must throw a `java.lang.NullPointerException` if the `Subscription` is `null`
        Objects.requireNonNull(subscription);

        if (this.subscription != null) {
            // by rule 2.5, a `Subscriber` must cancel the given `Subscription` if it already has an active `Subscription`
            subscription.cancel();
        } else {
            this.subscription = subscription;
            this.subscription.request(1);
        }
    }

    @Override
    public void onNext(T item) {
        logger.info("({}) subscriber.next: {}", id, item);
        // by rule 2.13, a `Subscriber` must throw a `java.lang.NullPointerException` if the `item` is `null`
        Objects.requireNonNull(item);

        if (!terminated) {
            if (whenNext(item)) {
                subscription.request(1);
            } else {
                doTerminate();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
        // by rule 2.13, a `Subscriber` must throw a `java.lang.NullPointerException` if the `Throwable` is `null`
        Objects.requireNonNull(throwable);
        // by rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
        terminated = true;
        whenError(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);
        // by rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
        terminated = true;
        whenComplete();
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    // This method is invoked when the `OnNext` signals arrive and returns whether more elements are desired or not, intended to be overridden.
    protected boolean whenNext(T item) {
        return true;
    }

    // This method is invoked if the `OnError` signal arrives, intended to be overridden.
    protected void whenError(Throwable throwable) {
    }

    // This method is invoked when the `OnComplete` signal arrives, intended to be overridden.
    protected void whenComplete() {
        completed.countDown();
    }

    private void doTerminate() {
        logger.debug("({}) subscriber.terminate", id);
        terminated = true;
        subscription.cancel();
    }
}
