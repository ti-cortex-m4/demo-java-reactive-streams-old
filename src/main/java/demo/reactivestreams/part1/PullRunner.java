package demo.reactivestreams.part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PullRunner {

    private static final Logger logger = LoggerFactory.getLogger(PullRunner.class);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch completeLatch = new CountDownLatch(1);

        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).iterator());

        Flow.Subscriber<Integer> subscriber = new PullSubscriber<>(completeLatch);
        publisher.subscribe(subscriber);

//        publisher.getIterator().forEachRemaining(item -> {
//            logger.info("publisher.next: {}", item);
//            subscriber.onNext(item);
//        });

//        logger.info("publisher.close");
//        publisher.close();

        completeLatch.await();
    }
}
