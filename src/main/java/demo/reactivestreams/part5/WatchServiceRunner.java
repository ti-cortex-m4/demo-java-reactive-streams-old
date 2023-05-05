package demo.reactivestreams.part5;

import demo.reactivestreams.part1.SyncSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class WatchServiceRunner {

    private static final Logger logger = LoggerFactory.getLogger(WatchServiceRunner.class);

    public static void main(String[] args) throws InterruptedException {
        String folderName = System.getProperty("user.home");
        String extension = ".txt";
        try (SubmissionPublisher<WatchEvent<Path>> publisher = new WatchServiceSubmissionPublisher(folderName);
             WatchEventSubmissionProcessor processor = new WatchEventSubmissionProcessor(extension)) {

            SyncSubscriber<String> subscriber = new SyncSubscriber<>(1);
            processor.subscribe(subscriber);
            publisher.subscribe(processor);

            TimeUnit.SECONDS.sleep(180);

            logger.info("publisher.close");
            publisher.close();

            subscriber.awaitCompletion();
        }
    }
}
