package part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Predicate;

public class NumbersProcessor2 extends SubmissionPublisher<Integer> implements Flow.Processor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(NumbersProcessor2.class);

    private Flow.Subscription subscription;

    private Predicate<Integer> predicate = new Predicate<Integer>() {
        @Override
        public boolean test(Integer s) {
            return s % 2 == 0;
        }
    };

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("Processor.onSubscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);

    }

    @Override
    public void onNext(Integer item) {
        logger.info("Processor.onNext: {}", item);
        if (predicate.test(item)) {
            submit(item);
        }
        this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.info("Processor.onError: {}", throwable.getMessage());
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("Processor.onComplete");
        close();
    }
}
