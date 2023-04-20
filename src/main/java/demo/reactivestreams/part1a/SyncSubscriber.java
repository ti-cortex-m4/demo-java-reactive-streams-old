package demo.reactivestreams.part1a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

public abstract class SyncSubscriber<T> implements Flow.Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(SyncSubscriber.class);

    private Flow.Subscription subscription;
    private boolean done = false;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("subscriber.subscribe: {}", subscription);
        if (subscription == null) {
            throw new NullPointerException();
        }

        if (this.subscription != null) {
            subscription.cancel();
        } else {
            this.subscription = subscription;
            subscription.request(1);
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
                this.subscription.request(1);
            } else {
                done = true;
                this.subscription.cancel();
            }
        }
    }

    protected abstract boolean whenNext(T element);

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
