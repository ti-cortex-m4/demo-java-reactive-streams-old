package part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

public class NumbersSubscriber implements Flow.Subscriber<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersSubscriber.class);

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("Subscriber.onSubscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(Integer item) {
        logger.info("Subscriber.onNext: {}", item);
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.info("Subscriber.onError: {}", throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        logger.info("Subscriber.onComplete");
    }
}
