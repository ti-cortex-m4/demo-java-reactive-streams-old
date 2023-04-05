package demo.reactivestreams.part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

public class NoBackpressureSubscriber implements Flow.Subscriber<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NoBackpressureSubscriber.class);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("subscribed: {}", subscription);
    }

    @Override
    public void onNext(Integer item) {
        logger.info("next: {}", item);
    }

    @Override
    public void onError(Throwable t) {
        logger.error("error", t);
    }

    @Override
    public void onComplete() {
        logger.info("completed");
    }
}
