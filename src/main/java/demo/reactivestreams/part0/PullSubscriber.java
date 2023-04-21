package demo.reactivestreams.part0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PullSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(PullSubscriber.class);

    private final int id;
    private final CountDownLatch completed = new CountDownLatch(1);

    private Flow.Subscription subscription;

    public PullSubscriber(int id) {
        this.id = id;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        logger.info("({}) subscriber.next: {}", id, item);
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);
        whenComplete();
    }

    public void awaitCompletion() throws InterruptedException {
        completed.await();
    }

    protected void whenComplete() {
        completed.countDown();
    }
}
