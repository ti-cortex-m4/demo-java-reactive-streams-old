package demo.reactivestreams.part2;

import demo.reactivestreams.Delay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class Subscriber implements Flow.Subscriber<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Subscriber.class);

    private final int id;
    private final CountDownLatch countDownLatch;
    private final long onSubscribeRequestCount;
    private final long onNextRequestCount;

    private Flow.Subscription subscription;

    public Subscriber(int id, CountDownLatch countDownLatch, long onSubscribeRequestCount, long onNextRequestCount) {
        this.id = id;
        this.countDownLatch = countDownLatch;
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
    public void onNext(Integer item) {
        Delay.delay();
        logger.info("({}) subscriber.next: {}", id, item);
        this.subscription.request(onNextRequestCount);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("({}) subscriber.error", id, throwable);
    }

    @Override
    public void onComplete() {
        logger.info("({}) subscriber.complete", id);
        countDownLatch.countDown();
    }
}
