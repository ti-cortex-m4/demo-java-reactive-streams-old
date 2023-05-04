package demo.reactivestreams.part5;

import demo.reactivestreams.part1.SyncSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<WatchEvent<Path>> publisher = new PathWatchServiceSubmissionPublisher(System.getProperty("user.home"));
             WatchEventSubmissionProcessor processor = new WatchEventSubmissionProcessor()) {

            SyncSubscriber<String> subscriber = new SyncSubscriber<>(1);
            processor.subscribe(subscriber);
            publisher.subscribe(processor);

            TimeUnit.SECONDS.sleep(60);

            logger.info("runner.close");
            publisher.close();

            subscriber.awaitCompletion();
        }
    }
}
