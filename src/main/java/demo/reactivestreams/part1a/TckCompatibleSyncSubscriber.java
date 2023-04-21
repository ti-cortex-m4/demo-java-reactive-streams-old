package demo.reactivestreams.part1a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class TckCompatibleSyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(TckCompatibleSyncSubscriber.class);

    private final int id;
    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;
    private boolean done = false;

    public TckCompatibleSyncSubscriber(int id) {
        this.id = id;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        if (subscription == null) {
            throw new NullPointerException();
        }

        if (this.subscription != null) {
            subscription.cancel();
        } else {
            this.subscription = subscription;
            this.subscription.request(1);
        }
    }

    @Override
    public void onNext(T element) {
        logger.info("({}) subscriber.next: {}", id, element);
        if (element == null) {
            throw new NullPointerException();
        }

        if (!done) {
            if (whenNext(element)) {
                subscription.request(1);
            } else {
                done();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
        if (throwable == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete",id);
        completed.countDown();
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    protected boolean whenNext(T element) {
        return true;
    }

    private void done() {
        logger.info("({}) subscriber.done", id);
        done = true;
        subscription.cancel();
    }
}
