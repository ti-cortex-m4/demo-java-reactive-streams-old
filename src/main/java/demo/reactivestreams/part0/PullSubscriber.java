package demo.reactivestreams.part0;

import demo.reactivestreams.Delay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PullSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(PullSubscriber.class);

    private final CountDownLatch countDownLatch;

    private Flow.Subscription subscription;

    public PullSubscriber(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("subscriber.subscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        Delay.delay();
        logger.info("subscriber.next: {}", item);
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("subscriber.error", throwable);
    }

    @Override
    public void onComplete() {
        logger.info("subscriber.complete");
        countDownLatch.countDown();
    }
}