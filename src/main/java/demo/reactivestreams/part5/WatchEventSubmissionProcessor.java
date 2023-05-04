package demo.reactivestreams.part5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class WatchEventSubmissionProcessor extends SubmissionPublisher<String> implements Flow.Processor<WatchEvent<Path>, String> {

    private static final Logger logger = LoggerFactory.getLogger(WatchEventSubmissionProcessor.class);

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        logger.info("processor.subscribe: {}", subscription);
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(WatchEvent<Path> item) {
        logger.info("processor.next: {}", item);
        if (item.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            submit(String.format("file %s is %s", item.context(), item.kind()));
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
