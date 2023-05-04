package demo.reactivestreams.part5;

import demo.reactivestreams.Delay;
import demo.reactivestreams.part1.SyncSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.SubmissionPublisher;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<WatchEvent<Path>> publisher = new FolderWatchServiceSubmissionPublisher(System.getProperty("user.home"));
             SubmissionProcessor processor = new SubmissionProcessor()) {

            SyncSubscriber<String> subscriber = new SyncSubscriber<>(1);
            processor.subscribe(subscriber);
            publisher.subscribe(processor);

            Delay.delay(100);
            logger.info("runner.close");
            publisher.close();

            subscriber.awaitCompletion();
        }
    }
}
