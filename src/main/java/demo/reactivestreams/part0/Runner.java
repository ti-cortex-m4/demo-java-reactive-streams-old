package demo.reactivestreams.part0;

import demo.reactivestreams.part2.BackpressureSubscriber;
import demo.reactivestreams.part2.SubmissionIteratorPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SubmissionIteratorPublisher publisher = new SubmissionIteratorPublisher(10);
        demo.reactivestreams.part2.BackpressureSubscriber subscriber = new BackpressureSubscriber(countDownLatch);

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
