package demo.reactivestreams.part1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class PushRunner {

    private static final Logger logger = LoggerFactory.getLogger(PushRunner.class);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch1 = new CountDownLatch(1);
//        CountDownLatch countDownLatch2 = new CountDownLatch(1);

        IteratorPublisher<Integer> publisher = new IteratorPublisher<>(() -> List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).iterator());

        Flow.Subscriber<Integer> subscriber1 = new PushSubscriber<>(countDownLatch1);
        publisher.subscribe(subscriber1);

//        Delay.delay(5);
//
//        Flow.Subscriber<Integer> subscriber2 = new PushSubscriber<>(countDownLatch2);
//        publisher.subscribe(subscriber2);

//        publisher.getIterator().forEachRemaining(item -> {
//            logger.info("publisher.next: {}", item);
//            subscriber.onNext(item);
//        });

        logger.info("publisher.close");
//        publisher.close();

        countDownLatch1.await();
//        countDownLatch2.await();
    }
}
