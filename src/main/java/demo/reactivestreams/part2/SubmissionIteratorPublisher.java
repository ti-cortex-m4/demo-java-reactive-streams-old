package demo.reactivestreams.part2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class SubmissionIteratorPublisher extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionIteratorPublisher.class);

    private final Iterator<Integer> iterator;

    public SubmissionIteratorPublisher(int count) {
        this.iterator = IntStream.rangeClosed(1, count).iterator();
    }

    public Iterator<Integer> getIterator() {
        return iterator;
    }

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SubmissionIteratorPublisher publisher = new SubmissionIteratorPublisher(10);
        BackpressureSubscriber subscriber = new BackpressureSubscriber(countDownLatch);

        publisher.subscribe(subscriber);

        publisher.getIterator().forEachRemaining(item -> {
            logger.info("publisher.next: {}", item);
            subscriber.onNext(item);
        });

        logger.info("publisher.close");
        publisher.close();

        countDownLatch.await();
    }
}
