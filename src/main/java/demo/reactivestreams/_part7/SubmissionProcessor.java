package demo.reactivestreams._part7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionProcessor extends SubmissionPublisher<FolderWatchEvent> implements Flow.Processor<FolderWatchEvent, String> {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionProcessor.class);

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("processor.subscribed: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(FolderWatchEvent item) {
        logger.info("processor.next: {}", item);
        if (item % 2 == 0) {
            logger.info("processor.submit: {}", item);
            submit(item * 10);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("processor.error", throwable);
        closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        logger.info("processor.completed");
        close();
    }
}
