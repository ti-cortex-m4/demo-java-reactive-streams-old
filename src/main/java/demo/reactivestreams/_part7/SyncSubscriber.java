package demo.reactivestreams._part7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class SyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(SyncSubscriber.class);

    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("subscriber.subscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        logger.info("subscriber.next: {}", item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("subscriber.error", throwable);
    }

    @Override
    public void onComplete() {
        logger.info("subscriber.complete");
        completed.countDown();
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }
}
