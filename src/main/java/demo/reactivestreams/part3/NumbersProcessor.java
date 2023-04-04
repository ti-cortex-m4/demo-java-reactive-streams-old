package demo.reactivestreams.part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Predicate;

public class NumbersProcessor extends SubmissionPublisher<Integer> implements Flow.Processor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersProcessor.class);

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("processor.subscribed: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);

    }

    @Override
    public void onNext(Integer item) {
        logger.info("processor.next: {}", item);
        if (item % 2 == 0) {
            logger.info("processor.submit: {}", item);
            submit(item*item);
        } else {
            logger.info("processor.skip: {}", item);
        }
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("processor.error",throwable);
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("processor.completed");
        close();
    }
}
