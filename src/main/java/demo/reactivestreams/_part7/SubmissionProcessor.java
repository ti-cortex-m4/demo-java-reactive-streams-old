package demo.reactivestreams._part7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.StandardWatchEventKinds;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionProcessor extends SubmissionPublisher<String> implements Flow.Processor<FolderWatchEvent, String> {

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
        if (item.getEvent().kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            submit(String.format("file %s is %s", item.getEvent().context(), item.getEvent().kind()));
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
