package demo.reactivestreams.part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        Iterator<Integer> iterator = IntStream.rangeClosed(0, 9).iterator();
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<Integer>();

        CountDownLatch completeLatch1 = new CountDownLatch(1);
        PullSubscriber subscriber1 = new PullSubscriber(0, completeLatch1, 1, 1);
        publisher.subscribe(subscriber1);

        CountDownLatch completeLatch2 = new CountDownLatch(1);
        PullSubscriber subscriber2 = new PullSubscriber(1, completeLatch2, 1, 1);
        publisher.subscribe(subscriber2);

        iterator.forEachRemaining(item -> {
            logger.info("publisher.submit: {}", item);
            publisher.submit(item);
        });

        logger.info("publisher.close");
        publisher.close();

        completeLatch1.await();
        completeLatch2.await();
    }
}
