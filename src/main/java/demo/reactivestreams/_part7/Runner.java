package demo.reactivestreams._part7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();
        SubmissionProcessor processor = new SubmissionProcessor();
        SyncSubscriber<Integer> subscriber = new SyncSubscriber<>();

        processor.subscribe(subscriber);
        publisher.subscribe(processor);

        IntStream.range(0, 10).forEach(i -> {
            logger.info("publisher.submit: {}", i);
            publisher.submit(i);
        });

        logger.info("publisher.close");
        publisher.close();

        subscriber.awaitCompletion();
    }
}
