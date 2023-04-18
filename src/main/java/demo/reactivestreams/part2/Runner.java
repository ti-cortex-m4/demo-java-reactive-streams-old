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
        PullSubscriber subscriber = new PullSubscriber(completeLatch);
        publisher.subscribe(subscriber);

        publisher.getIterator().forEachRemaining(item -> {
            logger.info("publisher.next: {}", item);
            subscriber.onNext(item);
        });

        logger.info("publisher.close");
        publisher.close();

        completeLatch.await();
    }
}
