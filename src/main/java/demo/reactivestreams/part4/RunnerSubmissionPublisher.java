package demo.reactivestreams.part4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class RunnerSubmissionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RunnerSubmissionPublisher.class);

    public static void main(String[] args) throws InterruptedException {
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();

        SyncSubscriber<Integer> subscriber1 = new SyncSubscriber<>(1);
        publisher.subscribe(subscriber1);

        SyncSubscriber<Integer> subscriber2 = new SyncSubscriber<>(2);
        publisher.subscribe(subscriber2);

        Iterator<Integer> iterator = IntStream.rangeClosed(0, 9).iterator();
        iterator.forEachRemaining(item -> {
            logger.info("publisher.submit: {}", item);
            publisher.submit(item);
        });

        logger.info("publisher.close");
        publisher.close();

        subscriber1.awaitCompletion();
        subscriber2.awaitCompletion();
    }
}
