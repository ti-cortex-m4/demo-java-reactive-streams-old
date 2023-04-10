package demo.reactivestreams.part3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;

public class SubmissionIteratorPublisher extends SubmissionPublisher<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionIteratorPublisher.class);

    private final int count;

    public SubmissionIteratorPublisher(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        SubmissionIteratorPublisher publisher = new SubmissionIteratorPublisher(10);
        SubmissionProcessor processor = new SubmissionProcessor();
        BackpressureSubscriber subscriber = new BackpressureSubscriber(countDownLatch);

        processor.subscribe(subscriber);
        publisher.subscribe(processor);

        IntStream.range(0, publisher.getCount()).forEach(i -> {
            logger.info("publisher.submit: {}", i);
            publisher.submit(i);
        });

        logger.info("publisher.close");
        publisher.close();

        countDownLatch.await();
    }
}