package demo.reactivestreams.demo.demo1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class SyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(SyncSubscriber.class);

    private final int id;
    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;
    private boolean cancelled = false;

    public SyncSubscriber(int id) {
        this.id = id;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        // By rule 2.13, calling onSubscribe must throw a NullPointerException when the given parameter is null.
        Objects.requireNonNull(subscription);

        if (this.subscription != null) {
            // By_rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
            subscription.cancel();
        } else {
            this.subscription = subscription;
            // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
            this.subscription.request(1);
        }
    }

    @Override
    public void onNext(T item) {
        logger.info("({}) subscriber.next: {}", id, item);
        // By rule 2.13, calling onNext must throw a NullPointerException when the given parameter is null.
        Objects.requireNonNull(item);

        // By_rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
        if (!cancelled) {
            if (whenNext(item)) {
                // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
                subscription.request(1);
            } else {
                // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
                doCancel();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
        // By rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
        Objects.requireNonNull(throwable);

        // By rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
        cancelled = true;
        whenError(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);

        // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
        cancelled = true;
        whenComplete();
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    // this method is invoked when OnNext signals arrive and returns whether more elements are desired or not (is intended to override).
    protected boolean whenNext(T item) {
        return true;
    }

    // this method is invoked when an OnError signal arrives (is intended to override).
    protected void whenError(Throwable throwable) {
    }

    // this method is invoked when an OnComplete signal arrives (is intended to override).
    protected void whenComplete() {
        completed.countDown();
    }

    private void doCancel() {
        cancelled = true;
        subscription.cancel();
    }
}
