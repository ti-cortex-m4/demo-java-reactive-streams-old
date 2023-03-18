package part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Predicate;

public class NumbersFilterProcessor extends SubmissionPublisher<Integer> implements Flow.Processor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersFilterProcessor.class);

    private Flow.Subscription subscription;

    private Predicate<Integer> predicate = new Predicate<Integer>() {
        @Override
        public boolean test(Integer s) {
            return s % 2 == 0;
        }
    };

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("Filter.onSubscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);

    }

    @Override
    public void onNext(Integer item) {
        if (predicate.test(item)) {
            logger.info("Filter.onNext: {}", item);
            submit(item);
        }
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.info("Filter.onError: {}", throwable.getMessage());
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("Filter.onComplete");
        close();
    }
}
