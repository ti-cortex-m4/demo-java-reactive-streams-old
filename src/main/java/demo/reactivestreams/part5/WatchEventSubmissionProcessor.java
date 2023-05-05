package demo.reactivestreams.part5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class WatchEventSubmissionProcessor extends SubmissionPublisher<String>
    implements Flow.Processor<WatchEvent<Path>, String> {

    private static final Logger logger = LoggerFactory.getLogger(WatchEventSubmissionProcessor.class);

    private final String extension;

    private Flow.Subscription subscription;

    public WatchEventSubmissionProcessor(String extension) {
        this.extension = extension;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("processor.subscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(WatchEvent<Path> watchEvent) {
        logger.info("processor.next: {} {}", watchEvent.context(), watchEvent.kind());
        if (watchEvent.context().toString().endsWith(extension)) {
            logger.info("processor.submit");
            submit(String.format("file: '%s', action: %s", watchEvent.context(), watchEvent.kind()));
        } else {
            logger.info("processor.skip");
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable t) {
        logger.error("processor.error", t);
        closeExceptionally(t);
    }

    @Override
    public void onComplete() {
        logger.info("processor.completed");
        close();
    }
}
