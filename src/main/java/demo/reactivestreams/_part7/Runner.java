package demo.reactivestreams._part7;

import demo.reactivestreams.Delay;
import demo.reactivestreams.part1.SyncSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        try (SubmissionPublisher<FolderWatchEvent> publisher = new FolderWatchServiceSubmissionPublisher(System.getProperty("user.home"));
             SubmissionProcessor processor = new SubmissionProcessor()) {

            SyncSubscriber<String> subscriber = new SyncSubscriber<>(1);
            processor.subscribe(subscriber);
            publisher.subscribe(processor);

            Delay.delay(10);
            logger.info("publisher.close");
            publisher.close();

            subscriber.awaitCompletion();
        }
    }
}
