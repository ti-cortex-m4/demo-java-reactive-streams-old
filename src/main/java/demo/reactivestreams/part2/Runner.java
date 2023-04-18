package demo.reactivestreams.part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        Iterator<Integer> iterator = IntStream.rangeClosed(0, 9).iterator();
        SubmissionIteratorPublisher publisher = new SubmissionIteratorPublisher(iterator);

        CountDownLatch completeLatch = new CountDownLatch(1);
        PullSubscriber subscriber = new PullSubscriber(0, completeLatch);
        publisher.subscribe(subscriber);

        CountDownLatch completeLatch2 = new CountDownLatch(1);
        PullSubscriber subscriber2 = new PullSubscriber(1, completeLatch2);
        publisher.subscribe(subscriber2);

        publisher.getIterator().forEachRemaining(item -> {
            logger.info("publisher.next: {}", item);
            subscriber.onNext(item);
        });

        logger.info("publisher.close");
        publisher.close();

        completeLatch.await();
        completeLatch2.await();
    }
}
