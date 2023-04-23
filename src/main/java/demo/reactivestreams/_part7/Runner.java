package demo.reactivestreams._part7;

import demo.reactivestreams._part6.SyncSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();
        SubmissionProcessor processor = new SubmissionProcessor();
        BackpressureSubscriber subscriber = new BackpressureSubscriber(countDownLatch);

        processor.subscribe(subscriber);
        publisher.subscribe(processor);

        IntStream.range(0, 10).forEach(i -> {
            logger.info("publisher.submit: {}", i);
            publisher.submit(i);
        });

        logger.info("publisher.close");
        publisher.close();

        countDownLatch.await();
    }
}
