package demo.reactivestreams.part1;

import demo.reactivestreams.Delay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class SimpleSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleSubscriber.class);

    private final int id;
    private final CountDownLatch completeLatch;
    private final long onSubscribeRequestCount;
    private final long onNextRequestCount;

    private Flow.Subscription subscription;

    public SimpleSubscriber(int id, CountDownLatch completeLatch, long onSubscribeRequestCount, long onNextRequestCount) {
        this.id = id;
        this.completeLatch = completeLatch;
        this.onSubscribeRequestCount = onSubscribeRequestCount;
        this.onNextRequestCount = onNextRequestCount;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("({}) subscriber.subscribe: {}", id, subscription);
        this.subscription = subscription;
        this.subscription.request(onSubscribeRequestCount);
    }

    @Override
    public void onNext(T item) {
        //Delay.delay();
        logger.info("({}) subscriber.next: {}", id, item);
        if (onNextRequestCount > 0) {
            this.subscription.request(onNextRequestCount);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);
        completeLatch.countDown();
    }
}
